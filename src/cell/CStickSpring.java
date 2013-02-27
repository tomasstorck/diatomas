package cell;

public class CStickSpring extends CSpring {
	private static final long serialVersionUID = 1L;

	///////////////////////////////////////////////////////////////////
	
	public CStickSpring(CBall ball0, CBall ball1) {
		super(ball0, ball1);
		// Add to arrays
		final CModel model = ball0.cell.model;
		model.stickSpringArray.add(this);
		ball0.cell.stickSpringArray.add(this);
		ball1.cell.stickSpringArray.add(this);
		ball0.cell.stickCellArray.add(ball1.cell);
		ball1.cell.stickCellArray.add(ball0.cell);
	}
	
	public void ResetRestLength() {
		restLength = Math.max(
				ballArray[1].pos.minus(ballArray[0].pos).norm(),		// The rest length we desire
				1.0*(ballArray[0].radius + ballArray[1].radius));		// But we want the spring not to cause cell overlap in relaxed state
	}
	
	public void ResetK() {
		final CModel model = ballArray[0].cell.model;
		double springDiv;
		CCell cell0 = ballArray[0].cell;
		CCell cell1 = ballArray[1].cell;
		if(cell0.type<2 && cell1.type<2)		springDiv = 1.0;
		else if(cell0.type>1 && cell1.type>1) {
			if(cell0.type<6 && cell1.type<6) 	springDiv = 4.0;
			else throw new IndexOutOfBoundsException("Cell types: " + cell0.type + " and " + cell1.type);
		} else 									springDiv = 2.0;
		K = model.Ks/springDiv;
	}
	
	public int Break() {
		final CModel model = ballArray[0].cell.model;
		int count = 0;
		CCell cell0 = ballArray[0].cell;
		CCell cell1 = ballArray[1].cell;
		// Tell cells they're no longer stuck to each other
		cell0.stickCellArray.remove(cell1);
		cell1.stickCellArray.remove(cell0);	
		// Remove this spring and siblings from model and cells
		count += (model.stickSpringArray.remove(this))?1:0;			// Add one to counter if successfully removed
		cell0.stickSpringArray.remove(this);
		cell1.stickSpringArray.remove(this);
		for(CSpring sibling : siblingArray) {
			cell0.stickSpringArray.remove(sibling);
			cell1.stickSpringArray.remove(sibling);
			count += (model.stickSpringArray.remove(sibling))?1:0;
		}
		return count;
	}
	
	public int Index() {
		final CModel model = ballArray[0].cell.model;
		return super.Index(model.stickSpringArray);
	}
}
