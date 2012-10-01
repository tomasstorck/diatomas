// From: http://www.cis.upenn.edu/~dsl/PLAN/docs-java/serv_guide/node4.html

package random;

import java.util.Random;

public class rand {				// Nice idea, from http://www.cis.upenn.edu/~dsl/PLAN/docs-java/serv_guide/node4.html
	private static Random Stream = new Random(1);		// Set a default random seed, in case we forget, so we can still get reproducible results
	
	public static int Int(int maxInt) {				// Note that this method is EXCLUSIVE maxInt
		return Stream.nextInt(maxInt);					// Returns an int
	}
	
	public static int IntChoose(int[] range) {
		int ii = Stream.nextInt(range.length);		// Because nextInt is EXCLUSIVE
		return range[ii];
	}

	public static double Double() {
		return Stream.nextDouble();					// Returns a double, between 0 and 1
	}
	
	public static void Seed(int seed) {
		Stream.setSeed(seed);
	}
}

