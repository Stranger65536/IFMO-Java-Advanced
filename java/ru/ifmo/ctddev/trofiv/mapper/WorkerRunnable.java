package ru.ifmo.ctddev.trofiv.mapper;

import java.util.Queue;

@SuppressWarnings({"WaitOrAwaitWithoutTimeout", "WaitWithoutCorrespondingNotify", "NakedNotify"})
class WorkerRunnable implements Runnable {
    private final Queue<FutureTask> tasks;

    WorkerRunnable(final Queue<FutureTask> tasks) {
        this.tasks = tasks;
    }

    @Override
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            final FutureTask futureTask;
            synchronized (tasks) {
                while (tasks.isEmpty()) {
                    try {
                        tasks.wait();
                    } catch (InterruptedException ignored) {
                        return;
                    }
                }
                futureTask = tasks.remove();
            }
            futureTask.execute();
            synchronized (futureTask) {
                futureTask.notifyAll();
            }
        }
    }
}
