package ru.ifmo.ctddev.trofiv.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {
    private final TaskExecutor taskExecutor;

    public ParallelMapperImpl(final int threads) {
        taskExecutor = new TaskExecutor(threads);
        taskExecutor.start();
    }

    @Override
    public <T, R> List<R> map(
            final Function<? super T, ? extends R> function,
            final List<? extends T> args)
            throws InterruptedException {
        final Collection<FutureTask<R, T>> futureTasks = new ArrayList<>();

        for (T arg : args) {
            futureTasks.add(taskExecutor.submit(function::apply, arg));
        }

        final List<R> result = new ArrayList<>();

        for (FutureTask<R, T> futureTask : futureTasks) {
            futureTask.waitForDone();
            if (futureTask.isReady()) {
                result.add(futureTask.getResult());
            }
        }

        return result;
    }

    @Override
    public void close() throws InterruptedException {
        taskExecutor.shutdown();
    }
}
