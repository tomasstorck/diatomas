package cell;

import java.io.Serializable;
import java.util.ArrayList;

public class CFilSpring implements Serializable {
	private static final long serialVersionUID = 1L;
	//
	// We work with two "virtual" springs here big_ and small_:
	public CBall[] big_ballArray 	= new CBall[2];
	public double big_K;
	public double big_restLength; 
	public CBall[] small_ballArray = new CBall[2];
	public double small_K;
	public double small_restLength;
	
	///////////////////////////////////////////////////////////////////
	
	public CFilSpring(CCell mother, CCell daughter) {
		CModel model = mother.model;
		small_ballArray	= new CBall[]{daughter.ballArray[0],mother.ballArray[1]};
		small_K = model.Kf*(model.nBallInit[mother.type] + model.nBallInit[daughter.type])/2.0;
		this.ResetSmall();
		
		big_ballArray	= new CBall[]{daughter.ballArray[1],mother.ballArray[0]};
		big_K = model.Kf*(model.nBallInit[mother.type] + model.nBallInit[daughter.type])/2.0;
		this.ResetBig();		// Set restLength for big springs
		
		// Add to filSpringArray
		model.filSpringArray.add(this);
		mother.filSpringArray.add(this);
		daughter.filSpringArray.add(this);
	}

	public CFilSpring() {}		// Empty constructor for loading. Doesn't add to filSpringArray!
	
	public void UnFil() {
		CModel model = this.big_ballArray[0].cell.model;
		// Remove from model
		model.filSpringArray.remove(this);
		// Remove from both cells
		big_ballArray[0].cell.filSpringArray.remove(this);
		big_ballArray[1].cell.filSpringArray.remove(this);
	}
	/////////////////////////////
	
	public void set(CBall big_ballArray0, CBall big_ballArray1, CBall small_ballArray0, CBall small_ballArray1) {
		big_ballArray = new CBall[]{big_ballArray0, big_ballArray1};
		small_ballArray = new CBall[]{small_ballArray0, small_ballArray1};
	}
	
	public void set(CBall ball, int position) {
		if(position==0)		big_ballArray[0] = ball; 
		if(position==1)		big_ballArray[1] = ball; 
		if(position==2)		small_ballArray[0] = ball;
		if(position==3)		small_ballArray[1] = ball;
	}
	
	public int Index() {
		CModel model = this.big_ballArray[0].cell.model;
		ArrayList<CFilSpring> array = model.filSpringArray;
		for(int index=0; index<array.size(); index++) {
			if(array.equals(this))	return index;
		}
		return -1;
	}
	
	public void ResetSmall() {
		small_restLength = 1.1*(small_ballArray[0].radius + small_ballArray[1].radius);		// Chose 1.3 because mass can double --> radius will expand by factor 1.26 on both sides --> 1.3 will do
	}
	
	public void ResetBig() {			// Note that only bigSpring is reset here, as was in C++ model
		CCell bc0 = big_ballArray[0].cell;
		CCell bc1 = big_ballArray[1].cell;
		big_restLength = 1.6*small_restLength + bc0.rodSpringArray[0].restLength + bc1.rodSpringArray[0].restLength;
	}
}