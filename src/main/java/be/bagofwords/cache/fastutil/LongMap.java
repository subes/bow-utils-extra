package be.bagofwords.cache.fastutil;

import java.util.Collection;
import java.util.Set;

/**
 * Created by Koen Deschacht (koendeschacht@gmail.com) on 06/11/14.
 */
public interface LongMap<T> {

    int size();

    boolean isEmpty();

    boolean containsKey(long key);

    boolean containsValue(T value);

    T get(long key);

    T put(long key, T value);

    T remove(long key);

    void clear();

    Set<Long> keySet();

    Collection<T> values();

    Set<Entry<T>> entrySet();

    interface Entry<V> {

        long getKey();

        V getValue();

        void setValue(V value);

        boolean equals(Object o);

        int hashCode();
    }


    boolean equals(Object o);

    int hashCode();

}
