// Documentation in Vector is more extensive

package NR;

public class Matrix {	// It's NOT a template class anymore
	int nn; 				// Size of the array, since Java and C++ are 0 index based the last element is at nn-1
	int mm;
	double[][] v;		 	// Actual content

	/////////////////////////////////////////////////////////////////	

	public Matrix() {						// Empty 
		nn = 0;								// rows
		mm = 0;								// columns
		v = null;
	}

	public Matrix(int n, int m) {			// Just of the correct size, empty. 
		nn = n;
		mm = m;
		if(m*n>0) {
			v = new double[nn][mm];
		} else {v = null;}
	}

	public Matrix(int n, int m, double[][] a) {	// Correct size, based on the T[]'s elements
		nn = n;
		mm = m;
		if(m*n>0) {
			v = new double[nn][mm];
		} else {v = null;}
		
		for(int i=0; i<n; i++) for(int j=0; j<m; j++) v[i][j] = a[i][j];
	}

	public Matrix(Matrix rhs) {					// Clone AKA copy constructor
		nn = rhs.nn;
		mm = rhs.mm;
		if(mm*nn>0) {
			v = new double[nn][mm];
		} else {v = null;}
		
		for(int i=0; i<nn; i++) for(int j=0; j<mm; j++) v[i][j] = rhs.get(i,j);
	}
	
	/////////////////////////////////////////////////////////////////
	
	public double get(int i, int j) { 				// Extract information at i
		return v[i][j];
	}
	
	public void set(Matrix rhs) {		// Assignment
		if(this!=rhs) {
			if(nn!=rhs.nn || mm!=rhs.mm){
				// if(v!=null) delete[] v; TODO
				nn = rhs.nn;
				mm = rhs.mm;
				if(nn*mm>0) {
					v = new double[nn][mm];
				} else {v = null;}
				for(int i=0; i<nn; i++) for(int j=0; i<mm; j++) v[i][j]=rhs.get(i,j);
			}

		}
	}
	
	public void set(int i, int j, double a) {		// Assignment
		v[i][j] = a;
	}
	
	public int nrows() {return nn;}
	
	public int ncols() {return mm;}
	
	public void resize(int newn, int newm) {			// Resize to newn, without keeping information
		if(newn!=nn || newm!=mm) {
			nn = newn;
			mm = newm;
			if(newn*newm>0) {
				v = new double[nn][mm];
			} else {v = null;}
		}
	}
	
	public void assign(int newn, int newm, double a) {		// Resize to newn, assign constant values
		if(newn!=nn || newm!=mm) {
			// if(v!=null) delete[] v; TODO
			nn = newn;
			mm = newm;
			if(nn*mm>0) {
				v = new double[nn][mm];
			}
		}
		for(int i=0; i<nn; i++) for(int j=0; j<mm; i++) v[i][j]=a;
	}
}

