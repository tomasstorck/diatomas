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
		big_K = parent.model.Kf;
		big_restLength 	= 3*daughter.springArray[0].restLength;
		small_ballArray	= new CBall[]{daughter.ballArray[0],parent.ballArray[1]};
		small_K = parent.model.Kf;
		small_restLength = 1.05*(parent.ballArray[1].radius + daughter.ballArray[1].radius);
		arrayIndex = parent.model.filSpringArray.size();
		// Add to filSpringArray
		parent.model.filSpringArray.add(this);
	}

	public CFilSpring() {}		// Empty constructor for loading. Doesn't add to filSpringArray!
	
	/////////////////////////////
	
	public void ResetRestLength() {			// Note that only bigSpring is reset here, as was in C++ model (TODO)
		CCell cell = big_ballArray[0].cell;
		CCell cell2 = big_ballArray[1].cell;
		if(cell.type>0 && cell2.type>0) {
			double error = big_restLength - cell.springArray[0].restLength - cell2.springArray[0].restLength;
			if(error < 0) return;
			big_restLength = 1.5*(cell.springArray[0].restLength + cell2.springArray[0].restLength);
		}
	}
}