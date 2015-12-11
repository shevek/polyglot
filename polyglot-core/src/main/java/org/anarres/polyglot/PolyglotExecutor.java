/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

/**
 *
 * @author shevek
 */
public abstract class PolyglotExecutor {

    public static class Serial extends PolyglotExecutor {

        @Override
        public int getParallelism() {
            return 1;
        }

        @Override
        public <V> Future<V> execute(Callable<V> r) throws ExecutionException {
            try {
                V value = r.call();
                return Futures.immediateCheckedFuture(value);
            } catch (Exception e) {
                throw new ExecutionException("Failed to execute " + r + ": " + e, e);
            }
        }

        @Override
        public void await() throws InterruptedException, ExecutionException {
        }

        @Override
        public void shutdown() throws InterruptedException {
        }
    }

    public static class Parallel extends PolyglotExecutor {

        private final int parallelism;
        private final ExecutorService executor;
        private final BlockingQueue<Future<?>> futures = new LinkedBlockingQueue<>();

        public Parallel(@Nonnull String name, int parallelism) {
            this.parallelism = parallelism;
            this.executor = Executors.newFixedThreadPool(parallelism, new ThreadFactoryBuilder().setNameFormat("polyglot-%d (" + name + ")").build());
        }

        @Override
        public int getParallelism() {
            return parallelism;
        }

        @Override
        public <V> Future<V> execute(@Nonnull Callable<V> r) throws ExecutionException {
            Future<V> out = executor.submit(r);
            futures.add(out);
            return out;
        }

        @Override
        public void await() throws InterruptedException, ExecutionException {
            for (;;) {
                Future<?> f = futures.poll();
                if (f == null)
                    break;
                f.get();
            }
        }

        @Override
        public void shutdown() throws InterruptedException {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    @Nonnegative
    public abstract int getParallelism();

    @Nonnull
    public abstract <V> Future<V> execute(@Nonnull Callable<V> r) throws ExecutionException;

    public abstract void await() throws InterruptedException, ExecutionException;

    public void parallel(@Nonnull Callable<?> r) throws InterruptedException, ExecutionException {
        for (int i = 0; i < getParallelism(); i++)
            execute(r);
        await();
    }

    public abstract void shutdown() throws InterruptedException;
}
