package ibm;

public class RodSpring extends Spring {
	private static final long serialVersionUID = 1L;

	///////////////////////////////////////////////////////////////////
	
	public RodSpring(Ball ball0, Ball ball1) {
		ballArray = new Ball[2];
		ballArray[0] = ball0;
		ballArray[1] = ball1;
		ResetK();
		ResetRestLength();
		// Add to arrays
		final Model model = ball0.cell.model;
		model.rodSpringArray.add(this);
		ball0.cell.rodSpringArray.add(this);
	}
	
	public static double RestLength(double radius, double amount, int type, Model model) {
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
		Ball ball0 = ballArray[0];
		Cell cell0 = ballArray[0].cell;
		final Model model = cell0.model;
		restLength = RestLength(ball0.radius, cell0.GetAmount(), cell0.type, model);
	}

	public void ResetK() {
		final Model model = ballArray[0].cell.model;
		this.K = model.Kr;
	}
	
	public int Break() {
		Model model = ballArray[0].cell.model;
		boolean isRemovedModel = model.rodSpringArray.remove(this);
		boolean isRemovedCell = this.ballArray[0].cell.rodSpringArray.remove(this);
		if(isRemovedModel && isRemovedCell) {
			return 1;
		}
		else {
			throw new RuntimeException("RodSpring " + this.Index() + " was not present and could not be removed");
		}
	}
	
	public Vector3d GetL() {
		return ballArray[1].pos.minus(ballArray[0].pos);
	}
	
	public int Index() {
		final Model model = ballArray[0].cell.model;
		return super.Index(model.rodSpringArray);
	}
}
