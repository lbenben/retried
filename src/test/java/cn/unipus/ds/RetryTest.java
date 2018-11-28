package cn.unipus.ds;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.hamcrest.CoreMatchers.is;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Unit test for simple Retry.
 */
public class RetryTest {
    @Test
    public void testException() {
        List<Integer[]> cases = Arrays.asList(new Integer[][]{
//                {1, 1, 10},
                {1, 0, 10},
//                {20, 19, 10},
//                {3, 2, 10},
//                {3, 3, 10}
        });
        cases.stream().forEach(c -> {
            int failTime = c[0];
            int retryTime = c[1];
            int seed = c[2];
            try {
                Iterator<Integer> iter = Stream.iterate(seed, x -> x + 1).limit(retryTime).map(i -> {
                    try {
                        Thread.sleep(i);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return i;
                }).iterator();

                List<Integer> i = new ArrayList<>(1);
                i.add(0);
                Retry.retryIt(() -> {
                    int ti = i.get(0);
                    i.set(0, ti + 1);
                    System.out.println(ti);
                    if (ti < failTime) {
                        throw new Exception(String.format("%d", ti));
                    }
                    return ti;
                }, iter);
                fail("Expected an Exception to be thrown");
            } catch (Exception e) {
                assertThat(e.getMessage(), containsString("times, but failed."));
            }

        });
    }

    @Test
    public void testRetryOK() throws Exception {
        List<Integer[]> cases = Arrays.asList(new Integer[][]{
//                {1, 1, 10},
                {1, 2, 10},
//                {2, 2, 10},
                {2, 3, 10},
//                {3, 3, 10}
        });
        Iterator<Integer> it = Stream.iterate(50, x -> x + 1).limit(5).map(i -> {
            try {
                Thread.sleep(i);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return i;
        }).iterator();

        cases.stream().forEach(xs -> {
            int failTime = xs[0];
            int retryTime = xs[1];
            int backoffFactor = xs[2];
            List<Integer> i = new ArrayList<>(1);
            i.add(0);
            try {
                Retry.retry(() -> {
                    int ti = i.get(0);
                    i.set(0, ti + 1);
                    if (ti < failTime) {
                        throw new Exception(String.format("%d", ti));
                    }
                    return ti;
                }, retryTime, backoffFactor);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
