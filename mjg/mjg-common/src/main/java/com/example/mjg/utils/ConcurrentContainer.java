package com.example.mjg.utils;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

public class ConcurrentContainer<T> {
    private T container;
    private final ReentrantReadWriteLock rwLock;

    public ConcurrentContainer(T defaultValue) {
        this.container = defaultValue;
        this.rwLock = new ReentrantReadWriteLock();
    }

    public void read(Consumer<T> callback) {
        rwLock.readLock().lock();
        try {
            callback.accept(container);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void update(Function<T, T> callback) {
        rwLock.writeLock().lock();
        try {
            this.container = callback.apply(this.container);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
