package ericson;

public class Common {
	
	// "Clamps" a vlue between two extremities
	public static double Clamp(double n, double min, double max) {
		if(n<min)	return min;
		if(n>max) 	return max;
		return n;
	}
}