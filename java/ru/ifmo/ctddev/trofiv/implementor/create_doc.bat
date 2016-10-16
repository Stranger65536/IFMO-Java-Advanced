rmdir /S /Q doc
mkdir doc
javadoc -cp ../../../../../../lib/quickcheck-0.6.jar -private -author Implementor.java ../../../../../../java/info/kgeorgiy/java/advanced/implementor/Impler.java ../../../../../../java/info/kgeorgiy/java/advanced/implementor/JarImpler.java ../../../../../../java/info/kgeorgiy/java/advanced/implementor/ImplerException.java -d doc