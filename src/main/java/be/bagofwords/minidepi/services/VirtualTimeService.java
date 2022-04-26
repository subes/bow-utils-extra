/*******************************************************************************
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-6-21. For license
 * information see the LICENSE file in the root folder of this repository.
 ******************************************************************************/

package be.bagofwords.minidepi.services;

import java.util.Date;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class VirtualTimeService extends TimeService {

    private long NANOS_IN_MILLI = 1000_000;

    private boolean methodWasCalled = false;
    private long virtualTime = System.currentTimeMillis(); //in millis
    private long timeOfSettingVirtualTime = System.nanoTime(); //in nanos
    private long virtualSpeed = 1000;
    private final ReadWriteLock skipLock = new ReentrantReadWriteLock();
    private long timeSkipped = 0;
    private int MAX_REAL_SLEEP = 100; //millis

    public void setVirtualTime(long virtualTime) {
        setVirtualTime(virtualTime, 1000);
    }

    public void setVirtualTime(long virtualTime, long virtualSpeed) {
        if (methodWasCalled) {
            throw new RuntimeException("One of the methods of TimeService has already been called! You need to call this method earlier");
        }
        this.virtualTime = virtualTime;
        this.virtualSpeed = virtualSpeed;
        this.timeOfSettingVirtualTime = System.nanoTime();
        this.timeSkipped = 0;
    }

    @Override
    public void sleep(long timeToSleep) throws InterruptedException {
        methodWasCalled = true;
        long actualTimeToSleep = timeToSleep / virtualSpeed;
        if (actualTimeToSleep > 0) {
            sleepImpl(actualTimeToSleep);
        }
    }

    public void sleepImpl(long actualTimeToSleep) throws InterruptedException {
        skipLock.readLock().lock();
        if (actualTimeToSleep > MAX_REAL_SLEEP) {
            //Let's see if we can skip this sleep altogether
            skipLock.readLock().unlock();
            skipLock.writeLock().lock();
            long timeToSkip = (actualTimeToSleep - MAX_REAL_SLEEP) * NANOS_IN_MILLI;
            timeSkipped += timeToSkip;
            // Log.i("Should sleep " + actualTimeToSleep + " will " + MAX_REAL_SLEEP + " skipping " + timeToSkip);
            skipLock.writeLock().unlock();
            Thread.sleep(MAX_REAL_SLEEP);
        } else {
            skipLock.readLock().unlock();
            //Probably not worth skipping, let's just do a sleep
            Thread.sleep(actualTimeToSleep);
        }
    }

    @Override
    public long getTime() {
        methodWasCalled = true;
        skipLock.readLock().lock();
        long actualTime = System.nanoTime();
        long timeDiff = actualTime - timeOfSettingVirtualTime + timeSkipped;
        skipLock.readLock().unlock();
        return virtualTime + (timeDiff * virtualSpeed / NANOS_IN_MILLI);
    }

    @Override
    public Date getNow() {
        methodWasCalled = true;
        return new Date(getTime());
    }

}
