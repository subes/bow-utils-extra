/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-6-30. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.jobs;

import be.bagofwords.logging.Log;
import be.bagofwords.minidepi.LifeCycleBean;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AsyncJobService implements LifeCycleBean {

    private ScheduledExecutorService scheduledExecutorService;

    public void schedulePeriodicJob(Runnable job, long period) {
        ensureExecutorServiceCreated();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                job.run();
            } catch (Throwable t) {
                Log.e("Task " + job + " terminated with an error!", t);
            }
        }, period, period, TimeUnit.MILLISECONDS);
    }

    private synchronized void ensureExecutorServiceCreated() {
        if (scheduledExecutorService == null) {
            scheduledExecutorService = Executors.newScheduledThreadPool(5);
        }
    }

    @Override
    public void startBean() {
        //Do nothing
    }

    @Override
    public void stopBean() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
        }
    }
}
