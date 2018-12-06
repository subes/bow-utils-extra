package be.bagofwords.jobs;

import be.bagofwords.iterator.CloseableIterator;
import be.bagofwords.iterator.DataIterable;
import be.bagofwords.logging.Log;
import be.bagofwords.minidepi.LifeCycleBean;
import be.bagofwords.util.NumUtils;
import be.bagofwords.util.OccasionalAction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JobService implements LifeCycleBean {

    private final List<JobStatus> runningJobs;

    public JobService() {
        runningJobs = new ArrayList<>();
    }

    public <T extends Object> void runPartitionedJobs(final int numOfPartitions, int numOfThreads, final String name, DataIterable<T> iterable, final PartitionableJob<T> job) {
        runPartitionedJobs(true, numOfPartitions, numOfThreads, name, iterable, job);
    }

    public <T extends Object> void runPartitionedJobs(final boolean printProgress, int numOfPartitions, int numOfThreads, final String name, DataIterable<T> iterable, final PartitionableJob<T> job) {
        final JobStatus jobStatus = new JobStatus(name, iterable.apprSize(), numOfPartitions);
        synchronized (runningJobs) {
            runningJobs.add(jobStatus);
        }
        OccasionalAction<T> action = new OccasionalAction<T>(10000) {
            @Override
            protected void doAction(T curr) {
                if (printProgress) {
                    String outS = createOutputString(jobStatus);
                    Log.i(outS);
                }
            }
        };
        for (; jobStatus.getCurrentPartition() < numOfPartitions; jobStatus.setCurrentPartition(jobStatus.getCurrentPartition() + 1)) {
            CloseableIterator<T> iterator = iterable.iterator();
            ExecuteJobThreads[] threads = new ExecuteJobThreads[numOfThreads];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new ExecuteJobThreads<>(jobStatus, name + "_" + i, job, iterator, action);
                threads[i].start();
            }
            while (!jobStatus.getRunningThreads().isEmpty() && jobStatus.getThrownException() == null) {
                synchronized (jobStatus.statusChangedLock) {
                    try {
                        jobStatus.statusChangedLock.wait();
                    } catch (InterruptedException exp) {
                        throw new RuntimeException("Interrupted while waiting for job to terminate");
                    }
                }

            }
            iterator.close();
            if (jobStatus.getCurrentPartition() == 0) {
                jobStatus.setNumberOfObjects(jobStatus.getWindowedCounts().getTotalCounts()); //Set estimated size of iterator to actual size
            }
            if (jobStatus.getThrownException() != null) {
                throw new RuntimeException(jobStatus.getThrownException());
            }
        }
        synchronized (runningJobs) {
            runningJobs.remove(jobStatus);
        }
    }

    private <T extends Object> String createOutputString(JobStatus jobStatus) {
        long did = jobStatus.getWindowedCounts().getTotalCounts();
        long todo = jobStatus.getNumberOfObjects() * jobStatus.getNumberOfPartitions() - did;
        long didInIteration = did - jobStatus.getNumberOfObjects() * jobStatus.getCurrentPartition();
        String outS = "[Progress " + jobStatus.getName() + "]";
        if (jobStatus.getNumberOfPartitions() > 1) {
            outS += " " + jobStatus.getCurrentPartition() + "/" + jobStatus.getNumberOfPartitions();
        }
        outS += " did " + didInIteration + " of " + jobStatus.getNumberOfObjects();
        double percentage = Math.min(1, didInIteration * 1.0 / jobStatus.getNumberOfObjects());
        outS += " " + NumUtils.makeNicePercent(percentage) + "% ";
        String endString;
        if (todo > 0) {
            endString = " end is " + new Date(System.currentTimeMillis() + jobStatus.getWindowedCounts().getNeededTime(todo));
        } else {
            endString = " should finish any second now";
        }
        return outS + endString;
    }

    public <T extends Object> void runJob(int numOfThreads, final String name, DataIterable<T> iterable, final Job<T> job) {
        runJob(true, numOfThreads, name, iterable, job);
    }

    public <T extends Object> void runJob(final boolean printProgress, int numOfThreads, final String name, DataIterable<T> iterable, final Job<T> job) {
        runPartitionedJobs(printProgress, 1, numOfThreads, name, iterable, new PartitionableJob<T>() {
            @Override
            public void doAction(int partition, T target) throws Exception {
                job.doAction(target);
            }

        });
    }

    public <T extends Object> void runJob(final String name, DataIterable<T> iterable, final Job<T> job) {
        runJob(true, 1, name, iterable, job);
    }

    public String createHtmlStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("<h1>Job Runner</h1>");
        synchronized (runningJobs) {
            if (runningJobs.isEmpty()) {
                sb.append("No jobs currently running.");
            } else {
                sb.append("Currently running " + runningJobs.size() + " jobs<br>");
                for (JobStatus runningJob : runningJobs) {
                    sb.append("<pre>" + createOutputString(runningJob) + "</pre>");
                }
            }
        }
        return sb.toString();
    }

    @Override
    public void startBean() {
        //Do nothing
    }

    @Override
    public void stopBean() {
        for (JobStatus runningJob : runningJobs) {
            for (Thread thread : runningJob.getRunningThreads()) {
                thread.interrupt();
            }
        }
        long started = System.currentTimeMillis();
        boolean interrupted = false;
        while (System.currentTimeMillis() - started < 10 * 1000 && !runningJobs.isEmpty() && !interrupted) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (!runningJobs.isEmpty()) {
            Log.i("While stopping JobService " + runningJobs.size() + " jobs did not finish in time");
        }
    }


}