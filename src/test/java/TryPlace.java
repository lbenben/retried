import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.stream.Stream;

public class TryPlace {
	private static final Logger logger = LogManager.getLogger();

	public static void main(String[] args) {

		Stream.concat(Stream.iterate(1, x -> x + 1).limit(3).map(x -> {
			try {
				Thread.sleep(x * 100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return x;
		}), Stream.iterate(10, x -> x - 3).filter(x -> {
			if (x > 0)
				System.out.printf("x = %d, x * 100 = %d\n", x, x * 100);
			return x > 0;
		}).limit(6).map(x -> {
			logger.error(x * 100);
			try {
				Thread.sleep(x * 100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return x;
		})).forEach(x -> logger.error(x));
	}
}
