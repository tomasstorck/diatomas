package cell;

import java.util.Random;

public class rand {										// Nice idea, from http://www.cis.upenn.edu/~dsl/PLAN/docs-java/serv_guide/node4.html
	private static Random svcRand = new Random(1);			// Set a default random seed, in case we forget, so we can still get reproducible results
	
	public static int Int(int maxInt) {
		return svcRand.nextInt(maxInt);				// Returns an int, can also be negative
	}

	public static double Double() {
		return svcRand.nextDouble();		// Returns a double, between 0 and 1
	}
	
	public static void Seed(int seed) {
		svcRand.setSeed(seed);
	}
}

