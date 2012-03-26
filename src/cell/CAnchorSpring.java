package cell;

public class CAnchorSpring {
	CBall pBall;
	Vector anchor;
	double K;
	double restLength;
	CAnchorSpring[] siblingArray;
	
	public CAnchorSpring(CBall ball) {		// Note that siblingArray is not assigned here
		pBall = ball;
		anchor = new Vector(ball.pos.x, 0, ball.pos.z);
		K = ball.pCell.pModel.Ka;
		ball.pCell.pModel.anchorSpringArray.add(this);
	}
}
