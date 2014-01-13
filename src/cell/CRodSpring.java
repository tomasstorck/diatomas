package cell;

public class CRodSpring extends CSpring {
	private static final long serialVersionUID = 1L;

	///////////////////////////////////////////////////////////////////
	
	public CRodSpring(CBall ball0, CBall ball1) {
		ballArray = new CBall[2];
		ballArray[0] = ball0;
		ballArray[1] = ball1;
		ResetK();
		ResetRestLength();
		// Add to arrays
		final CModel model = ball0.cell.model;
		model.rodSpringArray.add(this);
		ball0.cell.rodSpringArray.add(this);
	}
	
	public static double RestLength(double radius, double amount, int type, CModel model) {
		// If type == 1 based on mass, type==2 based on max mass
		if(type<4) {
			return radius * model.lengthCellMax[type]/model.radiusCellMax[type];							// About 2 balls in the same cell, so no need to make it complicated  
		} else if (type<6) {
			return amount*model.MWX/(Math.PI*model.rhoX*radius*radius) - 4.0/3.0*radius;
		} else {
			throw new IndexOutOfBoundsException("Cell type: " + type);
		}
	}
	
	public void ResetRestLength() {
		CBall ball0 = ballArray[0];
		CCell cell0 = ballArray[0].cell;
		final CModel model = cell0.model;
		restLength = RestLength(ball0.radius, cell0.GetAmount(), cell0.type, model);
	}

	public void ResetK() {
		final CModel model = ballArray[0].cell.model;
		this.K = model.Kr;
	}
	
	// Left out Break(), CRodSpring can't break
	
	public Vector3d GetL() {
		return ballArray[1].pos.minus(ballArray[0].pos);
	}
	
	public int Index() {
		final CModel model = ballArray[0].cell.model;
		return super.Index(model.rodSpringArray);
	}
}
