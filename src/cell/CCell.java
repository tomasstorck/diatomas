package cell;

import java.util.ArrayList;

import NR.Vector3d;

import random.rand;

public class CCell {
	public int type;
	public boolean filament;
	public double[] colour = 	new double[3];
	public CBall[] ballArray = 	new CBall[1];							// Note that this ballArray has the same name as CModel's
	public CRodSpring[] springArray = new CRodSpring[0];
	public ArrayList<CCell> stickCellArray = new ArrayList<CCell>(0);
	public ArrayList<CStickSpring> stickSpringArray = new ArrayList<CStickSpring>(0);
	public CAnchorSpring[] anchorSpringArray = new CAnchorSpring[0];
	public CCell mother;
	public int motherIndex;
//	public int index;
//	public int[] ballArrayIndex;
	CModel model;
	// CFD stuff
	public double q;													// [mol reactions (CmolX * s)-1]	
	
	////////////////////////////////////////////////////////////////////////////////////////////
	
	public CCell(int type, double base0x, double base0y, double base0z, double base1x, double base1y, double base1z, boolean filament, double[] colour, CModel model) {
		this.type = type;
		this.filament = filament;
		this.colour = colour;
		this.model = model;
		
		model.cellArray.add(this);				// Add it here so we can use cell.Index()
		
		if(type<2) { // Leaves ballArray and springArray, and mother
			ballArray[0] = new CBall(base0x, base0y, base0z, model.nCellInit[type],   0, this);
		} else {
			ballArray = 	new CBall[2];		// Reinitialise ballArray to contain 2 balls
			springArray = new CRodSpring[1];	// Reinitialise springArray to contain a spring
			new CBall(base0x, base0y, base0z, model.nCellInit[type]/2.0, 0, this);		// Constructor adds it to the array
			new CBall(base1x, base1y, base1z, model.nCellInit[type]/2.0, 1, this);		// Constructor adds it to the array
			new CRodSpring(ballArray[0],ballArray[1]);								// Constructor adds it to the array
		}
	}
	
	public CCell(int type, double base0x, double base0y, double base0z, boolean filament, double[] colour, CModel model) {
		this.type = type;
		this.filament = filament;
		this.colour = colour;
		this.model = model;
		
		model.cellArray.add(this);				// Add it here so we can use cell.Index()
		
		if(type<2) { // Leaves ballArray and springArray, and mother
			new CBall(base0x, base0y, base0z, model.nCellInit[type],   0, this);
//			ballArrayIndex = new int[]{ballArray[0].index};
		} else {
			ballArray = 	new CBall[2];	// Reinitialise ballArray to contain 2 balls
			springArray = new CRodSpring[1];	// Reinitialise springArray to contain a spring
			new CBall(base0x, base0y, base0z, model.nCellInit[type]/2.0, 0, this);
			
			Vector3d direction = new Vector3d(rand.Double(),rand.Double(),rand.Double());
			direction.normalise();	// Normalise direction
			
			double distance;
			if(type<4) {
				distance = ballArray[0].radius * model.aspect[type]*2.0;										// type == 2||3 is fixed ball-ball distance
			} else {
				distance = ballArray[0].radius * model.aspect[type]*2.0 * ballArray[0].n/model.nCellMax[type];		// type == 4||5 is variable ball-ball distance
			}
			double base1x = base0x + direction.x * distance;
			double base1y = base0y + direction.y * distance;
			double base1z = base0z + direction.z * distance;
			
			new CBall(base1x, base1y, base1z, model.nCellInit[type]/2.0, 1, this);
			new CRodSpring(ballArray[0],ballArray[1]);
		}
	}
	
	public CCell(int type, Vector3d base, boolean filament, double[] colour, CModel model) {
		new CCell(type, base.x, base.y, base.z, filament, colour, model);
		// Add is taken care of through calling method
	}
	
	public CCell() {}		// Empty constructor for loading, note that this doesn't add the cell to the array!
	
	/////////////////////////////////////////////////////////
	
	public int Index() {
		ArrayList<CCell> array = this.model.cellArray;
		for(int index=0; index<array.size(); index++) {
			if(array.get(index).equals(this))	return index;
		}
		return -1;
	}
	
	public int Anchor() {
		int NBall = (type<2) ? 1 : 2;
		anchorSpringArray = new CAnchorSpring[NBall];
		for(int iBall = 0; iBall < NBall; iBall++) {
			anchorSpringArray[iBall] = new CAnchorSpring(ballArray[iBall]);
		}
		
		// Define siblings, just hardcode, saves time (for me)
		if(NBall > 1) {
			anchorSpringArray[0].siblingArray = new CAnchorSpring[1];
			anchorSpringArray[1].siblingArray = new CAnchorSpring[1];
			anchorSpringArray[0].siblingArray[0] = anchorSpringArray[1];
			anchorSpringArray[1].siblingArray[0] = anchorSpringArray[0];
		}
		
		return NBall;
	}
	

	public boolean IsFilament(CCell cell) {
		if(!this.filament && !cell.filament)	return false;
		
		if((mother!=null && mother.equals(cell)) || (cell.mother!=null && cell.mother.equals(this))) 		return true;
		else return false;
	}
	
	public int Stick(CCell cell) {
		// Determine how many sticking springs we need
		int NSpring0, NSpring1;
		if(type<2) 			{NSpring0 = 1;} else {NSpring0 = 2;}
		if(cell.type<2) 	{NSpring1 = 1;} else {NSpring1 = 2;}
		int NSpring = NSpring0 * NSpring1;
		CCell pA, pB;
		if(type > 1 && cell.type < 2) {		// Sphere goes first (see indexing next paragraph)
			pA = cell;
			pB = this;
		} else {							// Sphere goes first. Other cases, it doesn't matter
			pA = this;
			pB = cell;
		}
		
		CStickSpring[] stickArray = new CStickSpring[NSpring];
		for(int iSpring = 0; iSpring < NSpring; iSpring++) {					// Create all springs, including siblings, with input balls
			CStickSpring spring 	= new CStickSpring(	pA.ballArray[iSpring/2],	// 0, 0, 1, 1, ...
														pB.ballArray[iSpring%2]); 	// 0, 1, 0, 1, ...
			stickArray[iSpring] = spring;
			this.stickSpringArray.add(spring);
			cell.stickSpringArray.add(spring);
		}
		
		// Define siblings
		for(int iSpring = 0; iSpring < NSpring; iSpring++) {				// For each spring and siblingspring			
			CStickSpring spring = stickArray[iSpring];			
			spring.NSibling = NSpring-1;
			int ii = 0;
			for(int iSpring2 = 0; iSpring2 < NSpring; iSpring2++) {			
				if(iSpring != iSpring2) {									// For all its siblings
					spring.siblingArray[ii++] = stickArray[iSpring2];
				}
			}
		}
		
		this.stickCellArray.add(cell);
		cell.stickCellArray.add(this);
		
		return NSpring;
	}
			
	public double GetMass() {
		int NBall = (type<2) ? 1 : 2;
		double mass = 0; 
		for(int iBall=0; iBall<NBall; iBall++) {
			mass += ballArray[iBall].n;
		}
		return mass;
	}
	
	public void SetMass(double newMass) {
		if(type<2) {
			ballArray[0].n = newMass;
			ballArray[0].radius = ballArray[0].Radius();
		} else {
			ballArray[0].n = newMass/2.0;
			ballArray[0].radius = ballArray[0].Radius();
			ballArray[1].n = newMass/2.0;
			ballArray[1].radius = ballArray[1].Radius();
		}
	}
	
	public ArrayList<CCell> StickCellArray() {			// Currently used only for loading. Using the maintained stickCellArray field is much more CPU efficient.
		ArrayList<CCell> stickCellArray = new ArrayList<CCell>(1);
		for(CStickSpring spring : model.stickSpringArray) {
			CCell cell0 = spring.ballArray[0].cell;
			CCell cell1 = spring.ballArray[1].cell;
			if(this.equals(cell0) && !stickCellArray.contains(cell1)) {		// 2nd if argument makes sure we don't get duplicates (this'll happen when we encounter siblings)
				stickCellArray.add(cell1);	// Add the other cell
			}
			if(this.equals(cell1) && !stickCellArray.contains(cell0)) {
				stickCellArray.add(cell0);
			}
		}
		return stickCellArray;
	}
	
	/////////////////
	
	public double SurfaceArea() {
		if(type<2) {
			return 4*Math.PI * Math.pow(ballArray[0].radius, 2);
		} else {	// Assuming radii are equal
			double Aballs = 4*Math.PI * Math.pow(ballArray[0].radius, 2); 		// Two half balls
			double height = ballArray[1].pos.minus(ballArray[0].pos).length();	// height == distance between balls
			double Acyl = 	2*Math.PI * ballArray[0].radius * height;			// area of wall of cylinder. NOTE: Matt subtracted 2*radius, I don't see why
			return Aballs + Acyl;
		}
	}
}
