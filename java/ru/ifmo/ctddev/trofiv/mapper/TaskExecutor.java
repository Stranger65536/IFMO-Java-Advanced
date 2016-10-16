package ru.ifmo.ctddev.trofiv.mapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;

class TaskExecutor {
    private final Queue<FutureTask> tasks = new ArrayDeque<>(1024);
    private final List<Thread> threads;

    TaskExecutor(final int threads) {
        this.threads = new ArrayList<>(threads);
        for (int i = 0; i < threads; i++) {
            final Thread thread = new Thread(new WorkerRunnable(tasks), "WorkerThread" + i);
            this.threads.add(thread);
        }
    }

    void start() {
        threads.forEach(Thread::start);
    }

    @SuppressWarnings("NotifyWithoutCorrespondingWait")
    <R, T> FutureTask<R, T> submit(final Function<T, R> task, final T argument) {
        final FutureTask<R, T> futureTask = new FutureTask<>(task, argument);
        synchronized (tasks) {
            tasks.add(futureTask);
            tasks.notifyAll();
        }
        return futureTask;
    }

    void shutdown() throws InterruptedException {
        synchronized (tasks) {
            tasks.forEach(FutureTask::cancel);
            tasks.clear();
        }

        threads.forEach(Thread::interrupt);

        for (Thread thread : threads) {
            thread.join();
        }
    }
}