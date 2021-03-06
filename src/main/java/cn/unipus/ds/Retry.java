package cn.unipus.ds;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;



public class Retry {
	private static final Logger logger = LogManager.getLogger();

	public static final String RETRY_TIMES_ENV = "RETRY_TIMES";

	public static final int RETRY_TIMES_DEFAULT = 10;

	private static final int RETRY_TIMES;

	public static final String BACKOFF_FACTOR_ENV = "RETRY_TIMES";

	public static final int BACKOFF_FACTOR_DEFAULT = 5000;

	private static final int BACKOFF_FACTOR;

	public static final String NEED_RETRY_EXCEPTIONS_ENV = "NEED_RETRY_EXCEPTIONS_ENV";
	public static final String NEED_RETRY_EXCEPTIONS_DEFAULT = "java.lang.Exception";
	private static final List<Class> NEED_RETRY_EXCEPTIONS;

	private static int getEnvInt(String env, int def) {
		int envVal = Integer.parseInt(
				System.getenv(env) != null
						? System.getenv(env)
						: System.getProperty(env, "0"));
		return envVal == 0 ? def : envVal;
	}

	private static String[] getEnvStrArray(String env, String def) {
		String envVal = System.getenv(env) != null
				? System.getenv(env)
				: System.getProperty(env, "");
		return (envVal == "" ? def : envVal).split(",\\s*");
	}

	static {
		RETRY_TIMES = getEnvInt(RETRY_TIMES_ENV, RETRY_TIMES_DEFAULT);
		BACKOFF_FACTOR = getEnvInt(BACKOFF_FACTOR_ENV, BACKOFF_FACTOR_DEFAULT);

		NEED_RETRY_EXCEPTIONS = Arrays.stream(
				getEnvStrArray(NEED_RETRY_EXCEPTIONS_ENV, NEED_RETRY_EXCEPTIONS_DEFAULT)
		).map(cname -> {
			try {
				return Class.forName(cname);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}).collect(Collectors.toList());
	}

	public static <T> T retry(final Callable<T> callable) throws Exception {
		return retry(callable, getRetryTimes(), getBackoffFactor(), getNeedRetryExceptions());
	}

	protected static List<Class> getNeedRetryExceptions() {
		return NEED_RETRY_EXCEPTIONS;
	}

	protected static int getBackoffFactor() {
		return BACKOFF_FACTOR;
	}

	protected static int getRetryTimes() {
		return RETRY_TIMES;
	}

	public static <T> T retry(final Callable<T> callable, int times, long backoffFactor) throws Exception {
		return retry(callable, times, backoffFactor, getNeedRetryExceptions());
	}

	public static <T> T retry(Callable<T> callable, Iterator waitIterator) throws Exception {
		return retry(callable, waitIterator, getNeedRetryExceptions());
	}

	public static <T> T retry(Callable<T> callable, Iterator waitIterator, List<Class> exceptionClasses) throws Exception{
		List<T> ts = new ArrayList<>(1);

		boolean STOP = true;
		boolean CONTINUE = false;

		AtomicInteger tryTime = new AtomicInteger(0);
		Stream.iterate(0, x -> x + 1)
				.anyMatch(idx -> {
					try {
						System.out.println(tryTime.incrementAndGet());
						T t = callable.call();
						ts.add(t);
						return STOP;
					} catch (Exception e) {
						logger.warn("Failed: {} - {}", idx, e);
						e.printStackTrace();

						if (exceptionClasses.stream().anyMatch(c -> c.isInstance(e))) {
							if(waitIterator.hasNext()) {
								waitIterator.next();
								return CONTINUE;
							} else {
								return STOP;
							}
						} else {
							return STOP;
						}
					}
				});

		if (ts.size() < 1) {
			throw new RetryException(String.format("Retry %d times, but failed.", 5));
		} else {
			return ts.get(0);
		}
	}

	public static <T> T retry(Callable<T> callable, int times,
						long backoff,
						List<Class> exceptionClasses) throws Exception{
		if (times < 0) {
			times = 0;
		}

		final long backoffFactor = backoff <= 0 ? 1 : backoff;

		List<T> ts = new ArrayList<>(1);

		boolean STOP = true;
		boolean CONTINUE = false;

		Stream.iterate(0, x -> x + 1).limit(times + 1)
				.anyMatch(idx -> {
					try {
						T t = callable.call();

						System.out.println(t);
						ts.add(t);
						return STOP;
					} catch (Exception e) {
						logger.warn("Failed: {} - {}", idx, e);
						if (exceptionClasses.stream().anyMatch(c -> c.isInstance(e))) {
							try {
								Thread.sleep((long) (backoffFactor * Math.pow(2, idx)));
							} catch (InterruptedException e1) {
								e1.printStackTrace();
								return STOP;
							}
							return CONTINUE;
						} else {
							return STOP;
						}
					}
				});

		if (ts.size() < 1) {
			throw new RetryException(String.format("Retry %d times, but failed.", times));
		} else {
			return ts.get(0);
		}
	}

	private static class RetryException extends Exception {
		RetryException(String msg) {
			super(msg);
		}
	}
}
