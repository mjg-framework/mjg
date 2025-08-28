package com.example.mjg.algorithms.retrying;

import com.example.mjg.exceptions.BaseMigrationException;

@FunctionalInterface
public interface RetryableRunnable {
    void run() throws BaseMigrationException;
}
