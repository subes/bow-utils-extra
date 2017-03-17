package be.bagofwords.cache;

import be.bagofwords.util.KeyValue;

import java.util.Iterator;
import java.util.concurrent.Semaphore;

public class ReadCache<T> {

    private static final int NUMBER_OF_SEGMENTS_EXPONENT = 8;
    private static final int NUMBER_OF_SEGMENTS = 1 << NUMBER_OF_SEGMENTS_EXPONENT;
    private static final long SEGMENTS_KEY_MASK = NUMBER_OF_SEGMENTS - 1;

    private final Semaphore[] locks;
    private final DynamicMap<T>[] newCachedObjects;
    private final DynamicMap<T>[] cachedObjects;
    private final Class<? extends T> objectClass;
    private final String name;

    private long numHits;
    private long numOfFetches;

    public ReadCache(String name, Class<? extends T> objectClass) {
        this.objectClass = objectClass;
        this.newCachedObjects = new DynamicMap[NUMBER_OF_SEGMENTS];
        createMaps(newCachedObjects);
        this.cachedObjects = new DynamicMap[NUMBER_OF_SEGMENTS];
        createMaps(cachedObjects);
        this.locks = new Semaphore[NUMBER_OF_SEGMENTS];
        for (int i = 0; i < locks.length; i++) {
            this.locks[i] = new Semaphore(1);
        }
        this.numHits = 0;
        this.numOfFetches = 0;
        this.name = name;
    }

    public KeyValue<T> get(long key) {
        incrementFetches();
        int segmentInd = getSegmentInd(key);
        KeyValue<T> result = cachedObjects[segmentInd].get(key);
        if (result != null) {
            incrementHits();
        }
        return result;
    }

    public void put(long key, T value) {
        int segmentInd = getSegmentInd(key);
        lockWrite(segmentInd);
        newCachedObjects[segmentInd].put(key, value);
        unlockWrite(segmentInd);
    }

    public void updateCachedObjects() {
        int numUpdated = 0;
        int size = 0;
        for (int segmentInd = 0; segmentInd < NUMBER_OF_SEGMENTS; segmentInd++) {
            if (newCachedObjects[segmentInd].size() > 0) {
                numUpdated += newCachedObjects[segmentInd].size();
                lockWrite(segmentInd);
                DynamicMap<T> currNewCachedObjects = newCachedObjects[segmentInd];
                newCachedObjects[segmentInd] = new DynamicMap<>(objectClass);
                unlockWrite(segmentInd);
                cachedObjects[segmentInd] = merge(cachedObjects[segmentInd], currNewCachedObjects);
            }
            size += cachedObjects[segmentInd].size();
        }
//        if (numUpdated > 0) {
//            UI.write("Added " + numUpdated + " values to cache " + getName() + " new size is " + size);
//        }
    }

    private DynamicMap<T> merge(DynamicMap<T> cachedObjects, DynamicMap<T> newCachedObjects) {
        DynamicMap<T> updatedCachedObjects = new DynamicMap<>(objectClass, Math.max(cachedObjects.size(), newCachedObjects.size()));
        updatedCachedObjects.putAll(cachedObjects);
        updatedCachedObjects.putAll(newCachedObjects);
        return updatedCachedObjects;
    }

    public void clear() {
        lockWriteAll();
        createMaps(newCachedObjects);
        createMaps(cachedObjects);
        unlockWriteAll();
    }

    public void moveCachedObjectsToOld() {
        lockWriteAll();
        for (int i = 0; i < newCachedObjects.length; i++) {
            cachedObjects[i] = newCachedObjects[i];
        }
        createMaps(newCachedObjects);
        unlockWriteAll();
    }

    public long size() {
        long result = 0;
        for (DynamicMap<T> map : cachedObjects) {
            result += map.size();
        }
        return result;
    }

    public long completeSize() {
        long result = size();
        for (DynamicMap<T> map : newCachedObjects) {
            result += map.size();
        }
        return result;
    }

    public void remove(long key) {
        int segmentInd = getSegmentInd(key);
        lockWrite(segmentInd);
        newCachedObjects[segmentInd].remove(key);
        cachedObjects[segmentInd].remove(key);
        unlockWrite(segmentInd);
    }

    public String getName() {
        return name;
    }

    public long getNumberOfHits() {
        return numHits;
    }

    public long getNumberOfFetches() {
        return numOfFetches;
    }

    private void lockWrite(int segmentInd) {
        locks[segmentInd].acquireUninterruptibly();
    }

    private void unlockWrite(int segmentInd) {
        locks[segmentInd].release();
    }

    private void lockWriteAll() {
        for (int i = 0; i < NUMBER_OF_SEGMENTS; i++) {
            lockWrite(i);
        }
    }

    private boolean tryLockWrite(int segmentInd) {
        return locks[segmentInd].tryAcquire();
    }

    private void unlockWriteAll() {
        for (int i = 0; i < NUMBER_OF_SEGMENTS; i++) {
            unlockWrite(i);
        }
    }

    private void incrementFetches() {
        this.numOfFetches++;
    }

    private void incrementHits() {
        this.numHits++;
    }

    private void createMaps(DynamicMap[] result) {
        for (int i = 0; i < result.length; i++) {
            result[i] = new DynamicMap<>(objectClass);
        }
    }

    private int getSegmentInd(long key) {
        return (int) (key & SEGMENTS_KEY_MASK);
    }

    public Iterator<KeyValue<T>> iterator() {
        return new Iterator<KeyValue<T>>() {

            private Iterator<KeyValue<T>> valuesInCurrSegment = null;
            private int segmentInd = -1;

            {
                //constructor
                findNext();
            }

            private void findNext() {
                while (segmentInd < NUMBER_OF_SEGMENTS - 1 && (valuesInCurrSegment == null || !valuesInCurrSegment.hasNext())) {
                    segmentInd++;
                    valuesInCurrSegment = cachedObjects[segmentInd].iterator();
                }
            }

            @Override
            public boolean hasNext() {
                return valuesInCurrSegment != null && valuesInCurrSegment.hasNext();
            }

            @Override
            public KeyValue<T> next() {
                KeyValue<T> next = valuesInCurrSegment.next();
                if (!valuesInCurrSegment.hasNext()) {
                    findNext();
                }
                return next;
            }

            @Override
            public void remove() {
                throw new RuntimeException("Not supported");
            }
        };
    }

}
