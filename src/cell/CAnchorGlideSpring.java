package cell;

import java.util.ArrayList;

public class CAnchorGlideSpring extends CAnchorSpring {
	private static final long serialVersionUID = 1L;
//	public Vector3d anchorPoint = new Vector3d();
	public ArrayList<CAnchorSpring> siblingArray = new ArrayList<CAnchorSpring>(2);

	///////////////////////////////////////////////////////////////////
	
	public CAnchorGlideSpring(CBall ball0, Vector3d anchorPoint) {
		super(ball0, anchorPoint);									// WORKAROUND: this probably needs fixing, if CAnchorGlideSpring will indeed be a subclass of CAnchorSpring
		// We don't use the super constructor here: CAnchorSpring is different
		ballArray = new CBall[1];
		ballArray[0] = ball0;
//		this.anchorPoint = anchorPoint;
		ResetK();
		ResetRestLength();
		// Add to arrays
		final CModel model = ball0.cell.model;
		model.anchorSpringArray.add(this);
		ball0.cell.anchorSpringArray.add(this);
	}
	
	public static double RestLength(double height, double radius) {
		return Math.max(height,radius*1.01);						// WORKAROUND: Choose current position, but make sure it is not forcing the spring into the substratum
	}
	
	public void ResetRestLength() {
		final CBall ball = ballArray[0];							// Final because anchoring springs are not recycled
		restLength = RestLength(ball.pos.y, ball.radius);
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
		return new Vector3d(0.0, ballArray[0].pos.y, 0.0);
	}
}
