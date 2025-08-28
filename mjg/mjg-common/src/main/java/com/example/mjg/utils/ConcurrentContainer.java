package com.example.mjg.utils;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

public class ConcurrentContainer<T> {
    private T value;
    private final ReentrantReadWriteLock rwLock;

    public ConcurrentContainer(T defaultValue) {
        this.value = defaultValue;
        this.rwLock = new ReentrantReadWriteLock();
    }

    public void read(Consumer<T> callback) {
        rwLock.readLock().lock();
        try {
            callback.accept(value);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void update(Function<T, T> callback) {
        rwLock.writeLock().lock();
        try {
            this.value = callback.apply(this.value);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
