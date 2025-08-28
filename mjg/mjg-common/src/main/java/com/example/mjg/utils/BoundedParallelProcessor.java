package com.example.mjg.utils;

import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/*
 * By ChatGPT.
 * Usage:

* public class Demo {
    public static void main(String[] args) throws Exception {
        try (BoundedParallelProcessor<String> processor =
                 new BoundedParallelProcessor<>(4, 100, item -> {
                     System.out.println(Thread.currentThread().getName() + " processing " + item);
                     if ("c".equals(item)) {
                         System.out.println("!! Stop triggered");
                         throw new RuntimeException("fatal"); // auto stop
                     }
                 })) {

            processor.submitAll(java.util.List.of("a", "b", "c", "d", "e", "f"));

            // wait until workers finish (either by stop or after draining queue)
            processor.awaitTermination();
        }
    }
}
 */
public class BoundedParallelProcessor<T> implements AutoCloseable {

    private final BlockingQueue<T> queue;
    private final ExecutorService workers;
    private final AtomicBoolean stop = new AtomicBoolean(false);

    public BoundedParallelProcessor(int threads, int capacity, Consumer<T> workerLogic) {
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.workers = Executors.newFixedThreadPool(threads);

        // spawn worker threads
        for (int i = 0; i < threads; i++) {
            workers.submit(() -> {
                try {
                    while (!stop.get()) {
                        T item = queue.poll(100, TimeUnit.MILLISECONDS);
                        if (item == null) continue; // no item, check stop again
                        try {
                            workerLogic.accept(item);
                        } catch (Exception e) {
                            // trigger stop on worker failure
                            stop.set(true);
                            throw e;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }

    /** Submit a single item */
    public void submit(T item) throws InterruptedException {
        if (!stop.get()) {
            queue.put(item); // blocks if full
        }
    }

    /** Submit a batch */
    public void submitAll(Collection<T> items) throws InterruptedException {
        for (T item : items) {
            if (stop.get()) break;
            queue.put(item);
        }
    }

    /** Trigger a cooperative stop */
    public void stop() {
        stop.set(true);
    }

    /** Wait for workers to finish (after stop or after queue drains) */
    public boolean awaitTermination() throws InterruptedException {
        workers.shutdown();
        return workers.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    /** Force shutdown immediately */
    public void shutdownNow() {
        stop.set(true);
        workers.shutdownNow();
    }

    @Override
    public void close() {
        shutdownNow();
    }
}
