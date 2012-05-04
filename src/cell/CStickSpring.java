package cell;

public class CStickSpring {
	CBall[] ballArray = new CBall[2];
	double K;
	double restLength;
	CStickSpring[] siblingArray = new CStickSpring[4];					// TODO: get rid of siblingArray, make CStickSpring of size 2*siblingArray.size()
	int NSibling;
	double stickArrayIndex;
	
	public CStickSpring(CBall ball1, CBall ball2){			// Note that siblingArray is by default not initialised
		CModel pModel = ball1.pCell.pModel;
		K = pModel.Ks;
		restLength = ball1.radius * pModel.aspect * 2;
		ballArray[0] = ball1;
		ballArray[1] = ball2;
		stickArrayIndex = ballArray[0].pCell.pModel.stickSpringArray.size();
		ballArray[0].pCell.pModel.stickSpringArray.add(this);
	}
	
	public CStickSpring () {}								// Empty constructor for model loading. Note that the stickSpring is not automatically added to the array
} 