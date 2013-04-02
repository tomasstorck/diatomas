package cell;

import java.io.Serializable;
import java.util.ArrayList;

public class CCell implements Serializable {
	private static final long serialVersionUID = 1L;
	//
	public int type;
	public boolean filament;
	public CBall[] ballArray = 	new CBall[1];								// Note that this ballArray has the same name as CModel's
	public ArrayList<CRodSpring> rodSpringArray = new ArrayList<CRodSpring>(0);
	public ArrayList<CCell> stickCellArray = new ArrayList<CCell>(0);
	public ArrayList<CStickSpring> stickSpringArray = new ArrayList<CStickSpring>(0);
	public ArrayList<CAnchorSpring> anchorSpringArray = new ArrayList<CAnchorSpring>(0);
	public ArrayList<CFilSpring> filSpringArray = new ArrayList<CFilSpring>(0);
	public CCell mother;
	public int born;														// Growth iteration at which this cell was born
	// CFD stuff
	public double q = 0.0;													// [mol reactions (CmolX * s)-1]
	// Pointer stuff
	public CModel model;
	
	////////////////////////////////////////////////////////////////////////////////////////////
	
	public CCell(int type, double n, double base0x, double base0y, double base0z, double base1x, double base1y, double base1z, boolean filament, CModel model) {
		this.model = model;
		this.type = type;
		this.filament = filament;
		this.born = model.growthIter;
		
		model.cellArray.add(this);				// Add it here so we can use cell.Index()
		
		if(type<2) { // Leaves ballArray and springArray, and mother
			ballArray[0] = new CBall(base0x, base0y, base0z, n,   0, this);
		} else if(type<6){
			ballArray = 	new CBall[2];		// Reinitialise ballArray to contain 2 balls
			new CBall(base0x, base0y, base0z, n/2.0, 0, this);		// Constructor adds it to the array
			new CBall(base1x, base1y, base1z, n/2.0, 1, this);		// Constructor adds it to the array
			new CRodSpring(ballArray[0],ballArray[1]);				// Constructor adds it to the array
		} else {
			throw new IndexOutOfBoundsException("Cell type: " + type);
		}
	}
	
	public CCell(int type, double n, Vector3d base0, Vector3d base1, boolean filament, CModel model) {
		this(type, n, base0.x, base0.y, base0.z, base1.x, base1.y, base1.z, filament, model);
	}
	
	/////////////////////////////////////////////////////////
	
	public int Index() {
		ArrayList<CCell> array = model.cellArray;
		for(int index=0; index<array.size(); index++) {
			if(array.get(index).equals(this))	return index;
		}
		return -1;
	}
	
	public int Anchor() {
		for(CBall ball : ballArray) {
			Vector3d substratumPos = new Vector3d(ball.pos);
			substratumPos.y = 0.0;
			new CAnchorSpring(ball, substratumPos);
		}

		// Add sibling springs, assuming all anchors in this cell are siblings
		for(int ii=0; ii<anchorSpringArray.size(); ii++) {
			for(int jj=ii+1; jj<anchorSpringArray.size(); jj++) {
				CAnchorSpring anchor0 = anchorSpringArray.get(ii);
				CAnchorSpring anchor1 = anchorSpringArray.get(jj);
				anchor0.siblingArray.add(anchor1);
				anchor1.siblingArray.add(anchor0);
			}
		}
		return anchorSpringArray.size();
	}
	

	public boolean IsFilament(CCell cell) {
		if(!this.filament && !cell.filament)	return false;
		
		if((mother!=null && mother.equals(cell)) || (cell.mother!=null && cell.mother.equals(this))) 		return true;
		else return false;
	}
	
	public int Stick(CCell cell) {
		// Determine how many sticking springs we need
		int NSpring0 = 0, NSpring1 = 0;
		if(type<2) 			NSpring0 = 1; else 
		if(type<6)			NSpring0 = 2; else
			throw new IndexOutOfBoundsException("Cell type: " + type);
		if(cell.type<2) 	NSpring1 = 1; else 
		if(cell.type<6) 	NSpring1 = 2; else
			throw new IndexOutOfBoundsException("Cell type: " + cell.type);
		
		int NSpring = NSpring0 * NSpring1;
		CCell cell0, cell1;
		if(type > 1 && cell.type < 2) {		// Sphere goes first (see indexing next paragraph)
			cell0 = cell;
			cell1 = this;
		} else {							// Sphere goes first. Other cases, it doesn't matter
			cell0 = this;
			cell1 = cell;
		}
		
		CStickSpring[] stickArray = new CStickSpring[NSpring];
		for(int iSpring = 0; iSpring < NSpring; iSpring++) {					// Create all springs, including siblings, with input balls
			CBall ball0 = cell0.ballArray[iSpring/2];							// 0, 0, 1, 1, ...
			CBall ball1 = cell1.ballArray[iSpring%2];							// 0, 1, 0, 1, ...
			CStickSpring spring = new CStickSpring(	ball0, ball1);
			stickArray[iSpring] = spring;
		}
		
		// Define siblings, link them OPTIMISE
		for(int iSpring = 0; iSpring < NSpring; iSpring++) {				// For each spring and sibling spring			
			CStickSpring spring = stickArray[iSpring];			
			for(int iSpring2 = 0; iSpring2 < NSpring; iSpring2++) {			
				if(iSpring != iSpring2) {									// For all its siblings
					spring.siblingArray.add(stickArray[iSpring2]);
				}
			}
		}
		// Tell cells they're stuck to each other
		this.stickCellArray.add(cell);
		cell.stickCellArray.add(this);
		
		return NSpring;
	}
			
	public double GetAmount() {
		double amount = 0;
		for(CBall ball : ballArray) {
			amount += ball.n;
		}
		return amount;
	}
	
	public void SetAmount(double newAmount) {
		if(type<2) {
			ballArray[0].n = newAmount;
			ballArray[0].radius = ballArray[0].Radius();
		} else if(type<6){
			ballArray[0].n = newAmount/2.0;
			ballArray[0].radius = ballArray[0].Radius();
			ballArray[1].n = newAmount/2.0;
			ballArray[1].radius = ballArray[1].Radius();
			// Reset rod spring length
			for(CSpring rod : ballArray[0].cell.rodSpringArray) rod.ResetRestLength();
		} else {
			throw new IndexOutOfBoundsException("Cell type: " + type);
		}
	}
	
	/////////////////
	
	public double SurfaceArea() {
		if(type<2) {
			return 4*Math.PI * Math.pow(ballArray[0].radius, 2);
		} else if(type<6) {	// Assuming radii are equal
			double Aballs = 4.0*Math.PI * Math.pow(ballArray[0].radius, 2); 	// Two half balls
			double height = ballArray[1].pos.minus(ballArray[0].pos).norm();	// height == distance between balls
			double Acyl = 	2.0*Math.PI * ballArray[0].radius * height;			// area of wall of cylinder. NOTE: Matt subtracted 2*radius, I don't see why
			return Aballs + Acyl;
		} else {
			throw new IndexOutOfBoundsException("Cell type: " + type);
		}
	}
	
	public double GetDistance(CCell cell) {										// This method probably has a higher overhead than the code in CollisionDetection
		if(this.type<2) {														// Sphere-???
			if(cell.type<2)	{													// Sphere-sphere
				return cell.ballArray[0].pos.minus(ballArray[0].pos).norm();
			} else if(cell.type<6) {											// Sphere-rod
				EricsonObject C = model.DetectLinesegPoint(cell.ballArray[0].pos, cell.ballArray[1].pos, this.ballArray[0].pos);
				return C.dist;
			} else {															// Unknown!
				throw new IndexOutOfBoundsException("Cell type: " + cell.type);
			}
		} else if(this.type<6) {												// Rod-???
			if(cell.type<2) {													// Rod-sphere
				EricsonObject C = model.DetectLinesegPoint(this.ballArray[0].pos, this.ballArray[1].pos, cell.ballArray[0].pos);
				return C.dist; 
			} else if(cell.type<6) {											// Rod-rod
				EricsonObject C = model.DetectLinesegLineseg(this.ballArray[0].pos, this.ballArray[1].pos, cell.ballArray[0].pos, cell.ballArray[1].pos);
				return C.dist;		
			} else {															// Unknown!
				throw new IndexOutOfBoundsException("Cell type: " + cell.type);
			}
		} else {
			throw new IndexOutOfBoundsException("Cell type: " + this.type);
		}
	}
}
