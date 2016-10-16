package ru.ifmo.ctddev.trofiv.concurrent;

class Accumulator<T> {
    private T value;
    private int threadIndex;

    Accumulator(final T value, final int threadIndex) {
        this.value = value;
        this.threadIndex = threadIndex;
    }

    T getValue() {
        return value;
    }

    void setValue(final T value) {
        this.value = value;
    }

    void incrementThreadIndex() {
        this.threadIndex++;
    }

    int getThreadIndex() {
        return threadIndex;
    }
}