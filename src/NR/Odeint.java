package NR;

public class Odeint<Stepper extends StepperBase> {
	static int MAXSTP = 5000000;	// == 5e6, but int doesn't like e.
	double EPS;						// tolerance
	int nstp;						// Total number of steps
	int nok;						// number of successful steps
	int nbad;						// number of failed steps
	int nvar;						// number of variables
	double x1,x2,hmin;				// time steps?
	double x,h;
	boolean dense;					// whether or not we want a dense output, i.e. values at certain x points
	NRvector<Double> y,dydx;		// actual derivative values
	NRvector<Double> ystart;		// initial values
	Output<StepperDopr853> out;		// The out structure, see way below // TODO make generic
	feval derivs; 					// feval object containing information about the derivatives, including method to calculate them
	StepperDopr853 s;				// Our stepper // TODO make generic

	public Odeint(NRvector<Double> ystartt, double xx1, double xx2, 	// initial values, intial integration interval point, end of interval 
			double atol, double rtol, double h1, double hminn,			// absolute tolerance, relative tolerance, initial stepsize, minimum stepsize allowed
			Output<StepperDopr853> outt, feval derivss) {							// derivss should be a Dtype (=feval), I think FIXME
		nvar	= ystartt.size();
		y 		= new NRvector<Double>(nvar);
		dydx	= new NRvector<Double>(nvar);	// The derivative values, not as complicated as derivs
		ystart	= ystartt;			// Should be a ref based on C++ code
		x 		= xx1;
		nok 	= 0;
		nbad 	= 0;				// But retried and fixed
		x1 		= xx1;
		x2 		= xx2;
		hmin 	= hminn;
		dense 	= outt.dense;
		out		= outt;				// Should be a ref. Takes care of intermediate values, copied perhaps FIXME
		derivs	= derivss;				// should be a ref. feval object, takes care of solving. Needs to be copied perhaps? FIXME
		s 		= new StepperDopr853(y, dydx, x, atol, rtol, dense); // x should be the same x as in this class FIXME! A Stepper object. No matter what, it'll contain the properties defined in StepperBase (see extend at the top)	// TODO make generic
				
		EPS = Double.MIN_VALUE;		// Not sure about this, FIXME
		h = SIGN(h1,x2-x1);
		for(int i=0; i<nvar; i++) y.set(i,ystart.get(i));
		out.init(s.neqn, x1, x2);	// Initialise intermediate values object
	}
	
	public void integrate() {
		derivs.calculate(x,y,dydx);
		if(dense)	out.out(-1,x,y,s,h);	// -1 --> save initial values, see Output class
		else		out.save(x, y);

		for(nstp=0; nstp<MAXSTP; nstp++) {
			if((x+h*1.0001-x2)*(x2-x1) > 0.0) 	h = x2-x;	// Parameterise 1.0001? What's going on here? TODO
			s.step(h,derivs); 					// Take a step with the solver
			if(s.hdid==h) ++nok; else ++nbad;	// Did we succeed? Mark that down.
			// Save results
			if(dense) 	out.out(nstp, x, y, s, s.hdid);
			else 		out.save(x,y);
			// Determine what to do next
			if((x-x2)*(x2-x1) >= 0.0) {			// If we're done			// FIXME current problem: x is always 0!!!!!
				for(int i=0; i<nvar; i++) ystart.set(i, y.get(i)); // Update initial values for next iteration
				if(out.kmax>0 && 
						Math.abs(out.xsave.get(out.count-1)-x2) > 
						100.0*Math.abs(x2)*EPS) {
					out.save(x,y);				// Save the last step					
					return;						// Done.
				}
			}
			if (Math.abs(s.hnext)<=hmin) System.out.println("Warning: Step size too small in Odeint (" + s.hnext + ")");	// Make throw() TODO
			h=s.hnext;							// Set stepsize and continue
		}
		System.out.println("Too many steps in Odeint (>" + MAXSTP + ")");
	}
	
	double SIGN(double a, double b) {			// return a if both are same sign, else return -a
		return b >= 0 ? (a >= 0 ? a : -a) : (a >= 0 ? -a : a);
	}

}
