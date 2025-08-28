package com.example.mjg.algorithms.retrying;

@FunctionalInterface
public interface RetryableRunnable {
    void run() throws Exception;
}
