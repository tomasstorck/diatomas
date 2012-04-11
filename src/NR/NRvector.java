package NR;

public class NRvector<T> {	// It's a template class, like ArrayList
	int nn; 				// Size of the array, since Java and C++ are 0 index based the last element is at nn-1
	T[] v;					// Actual content

	/////////////////////////////////////////////////////////////////	

	public NRvector() {						// Empty 
		nn = 0;
		v = null;
	}

	@SuppressWarnings("unchecked")
	public NRvector(int n) {				// Just of the correct size, empty (null values). 
		nn = n;
		if(n>0) {
			v = (T[]) new Object[n];
		} else {v = null;}
	}

	@SuppressWarnings("unchecked")
	public NRvector(int n, T a) {			// Correct size, all based on the value of a elements
		nn = n;
		if(n>0) {
			v = (T[]) new Object[n];
		} else {v = null;}
		
		for(int i=0; i<n; i++) v[i] = a;
	}
	
	@SuppressWarnings("unchecked")
	public NRvector(int n, T[] a) {			// Correct size, based on the T[]'s elements
		nn = n;
		if(n>0) {
			v = (T[]) new Object[n];
		} else {v = null;}
		
		for(int i=0; i<n; i++) v[i] = a[i];
	}

	@SuppressWarnings("unchecked")
	public NRvector(NRvector<T> rhs) {		// Clone AKA copy constructor
		nn = rhs.nn;
		if(nn>0) {
			v = (T[]) new Object[nn];
		} else {v = null;}
		
		for(int i=0; i<nn; i++) v[i] = rhs.get(i);
	}
	
	/////////////////////////////////////////////////////////////////
	
	public T get(int i) {					// Extract information at i
		return v[i];
	}
	
	@SuppressWarnings("unchecked")
	public void set(NRvector<T> rhs) {		// Assignment
		if(this!=rhs) {
			if(nn!=rhs.nn){
				// if(v!=null) delete[] v; TODO
				nn = rhs.nn;
				if(nn>0) {
					v = (T[]) new Object[nn];
				} else {v = null;}
			}
			for(int i=0; i<nn; i++) v[i]=(T) rhs.get(i);
		}
	}
	
	public void set(int i, T a) {		// Assignment
		v[i] = a;
	}
	
	public int size() {return nn;}
	
	@SuppressWarnings("unchecked")
	public void resize(int newn) {			// Resize to newn, without keeping information
		if(newn!=nn) {
			// if(v!=null) delete[] v; TODO
			nn = newn;
			if(nn>0) {
				v = (T[]) new Object[nn];
			} else {v = null;}
		}
	}
	
	@SuppressWarnings("unchecked")
	public void assign(int newn, T a) {		// Resize to newn, assign constant values
		if(newn!=nn) {
			// if(v!=null) delete[] v; TODO
			nn = newn;
			if(nn>0) {
				v = (T[]) new Object[nn];
			}
		}
		for(int i=0; i<nn; i++) v[i]=a;
	}
}

