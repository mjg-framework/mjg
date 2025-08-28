package com.example.mjg.algorithms.retrying;

import com.example.mjg.exceptions.BaseMigrationException;

@FunctionalInterface
public interface RetryableFunction<T, R> {
    R apply(T t) throws BaseMigrationException;
}
