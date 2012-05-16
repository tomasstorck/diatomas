package cell;

public class CAnchorSpring {
	CBall pBall;
	Vector3d anchor;
	double K;
	double restLength;
	CAnchorSpring[] siblingArray = new CAnchorSpring[1];
//	int anchorArrayIndex;					// Would need to be maintained while UnAnchoring, too much work
	
	public CAnchorSpring(CBall ball) {		// Note that siblingArray is not assigned here
		pBall = ball;
		anchor = new Vector3d(ball.pos.x, 0, ball.pos.z);
		K = ball.pCell.pModel.Ka;
		ball.pCell.pModel.anchorSpringArray.add(this);
	}
	
	public CAnchorSpring() {}				// Note that this constructor does NOT add the anchor spring to the model!
}
