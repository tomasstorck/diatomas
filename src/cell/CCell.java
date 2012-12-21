package cell;

import java.io.Serializable;
import java.util.ArrayList;

import random.rand;
import NR.Vector3d;

public class CCell implements Serializable {
	private static final long serialVersionUID = 1L;
	//
	public int type;
	public boolean filament;
	public double[] colour = 	new double[3];
	public CBall[] ballArray = 	new CBall[1];							// Note that this ballArray has the same name as CModel's
	public ArrayList<CSpring> rodSpringArray = new ArrayList<CSpring>(0);
	public ArrayList<CCell> stickCellArray = new ArrayList<CCell>(0);
	public ArrayList<CSpring> stickSpringArray = new ArrayList<CSpring>(0);
	public CAnchorSpring[] anchorSpringArray = new CAnchorSpring[0];
	public ArrayList<CSpring> filSpringArray = new ArrayList<CSpring>(0);
	public CCell mother;
	public int motherIndex;
//	public int index;
//	public int[] ballArrayIndex;
	// CFD stuff
	public double q;													// [mol reactions (CmolX * s)-1]
	// Pointer stuff
	public CModel model;
	
	////////////////////////////////////////////////////////////////////////////////////////////
	
	public CCell(int type, double n, double base0x, double base0y, double base0z, double base1x, double base1y, double base1z, boolean filament, double[] colour, CModel model) {
		this.model = model;
		this.type = type;
		this.filament = filament;
		this.colour = colour;
		
		model.cellArray.add(this);				// Add it here so we can use cell.Index()
		
		if(type<2) { // Leaves ballArray and springArray, and mother
			ballArray[0] = new CBall(base0x, base0y, base0z, n,   0, this);
		} else if(type<6){
			ballArray = 	new CBall[2];		// Reinitialise ballArray to contain 2 balls
			new CBall(base0x, base0y, base0z, n/2.0, 0, this);		// Constructor adds it to the array
			new CBall(base1x, base1y, base1z, n/2.0, 1, this);		// Constructor adds it to the array
			new CSpring(ballArray[0],ballArray[1], 0);				// Constructor adds it to the array
		} else {
			model.Write("Unknown cell type during cell creation: " + type, "error");
		}
	}
	
	public CCell(int type, double n, double base0x, double base0y, double base0z, boolean filament, double[] colour, CModel model) {
		// Set cell based on other constructor
		this(type, n, base0x, base0y, base0z, base0x, base0y, base0z, filament, colour, model);
		// Leaves ballArray and springArray if rod cell
		if(type>1 && type<6) { 
			// Put cell in correct position
			Vector3d direction = new Vector3d(rand.Double(),rand.Double(),rand.Double());
			direction.normalise();	// Normalise direction
			double distance;
			if(type<4) {
				distance = ballArray[0].radius * model.cellLengthMax[ballArray[0].cell.type]/model.cellRadiusMax[ballArray[0].cell.type];										// type == 2||3 is fixed ball-ball distance
			} else if(type<6) {
				distance = ballArray[0].cell.GetAmount()*model.MWX/(Math.PI*model.rhoX*ballArray[0].radius*ballArray[0].radius) - 4.0/3.0*ballArray[0].radius;					// type == 4||5 is variable ball-ball distance. Correct? TODO
			} else {
				model.Write("Unknown type in cell creation: " + type, "error");
				distance = 0;
			}
			double base1x = base0x + direction.x * distance;
			double base1y = base0y + direction.y * distance;
			double base1z = base0z + direction.z * distance;
			// Set pos. Rest length is function of mass, not position, so no need to reset
			ballArray[1].pos.x = base1x;
			ballArray[1].pos.y = base1y;
			ballArray[1].pos.z = base1z;
		}
	}
	
	public CCell() {}		// Empty constructor for loading, note that this doesn't add the cell to the array!
	
	/////////////////////////////////////////////////////////
	
	public int Index() {
		ArrayList<CCell> array = model.cellArray;
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
		int NSpring0 = 0, NSpring1 = 0;
		if(type<2) 			NSpring0 = 1; else 
		if(type<6)			NSpring0 = 2; else
			model.Write("Unknown cell type while sticking: " + type, "error");
		if(cell.type<2) 	NSpring1 = 1; else 
		if(cell.type<6) 	NSpring1 = 2; else
			model.Write("Unknown cell type while sticking: " + type, "error");
		
		int NSpring = NSpring0 * NSpring1;
		CCell cell0, cell1;
		if(type > 1 && cell.type < 2) {		// Sphere goes first (see indexing next paragraph)
			cell0 = cell;
			cell1 = this;
		} else {							// Sphere goes first. Other cases, it doesn't matter
			cell0 = this;
			cell1 = cell;
		}
		
		CSpring[] stickArray = new CSpring[NSpring];
		for(int iSpring = 0; iSpring < NSpring; iSpring++) {					// Create all springs, including siblings, with input balls
			CBall ball0 = cell0.ballArray[iSpring/2];							// 0, 0, 1, 1, ...
			CBall ball1 = cell1.ballArray[iSpring%2];							// 0, 1, 0, 1, ...
			CSpring spring 	= new CSpring(	ball0,
											ball1,
											1);									// Type is sticking spring
			stickArray[iSpring] = spring;
		}
		
		// Define siblings, link them
		for(int iSpring = 0; iSpring < NSpring; iSpring++) {				// For each spring and sibling spring			
			CSpring spring = stickArray[iSpring];			
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
			model.Write("Unknown cell type while setting cell amount: " + type,"error");
		}
	}
	
	/////////////////
	
	public double SurfaceArea() {
		if(type<2) {
			return 4*Math.PI * Math.pow(ballArray[0].radius, 2);
		} else if(type<6) {	// Assuming radii are equal
			double Aballs = 4*Math.PI * Math.pow(ballArray[0].radius, 2); 		// Two half balls
			double height = ballArray[1].pos.minus(ballArray[0].pos).norm();	// height == distance between balls
			double Acyl = 	2*Math.PI * ballArray[0].radius * height;			// area of wall of cylinder. NOTE: Matt subtracted 2*radius, I don't see why
			return Aballs + Acyl;
		} else {
			model.Write("Unknown type during surface calculations", "error");
			return -1.0;
		}
	}
}
