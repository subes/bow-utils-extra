/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-17. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.memory;

import be.bagofwords.application.status.StatusViewable;
import be.bagofwords.counts.WindowOfCounts;
import be.bagofwords.logging.Log;
import be.bagofwords.minidepi.LifeCycleBean;
import be.bagofwords.util.NumUtils;
import be.bagofwords.util.SafeThread;
import be.bagofwords.util.Utils;
import com.sun.management.GarbageCollectionNotificationInfo;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.io.File;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.MemoryUsage;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class MemoryManager implements LifeCycleBean, StatusViewable {

    private final List<WeakReference<MemoryGobbler>> memoryGobblers;
    private MemoryStatus memoryStatus;
    private ReentrantLock globalCleanInProgressLock;
    private final FreeMemoryThread freeMemoryThread;
    private boolean dumpHeapToFileWhenMemoryFull;
    private WindowOfCounts numberOfMemoryFrees;
    private WindowOfCounts numberOfBlockedMethods;

    public MemoryManager() {
        memoryGobblers = new ArrayList<>();
        globalCleanInProgressLock = new ReentrantLock();
        memoryStatus = MemoryStatus.FREE;
        freeMemoryThread = new FreeMemoryThread();
        numberOfMemoryFrees = new WindowOfCounts(10 * 60 * 1000);
        numberOfBlockedMethods = new WindowOfCounts(10 * 60 * 1000);
    }

    public boolean getDumpHeapToFileWhenMemoryFull() {
        return dumpHeapToFileWhenMemoryFull;
    }

    public void setDumpHeapToFileWhenMemoryFull(boolean dumpHeapToFileWhenMemoryFull) {
        this.dumpHeapToFileWhenMemoryFull = dumpHeapToFileWhenMemoryFull;
    }

    @Override
    public void startBean() {
        freeMemoryThread.start();
    }


    @Override
    public void stopBean() {
        freeMemoryThread.terminateAndWaitForFinish();
    }

    /**
     * This method should be called by methods that consume a lot of memory in order to free up other
     * memory gobblers and prevent an Out-Of-Memory error.
     */

    public void waitForSufficientMemory() {
        if (needsToWaitForMemory()) {
            //Don't write until enough memory is free
            long start = System.currentTimeMillis();
            long timeOfLastWarning = System.currentTimeMillis();
            numberOfBlockedMethods.addCount();
            while (needsToWaitForMemory()) {
                Utils.threadSleep(20);
                if (System.currentTimeMillis() - timeOfLastWarning > 30000) {
                    Log.w("Method has been waiting for more memory for " + (System.currentTimeMillis() - start) + " ms");
                    timeOfLastWarning = System.currentTimeMillis();
                }
            }
        }
    }

    private boolean needsToWaitForMemory() {
        return memoryStatus == MemoryStatus.LOW || memoryStatus == MemoryStatus.CRITICAL && !(globalCleanInProgressLock.isLocked() && globalCleanInProgressLock.isHeldByCurrentThread());
    }

    public void registerMemoryGobbler(MemoryGobbler memoryGobbler) {
        synchronized (memoryGobblers) {
            this.memoryGobblers.add(new WeakReference<>(memoryGobbler));
        }
    }

    public MemoryStatus getMemoryStatus() {
        return memoryStatus;
    }

    @Override
    public void printHtmlStatus(StringBuilder sb) {
        sb.append("<h1>Memory</h1>");
        sb.append("Memory status: ").append(getMemoryStatus()).append("<br>");
        String memoryUsed = NumUtils.fixedLength((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000, 4);
        String freeMem = NumUtils.fixedLength((Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory()) / 1000000, 4);
        sb.append("Memory used: ").append(memoryUsed).append("Mb<br>");
        sb.append("Free memory: ").append(freeMem).append("Mb<br>");
        sb.append("On avg. one clean every ").append(numberOfMemoryFrees.getMsPerCount() * 1000).append(" s.<br>");
        sb.append("On avg. ").append((60 * 1000 / numberOfBlockedMethods.getMsPerCount())).append(" blocked methods per minute.<br>");
    }

    private class FreeMemoryThread extends SafeThread {

        private FreeMemoryThread() {
            super("FreeMemoryThread", true);
        }

        public void runImpl() {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

            registerGarbageCollectionListener();

            while (!isTerminateRequested()) {
                try {
                    if (memoryStatus == MemoryStatus.LOW || memoryStatus == MemoryStatus.CRITICAL) {
                        globalCleanInProgressLock.lock();
                        List<WeakReference<MemoryGobbler>> currGobblers;
                        synchronized (memoryGobblers) {
                            currGobblers = new ArrayList<>(memoryGobblers);
                        }
                        Log.i("Will free some memory, current status is " + memoryStatus);
                        long numberOfItemsFreed = freeMemory(currGobblers);
                        memoryStatus = MemoryStatus.FREE;
                        if (numberOfItemsFreed > 0) {
                            System.gc();
                        }
                        //Dump memory to find any memory leaks
                        if (dumpHeapToFileWhenMemoryFull) {
                            Log.i("Dumping heap...");
                            File dumpFile = new File("heap_" + System.currentTimeMillis() + ".bin");
                            HeapDumper.dumpHeap(dumpFile.getAbsolutePath(), false);
                            Log.i("Heap dumped to " + dumpFile.getAbsolutePath());
                        }
                        globalCleanInProgressLock.unlock();
                    }
                } catch (Throwable exp) {
                    Log.e("Exception in CleanObjectsThread!!!", exp);
                }
                Utils.threadSleep(50);
            }
        }

        //copied from http://www.fasterj.com/articles/gcnotifs.shtml

        private void registerGarbageCollectionListener() {
            List<GarbageCollectorMXBean> gcbeans = java.lang.management.ManagementFactory.getGarbageCollectorMXBeans();
            for (GarbageCollectorMXBean gcbean : gcbeans) {
                NotificationEmitter emitter = (NotificationEmitter) gcbean;

                NotificationListener listener = new NotificationListener() {
                    //keep a count of the total time spent in GCs
                    long totalGcDuration = 0;

                    //implement the notifier callback handler
                    @Override
                    public void handleNotification(Notification notification, Object handback) {
                        if (notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
                            //get the information associated with this notification
                            GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
                            Map<String, MemoryUsage> mem = info.getGcInfo().getMemoryUsageAfterGc();
                            for (Map.Entry<String, MemoryUsage> entry : mem.entrySet()) {
                                String name = entry.getKey();
                                MemoryUsage memoryDetail = entry.getValue();
                                if ("PS Old Gen".equals(name)) {
                                    memoryStatus = findStatus(memoryDetail.getUsed(), memoryDetail.getMax());
                                }
                            }
                            totalGcDuration += info.getGcInfo().getDuration();
                        }
                    }
                };

                //Add the listener
                emitter.addNotificationListener(listener, null, null);
            }
        }
    }

    private long freeMemory(List<WeakReference<MemoryGobbler>> currGobblers) {
        numberOfMemoryFrees.addCount();
        long numberOfItemsFreed = currGobblers.parallelStream().map(reference -> {
            MemoryGobbler memoryGobbler = reference.get();
            if (memoryGobbler != null) {
                return memoryGobbler.freeMemory();
            } else {
                return 0l;
            }
        }).collect(Collectors.summingLong(i -> i));
        return numberOfItemsFreed;
    }

    private MemoryStatus findStatus(long used, long max) {
        double fraction = used / (double) max;
        for (int i = MemoryStatus.values().length - 1; i >= 0; i--) {
            MemoryStatus curr = MemoryStatus.values()[i];
            if (curr.getMinMemoryUsage() <= fraction) {
                return curr;
            }
        }
        return MemoryStatus.FREE;
    }

    public long getAvailableMemoryInBytes() {
        return Runtime.getRuntime().maxMemory();
    }

}
