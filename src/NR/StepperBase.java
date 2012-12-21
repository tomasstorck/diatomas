package NR;


public class StepperBase {
		double x;
		double xold;
		Vector y, dydx;
		double h;
		double atol,rtol;
		boolean dense;
		double hdid;
		double hnext;
		double EPS;
		int n,neqn;
		Vector yout,yerr;
		
		public StepperBase(Vector yy, Vector dydxx, double xx, 
				double hh, double atoll, double rtoll, boolean dens) {
			x = xx;				// Is a reference in C++, now replaced by a single, localised double. What was a pointer now refers to this one directly
			y = yy;				// Is a reference
			dydx = dydxx;		// Is a reference
			h = hh;
			atol = atoll;
			rtol = rtoll;
			dense = dens;
			n = y.size();
			neqn = n;			// Not true for StepperStoerm, but that'll be overwritten
			yout = new Vector(n);	// New value for y
			yerr = new Vector(n);	// ... and for the error estimate
		};
		
		@SuppressWarnings("unused")
		public StepperBase(double x, Vector y, Vector dydx) {};
		
		// "Forward declare" the functions  below, they'll be overwritten by the actual Stepper
		@SuppressWarnings("unused")
		public void step(double h, feval derivs) throws Exception {}
		@SuppressWarnings("unused")
		public void dy(double h, feval derivs) {}
		@SuppressWarnings("unused")
		public void prepare_dense(double h, Vector dydxnew, feval derivs) {}
		@SuppressWarnings("unused")
		public double dense_out(int i,double xout, double h) {return 0.0;}
		@SuppressWarnings("unused")
		public double error(double h) {return 0.0;} 
}
