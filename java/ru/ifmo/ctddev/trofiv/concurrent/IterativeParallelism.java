package ru.ifmo.ctddev.trofiv.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.function.UnaryOperator.identity;

public class IterativeParallelism implements ListIP {
    private final Object lock = new Object();
    private final ParallelMapper mapper;

    public IterativeParallelism() {
        this.mapper = null;
    }

    public IterativeParallelism(final ParallelMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public <E> E maximum(
            final int threads,
            final List<? extends E> values,
            final Comparator<? super E> comparator)
            throws InterruptedException {
        final Monoid<E> Monoid = new Monoid<>((a, b) -> comparator.compare(a, b) >= 0 ? a : b, () -> values.get(0));
        return parallelizeList(threads, Monoid, identity(), values);
    }

    @Override
    public <E> E minimum(
            final int threads,
            final List<? extends E> values,
            final Comparator<? super E> comparator)
            throws InterruptedException {
        final Monoid<E> Monoid = new Monoid<>((a, b) -> comparator.compare(a, b) <= 0 ? a : b, () -> values.get(0));
        return parallelizeList(threads, Monoid, identity(), values);
    }

    @Override
    public <E> boolean all(
            final int threads,
            final List<? extends E> values,
            final Predicate<? super E> predicate)
            throws InterruptedException {
        final Monoid<Boolean> Monoid = new Monoid<>(Boolean::logicalAnd, () -> Boolean.TRUE);
        return parallelizeList(threads, Monoid, predicate::test, values);
    }

    @Override
    public <E> boolean any(
            final int threads,
            final List<? extends E> values,
            final Predicate<? super E> predicate)
            throws InterruptedException {
        final Monoid<Boolean> Monoid = new Monoid<>(Boolean::logicalOr, () -> Boolean.FALSE);
        return parallelizeList(threads, Monoid, predicate::test, values);
    }

    private <T, E> E parallelizeList(
            final int threads,
            final Monoid<E> monoid,
            final Function<? super T, ? extends E> caster,
            final List<? extends T> values)
            throws InterruptedException {
        final int actualThreads = threads > values.size() ? values.size() : threads;
        final int chunkSize = values.size() / actualThreads;

        return mapper == null
                ? parallelizeManually(monoid, caster, values, chunkSize)
                : parallelizeViaMapper(monoid, caster, values, chunkSize, mapper);
    }

    private <T, E> E parallelizeManually(
            final Monoid<E> monoid,
            final Function<? super T, ? extends E> caster,
            final List<? extends T> values,
            final int chunkSize)
            throws InterruptedException {
        final Accumulator<E> result = new Accumulator<>(monoid.getNeutral(), 0);
        final Collection<Thread> threadList = new ArrayList<>(values.size());

        for (int left = 0, index = 0; left < values.size(); left += chunkSize, index++) {
            final int right = Math.min(left + chunkSize, values.size());
            final List<? extends T> subList = values.subList(left, right);
            final ChunkRunnable<T, E> chunkRunnable = new ChunkRunnable<>(lock, index, monoid, result, subList, caster);
            final Thread thread = new Thread(chunkRunnable, "ThreadChunk" + index);
            thread.start();
            threadList.add(thread);
        }

        for (Thread thread : threadList) {
            thread.join();
        }

        return result.getValue();
    }

    private static <T, E> E parallelizeViaMapper(
            final Monoid<E> monoid,
            final Function<? super T, ? extends E> caster,
            final List<? extends T> values,
            final int chunkSize,
            final ParallelMapper mapper)
            throws InterruptedException {
        final List<List<? extends T>> tasks = new ArrayList<>(values.size());

        for (int left = 0; left < values.size(); left += chunkSize) {
            final int right = Math.min(left + chunkSize, values.size());
            tasks.add(values.subList(left, right));
        }

        final List<E> result = mapper.map(t -> {
            E accumulator = monoid.getNeutral();
            for (T element : t) {
                accumulator = monoid.operation(accumulator, caster.apply(element));
            }
            return accumulator;
        }, tasks);

        E answer = result.get(0);

        for (int i = 1; i < result.size(); i++) {
            answer = monoid.operation(answer, result.get(i));
        }

        return answer;
    }

    @Override
    public String join(
            final int threads,
            final List<?> values)
            throws InterruptedException {
        final Monoid<StringBuilder> Monoid = new Monoid<>(StringBuilder::append, StringBuilder::new);
        return this.parallelizeList(threads, Monoid, o -> new StringBuilder(o.toString()), values).toString();
    }

    @Override
    public <T> List<T> filter(
            final int threads,
            final List<? extends T> values,
            final Predicate<? super T> predicate)
            throws InterruptedException {
        final Monoid<List<T>> Monoid = new Monoid<>((a, b) -> {
            a.addAll(b);
            return a;
        }, ArrayList::new);
        return parallelizeList(threads, Monoid, a -> predicate.test(a) ? Collections.singletonList(a) : new ArrayList<>(), values);
    }

    @Override
    public <T, U> List<U> map(
            final int threads,
            final List<? extends T> values,
            final Function<? super T, ? extends U> function)
            throws InterruptedException {
        final Monoid<List<U>> Monoid = new Monoid<>((a, b) -> {
            a.addAll(b);
            return a;
        }, ArrayList::new);
        return parallelizeList(threads, Monoid, a -> Collections.singletonList(function.apply(a)), values);
    }
}