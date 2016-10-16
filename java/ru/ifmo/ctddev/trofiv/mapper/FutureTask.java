package ru.ifmo.ctddev.trofiv.mapper;

import java.util.function.Function;

import static ru.ifmo.ctddev.trofiv.mapper.FutureStatus.*;

@SuppressWarnings({"CallToNativeMethodWhileLocked", "SynchronizedMethod"})
class FutureTask<R, T> {
    private final T argument;
    private final Function<T, R> task;
    private volatile R result;
    private volatile Thread runner;
    private volatile FutureStatus status = STATUS_PENDING;

    FutureTask(final Function<T, R> task, final T argument) {
        this.task = task;
        this.argument = argument;
    }

    @SuppressWarnings({"NakedNotify", "SynchronizeOnThis"})
    void execute() {
        try {
            synchronized (this) {
                if (status == STATUS_PENDING) {
                    status = STATUS_RUNNING;
                    runner = Thread.currentThread();
                } else {
                    return;
                }
            }
            final R r = task.apply(argument);
            synchronized (this) {
                if (status == STATUS_RUNNING) {
                    this.result = r;
                    status = STATUS_READY;
                    notifyAll();
                }
            }
        } catch (Exception e) {
            status = STATUS_ABORTED;
            synchronized (this) {
                notifyAll();
            }
            throw new IllegalStateException(e);
        } finally {
            runner = null;
        }
    }

    synchronized void cancel() {
        if (status != STATUS_READY) {
            status = STATUS_ABORTED;
        }
        if (runner != null) {
            runner.interrupt();
        }
    }

    synchronized R getResult() {
        if (status == STATUS_READY) {
            return result;
        }
        throw new IllegalStateException("Result is not ready yet!");
    }

    @SuppressWarnings({"WaitOrAwaitWithoutTimeout", "SynchronizeOnThis"})
    synchronized void waitForDone() throws InterruptedException {
        while (!isReady() && !isAborted()) {
            wait();
        }
    }

    boolean isReady() {
        return status == STATUS_READY;
    }

    private boolean isAborted() {
        return status == STATUS_ABORTED;
    }
}