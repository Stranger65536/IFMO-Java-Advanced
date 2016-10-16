package info.kgeorgiy.java.advanced.mapper;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import info.kgeorgiy.java.advanced.concurrent.ScalarIPTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.Arrays;
import java.util.List;

/**
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ScalarMapperTest extends ScalarIPTest<ScalarIP> {
    private static ParallelMapper parallelMapper;
    public ScalarMapperTest() {
        factors = Arrays.asList(1, 2, 5, 10);
    }

    static Object create(final String className, final Class<?> argType, final Object value) {
        try {
            final Class<?> clazz = Class.forName(className);
            return clazz.getConstructor(argType).newInstance(value);
        } catch (final Exception e) {
            throw new AssertionError(e);
        }
    }

    @Override
    protected ScalarIP createInstance(final int threads) {
        return instance(threads);
    }

    static ScalarIP instance(final int threads) {
        close();
        final String[] names = System.getProperty("cut").split(",");
        parallelMapper = (ParallelMapper) create(names[0], int.class, threads);
        return (ScalarIP) create(names[1], ParallelMapper.class, parallelMapper);
    }

    @AfterClass
    public static void close() {
        try {
            if (parallelMapper != null) {
                parallelMapper.close();
            }
        } catch (final InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void test05_sleepPerformance() throws InterruptedException {
        final List<Integer> data = randomList(200);
        final int procs = Runtime.getRuntime().availableProcessors();
        final double speedupSeq = speedup(data, SLEEP_COMPARATOR, procs * 2, 1);
        Assert.assertTrue("Too parallel", speedupSeq < 1.2);
        final double speedupPar = speedup(data, SLEEP_COMPARATOR, procs * 2, procs * 2);
        Assert.assertTrue("Not parallel", speedupPar > procs / 1.5);
    }
}
