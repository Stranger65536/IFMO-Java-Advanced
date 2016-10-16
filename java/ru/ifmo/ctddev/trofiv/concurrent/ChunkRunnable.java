package ru.ifmo.ctddev.trofiv.concurrent;

import java.util.List;
import java.util.function.Function;

class ChunkRunnable<T, E> implements Runnable {
    private final Object lock;
    private final int currentIndex;
    private final Monoid<E> monoid;
    private final Accumulator<E> result;
    private final List<? extends T> subList;
    private final Function<? super T, ? extends E> caster;

    ChunkRunnable(
            final Object lock,
            final int currentIndex,
            final Monoid<E> monoid,
            final Accumulator<E> result,
            final List<? extends T> subList,
            final Function<? super T, ? extends E> caster) {
        this.lock = lock;
        this.currentIndex = currentIndex;
        this.monoid = monoid;
        this.result = result;
        this.subList = subList;
        this.caster = caster;
    }

    @Override
    @SuppressWarnings("WaitOrAwaitWithoutTimeout")
    public void run() {
        E accumulator = monoid.getNeutral();
        for (T element : subList) {
            accumulator = monoid.operation(accumulator, caster.apply(element));
        }

        synchronized (lock) {
            while (result.getThreadIndex() != currentIndex) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            result.setValue(monoid.operation(result.getValue(), accumulator));
            result.incrementThreadIndex();
            lock.notifyAll();
        }
    }
}
