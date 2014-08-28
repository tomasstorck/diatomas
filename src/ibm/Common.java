package ibm;

public class Common {
	public static double maxArray(double[] array) {
		double max = array[0];
		for(int ii=1; ii<array.length; ii++) {
			max = (array[ii]>max) ? array[ii] : max;
		}
		return max;
	}

}
