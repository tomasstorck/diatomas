package cell;

import java.io.Serializable;
import java.util.ArrayList;


public class CBall implements Serializable {
	private static final long serialVersionUID = 1L;
	//
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
	
	public CBall(double posx, double posy, double posz, double amount, int ballArrayIndex, CCell cell){	// Create a new ball, make it belong to cell
		this.cell = cell;
		CModel model = cell.model; 
		
		pos = new Vector3d(posx, posy, posz);
		vel = new Vector3d(0, 0, 0);
		force = new Vector3d(0, 0, 0);

		int NSave = (int)(model.relaxationTimeStep/model.relaxationTimeStepdt);	// -1 for not saving the final value, +1 for saving the initial value
		posSave = new Vector3d[NSave];
		velSave = new Vector3d[NSave];
//		forceSave = new Vector3d[NSave];
		for(int iSave=0; iSave<NSave; iSave++) {
			posSave[iSave] = new Vector3d();
			velSave[iSave] = new Vector3d();
//			forceSave[iSave] = new Vector3d();
		}
		
		this.n = amount;
		
		// Add ball to required arrays
//		this.ballArrayIndex = ballArrayIndex;
		cell.ballArray[ballArrayIndex] = this;
		model.ballArray.add(this);
		this.cellIndex = cell.Index();
		// Update the radius
		this.radius = Radius();
	}
		
	public CBall() {} 				// Empty constructor for loading. Doesn't add the ball to any ball arrays!
	
	/////////////////////////////////////////////////////
	
	public double Radius() {		
		return Radius(n, cell.type, cell.model);
	}
	
	public static double Radius(double n, int type, CModel model) {
		if (type<2) {
			return Math.pow( 							n*model.MWX / (Math.PI * model.rhoX * 4.0/3.0), .333333);						// Note that rho is in kg m-3 but cell mass is in Cmol
		} else {
			double aspect = model.lengthCellMax[type] / model.radiusCellMax[type];														// Aspect is here length over radius (not diameter) 
			if(type<4) {			// type == 2 || 3 is variable radius balls
				return Math.pow(				    2.0*n*model.MWX / (Math.PI * model.rhoX * (aspect + 4.0/3.0)), .333333);			// Note that 2.0*mass could at some point in the future be wrong. Can't use GetMass() yet
			} else {					// type == 4 || 5 is fixed radius (variable length) rod
				return Math.pow(    model.nCellMax[type]*model.MWX	/ (Math.PI * model.rhoX * (aspect + 4.0/3.0)), .333333);			// Static
			}
		}
	}
	
	public int Index() {
		CModel model = cell.model;
		ArrayList<CBall> array = model.ballArray;
		for(int index=0; index<array.size(); index++) {
			if(array.get(index).equals(this))	return index;
		}
		return -1;			// Error
	}
}

