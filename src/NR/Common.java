package NR;

public class Common {
	
	// "Clamps" a vlue between two extremities
	public static double Clamp(double n, double min, double max) {
		if(n<min)	return min;
		if(n>max) 	return max;
		return n;
	}
	
	// Keeps only the values that occur in an int array once
	public static int[] Unique(int[] input) {
		int[] output = new int[input.length];
		for(int ii=0; ii<output.length; ii++)		output[ii] = Integer.MAX_VALUE;
		int index = 0;
		for(int ii=0; ii<input.length; ii++) {
			boolean seen = false;
			for(int jj=0; jj<output.length; jj++) {
				if(input[ii] == output[jj]) 	seen = true;
			}
			if(!seen)		output[index++] = input[ii]; 
		}
		// Cut to size
		int[] returnOutput = new int[index];
		for(int ii=0; ii<index; ii++)		returnOutput[ii] = output[ii]; 
		return returnOutput;
	}
	
	// Converts a boolean[][] to a double[][] with true == 1 and false == 0 (MATLAB style)
	public static double[][] boolean2double(boolean[][] input) {
		double[][] output = new double[input.length][input[0].length]; 
		for(int ii=0; ii<input.length; ii++) {
			for(int jj=0; jj<input[0].length; jj++) {
				output[ii][jj] = input[ii][jj] ? 1.0 : 0.0;
			}
		}
		return output;
	}
}
