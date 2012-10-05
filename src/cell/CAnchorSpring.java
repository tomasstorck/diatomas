package cell;

import java.util.ArrayList;

import NR.Vector3d;

public class CAnchorSpring {
	CBall ball;
	Vector3d anchor;
	double K;
	double restLength;
	CAnchorSpring[] siblingArray = new CAnchorSpring[0];
	
//	int anchorArrayIndex;					// Would need to be maintained while UnAnchoring, too much work
	
	///////////////////////////////////////////////////////////////////
	
	public CAnchorSpring(CBall ball) {		// Note that siblingArray is not assigned here
		CModel model = ball.cell.model;
		this.ball = ball;
		anchor = new Vector3d(ball.pos.x, 0, ball.pos.z);
		K = ball.cell.model.Kan*model.MBallInit[ball.cell.type];
		restLength = Math.max(ball.pos.y,ball.radius*1.01);			// Whatever is largest: distance ball-floor or radius plus a little push
		ball.cell.model.anchorSpringArray.add(this);
	}
	
	public CAnchorSpring() {}				// Note that this constructor does NOT add the anchor spring to the model!
	
	/////////////////////
	
	public int UnAnchor() {
		CModel model = this.ball.cell.model;
		model.anchorSpringArray.remove(this);
		for(int ii=0; ii<siblingArray.length; ii++) {
			model.anchorSpringArray.remove(this.siblingArray[ii]);
		}
		
		this.ball.cell.anchorSpringArray = new CAnchorSpring[0];
		
		return 1+siblingArray.length;
	}
	
	//////////////////////
	
	public int Index() {
		ArrayList<CAnchorSpring> array = this.ball.cell.model.anchorSpringArray;
		for(int index=0; index<array.size(); index++) {
			if(array.equals(this))	return index;
		}
		return -1;
	}
}
