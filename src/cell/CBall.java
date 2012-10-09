package cell;

import java.util.ArrayList;

import NR.Vector3d;

public class CBall {
	public double n;		// [mol] Chemical amount 
	public 	double radius;
	public Vector3d pos;
	public Vector3d vel;
	public Vector3d force;
	public Vector3d[] posSave;
	public Vector3d[] velSave;
//	public int index;
	public CCell cell;
	public int cellIndex;
//	public int ballArrayIndex;
	
	///////////////////////////////////////////////////////////////////
	
	public CBall(double posx, double posy, double posz, double mass, int ballArrayIndex, CCell cell){	// Create a new ball, make it belong to cell
		this.cell = cell;
		
		pos = new Vector3d(posx, posy, posz);
		vel = new Vector3d(0, 0, 0);
		force = new Vector3d(0, 0, 0);

		int NSave = (int)(cell.model.movementTimeStepEnd/cell.model.movementTimeStep);	// -1 for not saving the final value, +1 for saving the initial value
		posSave = new Vector3d[NSave];
		velSave = new Vector3d[NSave];
//		forceSave = new Vector3d[NSave];
		for(int iSave=0; iSave<NSave; iSave++) {
			posSave[iSave] = new Vector3d();
			velSave[iSave] = new Vector3d();
//			forceSave[iSave] = new Vector3d();
		}
		
		this.n = mass;
		
		// Add ball to required arrays
//		this.ballArrayIndex = ballArrayIndex;
		cell.ballArray[ballArrayIndex] = this;
		cell.model.ballArray.add(this);
		this.cellIndex = cell.Index();
		// Update the radius
		this.radius = Radius();
	}
		
	public CBall() {} 				// Empty constructor for loading. Doesn't add the ball to any ball arrays!
	
	/////////////////////////////////////////////////////
	
	public double Radius() {		// Note that rho is in kg m-3 but cell mass is in Cmol
		if (cell.type<2) {
			return Math.pow( 							n*cell.model.MWX / (Math.PI * cell.model.rhoX * 4.0/3.0), .333333);
		} else if(cell.type<4) {	// type == 2 || 3 is variable radius balls
			return Math.pow( 				 	  2.0*n * cell.model.MWX / (Math.PI * cell.model.rhoX * (2.0*cell.model.aspect[cell.type] + 4.0/3.0)), .333333);			// Note that 2.0*mass could at some point in the future be wrong. Can't use GetMass() yet
		} else {									// type == 4 || 5 is fixed radius (variable length) rod
			return Math.pow( cell.model.nCellMax[cell.type]*cell.model.MWX / (Math.PI * cell.model.rhoX * (2.0*cell.model.aspect[cell.type] + 4.0/3.0)), .333333);			// Static
		}
		
	}
	
	public int Index() {
		ArrayList<CBall> array = this.cell.model.ballArray;
		for(int index=0; index<array.size(); index++) {
			if(array.get(index).equals(this))	return index;
		}
		return -1;			// Error
	}
}

