package cell;

import java.util.ArrayList;

public class CFilSpring {
	// We work with two "virtual" springs here big_ and small_:
	CBall[] big_ballArray 	= new CBall[2];
	double big_K;
	double big_restLength; 
	CBall[] small_ballArray = new CBall[2];
	double small_K;
	double small_restLength;
	
	///////////////////////////////////////////////////////////////////
	
	public CFilSpring(CCell parent, CCell daughter) {
		CModel model = parent.model;
		small_ballArray	= new CBall[]{daughter.ballArray[0],parent.ballArray[1]};
		small_K = parent.model.Kf*(model.MBallInit[parent.type] + model.MBallInit[daughter.type])/2.0;
		this.ResetSmall();
		
		big_ballArray	= new CBall[]{daughter.ballArray[1],parent.ballArray[0]};
		big_K = parent.model.Kf*(model.MBallInit[parent.type] + model.MBallInit[daughter.type])/2.0;
		this.ResetBig();		// Set restLength for big springs
		
		// Add to filSpringArray
		parent.model.filSpringArray.add(this);
	}

	public CFilSpring() {}		// Empty constructor for loading. Doesn't add to filSpringArray!
	
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
		ArrayList<CFilSpring> array = this.big_ballArray[0].cell.model.filSpringArray;
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
		big_restLength = 1.6*small_restLength + bc0.springArray[0].restLength + bc1.springArray[0].restLength;
	}
}