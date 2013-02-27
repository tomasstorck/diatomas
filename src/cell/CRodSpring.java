package cell;

public class CRodSpring extends CSpring {
	private static final long serialVersionUID = 1L;

	///////////////////////////////////////////////////////////////////
	
	public CRodSpring(CBall ball0, CBall ball1) {
		super(ball0, ball1);
		// Add to arrays
		final CModel model = ball0.cell.model;
		model.rodSpringArray.add(this);
		ball0.cell.rodSpringArray.add(this);
	}
	
	public void ResetRestLength() {
		final CModel model = ballArray[0].cell.model;
		// If type == 1 based on mass, type==2 based on max mass
		if(ballArray[0].cell.type<4) {
			restLength = ballArray[0].radius * model.lengthCellMax[ballArray[0].cell.type]/model.radiusCellMax[ballArray[0].cell.type];							// About 2 balls in the same cell, so no need to make it complicated  
		} else if (ballArray[0].cell.type<6) {
			restLength = ballArray[0].cell.GetAmount()*model.MWX/(Math.PI*model.rhoX*ballArray[0].radius*ballArray[0].radius) - 4.0/3.0*ballArray[0].radius;
		} else {
			throw new IndexOutOfBoundsException("Cell type: " + ballArray[0].cell.type);
		}
	}

	public void ResetK() {
		final CModel model = ballArray[0].cell.model;
		this.K = model.Kr;
	}
	
	// Left out Break(), CRodSpring can't break
	
	public int Index() {
		final CModel model = ballArray[0].cell.model;
		return super.Index(model.rodSpringArray);
	}
}
