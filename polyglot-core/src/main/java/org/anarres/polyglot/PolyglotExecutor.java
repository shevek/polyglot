/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
        public void execute(Callable<?> r) throws ExecutionException {
            try {
                r.call();
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
        private final Queue<Future<?>> futures = new ArrayDeque<>();

        public Parallel(@Nonnull String name, @Nonnegative int parallelism) {
            this.parallelism = parallelism;
            this.executor = Executors.newFixedThreadPool(parallelism, new ThreadFactoryBuilder().setNameFormat("polyglot-%d (" + name + ")").build());
        }

        @Override
        public int getParallelism() {
            return parallelism;
        }

        @Override
        public void execute(@Nonnull Callable<?> r) throws ExecutionException {
            Future<?> out = executor.submit(r);
            futures.add(out);
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
    public abstract void execute(@Nonnull Callable<?> r) throws ExecutionException;

    public abstract void await() throws InterruptedException, ExecutionException;

    public void parallel(@Nonnull Callable<?> r) throws InterruptedException, ExecutionException {
        for (int i = 0; i < getParallelism(); i++)
            execute(r);
        await();
    }

    public abstract void shutdown() throws InterruptedException;
}
