package ru.ifmo.ctddev.trofiv.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;
import net.java.quickcheck.collection.Pair;
import net.java.quickcheck.collection.Triple;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.text.MessageFormat.format;

/**
 * @author trofiv (vladisl.trofimov@gmail.com)
 */
public class Implementor implements JarImpler {
    /**
     * Precompiled pattern of a single dot
     */
    private static final Pattern DOTS = Pattern.compile("\\.");
    /**
     * Precompiled pattern of modifier names that should be removed from implementation signature
     */
    private static final Pattern REDUNDANT_MODIFIERS = Pattern.compile("abstract|transient");
    /**
     * Constant contains default system java classpath
     */
    @SuppressWarnings("DuplicateStringLiteralInspection")
    private static final String CLASSPATH = System.getProperty("java.class.path");

    /**
     * Closure for generating class-unique variable names
     */
    private static final Supplier<String> VARIABLES_GENERATOR = new Supplier<String>() {
        /**
         * Counter field
         */
        private int count;

        @Override
        public String get() {
            return "var" + count++;
        }
    };

    /**
     * Map of typetokens and their default value string representations
     */
    private static final Map<Class<?>, String> DEFAULT_VALUES = Stream.of(
            new SimpleEntry<>(Void.class, ""),
            new SimpleEntry<>(void.class, ""),
            new SimpleEntry<>(char.class, " (char) 0"),
            new SimpleEntry<>(long.class, " 0L"),
            new SimpleEntry<>(float.class, " 0.0f")
    ).collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    /**
     * Generates source code of class that extends/implements specified
     * typetoken and places generated directory tree in the specified path
     *
     * @param token typetoken of class/interface to be extended/implemented
     * @param path  base directory where implemented directory tree will be generated
     * @throws ImplerException if class can't be generated
     */
    private static void generateClassSourceCode(
            final Class<?> token,
            final Path path) throws ImplerException {
        if (token.isPrimitive()) {
            throw new ImplerException("Can't extend primitive!");
        }
        final String packageName = token.getPackage().getName();
        final Path packagePath = path.resolve(Paths.get(DOTS.matcher(packageName).replaceAll("/")));
        final File packageFile = packagePath.toFile();
        if (!packageFile.exists() && !packageFile.mkdirs()) {
            throw new ImplerException("Can't create directory for generated class!");
        }
        final Path generatedClassPath = packagePath.resolve(token.getSimpleName() + "Impl.java");
        checkFinalClass(token);
        final Collection<String> importsSet = new HashSet<>(1024);
        final String packageString = generatePackage(packageName);
        final String classSignature = generatesClassSignature(token);
        final Pair<String, Set<String>> constructorsWithImports = generateConstructors(token);
        final Collection<Method> methods = getOverriddableMethods(token);
        final Pair<String, Set<String>> methodsStringWithImports = generateMethods(methods);
        importsSet.addAll(methodsStringWithImports.getSecond());
        importsSet.addAll(constructorsWithImports.getSecond());
        final String imports = generateImports(importsSet);
        try (BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(generatedClassPath.toFile()), StandardCharsets.UTF_8))) {
            out.write(packageString);
            out.write(imports);
            out.write(classSignature);
            out.write(constructorsWithImports.getFirst());
            out.write(methodsStringWithImports.getFirst());
            out.write('}');
        } catch (IOException e) {
            throw new ImplerException("Error occurred during class generation!", e);
        }
    }

    /**
     * Performs check if class specified by typetoken if final
     *
     * @param token specified class typetoken
     * @throws ImplerException if class specified by typetoken if final
     */
    private static void checkFinalClass(final Class<?> token) throws ImplerException {
        if (Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("Can't extend final class!");
        }
    }

    /**
     * Generates string that contains package declaration for the target implementation file using package name
     *
     * @param packageName specified package name
     * @return string that contains package declaration for the target implementation file
     */
    private static String generatePackage(final String packageName) {
        return format("package {0};", packageName) + System.lineSeparator() + System.lineSeparator();
    }

    /**
     * Generates string that contains class declaration for the target implementation file using specified typetoken
     *
     * @param token specified typetoken of a class/interface for which implementation class signature will be generated
     * @return string that contains class declaration for the target implementation file
     */
    private static String generatesClassSignature(final Class<?> token) {
        return format("public class {0} {1} {2} '{'",
                token.getSimpleName() + "Impl",
                token.isInterface() ? "implements" : "extends",
                token.getSimpleName()) +
                System.lineSeparator();
    }

    /**
     * @param token specified typetoken of a class for which implementation constructors will be generated
     * @return {@link Pair} of string that contains constructors for the target implementation file and {@link Set} of
     * import strings for the target file
     * @throws ImplerException if class specified by {@code token} doesn't have any public/protected constructors
     */
    private static Pair<String, Set<String>> generateConstructors(final Class<?> token) throws ImplerException {
        final StringBuilder sb = new StringBuilder(1024);
        final Set<String> set = new HashSet<>(1024, 1.0f);
        final Collection<Constructor<?>> constructors = checkConstructors(token);
        for (Constructor<?> constructor : constructors) {
            final Triple<String, Set<String>, List<String>> parametersWithImports = generateParameters(constructor);
            final Pair<String, Set<String>> exceptionsWithImports = getThrownExceptions(constructor);
            sb.append(format("    {0} {1}({2}) {3} '{'",
                    REDUNDANT_MODIFIERS.matcher(Modifier.toString(constructor.getModifiers())).replaceAll(Matcher.quoteReplacement("")),
                    token.getSimpleName() + "Impl",
                    parametersWithImports.getFirst(),
                    exceptionsWithImports.getFirst())).append(System.lineSeparator());
            sb.append("        ").append(getSuperCall(parametersWithImports.getThird())).append(System.lineSeparator());
            sb.append("    }").append(System.lineSeparator());
            set.addAll(parametersWithImports.getSecond());
            set.addAll(exceptionsWithImports.getSecond());
        }
        return new Pair<>(sb.toString(), set);
    }


    /**
     * Traverses over all class hierarchy and returns all methods that must be implemented.
     * Only class hierarchy is scanned (scanning of interface hierarchy requires using one of
     * graph processing algorithms)
     *
     * @param token specified typetoken of a class/interface for which all methods which are necessary to implement will
     *              be gathered
     * @return {@link Set} of methods which are necessary to implement
     */
    @SuppressWarnings("TypeMayBeWeakened")
    private static Set<Method> getOverriddableMethods(final Class<?> token) {
        return token.isInterface()
                ? getInterfaceOverriddableMethods(token)
                : getClassOverriddableMethods(token);
    }


    /**
     * Generates string representation of the specified methods implementation
     *
     * @param methods specified methods
     * @return {@link Pair} of corresponding string representation of the specified methods implementation with default
     * return values and {@link Set} of import strings for the target file
     */
    private static Pair<String, Set<String>> generateMethods(final Iterable<Method> methods) {
        final StringBuilder sb = new StringBuilder(1024);
        final Set<String> set = new HashSet<>(1024, 1.0f);
        for (Method method : methods) {
            final Pair<String, Set<String>> parametersWithImports = generateParameters(method);
            sb.append("    @Override").append(System.lineSeparator());
            sb.append(format("    {0}{1} {2}({3}) {4} '{'",
                    REDUNDANT_MODIFIERS.matcher(Modifier.toString(method.getModifiers())).replaceAll(Matcher.quoteReplacement("")),
                    method.getReturnType().getSimpleName(),
                    method.getName(),
                    parametersWithImports.getFirst(),
                    "")).append(System.lineSeparator());
            sb.append(format("        return{0};", getDefaultValue(method.getReturnType()))).append(System.lineSeparator());
            sb.append("    }").append(System.lineSeparator());
            final Optional<String> importStringOptional = getImportForReferenceType(method.getReturnType());
            if (importStringOptional.isPresent()) {
                set.add(importStringOptional.get());
            }
            set.addAll(parametersWithImports.getSecond());
        }
        return new Pair<>(sb.toString(), set);
    }

    /**
     * Generates string that contains all imports declaration for the target
     * implementation file using specified import entities
     *
     * @param imports specified single import declarations
     * @return string that contains all imports declaration for the target implementation file
     */
    private static String generateImports(final Collection<String> imports) {
        final StringBuilder sb = new StringBuilder(imports.size() * 30);
        imports.forEach(i -> sb.append("import ").append(i).append(';').append(System.lineSeparator()));
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    /**
     * Performs check that class specified by typetoken has at least one public or protected constructor
     *
     * @param token specified class typetoken
     * @return {@link Collection} of public and protected constructors of class specified by token
     * @throws ImplerException if class specified by token has no one public or protected constructor or if interface
     *                         passed b token
     */
    private static Collection<Constructor<?>> checkConstructors(final Class<?> token) throws ImplerException {
        final Collection<Constructor<?>> result = Stream.of(token.getDeclaredConstructors())
                .filter(constructor -> {
                    final int modifiers = constructor.getModifiers();
                    return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
                }).collect(Collectors.toList());
        if (result.isEmpty() && !token.isInterface()) {
            throw new ImplerException("Can't extend class without public / protected constructor provided!");
        } else {
            return result;
        }
    }

    /**
     * Generates string representation of the specified constructor parameters with variable names
     *
     * @param constructor specified constructor
     * @return {@link Triple} of corresponding string representation of constructor parameters, {@link Set} of import
     * strings for the target file and {@link List of generated variable names}
     */
    private static Triple<String, Set<String>, List<String>> generateParameters(final Constructor<?> constructor) {
        final StringBuilder sb = new StringBuilder(1024);
        final Set<String> imports = new HashSet<>(constructor.getParameterCount(), 1.0f);
        final List<String> parameters = new ArrayList<>(constructor.getParameterCount());
        for (Class<?> parameter : constructor.getParameterTypes()) {
            final String parameterName = VARIABLES_GENERATOR.get();
            sb.append(parameter.getSimpleName()).append(' ').append(parameterName).append(',');
            parameters.add(parameterName);
            final Optional<String> importStringOptional = getImportForReferenceType(parameter);
            if (importStringOptional.isPresent()) {
                imports.add(importStringOptional.get());
            }
        }
        return constructor.getParameterCount() == 0
                ? new Triple<>("", Collections.emptySet(), Collections.emptyList())
                : new Triple<>(sb.substring(0, sb.length() - 1), imports, parameters);
    }

    /**
     * Generates string representation of checked exceptions thrown by the specified constructor
     *
     * @param constructor specified constructor
     * @return pair of corresponding string representation of checked exceptions thrown by the specified constructor and
     * {@link Set} of import strings for the target file
     */
    private static Pair<String, Set<String>> getThrownExceptions(final Constructor<?> constructor) {
        final StringBuilder sb = new StringBuilder(1024);
        final Set<String> imports = new HashSet<>(constructor.getExceptionTypes().length, 1.0f);
        for (Class<?> exception : constructor.getExceptionTypes()) {
            sb.append(exception.getSimpleName()).append(',');
            final Optional<String> importStringOptional = getImportForReferenceType(exception);
            if (importStringOptional.isPresent()) {
                imports.add(importStringOptional.get());
            }
        }
        return constructor.getExceptionTypes().length == 0
                ? new Pair<>("", Collections.emptySet())
                : new Pair<>("throws " + sb.substring(0, sb.length() - 1), imports);
    }


    /**
     * Generates string that contains super class call for the target implementation file using specified parameters
     *
     * @param parameters specified super constructor parameters
     * @return string that contains super class call for the target implementation file
     */
    private static String getSuperCall(final Collection<String> parameters) {
        final Optional<String> optionalResult = parameters.stream()
                .map(parameter -> parameter + ',')
                .reduce((s, s2) -> s + s2);
        if (optionalResult.isPresent()) {
            final String result = optionalResult.get();
            return format("super({0});", result.substring(0, result.length() - 1));
        } else {
            return "";
        }
    }

    /**
     * Returns all methods that must be implemented include a first-level inherited abstract methods.
     *
     * @param token specified typetoken of an interface for which all methods which are necessary to implement will be
     *              gathered
     * @return set of methods which are necessary to implement
     */
    private static Set<Method> getInterfaceOverriddableMethods(final Class<?> token) {
        return Stream.of(token.getDeclaredMethods(), token.getMethods())
                .flatMap(Arrays::stream)
                .filter(m -> Modifier.isAbstract(m.getModifiers()))
                .collect(Collectors.toSet());
    }

    /**
     * Traverses over all class hierarchy and returns all methods that must be implemented.
     *
     * @param token specified typetoken of a class for which all methods which are necessary to implement will be
     *              gathered
     * @return set of methods which are necessary to implement
     */
    @SuppressWarnings("MethodWithMoreThanThreeNegations")
    private static Set<Method> getClassOverriddableMethods(final Class<?> token) {
        Class<?> currentToken = token;
        final Collection<ReducedEqualityMethod> abstractMethods = new HashSet<>(1024, 1.0f);
        final Collection<ReducedEqualityMethod> nonAbstractOverriddableMethods = new HashSet<>(1024, 1.0f);
        while (currentToken.getSuperclass() != null) {
            abstractMethods.addAll(Stream.of(currentToken.getDeclaredMethods())
                    .filter(m -> Modifier.isAbstract(m.getModifiers()))
                    .map(ReducedEqualityMethod::new)
                    .collect(Collectors.toList()));
            nonAbstractOverriddableMethods.addAll(Stream.of(currentToken.getDeclaredMethods())
                    .filter(m -> {
                        final int modifiers = m.getModifiers();
                        return !Modifier.isAbstract(modifiers)
                                && !Modifier.isStatic(modifiers)
                                && !Modifier.isPrivate(modifiers);
                    })
                    .map(ReducedEqualityMethod::new)
                    .collect(Collectors.toSet()));
            final Iterator<ReducedEqualityMethod> it = abstractMethods.iterator();
            while (it.hasNext()) {
                final ReducedEqualityMethod currentAbstract = it.next();
                for (ReducedEqualityMethod method : nonAbstractOverriddableMethods) {
                    if (isOverridden(method, currentAbstract)) {
                        nonAbstractOverriddableMethods.remove(method);
                        it.remove();
                        break;
                    }
                }
            }
            currentToken = currentToken.getSuperclass();
        }

        return abstractMethods.stream()
                .map(ReducedEqualityMethod::getMethod)
                .collect(Collectors.toSet());
    }

    /**
     * Generates string representation of parameters with variable names of the specified method
     *
     * @param method specified method
     * @return pair of corresponding string representation of the specified method parameters and {@link Set} of import
     * strings for the target file
     */
    private static Pair<String, Set<String>> generateParameters(final Method method) {
        final StringBuilder sb = new StringBuilder(1024);
        final Set<String> imports = new HashSet<>(method.getParameterCount(), 1.0f);
        for (Class<?> parameter : method.getParameterTypes()) {
            sb.append(parameter.getSimpleName()).append(' ').append(VARIABLES_GENERATOR.get()).append(',');
            final Optional<String> importStringOptional = getImportForReferenceType(parameter);
            if (importStringOptional.isPresent()) {
                imports.add(importStringOptional.get());
            }
        }
        return method.getParameterCount() == 0
                ? new Pair<>("", Collections.emptySet())
                : new Pair<>(sb.substring(0, sb.length() - 1), imports);
    }

    /**
     * Returns default value for a type specified by the token
     *
     * @param token specified typetoken
     * @return String that contains default value for the specified type
     */
    private static String getDefaultValue(final Class<?> token) {
        return DEFAULT_VALUES.containsKey(token)
                ? DEFAULT_VALUES.get(token)
                : ' ' + Objects.toString(Array.get(Array.newInstance(token, 1), 0));
    }

    /**
     * Returns optional String representation of entity that must be imported in class which uses that token
     *
     * @param token specified typetoken
     * @return string with canonical name of entity to import or empty optional if specified typetoken represent
     * primitive type
     */
    private static Optional<String> getImportForReferenceType(final Class<?> token) {
        Class<?> current = token;
        while (current.isArray()) {
            current = current.getComponentType();
        }
        return current.isPrimitive()
                ? Optional.empty()
                : Optional.of(current.getCanonicalName());
    }

    /**
     * Performs check that method represented by reducedOverrider overrides method represented by reducedOverriddable
     *
     * @param reducedOverrider    {@link Method} represented by {@link ReducedEqualityMethod} that potentially overrides
     *                            method represented by reducedOverriddable
     * @param reducedOverriddable {@link Method} represented by {@link ReducedEqualityMethod} that potentially
     *                            overridden by method represented by reducedOverrider
     * @return true if method represented by reducedOverrider overrides method represented by reducedOverriddable
     */
    @SuppressWarnings("OverlyComplexBooleanExpression")
    private static boolean isOverridden(
            final ReducedEqualityMethod reducedOverrider,
            final ReducedEqualityMethod reducedOverriddable) {
        final Method overrider = reducedOverrider.getMethod();
        final Method overriddable = reducedOverriddable.getMethod();
        return overrider.getName().equals(overriddable.getName())
                && overriddable.getParameterCount() == overrider.getParameterCount()
                && Arrays.equals(overriddable.getParameterTypes(), overrider.getParameterTypes());
    }

    /**
     * Main method. Validates input parameters and delegates them to a correspond implementing methods
     *
     * @param args specified program arguments
     */
    public static void main(final String[] args) {
        try {
            if (args.length == 1) {
                new Implementor().implement(Class.forName(args[0]), Paths.get("."));
            } else if (args.length == 3 && "-jar".equals(args[0])) {
                new Implementor().implementJar(Class.forName(args[1]), Paths.get(args[2]));
            } else {
                printHelp();
            }
        } catch (ClassNotFoundException ignored) {
            System.out.println("Class not found in classpath");
        } catch (ImplerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints usage information to the standard output
     */
    private static void printHelp() {
        System.out.println("Usage: java Implementor full_class_name" + System.lineSeparator() +
                "       java Implementor -jar full_class_name jar_file");
    }

    /**
     * Implements class/interface specified by token, compiles the generated implementation
     * and packs it to a jar archive located at path specified
     *
     * @param token   specified class/interface token
     * @param jarFile path to the target jar file including file name
     * @throws ImplerException if class/interface can't be extended/implemented or generated source file can't be
     *                         compile or corresponding jar file couldn't be created
     */
    @SuppressWarnings("MethodWithMoreThanThreeNegations")
    private static void createJar(final Class<?> token, final Path jarFile) throws ImplerException {
        final String sourceLocation = token.getCanonicalName().replace('.', '/') + "Impl.java";
        final String classLocation = token.getCanonicalName().replace('.', '/') + "Impl.class";
        final Path parent = jarFile.getParent() == null ? Paths.get(".") : jarFile.getParent();
        final Path classPath = parent.resolve(classLocation);
        compile(parent, sourceLocation);

        try (FileInputStream in = new FileInputStream(classPath.toFile());
             JarOutputStream out = new JarOutputStream(
                     new BufferedOutputStream(
                             new FileOutputStream(jarFile.toFile())))) {
            final JarEntry zipEntry = new JarEntry(classLocation);
            out.putNextEntry(zipEntry);
            int nread;
            final byte[] buffer = new byte[1024];
            while ((nread = in.read(buffer)) != -1) {
                out.write(buffer, 0, nread);
            }
            out.closeEntry();
        } catch (IOException e) {
            throw new ImplerException("Error during jar generation!", e);
        }
    }

    /**
     * Compiles specified source file at specified path
     *
     * @param root base directory for source file and compiled class
     * @param file string representation of compiling source file relative to the specified root path
     * @throws ImplerException in case of compilation error or if system java compiler couldn't be found
     */
    private static void compile(final Path root, final String file) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Could not find java compiler!");
        }
        final List<String> args = new ArrayList<>(7);
        args.add(root.resolve(file).toString());
        args.add("-sourcepath");
        args.add(root.toString());
        args.add("-cp");
        args.add(root + File.pathSeparator + CLASSPATH);
        args.add("-d");
        args.add(root.toString());
        final int exitCode = compiler.run(null, System.out, System.err, args.toArray(new String[args.size()]));
        if (exitCode != 0) {
            throw new ImplerException(format("Can't compile file {0}!", root.resolve(file)));
        }
    }

    @Override
    public void implement(final Class<?> token, final Path root) throws ImplerException {
        generateClassSourceCode(token, root);
    }


    @Override
    public void implementJar(final Class<?> token, final Path jarFile) throws ImplerException {
        implement(token, jarFile.getParent() == null ? Paths.get(".") : jarFile.getParent());
        createJar(token, jarFile);
    }

    /**
     * Wrapper class for the {@link Method} class with reduced logic in overridden equals and
     * hashCode methods to meet equality by method's name and arguments
     */
    private static class ReducedEqualityMethod {
        /**
         * Underlying method which equality reduced
         */
        private final Method method;

        /**
         * Constructs instance using specified method
         *
         * @param method specified method to wrap
         */
        ReducedEqualityMethod(final Method method) {
            this.method = method;
        }

        /**
         * Simple getter for underlying method
         *
         * @return underlying method
         */
        @SuppressWarnings("WeakerAccess")
        public Method getMethod() {
            return method;
        }

        @Override
        public int hashCode() {
            return 37 * Arrays.hashCode(method.getParameterTypes()) + method.getName().hashCode();
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ReducedEqualityMethod)) {
                return false;
            }

            final ReducedEqualityMethod second = (ReducedEqualityMethod) o;

            return method.getName().equals(second.method.getName())
                    && second.method.getParameterCount() == method.getParameterCount()
                    && Arrays.equals(second.method.getParameterTypes(), method.getParameterTypes());
        }
    }
}