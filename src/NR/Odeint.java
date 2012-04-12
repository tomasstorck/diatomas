package NR;

public class Odeint<Stepper extends StepperBase> {
	static int MAXSTP = 5000000;	// == 5e6, but int doesn't like e.
	double EPS;						// tolerance
	int nstp;						// Total number of steps
	int nok;						// number of successful steps
	int nbad;						// number of failed steps
	int nvar;						// number of variables
	double x1,x2,hmin;				// time steps?
//	double x,h;
	boolean dense;					// whether or not we want a dense output, i.e. values at certain x points
//	NRvector<Double> y,dydx;		// actual derivative values
	NRvector<Double> ystart;		// initial values
	Output<StepperDopr853> out;		// The out structure, see way below // TODO make generic
	feval derivs; 					// feval object containing information about the derivatives, including method to calculate them
	StepperDopr853 s;				// Our stepper // TODO make generic

	public Odeint(NRvector<Double> ystartt, double xx1, double xx2, 	// initial values, intial integration interval point, end of interval 
			double atol, double rtol, double h1, double hminn,			// absolute tolerance, relative tolerance, initial stepsize, minimum stepsize allowed
			Output<StepperDopr853> outt, feval derivss) {							// derivss should be a Dtype (=feval), I think FIXME
		nvar	= ystartt.size();
		ystart	= ystartt;			// Should be a ref based on C++ code
		nok 	= 0;
		nbad 	= 0;				// But retried and fixed
		x1 		= xx1;
		x2 		= xx2;
		hmin 	= hminn;
		dense 	= outt.dense;
		out		= outt;				// Takes care of intermediate values
		derivs	= derivss;			// feval object, takes care of solving
		
		// Construct stepper with temporary variables
		double x 		= xx1;
		NRvector<Double> y 		= new NRvector<Double>(nvar);
		NRvector<Double> dydx	= new NRvector<Double>(nvar);	// The derivative values, not as complicated as derivs
		double h = SIGN(h1,x2-x1);
		s 		= new StepperDopr853(y, dydx, x, h, atol, rtol, dense); // x should be the same x as in this class FIXME! A Stepper object. No matter what, it'll contain the properties defined in StepperBase (see extend at the top)	// TODO make generic
				
		EPS = Double.MIN_VALUE;		// Not sure about this, FIXME
		for(int i=0; i<nvar; i++) y.set(i,ystart.get(i));
		out.init(s.neqn, x1, x2);	// Initialise intermediate values object
	}
	
	public void integrate() {
		derivs.calculate(s.x,s.y,s.dydx);
		if(dense)	out.out(-1,s.x,s.y,s,s.h);	// -1 --> save initial values, see Output class. Note that h should not be obtained in out() as the syntax differs a few lines below here
		else		out.save(s.x, s.y);

		for(nstp=0; nstp<MAXSTP; nstp++) {
			if((s.x+s.h*1.0001-x2)*(x2-x1) > 0.0) 	s.h = x2-s.x;	// Parameterise 1.0001? Set the stepsize h for next try 
			s.step(derivs); 					// Take a step with the solver. This changes various fields of the stepper
			if(s.hdid==s.h) ++nok; else ++nbad;	// Did we succeed? Mark that down.
			// Save results
			if(dense) 	out.out(nstp, s.x, s.y, s, s.hdid);
			else 		out.save(s.x,s.y);
			// Determine what to do next
			if((s.x-x2)*(x2-x1) >= 0.0) {			// If we're done
				for(int i=0; i<nvar; i++) ystart.set(i, s.y.get(i)); // Update initial values for next iteration
				if(out.kmax>0 && 
						Math.abs(out.xsave.get(out.count-1)-x2) > 
						100.0*Math.abs(x2)*EPS) {
					out.save(s.x,s.y);				// Save the last step					
					return;						// Done.
				}
			}
			if (Math.abs(s.hnext)<=hmin) System.out.println("Warning: Step size too small in Odeint (" + s.hnext + ")");	// Make throw() TODO
			s.h=s.hnext;							// Set stepsize and continue
		}
		System.out.println("Too many steps in Odeint (>" + MAXSTP + ")");
	}
	
	double SIGN(double a, double b) {			// return a if both are same sign, else return -a
		return b >= 0.0 ? (a >= 0.0 ? a : -a) : (a >= 0.0 ? -a : a);
	}

}
