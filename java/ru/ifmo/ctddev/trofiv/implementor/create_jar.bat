javac -d . -cp ../../../../../../lib/quickcheck-0.6.jar Implementor.java ../../../../../../java/info/kgeorgiy/java/advanced/implementor/Impler.java ../../../../../../java/info/kgeorgiy/java/advanced/implementor/JarImpler.java ../../../../../../java/info/kgeorgiy/java/advanced/implementor/ImplerException.java
jar cfm ../../../../../../lib/Implementor.jar MANIFEST.MF info/kgeorgiy/java/advanced/implementor/*.class ru/ifmo/ctddev/trofiv/implementor/*.class
rmdir /S /Q info ru