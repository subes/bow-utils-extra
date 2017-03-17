package be.bagofwords.cache;

import be.bagofwords.cache.fastutil.*;
import be.bagofwords.util.KeyValue;

import java.util.Iterator;

/**
 * Created by Koen Deschacht (koendeschacht@gmail.com) on 27/10/14.
 */
public class DynamicMap<T> {

    private LongMap<T> values;
    private final T nullValue;

    public DynamicMap(Class<? extends T> objectClass, int initialSize) {
        this.values = createMap(objectClass, initialSize);
        this.nullValue = getNullValueForType(objectClass);
    }

    public DynamicMap(Class<? extends T> objectClass) {
        this(objectClass, 0);
    }

    private LongMap<T> createMap(Class<? extends T> objectClass, int initialSize) {
        if (objectClass == Long.class) {
            return (LongMap) new Long2LongOpenHashMap(initialSize);
        } else if (objectClass == Integer.class) {
            return (LongMap) new Long2IntOpenHashMap(initialSize);
        } else if (objectClass == Float.class) {
            return (LongMap) new Long2FloatOpenHashMap(initialSize);
        } else if (objectClass == Double.class) {
            return (LongMap) new Long2DoubleOpenHashMap(initialSize);
        } else {
            return new Long2ObjectOpenHashMap(initialSize);
        }
    }

    private static <T> T getNullValueForType(Class<T> objectClass) {
        if (objectClass == Long.class) {
            return (T) new Long(Long.MAX_VALUE);
        } else if (objectClass == Double.class) {
            return (T) new Double(Double.MAX_VALUE);
        } else if (objectClass == Float.class) {
            return (T) new Float(Float.MAX_VALUE);
        } else if (objectClass == Integer.class) {
            return (T) new Integer(Integer.MAX_VALUE);
        } else {
            return (T) "xxxNULLxxx"; //we don't use a primitive map, so we can just use any object here
        }
    }


    public KeyValue<T> get(long key) {
        T value = values.get(key);
        if (value == null) {
            return null;
        } else if (value.equals(nullValue)) {
            return new KeyValue<>(key, null);
        } else {
            return new KeyValue<>(key, value);
        }
    }

    public void put(long key, T value) {
        if (value == null) {
            value = nullValue;
        } else if (value.equals(nullValue)) {
            throw new RuntimeException("Sorry but " + value + " is a reserved value to indicate null.");
        }
        values.put(key, value);
    }

    public void remove(long key) {
        values.remove(key);
    }

    public int size() {
        return values.size();
    }

    public Iterator<Long> keyIterator() {
        return values.keySet().iterator();
    }

    public Iterator<KeyValue<T>> iterator() {
        Iterator<LongMap.Entry<T>> entries = values.entrySet().iterator();
        return new Iterator<KeyValue<T>>() {
            @Override
            public boolean hasNext() {
                return entries.hasNext();
            }

            @Override
            public KeyValue<T> next() {
                LongMap.Entry<T> entry = entries.next();
                T value = entry.getValue();
                if (value.equals(nullValue)) {
                    value = null;
                }
                return new KeyValue<>(entry.getKey(), value);
            }
        };
    }

    public void clear() {
        values.clear();
    }

    public void putAll(DynamicMap<T> map) {
        for (LongMap.Entry<T> entry : map.values.entrySet()) {
            values.put(entry.getKey(), entry.getValue());
        }
    }
}
