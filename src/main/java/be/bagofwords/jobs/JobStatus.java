/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-6-30. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.jobs;

import be.bagofwords.counts.WindowOfCounts;

import java.util.HashSet;
import java.util.Set;

class JobStatus {

    public final String name;
    private final WindowOfCounts windowedCounts;
    private final int numberOfPartitions;
    private long numberOfObjects;
    private int currentPartition;
    private boolean setupFinished = false;
    private boolean allObjectsProcessed = false;
    private boolean tearDownFinished = false;
    public final Object statusChangedLock = new Object();
    private Set<Thread> runningThreads= new HashSet<>();
    private Throwable thrownException;

    JobStatus(String name, long numberOfObjects, int numberOfPartitions) {
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

    public void addRunningThread(Thread thread) {
        synchronized (statusChangedLock) {
            runningThreads.add(thread);
            statusChangedLock.notifyAll();
        }
    }

    public void removeRunningThread(Thread thread) {
        synchronized (statusChangedLock) {
            runningThreads.remove(thread);
            statusChangedLock.notifyAll();
        }
    }

    public void setupHasFinished() {
        synchronized (statusChangedLock) {
            setupFinished = true;
            statusChangedLock.notifyAll();
        }
    }

    public void tearDownHasFinished() {
        synchronized (statusChangedLock) {
            tearDownFinished = true;
            statusChangedLock.notifyAll();
        }
    }

    public boolean isSetupFinished() {
        return setupFinished;
    }

    public boolean isTearDownFinished() {
        return tearDownFinished;
    }

    public boolean isAllObjectsProcessed() {
        return allObjectsProcessed;
    }

    public Object getStatusChangedLock() {
        return statusChangedLock;
    }

    public Set<Thread> getRunningThreads() {
        return runningThreads;
    }

    public Throwable getThrownException() {
        return thrownException;
    }

    public void setThrownException(Throwable thrownException) {
        synchronized (statusChangedLock) {
            this.thrownException = thrownException;
            statusChangedLock.notifyAll();
        }
    }
}
