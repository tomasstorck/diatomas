package NR;

public class Output<Stepper extends StepperBase> {
	int kmax;
	int nvar;
	int nsave;
	boolean dense;
	public int count;				// Public to enable reading of solutions after integration
	double x1,x2,xout,dxout;
	NRvector<Double> xsave;			// Vecdouble
	public NRmatrix<Double> ysave;	// Matdouble, public to enable reading of solutions after integration

	// Constructors
	public Output() {				// Default constructor
		kmax = -1;
		dense = false;
		count = 0;
	}

	public Output(int nsavee) {	// Construct Output as a dense object, i.e. saving the results at nsavee equaly spaced intervals, instead of just at the values the solver thinks are useful. If nsavee <= 0, it's not a dense object   
		kmax = 500;					// I suppose that when nsavee exceeds kmax, we're in trouble. Higher kmax --> more memory use, possibly lower performance TODO
		nsave = nsavee; 
		count = 0;
		xsave = new NRvector<Double>(kmax);		// Since xsave is used to store at most kmax values, we'll make it this size
		dense = (nsave > 0) ? true : false;
	}

	// Methods
	void init(int neqn, double xlo, double xhi) { 	// Odeint constructor is said to call this one, passing resp. (i) neqn number of equations (ii) xlo starting point of integration and (iii) the xhi ending point
		nvar=neqn;
		if (kmax == -1) return;
		ysave = new NRmatrix<Double>(nvar,kmax);		// Initialise ysave to the correct size // was: ysave.resize(nvar,kmax); 
		if (dense) {									// If dense, we work with set limits, they're defined here
			x1=xlo;
			x2=xhi;
			xout=x1;									// Set initial xout to the lower x limit
			dxout=(x2-x1)/nsave;
		}
	}

	void resize() {										// Resize storage arrays by a factor 2, keeping the saved data (unlike NR* resize())
		int kold=kmax;
		kmax *= 2;										// Can we find a better factor? (TODO)
		NRvector<Double> tempvec = new NRvector<Double>(xsave);	// Copy this guy so we can extract values later on
		// Fill xsave
		xsave.resize(kmax);								// Note that this is the other resize method, WITH arguments and defined in NR* classes   
		for(int i=0; i<kold; i++) xsave.set(i,tempvec.get(i));
		// Fill ysave
		NRmatrix<Double> tempmat = new NRmatrix<Double>(ysave);
		ysave.resize(nvar,kmax);
		for (int i=0; i<nvar; i++) for (int k=0; k<kold; k++) ysave.set(i,k,tempmat.get(i,k));
	}

	void save_dense(Stepper s, double xout, double h) {	// Save y to ysave (dense). Produces the output at xout. h is used as the step size: xold < xout < xold+h. Often called from out method below, not directly.
		if(count == kmax) resize();						// Resize the ysave if we've approached the limit (like with init())
		xsave.set(count,xout);
		for(int i=0; i<nvar; i++) ysave.set(i, count, s.dense_out(i,xout,h));
		count++;
//		pModel.movement_time = xout; // Important? FIXME
	}

	void save(double x, NRvector<Double> y) {			// Save y to ysave (non-dense)
		if(kmax <= 0) return;
		if(count == kmax) resize();						// Resize the saves if we've approached the limit (like with init())
		xsave.set(count,x);
		for(int i=0; i<nvar; i++) ysave.set(i, count, y.get(i));
		count++;
	}

	void out(int nstp, double x, NRvector<Double> y, Stepper s, double h) {	// This one is called by Odeint to produce dense output. nstp is the current step number (-1 saves initial values)
		if(!dense) System.out.println("Dense output not set in Output");
		if(nstp == -1) {
			save(x,y);
			xout += dxout;
		} else {
			while((x-xout)*(x2-x1) > 0) {
				save_dense(s,xout,h);					// x stays the same
				xout += dxout;							// xout increases
			}
		}
	}
}