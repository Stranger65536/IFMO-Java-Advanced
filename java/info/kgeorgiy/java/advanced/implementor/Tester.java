package info.kgeorgiy.java.advanced.implementor;

import info.kgeorgiy.java.advanced.base.BaseTester;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
public class Tester extends BaseTester {
    public static void main(final String[] args) throws NoSuchAlgorithmException, IOException {
        new Tester()
                .add("interface", InterfaceImplementorTest.class)
                .add("class", ClassImplementorTest.class)
                .add("jar-interface", InterfaceJarImplementorTest.class)
                .add("jar-class", ClassJarImplementorTest.class)
                .run(args);
    }
}
