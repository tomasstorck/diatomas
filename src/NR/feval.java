package NR;

import cell.CModel;

public class feval {
	CModel model;
	
	public feval(CModel model) {
		this.model = model;
	}

	@SuppressWarnings("unused")
	public void Calculate(double t, Vector yode, Vector dydx) {
		dydx.set(model.CalculateForces(yode));
	}
}
