package cell;

public class CSpring {
	CBall[] ballArray = new CBall[2];
	double K;
	double restLength;
	
	public CSpring(CBall ball1, CBall ball2){
		CModel pModel = ball1.pCell.pModel;
		K = pModel.Ks;
		restLength = ball1.Radius() * pModel.aspect * 2;
		ballArray[0] = ball1;
		ballArray[1] = ball2;
	}
	
	public CSpring(CBall ball1, CBall ball2, double K, double restLength){
		this.K = K;
		this.restLength = restLength;
		ballArray[0] = ball1;
		ballArray[1] = ball2;
	}
}
