package info.kgeorgiy.java.advanced.mapper;

import info.kgeorgiy.java.advanced.concurrent.ListIPTest;
import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import org.junit.AfterClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.Arrays;

/**
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ListMapperTest extends ListIPTest {
    public ListMapperTest() {
        factors = Arrays.asList(1, 2, 5, 10);
    }

    @Test
    public void test05_sleepPerformance() throws InterruptedException {
        new ScalarMapperTest().test05_sleepPerformance();
    }

    @Override
    protected ScalarIP createInstance(final int threads) {
        return ScalarMapperTest.instance(threads);
    }

    @AfterClass
    public static void close() {
        ScalarMapperTest.close();
    }
}
