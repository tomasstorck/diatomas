// Purely used for cell-cell internal springs, not for filament springs anymore.

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
		ball0.cell.springArray[0] = this;
		ball0.cell.pModel.rodSpringArray.add(this);
	}
	
	public CSpring(CBall ball0, CBall ball1){
		double restLength;
		if(ball0.cell.type == 1) {
			restLength = ball0.radius*ball0.cell.pModel.aspect * 2;
		} else {
			restLength = ball0.radius*ball0.cell.pModel.aspect * 2 * ball0.mass/ball0.cell.pModel.MCellMax;		// Note that with this mass will have to be set before spring is constructed
		}
		
		this.K = ball0.cell.pModel.Ks;
		this.restLength = restLength;
		ballArray[0] = ball0;
		ballArray[1] = ball1;
		ball0.cell.springArray[0] = this;
		ball0.cell.pModel.rodSpringArray.add(this);
	}
	
	public CSpring() {}
	
	////////////////////////////////////////////////////
	
	public double ResetRestLength() {
		// If type == 1 based on mass, else (so type==2) based on max mass
		CModel pModel = ballArray[0].cell.pModel;
		if(ballArray[0].cell.type == 1) {
			restLength = ballArray[0].radius*pModel.aspect*2;  
		} else {
			restLength = ballArray[0].radius*pModel.aspect*2 * ballArray[0].mass/pModel.MCellMax;			// Used to say 4 in C++ code but that doesn't make sense to me TODO
		}
		return restLength;
	}
}
