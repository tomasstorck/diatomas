package cell;

import java.io.Serializable;
import java.util.ArrayList;

import NR.Vector3d;

public class CAnchorSpring implements Serializable {
	private static final long serialVersionUID = 1L;
	//
	CBall[] ballArray = new CBall[1];
	Vector3d anchor;
	double K;
	double restLength;
	CAnchorSpring[] siblingArray = new CAnchorSpring[0];
	
//	int anchorArrayIndex;					// Would need to be maintained while UnAnchoring, too much work
	
	///////////////////////////////////////////////////////////////////
	
	public CAnchorSpring(CBall ball) {		// Note that siblingArray is not assigned here
		this.ballArray[0] = ball;
		anchor = new Vector3d(ball.pos.x, 0, ball.pos.z);
		K = CModel.Kan*CModel.nBallInit[ball.cell.type];
		restLength = Math.max(ball.pos.y,ball.radius*1.01);			// Whatever is largest: distance ball-floor or radius plus a little push
		
		CModel.anchorSpringArray.add(this);
		// CAnchorSpring is added to the cell from where this function is called 
	}
	
	public CAnchorSpring() {}				// Note that this constructor does NOT add the anchor spring to the model!
	
	/////////////////////
	
	public int UnAnchor() {
		int count = 0;
		count += (CModel.anchorSpringArray.remove(this))?1:0;
		for(int ii=0; ii<siblingArray.length; ii++) {
			count += (CModel.anchorSpringArray.remove(this.siblingArray[ii]))?1:0;
		}
		
		this.ballArray[0].cell.anchorSpringArray = new CAnchorSpring[0];
		
		return count;
	}
	
	//////////////////////
	
	public int Index() {
		ArrayList<CAnchorSpring> array = CModel.anchorSpringArray;
		for(int index=0; index<array.size(); index++) {
			if(array.equals(this))	return index;
		}
		return -1;
	}
}
