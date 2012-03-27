package cell;

public class CSpring {
	CBall[] ballArray = new CBall[2];
	double K;
	double restLength;

	public CSpring(CBall ball0, CBall ball1, double K, double restLength){
		this.K = K;
		this.restLength = restLength;
		ballArray[0] = ball0;
		ballArray[1] = ball1;
		ball0.pCell.springArray[0] = this;
	}
	
	public CSpring(CBall ball0, CBall ball1){
		new CSpring(ball0, ball1, ball0.pCell.pModel.Ks, ball0.Radius()*ball0.pCell.pModel.aspect * 2);
	}
}
