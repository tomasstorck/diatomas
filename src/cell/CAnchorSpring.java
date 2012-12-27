package cell;

import java.io.Serializable;
import java.util.ArrayList;

import NR.Vector3d;

public class CAnchorSpring implements Serializable {
	private static final long serialVersionUID = 1L;
	//
	public CBall[] ballArray = new CBall[1];
	public Vector3d anchor;
	public double k;
	public double restLength;
	public CAnchorSpring[] siblingArray = new CAnchorSpring[0];
	
//	int anchorArrayIndex;					// Would need to be maintained while UnAnchoring, too much work
	
	///////////////////////////////////////////////////////////////////
	
	public CAnchorSpring(CBall ball) {		// Note that siblingArray is not assigned here
		CModel model = ball.cell.model;
		this.ballArray[0] = ball;
		anchor = new Vector3d(ball.pos.x, 0, ball.pos.z);
		k = model.Kan*model.nCellMax[ball.cell.type]/((ball.cell.type<2) ? 2.0 : 4.0);
		restLength = ball.radius*1.01;			// Whatever is largest: distance ball-floor or radius plus a little push
		
		model.anchorSpringArray.add(this);
		// CAnchorSpring is added to the cell from where this function is called 
	}
	
	public CAnchorSpring() {}				// Note that this constructor does NOT add the anchor spring to the model!
	
	/////////////////////
	
	public int UnAnchor() {
		CModel model = this.ballArray[0].cell.model;
		int count = 0;
		count += (model.anchorSpringArray.remove(this))?1:0;
		for(int ii=0; ii<siblingArray.length; ii++) {
			count += (model.anchorSpringArray.remove(this.siblingArray[ii]))?1:0;
		}
		
		this.ballArray[0].cell.anchorSpringArray = new CAnchorSpring[0];
		
		return count;
	}
	
	//////////////////////
	
	public int Index() {
		CModel model = this.ballArray[0].cell.model;
		ArrayList<CAnchorSpring> array = model.anchorSpringArray;
		for(int index=0; index<array.size(); index++) {
			if(array.equals(this))	return index;
		}
		return -1;
	}
}
