/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-17. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.util;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

public class TimeUtils {

    private static final Logger logger = LoggerFactory.getLogger(TimeUtils.class);
    private static final ExecutorService execService = Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors()), new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("TimeUtilsExecutor");
            t.setDaemon(true);
            return t;
        }
    });

    public static <T> TimedResult<T> limitInTime(long maxRuntimeInMs, Supplier<T> generatorFunction) {
        MutableObject<T> result = new MutableObject<>(null);
        MutableObject<Throwable> error = new MutableObject<>(null);
        MutableBoolean functionFinished = new MutableBoolean(false);
        MutableLong started = new MutableLong(-1);
        Future<?> future = execService.submit(() -> {
            started.setValue(System.currentTimeMillis());
            try {
                result.setValue(generatorFunction.get());
            } catch (Throwable t) {
                error.setValue(t);
            }
            functionFinished.setTrue();
        });
        while (!future.isDone() && (started.getValue() == -1 || System.currentTimeMillis() - started.getValue() < maxRuntimeInMs)) {
            try {
                Thread.sleep(maxRuntimeInMs / 10);
            } catch (InterruptedException e) {
                return new TimedResult<>(null, false, null);
            }
        }
        if (!future.isDone()) {
            future.cancel(true);
        }
        if (functionFinished.isTrue()) {
            return new TimedResult<>(result.getValue(), true, error.getValue());
        } else {
            return new TimedResult<>(null, false, error.getValue());
        }
    }

    public static TimedResult<Void> limitInTime(long maxRuntimeInMs, Runnable runnable) {
        return limitInTime(maxRuntimeInMs, () -> {
            runnable.run();
            return null;
        });
    }


    private static class ResultGeneratingThread<T> extends Thread {
        public T result;
        private Supplier<T> supplier;

        public ResultGeneratingThread(Supplier<T> supplier) {
            super("limitInTimeThread");
            this.supplier = supplier;
        }

        @Override
        public void run() {
            logger.debug("Starting execution of " + supplier);
            this.result = supplier.get();
            logger.debug("Finished execution of " + supplier);
        }
    }

    public static class TimedResult<T> {
        public T result;
        public boolean finishedInTime;
        public Throwable error;

        public TimedResult(T result, boolean finishedInTime, Throwable error) {
            this.result = result;
            this.finishedInTime = finishedInTime;
            this.error = error;
        }
    }

}

