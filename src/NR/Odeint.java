/* Fundamentals of the code are from Numerical Recipes book, with changes made by Cristian and Tomas */

package NR;

import cell.CModel;

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
//	NRvector y,dydx;		// actual derivative values
	Vector ystart;		// initial values
	Output<StepperDopr853> out;		// The out structure, see way below
	feval derivs; 					// feval object containing information about the derivatives, including method to calculate them
	StepperDopr853 s;				// Our stepper

	public Odeint(Vector ystartt, double xx1, double xx2, 			// initial values, intial integration interval point, end of interval 
			double atol, double rtol, double h1, double hminn,		// absolute tolerance, relative tolerance, initial stepsize, minimum stepsize allowed
			Output<StepperDopr853> outt, feval derivss) {			// derivss should be a Dtype (=feval)
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
		Vector y 		= new Vector(nvar);
		Vector dydx	= new Vector(nvar);	// The derivative values, not as complicated as derivs
		double h = SIGN(h1,x2-x1);
		s = new StepperDopr853(y, dydx, x, h, atol, rtol, dense); // A Stepper object. No matter what, it'll contain the properties defined in StepperBase (see extend at the top)
				
		EPS = Double.MIN_VALUE;		// Should be correct, though this is a much smaller number than in C++
		for (int i = 0; i < nvar; i++)
			y.set(i,ystart.get(i));
		out.init(s.neqn, x1, x2);	// Initialise intermediate values object
	}
	
	public int integrate() throws Exception {
		derivs.Calculate(s.x,s.y,s.dydx);
		if(dense)	out.out(-1,s.x,s.y,s,s.h);	// -1 --> save initial values, see Output class. Note that h should not be obtained in out() as the syntax differs a few lines below here
		else		out.save(s.x, s.y);

		for(nstp=0; nstp<MAXSTP; nstp++) {
			if((s.x+s.h*1.0001-x2)*(x2-x1) > 0.0) 	s.h = x2-s.x;	// Make sure current h doesn't result in next x outside the interval  
			s.step(s.h,derivs); 					// Take a step with the solver. This changes various fields of the stepper
			if(s.hdid==s.h) {						// Did we succeed? Mark that down.
				++nok;
				// What else do we want to do after a successful step --> model specific! Remove this if solver applied to other model
				if(CModel.anchoring) 	CModel.AnchorUnAnchor();
				if(CModel.sticking)		CModel.StickUnStick();
			} else {
				++nbad;
			}
			
			// Save results
			if(dense){
				out.out(nstp, s.x, s.y, s, s.hdid);
			} else {
				out.save(s.x,s.y);
			}
			// Determine what to do next
			if((s.x-x2)*(x2-x1) >= 0.0 && Output.kmax>0) {				// If we're done. Criterium is kmax > 0 or we'll crash as there's nothing to save
				for(int i=0; i<nvar; i++) ystart.set(i, s.y.get(i)); 	// Update initial values for next iteration
				if(	Math.abs(out.xsave.get(out.count-1)-x2) > 100.0*Math.abs(x2)*EPS) {
					out.save(s.x,s.y);				// Save the last step					
					return nstp;					// Done.
				}
			}
			if (Math.abs(s.hnext)<=hmin) {
				throw new Exception("Warning: Step size too small in Odeint (" + s.hnext + ")");
			}
			s.h=s.hnext;							// Set stepsize and continue
		}
		throw new Exception("Too many steps in Odeint (>" + MAXSTP + ")");
	}
	
	double SIGN(double a, double b) {				// Return a if both are same sign, else return -a
		return b >= 0.0 ? (a >= 0.0 ? a : -a) : (a >= 0.0 ? -a : a);
	}

}
