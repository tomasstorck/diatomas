package cell;

public class CFilSpring {
	CSpring bigSpring;
	CSpring smallSpring;
	double K;
	
	public CFilSpring(CCell parent, CCell daughter) {
		double Kf = parent.pModel.Kf; 
		bigSpring 		= new CSpring(daughter.ballArray[0],parent.ballArray[0], Kf, 3*daughter.springArray[0].restLength);
		smallSpring 	= new CSpring(daughter.ballArray[1],parent.ballArray[1], Kf, 1.05*(parent.ballArray[1].Radius() + daughter.ballArray[1].Radius()));

		// Add to filSpringArray
		parent.pModel.filSpringArray.add(this);
	}
}