package cell;

public class CFilSpring {
	CSpring bigSpring;
	CSpring smallSpring;
	
	public CFilSpring(CCell parent, CCell daughter) {
		double Kf = parent.pModel.Kf; 
		bigSpring 		= new CSpring(daughter.ballArray[0],parent.ballArray[0], Kf, 3*daughter.springArray[0].restLength);
		smallSpring 	= new CSpring(daughter.ballArray[1],parent.ballArray[1], Kf, 1.05*(parent.ballArray[1].radius + daughter.ballArray[1].radius));

		// Add to filSpringArray
		parent.pModel.filSpringArray.add(this);
	}

	public CFilSpring() {}		// Empty constructor for loading. Doesn't add to filSpringArray!
	
	/////////////////////////////
	
	public void ResetRestLength() {			// Note that only bigSpring is reset here, as was in C++ model (TODO)
		CCell pCell = bigSpring.ballArray[0].pCell;
		CCell pCell2 = bigSpring.ballArray[1].pCell;
		if(pCell.type>0 && pCell2.type>0) {
			double error = bigSpring.restLength - pCell.springArray[0].restLength - pCell2.springArray[0].restLength;
			if(error < 0) return;
			bigSpring.restLength = 1.5*(pCell.springArray[0].restLength + pCell2.springArray[0].restLength);
		}
	}
}