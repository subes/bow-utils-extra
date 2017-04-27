package be.bagofwords.application;

import be.bagofwords.logging.Log;
import be.bagofwords.minidepi.LifeCycleBean;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Koen Deschacht (koendeschacht@gmail.com) on 14/11/14.
 */

public class TaskSchedulerService implements LifeCycleBean {

    private ScheduledExecutorService scheduledExecutorService;

    /**
     * @param task
     * @param period In milliseconds
     */

    public synchronized void schedulePeriodicTask(Runnable task, long period) {
        if (scheduledExecutorService == null) {
            scheduledExecutorService = Executors.newScheduledThreadPool(5);
        }
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                Log.e("Task " + task + " terminated with an error!", t);
            }
        }, 0, period, TimeUnit.MILLISECONDS);
    }

    @Override
    public void startBean() {
        //Do nothing
    }

    @Override
    public void stopBean() {
        scheduledExecutorService.shutdown();
    }
}
