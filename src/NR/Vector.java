// Initial idea and part of the code based on Numerical Recipes, with more additions over the whole project

package NR;

public class Vector {		// It's NOT a template class, but fixed double
	int n; 					// Size of the array, since Java and C++ are 0 index based the last element is at n-1
	double[] v;				// Actual content
	static double scale=2;	// The default scaling factor. Can be changed easily. Static to hopefully save some memory

	/////////////////////////////////////////////////////////////////	

	public Vector() {						// Empty 
		n = 0;
		v = null;
	}

	public Vector(int size) {					// Just of the correct size, empty (null values). 
		n = size;
		if(size>0) {
			v = new double[size];
		} else {v = null;}
	}

	public Vector(int size, double a) {			// Correct size, all based on the value of a elements
		n = size;
		if(size>0) {
			v = new double[size];
		} else {v = null;}
		
		for(int i=0; i<size; i++) v[i] = a;
	}
	
	public Vector(int size, double[] a) {			// Correct size, based on the argument's elements
		n = size;
		if(n>0) {
			v = new double[n];
		} else {v = null;}
		
		for(int i=0; i<n; i++) v[i] = a[i];
	}
	
	public Vector(double[] a) {
		n = a.length;
		
		if(n>0) {
			v = new double[n];
		} else {v = null;}
		
		for(int i=0; i<n; i++) v[i] = a[i];
	}

	public Vector(Vector orig) {			// Clone AKA copy constructor
		n = orig.n;
		if(n>0) {
			v = new double[n];
		} else {v = null;}
		
		for(int i=0; i<n; i++) v[i] = orig.v[i];
	}
	
	////////////////////////
	// Vector alterations //
	////////////////////////
	
	// Append
	public void append(double elem) {
		if(n+1 >= v.length) this.resize((int) (n*scale));			// Check if we need to resize
		v[n] = elem;
	}
	
	public void append(int nelem, double[] elem) {
		if(n+nelem >= v.length) this.resize((int) (n*scale));
		for(int ii=0; ii<nelem; ii++) v[n+ii] = elem[ii];
	}
	
	public void append(double[] elem) {
		append(elem.length, elem);
	}
	
	public void append(Vector vector) {
		if(n+vector.n >= v.length) this.resize((int) (this.n*scale));
		for(int ii=0; ii<vector.n; ii++) v[n+ii] = vector.get(ii);
	}
	
	// Getter and setter
	public double get(int i) {		// Extract information at i
		return v[i];
	}
	
	public void set(Vector orig) {		// Assignment
		if(this!=orig) {
			if(n!=orig.n){
				n = orig.n;
				if(n>0) {
					v = new double[n];
				} else {v = null;}
			}
			
			for(int i=0; i<n; i++) v[i]=orig.get(i);
		}
	}
	
	public void set(int i, double a) {		// Assignment
		v[i] = a;
	}
	
	public double[] getDouble() {
		return v;
	}
	
	// Size	
	public int size() {return n;}
	
	// Resize
	public void resize(int nnew) {			// Resize to newn, without keeping information
		if(nnew!=n) {
			n = nnew;
			if(n>0) {
				v = new double[n];
			} else {v = null;}
		}
	}
	
	// Assign
	public void assign(int nnew, double a) {		// Resize to nnew, assign constant values
		if(nnew!=n) {
			n = nnew;
			if(n>0) {
				v = new double[n];
			}
		}
		for(int i=0; i<n; i++) v[i]=a;
	}
	
	////////////////////////
	// Vector arithmetics //
	////////////////////////
	
	public Vector times(double val) {
		Vector product = new Vector(n); 
		for(int ii=0; ii<n; ii++) {
			product.set(ii,v[ii]*val);
		}
		return product;
	}
	
}

