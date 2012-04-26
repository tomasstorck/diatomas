package NR;

public class Output<Stepper extends StepperBase> {
	static int kmax = 10;			// Influence kmax on memory and CPU investigated by Tomas 120426. Chosen static as the model gets gradually more complicated
	static double resizeFactor = 2.0;
	int nvar;
	int nsave;
	boolean dense;
	public int count;				// Public to enable reading of solutions after integration
	double x1,x2,xout,dxout;
	public Vector xsave;			// Vecdouble
	public Matrix ysave;	// Matdouble, public to enable reading of solutions after integration

	// Constructors
	public Output() {				// Default constructor
		dense = false;
		count = 0;
	}

	public Output(int nsavee) {	// Construct Output as a dense object, i.e. saving the results at nsavee equaly spaced intervals, instead of just at the values the solver thinks are useful. If nsavee <= 0, it's not a dense object
		kmax = 500;
		nsave = nsavee; 
		count = 0;
		xsave = new Vector(kmax);		// Since xsave is used to store at most kmax values, we'll make it this size
		dense = (nsave > 0) ? true : false;
	}

	// Methods
	void init(int neqn, double xlo, double xhi) { 	// Odeint constructor is said to call this one, passing resp. (i) neqn number of equations (ii) xlo starting point of integration and (iii) the xhi ending point
		nvar=neqn;
//		if (kmax == -1) return;							// No point in this IMHO (Tomas 120426)
		ysave = new Matrix(nvar,kmax);					// Initialise ysave to the correct size // was: ysave.resize(nvar,kmax); 
		if (dense) {									// If dense, we work with set limits, they're defined here
			x1=xlo;
			x2=xhi;
			xout=x1;									// Set initial xout to the lower x limit
			dxout=(x2-x1)/nsave;
		}
	}

	void resize() {										// Resize storage arrays by a factor 2, keeping the saved data (unlike NR* resize())
		int kold=kmax;
		kmax *= resizeFactor;							// Can we find a better factor? (TODO)
		Vector tempvec = new Vector(xsave);				// Copy this guy so we can extract values later on
		// Fill xsave
		xsave.resize(kmax);								// Note that this is the other resize method, WITH arguments and defined in NR* classes   
		for(int i=0; i<kold; i++) xsave.set(i,tempvec.get(i));
		// Fill ysave
		Matrix tempmat = new Matrix(ysave);
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

	void save(double x, Vector y) {					// Save y to ysave (non-dense)
//		if(kmax <= 0) return;							// We'll do that somewhere else (Tomas 260412)
		if(count == kmax) resize();						// Resize the saves if we've approached the limit (like with init())
		xsave.set(count,x);
		for(int i=0; i<nvar; i++) ysave.set(i, count, y.get(i));
		count++;
	}

	void out(int nstp, double x, Vector y, Stepper s, double h) {	// This one is called by Odeint to produce dense output. nstp is the current step number (-1 saves initial values)
		if(!dense) System.out.println("Dense output not set in Output");
		if(nstp == -1) {
			save(x,y);
			xout += dxout;
		} else {
			while((x-xout)*(x2-x1) > 0.0) {
				save_dense(s,xout,h);					// x stays the same
				xout += dxout;							// xout increases
			}
		}
	}
}