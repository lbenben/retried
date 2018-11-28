package cn.unipus.ds;

import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

/**
 * Unit test for Retry.
 */
public class RetryTest {

	private static final List<Object[]> CASES = Arrays.asList(new Object[][]{
			{1, 0, 1, Exception.class, "times, but failed."},
			{1, 1, 1, null, null},
			{3, 2, 1, Exception.class, "times, but failed."},
			{3, 3, 1, null, null},
			{4, 2, 1, Exception.class, "times, but failed."},
			{4, 3, 1, Exception.class, "times, but failed."},
			{4, 4, 1, null, null},
			{4, 5, 1, null, null},
	});

	@Test
	public void testNeedRetryExceptions() {
		CASES.stream().forEach(c -> {
			int failTime = (int)c[0];
			int retryTime = (int)c[1];
			int seed = (int)c[2];
			Class klass = (Class)c[3];
			String containedStr = (String)c[4];

			List<Integer> i = new ArrayList<>(1);
			i.add(0);
			retryWithIteratorTestTmpl(() -> {
				int ti = i.get(0);
				i.set(0, ti + 1);
				if (ti < failTime) {
					throw new Exception(String.format("%d", ti));
				}
				return ti;
			}, failTime, failTime, retryTime, seed, klass, containedStr);
		});
	}

	@Test
	public void testRetryWithIterator() {
		CASES.stream().forEach(c -> {
			int failTime = (int)c[0];
			int retryTime = (int)c[1];
			int seed = (int)c[2];
			Class klass = (Class)c[3];
			String containedStr = (String)c[4];

			List<Integer> i = new ArrayList<>(1);
			i.add(0);
			retryWithIteratorTestTmpl(() -> {
				int ti = i.get(0);
				i.set(0, ti + 1);
				if (ti < failTime) {
					throw new Exception(String.format("%d", ti));
				}
				return ti;
			}, failTime, failTime, retryTime, seed, klass, containedStr);
		});
	}

    private <T> void retryWithIteratorTestTmpl(Callable<T> callable, T expectRes, int failTime, int retryTime, int seed, Class exptClass, String containedStr) {
        Iterator<Integer> iter = Stream.iterate(seed, x -> x + 1).limit(retryTime).map(i -> {
            try {
                Thread.sleep(i);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return i;
        }).iterator();
        if (exptClass != null) {
            try {
                Retry.retry(callable, iter);
                fail(format("Expected an Exception to be thrown.\n args:(%d, %d, %d, %s, %s)", failTime, retryTime, seed, exptClass, containedStr));
            } catch (Exception e) {
                assertThat(e.getMessage(), containsString(containedStr));
            }
        } else {
            try {
	            T res = Retry.retry(callable, iter);

	            assertEquals(expectRes, res);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private <T> void retryWithArgumentsTestTmpl(Callable<T> callable, T expectRes, int failTime, int retryTime, int seed, Class exptClass, String containedStr) {
        if (exptClass != null) {
            try {
                Retry.retry(callable, retryTime, seed);
                fail(format("Expected an Exception to be thrown.\n args:(%d, %d, %d, %s, '%s')", failTime, retryTime, seed, exptClass, containedStr));
            } catch (Exception e) {
                assertThat(e.getMessage(), containsString(containedStr));
            }
        } else {
            try {
                T res = Retry.retry(callable, retryTime, seed);
	            assertEquals(expectRes, res);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void testRetryWithArguments() throws Exception {

        Iterator<Integer> it = Stream.iterate(50, x -> x + 1).limit(5).map(i -> {
            try {
                Thread.sleep(i);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return i;
        }).iterator();

        CASES.stream().forEach(c -> {
            int failTime = (int)c[0];
            int retryTime = (int)c[1];
            int seed = (int)c[2];
            Class klass = (Class)c[3];
            String containedStr = (String)c[4];

            List<Integer> i = new ArrayList<>(1);
            i.add(0);
            retryWithArgumentsTestTmpl(() -> {
                int ti = i.get(0);
                i.set(0, ti + 1);
                if (ti < failTime) {
                    throw new Exception(String.format("%d", ti));
                }
                return ti;
            }, failTime, failTime, retryTime, seed, klass, containedStr);
        });
    }
}
