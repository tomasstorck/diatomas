package cell;

public class CBall {
	double mass;
//	double radius;	// Will put in method
	Vector pos;
	Vector vel;
	Vector force;
	int ballArrayIndex;
	CCell pCell;
	
	public CBall(double posx, double posy, double posz, double mass, int ballArrayIndex, CCell cell){	// Create a new ball, make it belong to pCell
//		if(cell.type == 0) {
//			mass = cell.pModel.MCellInit;
//		} else {
//			mass = cell.pModel.MCellInit;
//		}
		
		pos = new Vector(posx, posy, posz);
		vel = new Vector(0, 0, 0);
		force = new Vector(0, 0, 0);

		this.mass = mass;
		
		// Add ball to cell's ballArary
		pCell = cell;
		this.ballArrayIndex = ballArrayIndex;
		pCell.ballArray[ballArrayIndex] = this;
		CModel.NBall++;
	}
	
//	public CBall(Vector pos, int ballArrayIndex, CCell cell){																	// Same as above, but from Vector pos
//		pos = new Vector(pos.x, pos.y, pos.z);
//		vel = new Vector(0, 0, 0);
//		force = new Vector(0, 0, 0);
//		
//		// Add ball to cell's ballArary
//		pCell = cell;
//		this.ballArrayIndex = ballArrayIndex;
//		pCell.ballArray[ballArrayIndex] = this;
//	
//	}
	
	public double Radius() {							// Doing this here might save some calculations on the long run
		if (pCell.type == 0) {
			return Math.pow(0.75 * mass/(Math.PI * this.pCell.pModel.rho_m), .333333);
		} else if(pCell.type == 1) {					// type == 1 is variable radius balls
			return Math.pow(mass 					/ (2*Math.PI*pCell.pModel.rho_m*pCell.pModel.aspect), .333333);		// FIXME: original code: Rpos=pow((sBall->mass/PI/pModel->RHO_M/aspect),0.333333333333);
		} else if(pCell.type == 2) {					// type == 2 is fixed radius balls
			return Math.pow(pCell.pModel.MCellMax	/ (2*Math.PI*pCell.pModel.rho_m*pCell.pModel.aspect), .333333);		// TODO: this is constant, would making it static help here?
		} else {
			return -1;		// Error
		}
		
	}
}

