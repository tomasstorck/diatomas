package ibm;

import java.util.ArrayList;

public class AnchorSpring extends Spring {
	private static final long serialVersionUID = 1L;
	public Vector3d anchorPoint = new Vector3d();
	public ArrayList<AnchorSpring> siblingArray = new ArrayList<AnchorSpring>(2);
	public boolean gliding;

	///////////////////////////////////////////////////////////////////
	// This class can have two forms: normal (static point) or gliding (moving point) anchoring links
	// To switch, only the GetL() function needs to be changed.
	
	public AnchorSpring(Ball ball0, Vector3d anchorPoint, boolean gliding) {
		// We don't use the super constructor here: AnchorSpring is different
		ballArray = new Ball[1];
		ballArray[0] = ball0;
		this.anchorPoint = anchorPoint;
		this.gliding = gliding;
		ResetK();
		ResetRestLength();
		// Add to arrays
		final Model model = ball0.cell.model;
		model.anchorSpringArray.add(this);
		ball0.cell.anchorSpringArray.add(this);	
	}
	
	public static double RestLength(double height, double radius) {
		return Math.max(height,radius*1.01);						// Choose current position, but make sure it is not forcing the spring into the substratum
	}
	
	public void ResetRestLength() {
		final Ball ball = ballArray[0];							// Final because anchoring springs are not recycled
		restLength = RestLength(ball.pos.z, ball.radius);
	}
	
	public void ResetK() {
		final Model model = ballArray[0].cell.model;
		double springDiv;
		Cell cell = ballArray[0].cell;
		int shape = model.shapeX[cell.type];
		if(shape==0)						springDiv = 1.0;
		else if(shape==1 || shape==2)		springDiv = 2.0;
		else throw new IndexOutOfBoundsException("Cell type: " + cell.type);
		K = model.Kan/springDiv;
	}
	
	public int Break() {
		final Model model = ballArray[0].cell.model;
		int count = 0;
		Cell cell0 = ballArray[0].cell;
		// Remove this and siblings from model and cells			// Anchoring springs
		count += (model.anchorSpringArray.remove(this))?1:0;		// Add one to counter if successfully removed
		cell0.anchorSpringArray.remove(this);
		for(Spring sibling : siblingArray) {
			cell0.anchorSpringArray.remove(sibling);
			count += (model.anchorSpringArray.remove(sibling))?1:0;
		}
		return count;
	}
	
	public int Index() {
		final Model model = ballArray[0].cell.model;
		return super.Index(model.anchorSpringArray);
	}
	
	public Vector3d GetL() {
		if(gliding) {
			// Moving (gliding) anchoring links
			return new Vector3d(0.0, 0.0, -1.0*ballArray[0].pos.z);
		} else {
			// Static anchoring links
			return anchorPoint.minus(ballArray[0].pos);
		}

	}
}
