// Purely used for cell-cell internal springs, not for filament springs anymore.

package cell;

public class CSpring {
	CBall[] ballArray = new CBall[2];
	double K;
	double restLength;

	public CSpring(CBall ball0, CBall ball1, double K, double restLength){
		this.K = K;
		ballArray[0] = ball0;
		ballArray[1] = ball1;
		this.restLength = restLength;
		ball0.cell.springArray[0] = this;
		ball0.cell.model.rodSpringArray.add(this);
	}
	
	public CSpring(CBall ball0, CBall ball1){
		this.K = ball0.cell.model.Ki;
		ballArray[0] = ball0;
		ballArray[1] = ball1;
		this.ResetRestLength();
		ball0.cell.springArray[0] = this;
		ball0.cell.model.rodSpringArray.add(this);
	}
	
	public CSpring() {}
	
	////////////////////////////////////////////////////
	
	public double ResetRestLength() {
		// If type == 1 based on mass, type==2 based on max mass
		CModel pModel = ballArray[0].cell.model;
		if(ballArray[0].cell.type == 1) {
			restLength = ballArray[0].radius*pModel.aspect*2;  
		} else if(ballArray[0].cell.type == 2) {
			restLength = ballArray[0].radius*pModel.aspect*2 * ballArray[0].mass/pModel.MCellMax;			// Used to say 4 in C++ code but that doesn't make sense to me TODO
		}
		return restLength;
	}
}
