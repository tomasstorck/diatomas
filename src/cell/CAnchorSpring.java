package cell;

public class CAnchorSpring {
	CBall ball;
	Vector3d anchor;
	double K;
	double restLength;
	CAnchorSpring[] siblingArray = new CAnchorSpring[0];
//	int anchorArrayIndex;					// Would need to be maintained while UnAnchoring, too much work
	
	public CAnchorSpring(CBall ball) {		// Note that siblingArray is not assigned here
		this.ball = ball;
		anchor = new Vector3d(ball.pos.x, 0, ball.pos.z);
		K = ball.cell.pModel.Ka;
		restLength = ball.radius;
		ball.cell.pModel.anchorSpringArray.add(this);
	}
	
	public CAnchorSpring() {}				// Note that this constructor does NOT add the anchor spring to the model!
	
	/////////////////////
	
	public int UnAnchor() {
		this.ball.cell.pModel.anchorSpringArray.remove(this);
		int counter=1;
		for(int ii=0; ii<siblingArray.length; ii++) {
			this.ball.cell.pModel.anchorSpringArray.remove(this.siblingArray[ii]);
			counter++;
		}
		return counter;
	}
}
