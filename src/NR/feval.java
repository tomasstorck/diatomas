package NR;

import cell.CModel;

public class feval {
	
	public feval() {}

	public void Calculate(double t, Vector yode, Vector dydx) {
		dydx.set(CModel.CalculateForces(t, yode));
	}
}
