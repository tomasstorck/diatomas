package cell;

public class CFilSpring {
//	CSpring bigSpring;
//	CSpring smallSpring;
	// We work with two "virtual" springs here big_ and small_:
	CBall[] big_ballArray 	= new CBall[2];
	double big_K;
	double big_restLength; 
	CBall[] small_ballArray = new CBall[2];
	double small_K;
	double small_restLength;
	int arrayIndex;
	
	public CFilSpring(CCell parent, CCell daughter) {
		big_ballArray	= new CBall[]{daughter.ballArray[1],parent.ballArray[0]};
		big_K = parent.pModel.Kf;
		big_restLength 	= 3*daughter.springArray[0].restLength;
		small_ballArray	= new CBall[]{daughter.ballArray[0],parent.ballArray[1]};
		small_K = parent.pModel.Kf;
		small_restLength = 1.05*(parent.ballArray[1].radius + daughter.ballArray[1].radius);
		arrayIndex = parent.pModel.filSpringArray.size();
		// Add to filSpringArray
		parent.pModel.filSpringArray.add(this);
	}

	public CFilSpring() {}		// Empty constructor for loading. Doesn't add to filSpringArray!
	
	/////////////////////////////
	
	public void ResetRestLength() {			// Note that only bigSpring is reset here, as was in C++ model (TODO)
		CCell pCell = big_ballArray[0].pCell;
		CCell pCell2 = big_ballArray[1].pCell;
		if(pCell.type>0 && pCell2.type>0) {
			double error = big_restLength - pCell.springArray[0].restLength - pCell2.springArray[0].restLength;
			if(error < 0) return;
			big_restLength = 1.5*(pCell.springArray[0].restLength + pCell2.springArray[0].restLength);
		}
	}
}