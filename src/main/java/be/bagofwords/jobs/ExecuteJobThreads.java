/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-6-30. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.jobs;

import be.bagofwords.util.OccasionalAction;
import be.bagofwords.util.SafeThread;

import java.util.Iterator;

class ExecuteJobThreads<T> extends SafeThread {

    private final PartitionableJob<T> job;
    private final Iterator<T> iterator;
    private final OccasionalAction action;
    private final JobStatus jobStatus;

    public ExecuteJobThreads(JobStatus jobStatus, String nameOfThread, PartitionableJob<T> job, Iterator<T> iterator, OccasionalAction action) {
        super(nameOfThread, false);
        this.job = job;
        this.iterator = iterator;
        this.jobStatus = jobStatus;
        this.action = action;
        jobStatus.addRunningThread(this);
    }

    @Override
    public void runImpl() {
        try {
            boolean finished = false;
            while (!finished && !isTerminateRequested()) {
                T next = null;
                synchronized (iterator) {
                    if (iterator.hasNext()) {
                        next = iterator.next();
                    } else {
                        finished = true;
                    }
                }
                if (next != null) {
                    job.doAction(jobStatus.getCurrentPartition(), next);
                    jobStatus.getWindowedCounts().addCount();
                    action.doOccasionalAction(next);
                }
            }
            jobStatus.removeRunningThread(this);
        } catch (Throwable exp) {
            jobStatus.setThrownException(exp);
        }
    }

}
