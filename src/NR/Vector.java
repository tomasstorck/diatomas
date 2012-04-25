package NR;

public class Vector {	// It's NOT a template class, but fixed double
	int nn; 				// Size of the array, since Java and C++ are 0 index based the last element is at nn-1
	double[] v;				// Actual content
	static double scale=2;	// The default scaling factor. Can be changed easily. Static to hopefully save some memory

	/////////////////////////////////////////////////////////////////	

	public Vector() {						// Empty 
		nn = 0;
		v = null;
	}

	public Vector(int n) {					// Just of the correct size, empty (null values). 
		nn = n;
		if(n>0) {
			v = new double[n];
		} else {v = null;}
	}

	public Vector(int n, double a) {			// Correct size, all based on the value of a elements
		nn = n;
		if(n>0) {
			v = new double[n];
		} else {v = null;}
		
		for(int i=0; i<n; i++) v[i] = a;
	}
	
	public Vector(int n, double[] a) {			// Correct size, based on the argument's elements
		nn = n;
		if(n>0) {
			v = new double[n];
		} else {v = null;}
		
		for(int i=0; i<n; i++) v[i] = a[i];
	}

	public Vector(Vector rhs) {		// Clone AKA copy constructor
		nn = rhs.nn;
		if(nn>0) {
			v = new double[nn];
		} else {v = null;}
		
		for(int i=0; i<nn; i++) v[i] = rhs.v[i];
	}
	
	/////////////////////////////////////////////////////////////////
	
	public void append(double a) {
		if(nn+1 >= v.length) this.resize((int) (nn*scale));			// Does this slow down the code too much? TODO
		v[nn] = a;
	}
	
	public void append(int nelem, double[] a) {
		if(nn+nelem >= v.length) this.resize((int) (nn*scale));
		for(int ii=0; ii<nelem; ii++) v[nn+ii] = a[ii];
	}
	
	public void append(Vector vector) {
		if(nn+vector.nn >= v.length) this.resize((int) (this.nn*scale));
		for(int ii=0; ii<vector.nn; ii++) v[nn+ii] = vector.get(ii);
	}
	
	public double get(int i) {		// Extract information at i
		return v[i];
	}
	
	public void set(Vector rhs) {		// Assignment
		if(this!=rhs) {
			if(nn!=rhs.nn){
				nn = rhs.nn;
				if(nn>0) {
					v = new double[nn];
				} else {v = null;}
			}
			
			for(int i=0; i<nn; i++) v[i]=rhs.get(i);
		}
	}
	
	public void set(int i, double a) {		// Assignment
		v[i] = a;
	}
	
	public int size() {return nn;}
	
	public void resize(int newn) {			// Resize to newn, without keeping information
		if(newn!=nn) {
			nn = newn;
			if(nn>0) {
				v = new double[nn];
			} else {v = null;}
		}
	}
	
	public void assign(int newn, double a) {		// Resize to newn, assign constant values
		if(newn!=nn) {
			nn = newn;
			if(nn>0) {
				v = new double[nn];
			}
		}
		for(int i=0; i<nn; i++) v[i]=a;
	}
}

