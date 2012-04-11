package NR;

public class NRmatrix<T> {	// <T> --> It's a template class, like ArrayList
	int nn; 				// Size of the array, since Java and C++ are 0 index based the last element is at nn-1
	int mm;
	T[][] v;					// Actual content

	/////////////////////////////////////////////////////////////////	

	public NRmatrix() {						// Empty 
		nn = 0;								// rows
		mm = 0;								// columns
		v = null;
	}

	@SuppressWarnings("unchecked")
	public NRmatrix(int n, int m) {			// Just of the correct size, empty. 
		nn = n;
		mm = m;
		if(m*n>0) {
			v = (T[][]) new Object[nn][mm];
		} else {v = null;}
	}

	@SuppressWarnings("unchecked")
	public NRmatrix(int n, int m, T[][] a) {	// Correct size, based on the T[]'s elements
		nn = n;
		mm = m;
		if(m*n>0) {
			v = (T[][]) new Object[nn][mm];
		} else {v = null;}
		
		for(int i=0; i<n; i++) for(int j=0; j<m; j++) v[i][j] = a[i][j];
	}

	@SuppressWarnings("unchecked")
	public NRmatrix(NRmatrix<T> rhs) {		// Clone AKA copy constructor
		nn = rhs.nn;
		mm = rhs.mm;
		if(mm*nn>0) {
			v = (T[][]) new Object[nn][mm];
		} else {v = null;}
		
		for(int i=0; i<nn; i++) for(int j=0; j<mm; j++) v[i][j] = rhs.get(i,j);
	}
	
	/////////////////////////////////////////////////////////////////
	
	public T get(int i, int j) { 				// Extract information at i
		return v[i][j];
	}
	
	@SuppressWarnings("unchecked")			// Not sure about the use of this one TODO
	public void set(NRmatrix<T> rhs) {		// Assignment
		if(this!=rhs) {
			if(nn!=rhs.nn || mm!=rhs.mm){
				// if(v!=null) delete[] v; TODO
				nn = rhs.nn;
				mm = rhs.mm;
				if(nn*mm>0) {
					v = (T[][]) new Object[nn][mm];
				} else {v = null;}
				for(int i=0; i<nn; i++) for(int j=0; i<mm; j++) v[i][j]=(T) rhs.get(i,j);
			}

		}
	}
	
	public void set(int i, int j, T a) {		// Assignment
		v[i][j] = a;
	}
	
	public int nrows() {return nn;}
	
	public int ncols() {return mm;}
	
	@SuppressWarnings("unchecked")
	public void resize(int newn, int newm) {			// Resize to newn, without keeping information
		if(newn!=nn || newm!=mm) {
			// if(v!=null) delete[] v; TODO
			nn = newn;
			mm = newm;
			if(newn*newm>0) {
				v = (T[][]) new Object[nn][mm];
			} else {v = null;}
		}
	}
	
	@SuppressWarnings("unchecked")
	public void assign(int newn, int newm, T a) {		// Resize to newn, assign constant values
		if(newn!=nn || newm!=mm) {
			// if(v!=null) delete[] v; TODO
			nn = newn;
			mm = newm;
			if(nn*mm>0) {
				v = (T[][]) new Object[nn][mm];
			}
		}
		for(int i=0; i<nn; i++) for(int j=0; j<mm; i++) v[i][j]=a;
	}
}

