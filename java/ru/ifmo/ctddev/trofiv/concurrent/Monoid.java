package ru.ifmo.ctddev.trofiv.concurrent;

import java.util.function.BinaryOperator;
import java.util.function.Supplier;

class Monoid<T> {
    private final BinaryOperator<T> operation;
    private final Supplier<T> neutralElementGenerator;

    Monoid(final BinaryOperator<T> operation,
           final Supplier<T> neutralElementGenerator) {
        this.operation = operation;
        this.neutralElementGenerator = neutralElementGenerator;
    }

    T operation(final T a, final T b) {
        return operation.apply(a, b);
    }

    T getNeutral() {
        return neutralElementGenerator.get();
    }
}