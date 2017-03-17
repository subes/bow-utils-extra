/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-17. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagwords.util;

import be.bagofwords.iterator.DataIterable;
import be.bagofwords.iterator.IterableUtils;
import be.bagofwords.util.StreamUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

public class StreamUtilsTest {

    @Test
    public void testStream() throws Exception {
        List<String> sourceList = Arrays.asList("two", "one", "three");
        DataIterable<String> iterable = IterableUtils.createIterable(sourceList);
        List<String> resultList = StreamUtils.stream(iterable).collect(toList());
        assertEquals(sourceList, resultList);
    }

    @Test
    public void testStream1() throws Exception {
        List<String> sourceList = Arrays.asList("one", "two", "three");
        DataIterable<String> iterable = IterableUtils.createIterable(sourceList);
        List<String> resultList = StreamUtils.stream(iterable, true).collect(toList());
        assertEquals(sourceList, resultList);
    }

}
