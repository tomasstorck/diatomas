package NR;

public class Common {
	
	public static double Clamp(double n, double min, double max) {
		if(n<min)	return min;
		if(n>max) 	return max;
		return n;
	}
	
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
}
