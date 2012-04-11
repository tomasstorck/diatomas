package NR;

public class StepperBase {
		double x;
		double xold;
		NRvector<Double> y, dydx;
		double atol,rtol;
		boolean dense;
		double hdid;
		double hnext;
		double EPS;
		int n,neqn;
		NRvector<Double> yout,yerr;
		
		public StepperBase(NRvector<Double> yy, NRvector<Double> dydxx, double xx, 
				double atoll, double rtoll, boolean dens) {
			x = xx;				// Should be a reference FIXME
			y = yy;				// Is a reference
			dydx = dydxx;		// Is a reference
			atol = atoll;
			rtol = rtoll;
			dense = dens;
			n = y.size();
			neqn = n;			// Not true for StepperStoerm, but that'll be overwritten
			yout = new NRvector<Double>(n);	// New value for y
			yerr = new NRvector<Double>(n);	// ... and for the error estimate
		};
		
		public StepperBase(double x, NRvector<Double> y, NRvector<Double> dydx) {};
		
		// "Forward declare" the functions  below, they'll be overwritten by <Stepper>, perhaps we can work around this TODO
		public void step(double h, feval derivs) {}
		public void dy(double h, feval derivs) {}
		public void prepare_dense(double h, NRvector<Double> dydxnew, feval derivs) {}
		public double dense_out(int i,double xout, double h) {return 0.0;} 
		public double error(double h) {return 0.0;} 
}
