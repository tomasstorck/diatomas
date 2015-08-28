package ibm;

import java.io.Serializable;
import java.util.ArrayList;

public class Cell implements Serializable {
	private static final long serialVersionUID = 1L;
	//
	public int type; 														// type is now defined in model (used to be 0||1==sphere, ...) 
	public boolean filament;
	public Ball[] ballArray = 	new Ball[1];								// Note that this ballArray has the same name as CModel's
	public ArrayList<RodSpring> rodSpringArray = new ArrayList<RodSpring>(0);
	public ArrayList<Cell> stickCellArray = new ArrayList<Cell>(0);
	public ArrayList<StickSpring> stickSpringArray = new ArrayList<StickSpring>(0);
	public ArrayList<AnchorSpring> anchorSpringArray = new ArrayList<AnchorSpring>(0);
	public ArrayList<FilSpring> filSpringArray = new ArrayList<FilSpring>(0);
	public Cell mother;
	public int born;														// Growth iteration at which this cell was born
	// CFD stuff
	public double Rx = 0.0;													// Reaction rate for this cell, normalised to substrate [mol/s]
	// Pointer stuff
	public Model model;
	// Var. radius stuff
	public double radiusModifier;

	////////////////////////////////////////////////////////////////////////////////////////////
	
	public Cell(int type, double n, double radiusModifier, double base0x, double base0y, double base0z, double base1x, double base1y, double base1z, boolean filament, Model model) {
		this.model = model;
		this.type = type; 						// type tells us shape
		this.filament = filament;
		this.radiusModifier = radiusModifier;
		this.born = model.growthIter;
		
		model.cellArray.add(this);				// Add it here so we can use cell.Index()
		
		int shape = model.shapeX[type]; 
		if(shape==0) { 												// sphere. Leaves ballArray and springArray, and mother
			ballArray[0] = new Ball(base0x, base0y, base0z, n,   0, this);
		} else if(shape==1 || shape==2){
			ballArray = 	new Ball[2];		// Reinitialise ballArray to contain 2 balls
			new Ball(base0x, base0y, base0z, n/2.0, 0, this);		// Constructor adds it to the array
			new Ball(base1x, base1y, base1z, n/2.0, 1, this);		// Constructor adds it to the array
			new RodSpring(ballArray[0],ballArray[1]);				// Constructor adds it to the array
		} else {
			throw new IndexOutOfBoundsException("Cell type: " + type);
		}
	}
	
	// Vector3d instead of double
	public Cell(int type, double n, double radiusModifier, Vector3d base0, Vector3d base1, boolean filament, Model model) {
		this(type, n, radiusModifier, base0.x, base0.y, base0.z, base1.x, base1.y, base1.z, filament, model);
	}
	
	// Without radiusModifier (generate randomly or skip, depending on model.radiusModifier)
	public Cell(int type, double n, double base0x, double base0y, double base0z, double base1x, double base1y, double base1z, boolean filament, Model model) {
		this(type, n, 
				model.radiusCellStDev[type]==0.0?0.0:(model.radiusCellStDev[type]*random.rand.Gaussian()),		// Assign radius modifier due to deviation. If no modifier skip this, maintains reproducibility (WORKAROUND). Has to be done inline due Java limitations
				base0x, base0y, base0z, base1x, base1y, base1z, filament, model);
	}
	
	public Cell(int type, double n, Vector3d base0, Vector3d base1, boolean filament, Model model) {
		this(type, n,
				model.radiusCellStDev[type]==0.0?0.0:(model.radiusCellStDev[type]*random.rand.Gaussian()),		// Assign radius modifier due to deviation. If no modifier skip this, maintains reproducibility (WORKAROUND). Has to be done inline due Java limitations 
				base0.x, base0.y, base0.z, base1.x, base1.y, base1.z, filament, model);
	}
	
	
	/////////////////////////////////////////////////////////
	
	public int Index() {
		ArrayList<Cell> array = model.cellArray;
		for(int index=0; index<array.size(); index++) {
			if(array.get(index).equals(this))	return index;
		}
		return -1;
	}
	
	public int Anchor() {
		for(Ball ball : ballArray) {
			Vector3d substratumPos = new Vector3d(ball.pos);
			substratumPos.z = 0.0;
			new AnchorSpring(ball, substratumPos, model.anchorGliding);
		}

		// Add sibling springs, assuming all anchors in this cell are siblings
		for(int ii=0; ii<anchorSpringArray.size(); ii++) {
			for(int jj=ii+1; jj<anchorSpringArray.size(); jj++) {
				AnchorSpring anchor0 = anchorSpringArray.get(ii);
				AnchorSpring anchor1 = anchorSpringArray.get(jj);
				anchor0.siblingArray.add(anchor1);
				anchor1.siblingArray.add(anchor0);
			}
		}
		return anchorSpringArray.size();
	}
	

	public boolean IsFilament(Cell cell) {
		if(!this.filament && !cell.filament)	return false;
		
		if((mother!=null && mother.equals(cell)) || (cell.mother!=null && cell.mother.equals(this))) 		return true;
		else return false;
	}
	
	public int Stick(Cell cell) {
		int shape0 = model.shapeX[this.type];
		int shape1 = model.shapeX[cell.type];
		// Determine how many sticking springs we need
		int NSpring0 = 0, NSpring1 = 0;
		if(shape0==0) 						NSpring0 = 1; 
		else if(shape0==1 || shape0==2)		NSpring0 = 2; 
		else throw new IndexOutOfBoundsException("Cell type: " + type);
		if(shape1==0) 	NSpring1 = 1; 
		else if(shape1==1 || shape1==2) 	NSpring1 = 2; 
		else throw new IndexOutOfBoundsException("Cell type: " + cell.type);
		
		int NSpring = NSpring0 * NSpring1;
		Cell cell0, cell1;
		if((shape0==1 || shape0==2) && shape1==0) {		// Sphere goes first (see indexing next paragraph)
			cell0 = cell;
			cell1 = this;
		} else {										// Sphere goes first. Other cases, it doesn't matter
			cell0 = this;
			cell1 = cell;
		}
		
		StickSpring[] stickArray = new StickSpring[NSpring];
		for(int iSpring = 0; iSpring < NSpring; iSpring++) {					// Create all springs, including siblings, with input balls
			Ball ball0 = cell0.ballArray[iSpring/2];							// 0, 0, 1, 1, ...
			Ball ball1 = cell1.ballArray[iSpring%2];							// 0, 1, 0, 1, ...
			StickSpring spring = new StickSpring(	ball0, ball1);
			stickArray[iSpring] = spring;
		}
		
		// Define siblings, link them OPTIMISE
		for(int iSpring = 0; iSpring < NSpring; iSpring++) {				// For each spring and sibling spring			
			StickSpring spring = stickArray[iSpring];			
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
		for(Ball ball : ballArray) {
			amount += ball.n;
		}
		return amount;
	}
	
	public void SetAmount(double newAmount) {
		int shape = model.shapeX[type];
		if(shape==0) {
			ballArray[0].n = newAmount;
			ballArray[0].radius = ballArray[0].Radius();
		} else if(shape==1 || shape==2){
			ballArray[0].n = newAmount/2.0;
			ballArray[0].radius = ballArray[0].Radius();
			ballArray[1].n = newAmount/2.0;
			ballArray[1].radius = ballArray[1].Radius();
			// Reset rod spring length
			for(Spring rod : ballArray[0].cell.rodSpringArray) rod.ResetRestLength();
		} else {
			throw new IndexOutOfBoundsException("Cell type: " + type);
		}
	}
	
	public double SurfaceArea() {
		return SurfaceArea(1.0);
	}
	
	public double SurfaceArea(double scale) {
		int shape = model.shapeX[this.type];
		if(shape==0) {
			return 4*Math.PI * Math.pow(ballArray[0].radius*scale, 2);
		} else if(shape==1 || shape==2) 	{										// Assuming radii of balls are equal
			double Aballs = 4.0*Math.PI * Math.pow(ballArray[0].radius*scale, 2); 	// Two half balls
			double height = rodSpringArray.get(0).restLength*scale;					// height == distance between balls
			double Acyl = 	2.0*Math.PI * ballArray[0].radius*scale * height;		// area of wall of cylinder
			return Aballs + Acyl;
		} else {
			throw new IndexOutOfBoundsException("Cell type: " + type);
		}
	}
	
	public double Volume() {
		return Volume(1.0);
	}
	
	public double Volume(double scale) {
		int shape = model.shapeX[this.type];
		if(shape==0) 										return 4.0/3.0*Math.PI*Math.pow(ballArray[0].radius*scale, 3); 
		else if(shape==1 || shape==2) 						return 4.0/3.0*Math.PI*Math.pow(ballArray[0].radius*scale, 3)  +  Math.PI*Math.pow(ballArray[0].radius*scale, 2)*rodSpringArray.get(0).restLength*scale;
		else throw new IndexOutOfBoundsException("Cell type: " + type);
	}
	
	public double GetDistance(Cell cell) {									// This method probably has a higher overhead than the code in CollisionDetection
		int shape0 = model.shapeX[this.type];
		int shape1 = model.shapeX[cell.type];
		if(shape0==0) {														// Sphere-???
			if(shape1==0)	{												// Sphere-sphere
				return cell.ballArray[0].pos.minus(ballArray[0].pos).norm();
			} else if(shape1==1 || shape1==2) {								// Sphere-rod
				ericson.ReturnObject C = ericson.DetectCollision.LinesegPoint(cell.ballArray[0].pos, cell.ballArray[1].pos, this.ballArray[0].pos);
				return C.dist;
			} else {														// Unknown!
				throw new IndexOutOfBoundsException("Cell type: " + cell.type);
			}
		} else if(shape0==1 || shape0==2) {									// Rod-???
			if(shape1==0) {													// Rod-sphere
				ericson.ReturnObject C = ericson.DetectCollision.LinesegPoint(this.ballArray[0].pos, this.ballArray[1].pos, cell.ballArray[0].pos);
				return C.dist; 
			} else if(shape1==1 || shape1==2) {								// Rod-rod
				ericson.ReturnObject C = ericson.DetectCollision.LinesegLineseg(this.ballArray[0].pos, this.ballArray[1].pos, cell.ballArray[0].pos, cell.ballArray[1].pos);
				return C.dist;		
			} else {															// Unknown!
				throw new IndexOutOfBoundsException("Cell type: " + cell.type);
			}
		} else {
			throw new IndexOutOfBoundsException("Cell type: " + this.type);
		}
	}
	
	public Cell GetNeighbour() {										// Returns neighbour of cell in straight filament. Not of branched
		for(FilSpring fil : filSpringArray) { 							// TODO: what if there are two neighbours?
			if(fil.type==3) { 											// sphere-sphere fil
				if(fil.ballArray[0] == ballArray[0])		return fil.ballArray[1].cell;
				if(fil.ballArray[1] == ballArray[0])		return fil.ballArray[0].cell;
			}
			if(fil.type==4) {											// Get the other cell in the straight filament, via short spring
				if(fil.ballArray[0] == ballArray[1])		return fil.ballArray[1].cell;		// We only look at ball1, so we're already excluding mother (that is connected at ball0)
				if(fil.ballArray[1] == ballArray[1])		return fil.ballArray[0].cell; 
			}
		}
		// Nothing found
		return null;
	}
	
	public void Remove() {
		for(StickSpring spring : stickSpringArray) {
			spring.Break();
		}
		for(AnchorSpring spring : anchorSpringArray) {
			spring.Break();
		}
		for(FilSpring spring : filSpringArray) {
			spring.Break();
		}
		for(RodSpring spring : rodSpringArray) {
			spring.Break();
		}
		for(Ball ball : ballArray) {
			ball.Remove();
		}
		model.cellArray.remove(this);
		// Error checking. TODO: This can probably be removed
		for(Cell cell : model.cellArray) {
			if(cell.stickCellArray.contains(this)) {
				throw new RuntimeException("Cell " + this.Index() + " was not properly removed from model");
			}
		}
		for(Ball ball : this.ballArray) {
			for(StickSpring spring : model.stickSpringArray) {
				if(spring.ballArray[0] == ball || spring.ballArray[1] == ball) {		// Compare by reference
					throw new RuntimeException("A ball in cell " + this.Index() + " was not properly removed from model (via stick)");
				}
			}
			for(AnchorSpring spring : model.anchorSpringArray) {
				if(spring.ballArray[0] == ball) {		// Compare by reference
					throw new RuntimeException("A ball in cell " + this.Index() + " was not properly removed from model (via anchor)");
				}
			}
			for(FilSpring spring : model.filSpringArray) {
				if(spring.ballArray[0] == ball || spring.ballArray[1] == ball) {		// Compare by reference
					throw new RuntimeException("A ball in cell " + this.Index() + " was not properly removed from model (via filament)");
				}
			}
			for(RodSpring spring : model.rodSpringArray) {
				if(spring.ballArray[0] == ball || spring.ballArray[1] == ball) {		// Compare by reference
					throw new RuntimeException("A ball in cell " + this.Index() + " was not properly removed from model (via rod)");
				}
			}
		}		
	}
}


