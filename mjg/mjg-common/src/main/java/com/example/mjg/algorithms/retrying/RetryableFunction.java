package com.example.mjg.algorithms.retrying;

@FunctionalInterface
public interface RetryableFunction<T, R> {
    R apply(T t) throws Exception;
}
