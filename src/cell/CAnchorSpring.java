package cell;

public class CAnchorSpring extends CSpring {
	private static final long serialVersionUID = 1L;
	public Vector3d anchorPoint = new Vector3d();

	///////////////////////////////////////////////////////////////////
	
	public CAnchorSpring(CBall ball0, Vector3d anchorPoint) {
		super(ball0, anchorPoint);
		// We don't use the super constructor here: CAnchorSpring is different
		ballArray = new CBall[1];
		ballArray[0] = ball0;
		this.anchorPoint = anchorPoint;
		ResetK();
		ResetRestLength();
		// Add to arrays
		final CModel model = ball0.cell.model;
		model.anchorSpringArray.add(this);
		ball0.cell.anchorSpringArray.add(this);
	}
	
	public void ResetRestLength() {
		CBall ball = ballArray[0];
		restLength = Math.max(ball.pos.y,ball.radius*1.01);				// WORKAROUND: Choose current position, but make sure it is not forcing the spring into the substratum
	}
	
	public void ResetK() {
		final CModel model = ballArray[0].cell.model;
		double springDiv;
		CCell cell = ballArray[0].cell;
		if(cell.type<2)						springDiv = 1.0;
		else if(cell.type<6)				springDiv = 2.0;
		else throw new IndexOutOfBoundsException("Cell type: " + cell.type);
		K = model.Kan/springDiv;
	}
	
	public int Break() {
		final CModel model = ballArray[0].cell.model;
		int count = 0;
		CCell cell0 = ballArray[0].cell;
		// Remove this and siblings from model and cells			// Anchoring springs
		count += (model.anchorSpringArray.remove(this))?1:0;		// Add one to counter if successfully removed
		cell0.anchorSpringArray.remove(this);
		for(CSpring sibling : siblingArray) {
			cell0.anchorSpringArray.remove(sibling);
			count += (model.anchorSpringArray.remove(sibling))?1:0;
		}
		return count;
	}
	
	public int Index() {
		final CModel model = ballArray[0].cell.model;
		return super.Index(model.anchorSpringArray);
	}
	
	public Vector3d GetL() {
		return ballArray[0].pos.minus(anchorPoint);
	}
}
