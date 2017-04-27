/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-28. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.util;

import be.bagofwords.logging.Log;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public class CleanForkJoinPool extends ForkJoinPool {

    private final String name;

    public CleanForkJoinPool(int parallelism, ForkJoinWorkerThreadFactory factory, Thread.UncaughtExceptionHandler handler, boolean asyncMode, String name) {
        super(parallelism, factory, handler, asyncMode);
        this.name = name;
    }

    @Override
    public ForkJoinTask submit(Runnable runnable) {
        ForkJoinTask task = new ForkJoinTask() {

            private Object result;

            @Override
            public Object getRawResult() {
                return result;
            }

            @Override
            protected void setRawResult(Object value) {
                this.result = value;
            }

            @Override
            protected boolean exec() {
                try {
                    runnable.run();
                } catch (Throwable t) {
                    Log.e("Unexpected exception in task for " + name, t);
                    throw t;
                }
                return true;
            }
        };
        return submit(task);
    }
}
