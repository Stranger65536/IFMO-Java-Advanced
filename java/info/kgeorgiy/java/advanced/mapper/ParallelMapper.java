package info.kgeorgiy.java.advanced.mapper;

import java.util.List;
import java.util.function.Function;

/**
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
public interface ParallelMapper extends AutoCloseable {

    <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException;

    @Override
    void close() throws InterruptedException;
}
