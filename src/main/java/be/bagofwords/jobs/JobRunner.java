package be.bagofwords.jobs;

import be.bagofwords.counts.WindowOfCounts;
import be.bagofwords.iterator.CloseableIterator;
import be.bagofwords.iterator.DataIterable;
import be.bagofwords.minidepi.LifeCycleBean;
import be.bagofwords.ui.UI;
import be.bagofwords.util.OccasionalAction;
import be.bagofwords.util.SafeThread;
import be.bagofwords.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class JobRunner implements LifeCycleBean {

    private static final Logger logger = LoggerFactory.getLogger(JobRunner.class);

    private final List<JobStatus> runningJobs;
    private boolean terminateRequested = false;

    public JobRunner() {
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
                    UI.write(outS);
                }
            }
        };
        for (; jobStatus.getCurrentPartition() < numOfPartitions; jobStatus.setCurrentPartition(jobStatus.getCurrentPartition() + 1)) {
            CloseableIterator<T> iterator = iterable.iterator();
            ExecuteActionRunnable[] threads = new ExecuteActionRunnable[numOfThreads];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new ExecuteActionRunnable<>(jobStatus, name + "_" + i, job, iterator, action);
                threads[i].start();
            }
            boolean allFinished = false;
            Throwable receivedException = null;
            while (!allFinished) {
                Utils.threadSleep(200);
                allFinished = true;
                if (terminateRequested) {
                    for (ExecuteActionRunnable thread : threads) {
                        thread.terminateAndWaitForFinish();
                        receivedException = new RuntimeException("Termination of application was requested.");
                    }
                } else {
                    for (ExecuteActionRunnable thread : threads) {
                        if (thread.getExp() != null) {
                            //Terminate all other threads
                            for (ExecuteActionRunnable runner : threads) {
                                runner.terminateAndWaitForFinish();
                            }
                            receivedException = thread.getExp();
                        }
                        allFinished &= thread.isFinished();
                    }
                }
            }
            iterator.close();
            if (jobStatus.getCurrentPartition() == 0) {
                jobStatus.setNumberOfObjects(jobStatus.getWindowedCounts().getTotalCounts()); //Set estimated size of iterator to actual size
            }
            if (receivedException != null) {
                throw new RuntimeException(receivedException);
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
        terminateRequested = true;
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
            logger.info("While stopping JobRunner " + runningJobs.size() + " jobs did not finish in time");
        }
    }

    private static class ExecuteActionRunnable<T> extends SafeThread {
        private final PartitionableJob<T> job;
        private final Iterator<T> iterator;
        private Throwable exp;
        private final OccasionalAction action;
        private final JobStatus jobStatus;

        public ExecuteActionRunnable(JobStatus jobStatus, String nameOfThread, PartitionableJob<T> job, Iterator<T> iterator, OccasionalAction action) {
            super(nameOfThread, false);
            this.job = job;
            this.iterator = iterator;
            this.jobStatus = jobStatus;
            this.action = action;
        }

        public Throwable getExp() {
            return exp;
        }

        @Override
        public void runInt() {
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
            } catch (Throwable exp) {
                this.exp = exp;
            }
        }
    }

    private class JobStatus {
        private final String name;
        private final WindowOfCounts windowedCounts;
        private final int numberOfPartitions;
        private long numberOfObjects;
        private int currentPartition;

        private JobStatus(String name, long numberOfObjects, int numberOfPartitions) {
            this.name = name;
            this.numberOfObjects = numberOfObjects;
            this.numberOfPartitions = numberOfPartitions;
            this.windowedCounts = new WindowOfCounts(60000); //TODO this window should also take into account the number of tasks executed per second, i.e. for long tasks this window should be larger
            this.currentPartition = 0;
        }

        public long getNumberOfObjects() {
            return numberOfObjects;
        }

        public void setNumberOfObjects(long numberOfObjects) {
            this.numberOfObjects = numberOfObjects;
        }

        public WindowOfCounts getWindowedCounts() {
            return windowedCounts;
        }

        public int getCurrentPartition() {
            return currentPartition;
        }

        public void setCurrentPartition(int currentPartition) {
            this.currentPartition = currentPartition;
        }

        public String getName() {
            return name;
        }

        public int getNumberOfPartitions() {
            return numberOfPartitions;
        }
    }
}