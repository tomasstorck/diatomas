package NR;

import cell.CModel;

public class feval {
	CModel model;
	
	public feval(CModel model) {
		this.model = model; 
	}

	public void calculate(double t, NRvector<Double> y, NRvector<Double> dydx) {
		dydx.set(model.CalculateForces(t, y));
	}
}
