package info.kgeorgiy.java.advanced.implementor;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.omg.CORBA_2_3.ORB;

import javax.annotation.processing.Completions;
import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.plugins.bmp.BMPImageWriteParam;
import javax.imageio.stream.FileCacheImageInputStream;
import javax.management.ImmutableDescriptor;
import javax.management.relation.RelationNotFoundException;
import javax.management.remote.rmi.RMIIIOPServerImpl;
import javax.management.remote.rmi.RMIServerImpl;
import javax.naming.ldap.LdapReferralException;
import java.io.IOException;

/**
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClassImplementorTest extends InterfaceImplementorTest {
    @Test
    public void test07_defaultConstructorClasses() throws IOException {
        test(false, BMPImageWriteParam.class, RelationNotFoundException.class);
    }

    @Test
    public void test08_noDefaultConstructorClasses() throws IOException {
        test(false, IIOException.class, ImmutableDescriptor.class, LdapReferralException.class);
    }

    @Test
    public void test09_ambiguousConstructorClasses() throws IOException {
        test(false, IIOImage.class);
    }

    @Test
    public void test10_utilityClasses() throws IOException {
        test(true, Completions.class);
    }

    @Test
    public void test11_finalClasses() throws IOException {
        test(true, Integer.class, String.class);
    }

    @Test
    public void test12_standardNonClasses() throws IOException {
        test(true, void.class, String[].class, int[].class, String.class, boolean.class);
    }

    @Test
    public void test13_constructorThrows() throws IOException {
        test(false, FileCacheImageInputStream.class);
    }

    @Test
    public void test14_nonPublicAbstractMethod() throws IOException {
        test(false, RMIServerImpl.class, RMIIIOPServerImpl.class);
    }

    @Test
    public void test15_inheritedNonPublicAbstractMethod() throws IOException {
        test(false, ORB.class);
    }
}
