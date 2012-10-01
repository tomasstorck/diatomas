// Documentation in Vector is more extensive

package NR;

public class Matrix {	// It's NOT a template class anymore
	int m; 				// Size of the array, since Java and C++ are 0 index based the last element is at nn-1
	int n;
	double[][] v;		 	// Actual content

	/////////////////////////////////////////////////////////////////	

	public Matrix() {						// Empty 
		m = 0;								// rows
		n = 0;								// columns
		v = new double[][]{{0.0}};
	}

	public Matrix(int rows, int cols) {			// Just of the correct size, empty. 
		m = rows;
		n = cols;
		if(cols*rows>0) {
			v = new double[m][n];
		} else {v = null;}
	}

	public Matrix(int rows, int cols, double[][] a) {	// Correct size, based on the T[]'s elements
		m = rows;
		n = cols;
		if(n*m>0) {
			v = new double[m][n];
		} else {v = null;}
		
		for(int i=0; i<m; i++) for(int j=0; j<n; j++) v[i][j] = a[i][j];
	}
	
	public Matrix(double[][] a) {
		m = a.length;
		n = a[0].length;
		if(n*m>0) {
			v = new double[m][n];
		} else {v = null;}
		
		for(int i=0; i<m; i++) for(int j=0; j<n; j++) v[i][j] = a[i][j];
	}
	
	public Matrix(Matrix orig) {					// Clone AKA copy constructor
		m = orig.m;
		n = orig.n;
		if(n*m>0) {
			v = new double[m][n];
		} else {v = null;}
		
		for(int i=0; i<m; i++) for(int j=0; j<n; j++) v[i][j] = orig.get(i,j);
	}
	
	/////////////////////////////////////////////////////////////////
	
	// Getter and setter
	public double get(int i, int j) { 				// Extract information at i
		return v[i][j];
	}
	
	public void set(Matrix orig) {		// Assignment
		if(this!=orig) {
			if(m!=orig.m || n!=orig.n){
				m = orig.m;
				n = orig.n;
				if(m*n>0) {
					v = new double[m][n];
				} else {v = null;}
				for(int i=0; i<m; i++) for(int j=0; i<n; j++) v[i][j]=orig.get(i,j);
			}

		}
	}
	
	public void set(int i, int j, double elem) {		// Assignment
		v[i][j] = elem;
	}
	
	public double[][] getDouble() {
		return v;
	}
	
	public Vector getRow(int i) {						// Returns a Vector with the row
		return new Vector(v[i]);
	}
	
	public Vector getCol(int i) {						// WARNING: better not to use this one but transpose instead if you need to use more than one call to this function 
		double[] col = new double[m];
		for(int ii=0; ii<m; ii++) {
			col[ii] = v[ii][i];
		}
		return new Vector(col);
	}
	
	public Matrix getTranspose() {
		Matrix trans = new Matrix(n,m);
		for(int i=0; i<m; i++) {
			for(int j=0; j<n; j++) 
				trans.set(j, i, this.get(i, j));
		}
		return trans;
	}
	
	// Size
	public int nrows() {return m;}
	
	public int ncols() {return n;}
	
	// Resize
	public void resize(int mnew, int nnew) {			// Resize to newn, without keeping information
		if(mnew!=m || nnew!=n) {
			m = mnew;
			n = nnew;
			if(mnew*nnew>0) {
				v = new double[m][n];
			} else {v = null;}
		}
	}
	
	// Assign
	public void assign(int mnew, int m2new, double elem) {		// Resize, assign constant values
		if(mnew!=m || m2new!=n) {
			m = mnew;
			n = m2new;
			if(m*n>0) {
				v = new double[m][n];
			}
		}
		for(int i=0; i<m; i++) for(int j=0; j<n; i++) v[i][j]=elem;
	}
	
	
}

