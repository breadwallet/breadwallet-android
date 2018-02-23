package com.breadwallet.tools.threads.executor;


import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 10/25/17.
 * Copyright (c) 2017 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public class PriorityThreadPoolExecutor extends ThreadPoolExecutor {

    public PriorityThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                                      TimeUnit unit, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new PriorityBlockingQueue<Runnable>(), threadFactory, handler);
    }

    @Override
    public Future<?> submit(Runnable task) {
        PriorityFutureTask futureTask = new PriorityFutureTask((PriorityRunnable) task);
        execute(futureTask);
        return futureTask;
    }

    private static final class PriorityFutureTask extends FutureTask<PriorityRunnable>
            implements Comparable<PriorityFutureTask> {
        private final PriorityRunnable priorityRunnable;

        public PriorityFutureTask(PriorityRunnable priorityRunnable) {
            super(priorityRunnable, null);
            this.priorityRunnable = priorityRunnable;
        }

        /*
         * compareTo() method is defined in interface java.lang.Comparable and it is used
         * to implement natural sorting on java classes. natural sorting means the the sort
         * order which naturally applies on object e.g. lexical order for String, numeric
         * order for Integer or Sorting employee by there ID etc. most of the java core
         * classes including String and Integer implements CompareTo() method and provide
         * natural sorting.
         */
        @Override
        public int compareTo(PriorityFutureTask other) {
            Priority p1 = priorityRunnable.getPriority();
            Priority p2 = other.priorityRunnable.getPriority();
            return p2.ordinal() - p1.ordinal();
        }
    }
}