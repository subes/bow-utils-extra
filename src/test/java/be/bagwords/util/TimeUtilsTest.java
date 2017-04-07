/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-17. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagwords.util;

import be.bagofwords.util.TimeUtils;
import org.junit.Assert;
import org.junit.Test;

public class TimeUtilsTest {

    @Test
    public void testShortJob() {
        TimeUtils.TimedResult<Integer> result = TimeUtils.limitInTime(1000, () -> 1 + 1);
        Assert.assertTrue(result.finishedInTime);
        Assert.assertNotNull(result.result);
        Assert.assertEquals(2, (int) result.result);
    }

    @Test
    public void testTooLongJob() {
        TimeUtils.TimedResult<Double> result = TimeUtils.limitInTime(1000, () -> {
            double sum = 0;
            for (long i = 0; i < Long.MAX_VALUE; i++) {
                sum += i;
            }
            return sum;
        });
        Assert.assertFalse(result.finishedInTime);
        Assert.assertNull(result.result);
    }

    @Test
    public void testJobWithException() {
        TimeUtils.TimedResult<Double> result = TimeUtils.limitInTime(1000, () -> {
            throw new RuntimeException("Test");
        });
        Assert.assertTrue(result.finishedInTime);
        Assert.assertNull(result.result);
        Assert.assertNotNull(result.error);
    }

}