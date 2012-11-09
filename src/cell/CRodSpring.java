// Purely used for cell-cell internal springs, not for filament springs anymore.

package cell;

import java.io.Serializable;
import java.util.ArrayList;

public class CRodSpring implements Serializable {
	private static final long serialVersionUID = 1L;
	//
	public CBall[] ballArray = new CBall[2];
	public double K;
	public double restLength;

	///////////////////////////////////////////////////////////////////
	
	public CRodSpring(CBall ball0, CBall ball1, double K, double restLength){
		ballArray[0] = ball0;
		ballArray[1] = ball1;
		CModel model = this.ballArray[0].cell.model;
		this.K = K;
		this.restLength = restLength;
		ball0.cell.springArray[0] = this;
		model.rodSpringArray.add(this);
	}
	
	public CRodSpring(CBall ball0, CBall ball1){
		ballArray[0] = ball0;
		ballArray[1] = ball1;
		CModel model = this.ballArray[0].cell.model;
		this.K = model.Kr*model.nBallInit[ball0.cell.type];
		this.ResetRestLength();
		ball0.cell.springArray[0] = this;
		model.rodSpringArray.add(this);
	}
	
	public CRodSpring() {}
	
	////////////////////////////////////////////////////
	
	public double ResetRestLength() {
		CModel model = this.ballArray[0].cell.model;
		// If type == 1 based on mass, type==2 based on max mass
		if(ballArray[0].cell.type<4) {
			restLength = 2.0*ballArray[0].radius*model.aspect[ballArray[0].cell.type];																		// About 2 balls in the same cell, so no need to make it complicated  
		} else {
			restLength = ballArray[0].cell.GetAmount()*model.MWX/(Math.PI*model.rhoX*ballArray[0].radius*ballArray[0].radius) - 4.0/3.0*ballArray[0].radius;
//			restLength = 2.0*ballArray[0].radius*model.aspect[ballArray[0].cell.type] * ballArray[0].cell.GetMass()/model.MCellMax[ballArray[0].cell.type];
		}
		return restLength;
	}
	
	public int Index() {
		CModel model = this.ballArray[0].cell.model;
		ArrayList<CRodSpring> array = model.rodSpringArray;
		for(int index=0; index<array.size(); index++) {
			if(array.equals(this))	return index;
		}
		return -1;
	}
}
