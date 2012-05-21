package cell;

public class CStickSpring {
	CBall[] ballArray = new CBall[2];
	double K;
	double restLength;
	CStickSpring[] siblingArray = new CStickSpring[3];					// TODO: get rid of siblingArray, make CStickSpring of size 2*siblingArray.size()
	int NSibling;
//	int stickArrayIndex;									// We'd need to maintain this while unsticking, too much work
	
	public CStickSpring(CBall ball1, CBall ball2){			// Note that siblingArray is by default not initialised
		CModel pModel = ball1.cell.pModel;
		K = pModel.Ks;
		restLength = ball1.radius * pModel.aspect * 2.0;
		ballArray[0] = ball1;
		ballArray[1] = ball2;
		// Add this sticking spring to the model
		ballArray[0].cell.pModel.stickSpringArray.add(this);
	}
	
	public CStickSpring () {}								// Empty constructor for model loading. Note that the stickSpring is not automatically added to the array
} 