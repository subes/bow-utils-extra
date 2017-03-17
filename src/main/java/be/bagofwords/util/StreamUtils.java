package be.bagofwords.util;

import be.bagofwords.iterator.CloseableIterator;
import be.bagofwords.iterator.DataIterable;

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by koen on 17/03/17.
 */
public class StreamUtils {

    public static <T> Stream<T> stream(DataIterable<T> iterable) {
        return stream(iterable, false);
    }

    public static <T> Stream<T> stream(DataIterable<T> iterable, boolean ordered) {
        CloseableIterator<T> iterator = iterable.iterator();
        long size = iterable.apprSize();
        return stream(iterator, size, ordered);
    }

    public static <T> Stream<T> stream(final CloseableIterator<T> iterator, final long size, final boolean ordered) {
        return StreamSupport.stream(new Spliterator<T>() {
            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                if (iterator.hasNext()) {
                    action.accept(iterator.next());
                    return true;
                } else {
                    if (!iterator.wasClosed()) {
                        iterator.close();
                    }
                    return false;
                }
            }

            @Override
            public Spliterator<T> trySplit() {
                return null; //Can't split
            }

            @Override
            public long estimateSize() {
                return size;
            }

            @Override
            public int characteristics() {
                if (ordered) {
                    return Spliterator.ORDERED;
                } else {
                    return 0;
                }
            }
        }, false);
    }
}
