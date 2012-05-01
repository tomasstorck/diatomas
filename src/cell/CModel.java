package cell;

// Import Java stuff
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import random.rand;

// Import MATLAB IO stuff
import jmatio.*;
import linuxinteractor.LinuxInteractor;

// Import NR stuff
import NR.*;

public class CModel {
	// Model properties
	String name;
	// Spring constants
	double K1;
	double Kf;
	double Kw;
	double Kc;
	double Ks;
	double Ka;
	double Kd;
	// Domain properties
	double G;
	double rho_w;
	double rho_m;
	Vector3d L;
	int randomSeed;
	// Cell properties
	int NType;
	int NInitCell;
	double aspect;
	// Ball properties
	double MCellInit;
	double MCellMax;
	// Progress
	double growthTime;
	double growthTimeStep;
	int  growthIter;
	double growthMaxIter;
	double movementTime;
	double movementTimeStep;
	double movementTimeEnd;
	int movementIter;
	// Counters
	static int NBall;
//	NBall; NCell; NSpring; NFilament; <--- disabled, more reliable and fast enough with length(), used in MEX though, reconstructed there for speed and to prevent errors
	// Arrays
	ArrayList<CCell> cellArray;
//	ArrayList<CBall> ballArray = new ArrayList<CBall>(NInitCell*NType);
	ArrayList<CStickSpring> stickSpringArray;
	ArrayList<CFilSpring> filSpringArray;
	ArrayList<CAnchorSpring> anchorSpringArray;

	//////////////////////////////////////////////////////////////////////////////////////////
	
	//////////////////
	// Constructors //
	//////////////////
	public CModel(String name) {	// Default constructor, includes default values
		this.name  = name;
	}
	
	public void LoadDefaultParameters() {
		K1 		= 0.5e-2;			// Cell spring
		Kf 		= 0.1e-4;			// filament spring
		Kw 		= 0.5e-5;			// wall spring
		Kc 		= 0.1e-4;			// collision
		Ks 		= 0.5e-5;			// sticking
		Ka 		= 0.5e-5;			// anchor
		Kd 		= 1e-9;				// drag force coefficient
		// Domain properties
		G		= -9.8;				// [m/s2], acceleration due to gravity
		rho_w	= 1000;				// [kg/m3], density of bulk liquid (water)
		rho_m	= 1100;				// [kg/m3], diatoma density
		L 		= new Vector3d(1200e-6, 300e-6, 1200e-6);	// [m], Dimensions of domain
		randomSeed = 1;
		// Cell properties
		NType 	= 2;				// Types of cell
		NInitCell = 15;				// Initial number of cells
		aspect	= 2;				// Aspect ratio of cells
		// Ball properties
		MCellInit = 1e-11;			// kg
		MCellMax = 2e-11; 			// max mass of cells before division
		// Progress
		growthTime = 0;				// [s] Current time for the growth
		growthTimeStep = 900;		// [s] Time step for growth
		growthIter = 0;				// [-] Counter time iterations for growth
		growthMaxIter = 672;		// [-] where we'll stop
		movementTime = 0;			// [s] initial time for movement (for ODE solver)
		movementTimeStep = 2e-2;	// [s] output time step  for movement
		movementTimeEnd	= 10e-2;	// [s] time interval for movement (for ODE solver), 5*movementTimeStep by default
		movementIter = 0;			// [-] counter time iterations for movement
		// Counters
		NBall 	= 0;
		// Arrays
		cellArray = new ArrayList<CCell>(NInitCell);
		stickSpringArray = new ArrayList<CStickSpring>(NInitCell);
		filSpringArray = new ArrayList<CFilSpring>(NInitCell);
		anchorSpringArray = new ArrayList<CAnchorSpring>(NInitCell);
	}
	
	///////////////////////
	// Get and Set stuff //
	///////////////////////
	public CBall[] BallArray() {
		CBall[] ballArray = new CBall[NBall];
		int iBall = 0;
		for(int iCell=0; iCell < cellArray.size(); iCell++) {
			CCell pCell = cellArray.get(iCell);
			int NBallInCell = (pCell.type==0) ? 1 : 2;
			for(int iBallInCell=0; iBallInCell < NBallInCell; iBallInCell++) {
				ballArray[iBall] = pCell.ballArray[iBallInCell];
				iBall++;
			}
		}
		return ballArray;
	}
	
	////////////////////////////
	// Saving, loading things //
	////////////////////////////
	public void Save() {
		MLStructure mlModel = new MLStructure("model", new int[] {1,1});
		mlModel.setField("aspect", 				new MLDouble(null, new double[] {aspect}, 1));
		mlModel.setField("G", 					new MLDouble(null, new double[] {G}, 1));
		mlModel.setField("growthIter", 			new MLDouble(null, new double[] {aspect}, 1));
		mlModel.setField("growthMaxIter",		new MLDouble(null, new double[] {growthMaxIter}, 1));
		mlModel.setField("growthTime",			new MLDouble(null, new double[] {growthTime}, 1));
		mlModel.setField("growthTimeStep",		new MLDouble(null, new double[] {growthTimeStep}, 1));
		mlModel.setField("K1",					new MLDouble(null, new double[] {K1}, 1));
		mlModel.setField("Ka",					new MLDouble(null, new double[] {Ka}, 1));
		mlModel.setField("Kc",					new MLDouble(null, new double[] {Kc}, 1));
		mlModel.setField("Kd",					new MLDouble(null, new double[] {Kd}, 1));
		mlModel.setField("Kf",					new MLDouble(null, new double[] {Kf}, 1));
		mlModel.setField("Ks",					new MLDouble(null, new double[] {Ks}, 1));
		mlModel.setField("Kw",					new MLDouble(null, new double[] {Kw}, 1));
		mlModel.setField("L",					new MLDouble(null, new double[] {L.x, L.y, L.z}, 3));
		mlModel.setField("MCellInit",			new MLDouble(null, new double[] {MCellInit}, 1));
		mlModel.setField("MCellMax",			new MLDouble(null, new double[] {MCellMax}, 1));
		mlModel.setField("movementIter",		new MLDouble(null, new double[] {movementIter}, 1));
		mlModel.setField("movementTime",		new MLDouble(null, new double[] {movementTime}, 1));
		mlModel.setField("movementTimeEnd",		new MLDouble(null, new double[] {movementTimeEnd}, 1));
		mlModel.setField("movementTimeStep",	new MLDouble(null, new double[] {movementTimeStep}, 1));
		mlModel.setField("name",				new MLChar(null, new String[] {name}, 1));
		mlModel.setField("NInitCell",			new MLDouble(null, new double[] {NInitCell}, 1));
		mlModel.setField("NType",				new MLDouble(null, new double[] {NType}, 1));
		mlModel.setField("randomSeed",			new MLDouble(null, new double[] {randomSeed}, 1));
		mlModel.setField("rho_m",				new MLDouble(null, new double[] {rho_m}, 1));
		mlModel.setField("rho_w",				new MLDouble(null, new double[] {rho_w}, 1));
		// anchorSpringArray
		int NAnchor = anchorSpringArray.size();
		MLCell mlAnchorSpringArray = new MLCell(null, new int[] {NAnchor,1});
		for(int iAnchor=0; iAnchor<NAnchor; iAnchor++) {
			CAnchorSpring pAnchor = anchorSpringArray.get(iAnchor);
			MLStructure mlAnchor = new MLStructure(null, new int[] {1,1});
			mlAnchor.setField("anchor", 		new MLDouble(null, new double[] {pAnchor.anchor.x, pAnchor.anchor. y, pAnchor.anchor.z}, 3));
			mlAnchor.setField("cellArrayIndex", new MLDouble(null, new double[] {pAnchor.pBall.pCell.cellArrayIndex+1}, 1));		// +1 for 0 vs 1 based indexing in Java vs MATLAB  
			mlAnchor.setField("ballArrayIndex", new MLDouble(null, new double[] {pAnchor.pBall.ballArrayIndex+1}, 1));
			mlAnchor.setField("K",				new MLDouble(null, new double[] {pAnchor.K}, 1));
			mlAnchor.setField("restLength",		new MLDouble(null, new double[] {pAnchor.restLength}, 1));
			mlAnchor.setField("anchorArrayIndex", new MLDouble(null, new double[] {pAnchor.anchorArrayIndex+1}, 1));
			if(pAnchor.pBall.pCell.type!=0) {
				mlAnchor.setField("siblingArrayIndex", new MLDouble(null, new double[] {pAnchor.anchorArrayIndex+1}, 1));
			}
			mlAnchorSpringArray.set(mlAnchor, iAnchor);
		}
		mlModel.setField("anchorSpringArray", mlAnchorSpringArray);
		// cellArray
		int Ncell = cellArray.size();
		MLCell mlCellArray = new MLCell(null, new int[] {Ncell,1});
		for(int iCell=0; iCell<Ncell; iCell++) {
			CCell pCell = cellArray.get(iCell);
			MLStructure mlCell = new MLStructure(null, new int[] {1,1});
			mlCell.setField("cellArrayIndex", 	new MLDouble(null, new double[] {pCell.cellArrayIndex+1}, 1));
			mlCell.setField("colour", 			new MLDouble(null, new double[] {pCell.colour[0], pCell.colour[1], pCell.colour[2]}, 3));
			mlCell.setField("filament", 		new MLDouble(null, new double[] {pCell.filament?1:0}, 1));
			mlCell.setField("type", 			new MLDouble(null, new double[] {pCell.type}, 1));
			// ballArray
			int Nball = (pCell.type==0 ? 1 : 2);
			MLCell mlBallArray = new MLCell(null,new int[] {Nball,1});
			for(int iBall=0; iBall<Nball; iBall++) {
				CBall pBall = pCell.ballArray[iBall];
				MLStructure mlBall = new MLStructure(null, new int[] {1,1});
				mlBall.setField("pos", 			new MLDouble(null, new double[] {pBall.pos.x, pBall.pos.y, pBall.pos.z}, 3));
				mlBall.setField("vel", 			new MLDouble(null, new double[] {pBall.vel.x, pBall.vel.y, pBall.vel.z}, 3));
				mlBall.setField("force",	 	new MLDouble(null, new double[] {pBall.force.x, pBall.force.y, pBall.force.z}, 3));
				mlBall.setField("ballArrayIndex", new MLDouble(null, new double[] {pBall.ballArrayIndex+1}, 1));
				mlBall.setField("mass", 		new MLDouble(null, new double[] {pBall.mass}, 1));
				mlBall.setField("radius", 		new MLDouble(null, new double[] {pBall.radius}, 1));
				mlBallArray.set(mlBall,iBall);
			}
			mlCell.setField("ballArray", mlBallArray);
			// springArray
			MLCell mlSpringArray = new MLCell(null,new int[] {1,1});
			if(pCell.type!=0) {
				MLStructure mlSpring = new MLStructure(null,new int[] {1,1});
				CSpring pSpring = pCell.springArray[0];
				mlSpring.setField("cellArrayIndex",	new MLDouble(null, new double[] {pSpring.ballArray[0].pCell.cellArrayIndex+1, pSpring.ballArray[1].pCell.cellArrayIndex+1}, 2));
				mlSpring.setField("ballArrayIndex",	new MLDouble(null, new double[] {pSpring.ballArray[0].ballArrayIndex+1, pSpring.ballArray[1].ballArrayIndex+1}, 2));
				mlSpring.setField("K",				new MLDouble(null, new double[] {pSpring.K}, 1));
				mlSpring.setField("restLength",		new MLDouble(null, new double[] {pSpring.restLength}, 1));	
				mlSpringArray.set(mlSpring, 0);
				mlCell.setField("springArray", mlSpringArray);
			}
			mlCellArray.set(mlCell, iCell);
		}
		mlModel.setField("cellArray", mlCellArray);
		// stickSpringArray
		int NStick = stickSpringArray.size();
		MLCell mlStickSpringArray = new MLCell(null, new int[] {NStick,1});
		for(int iStick=0; iStick<NStick; iStick++) {
			CStickSpring pStick = stickSpringArray.get(iStick);
			MLStructure mlStick = new MLStructure(null, new int[] {1,1});
			mlStick.setField("cellArrayIndex", 	new MLDouble(null, new double[] {pStick.ballArray[0].pCell.cellArrayIndex+1, pStick.ballArray[1].pCell.cellArrayIndex+1}, 1));
			mlStick.setField("ballArrayIndex", 	new MLDouble(null, new double[] {pStick.ballArray[0].ballArrayIndex+1, pStick.ballArray[1].ballArrayIndex+1}, 1));
			mlStick.setField("K",				new MLDouble(null, new double[] {pStick.K}, 1));
			mlStick.setField("restLength",		new MLDouble(null, new double[] {pStick.restLength}, 1));
			mlStick.setField("stickArrayIndex", new MLDouble(null, new double[] {pStick.stickArrayIndex+1}, 1));
			if(pStick.ballArray[0].pCell.type!=0) {
				if(pStick.ballArray[1].pCell.type==0) {
					mlStick.setField("siblingArrayIndex", new MLDouble(null, new double[] {pStick.siblingArray.get(0).stickArrayIndex+1}, 1));
				} else {
					mlStick.setField("siblingArrayIndex", new MLDouble(null, new double[] {pStick.siblingArray.get(0).stickArrayIndex+1, pStick.siblingArray.get(1).stickArrayIndex+1, pStick.siblingArray.get(2).stickArrayIndex+1}, 3));
				}
			}
			mlStickSpringArray.set(mlStick, iStick);
		}
		mlModel.setField("stickSpringArray", mlStickSpringArray);
		// Create a list and add mlModel
		ArrayList<MLArray> list = new ArrayList<MLArray>(1);
		list.add(mlModel);
		
		try {
			new MatFileWriter(name + "/output/m" + movementIter + "g" + growthIter + ".mat",list);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void Load(String fileName) {
		try {
			MatFileReader mlModel = new MatFileReader(fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/////////////////
	// Log writing //
	/////////////////
	public void Write(String message, String format, boolean suppressFileOutput) {
		// Construct date and time
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		// Extract format from input arguments
		String prefix = "   ";
		String suffix = "";
		if(format.equalsIgnoreCase("iter")) 	{suffix = " (" + movementIter + "/" + growthIter + ")";} 	else
			if(format.equalsIgnoreCase("warning")) 	{prefix = " WARNING: ";} 									else
				if(format.equalsIgnoreCase("error")) 	{prefix = " ERROR: ";}
		String string = dateFormat.format(cal.getTime()) + prefix + message + suffix;
		// Write to console
		System.out.println(string);
		// Write to file
		if(!suppressFileOutput) {
			try {
				if(!(new File(name)).exists()) {
					new File(name).mkdir();
				}
				PrintWriter fid = new PrintWriter(new FileWriter(name + "/" + "logfile.txt",true));		// True is for append // Not platform independent TODO
				fid.println(string);
				fid.close();
			} catch(IOException E) {
				E.printStackTrace();
			}
		}
	}
	
	public void Write(String message, String format) {
		Write(message,format,false);
	}
	
	//////////////////////////
	// Collision detection  //
	//////////////////////////
	public ArrayList<CCell> DetectFloorCollision() {
		ArrayList<CCell> collisionCell = new ArrayList<CCell>();
		for(CCell pCell : cellArray) {
			int NBall = (pCell.type==0) ? 1 : 2;	// Figure out number of balls based on type
			for(int iBall=0; iBall<NBall; iBall++) {
				CBall pBall = pCell.ballArray[iBall];
				if(pBall.pos.y - pBall.radius < 0) {
					collisionCell.add(pCell);
					break;
				}
			}
		}
		return collisionCell;
	}
	
	public ArrayList<CCell> DetectCellCollision_Simple() {				// Using ArrayList, no idea how big this one will get
		ArrayList<CCell> collisionCell = new ArrayList<CCell>();
		
		CBall[] ballArray = BallArray();
		for(int iBall=0; iBall<NBall; iBall++) {						// If we stick to indexing, it'll be easier to determine which cells don't need to be analysed
			CBall pBall = ballArray[iBall];
			for(int iBall2 = iBall+1; iBall2<NBall; iBall2++) {
				CBall pBall2 = ballArray[iBall2];
				if(pBall.pCell.cellArrayIndex!=pBall2.pCell.cellArrayIndex) {
					Vector3d diff = pBall2.pos.minus(pBall.pos);
					if(Math.abs(diff.length()) - pBall.radius - pBall2.radius < 0) {
						collisionCell.add(pBall.pCell);
						collisionCell.add(pBall2.pCell);
					}
				}
			}
		}
		return collisionCell;
	}
	
	///////////////////////////////
	// Spring breakage detection //
	///////////////////////////////
	public ArrayList<CAnchorSpring> DetectAnchorBreak() {
		double maxStretch = 1.2; 
		double minStretch = 0.8;
		ArrayList<CAnchorSpring> breakArray = new ArrayList<CAnchorSpring>();
		
		for(CAnchorSpring pSpring : anchorSpringArray) {
			double al = (pSpring.pBall.pos.minus(pSpring.anchor)).length();		// al = Actual Length
			if(al < minStretch*pSpring.restLength || al > maxStretch*pSpring.restLength) {
				breakArray.add(pSpring);
			}
		}
		return breakArray;
	}
	
	public ArrayList<CStickSpring> DetectStickBreak() {
		double maxStretch = 1.2; 
		double minStretch = 0.8;
		ArrayList<CStickSpring> breakArray = new ArrayList<CStickSpring>();
		
		int iSpring = 0;
		while(iSpring < stickSpringArray.size()) {
			CStickSpring pSpring = stickSpringArray.get(iSpring);
			double al = (pSpring.ballArray[1].pos.minus(  pSpring.ballArray[0].pos)  ).length();		// al = Actual Length
			if(al < minStretch*pSpring.restLength || al > maxStretch*pSpring.restLength) {				// TODO might be nice to just break overstretched springs
				breakArray.add(pSpring);
//				breakArray.addAll(pSpring.siblingArray);												// We'll do this in StickBreak
			}
			iSpring += pSpring.siblingArray.size()+1;
		}
		return breakArray;
	} 
	

	////////////////////
	// Movement stuff //
	////////////////////
	public int Movement() throws Exception {
		int nvar = 6*CModel.NBall;
		int ntimes = (int) (movementTimeEnd/movementTimeStep);
		double atol = 1.0e-6, rtol = atol;
		double h1 = 0.00001, hmin = 0;
		double t1 = movementTime; 
		double t2 = t1 + movementTime + movementTimeEnd;
		Vector ystart = new Vector(nvar,0.0);

		int ii=0;											// Determine initial value vector
		for(CBall pBall : BallArray()) { 
			ystart.set(ii++, pBall.pos.x);
			ystart.set(ii++, pBall.pos.y);
			ystart.set(ii++, pBall.pos.z);
			ystart.set(ii++, pBall.vel.x);
			ystart.set(ii++, pBall.vel.x);
			ystart.set(ii++, pBall.vel.x);
		}
		Output<StepperDopr853> out = new Output<StepperDopr853>(ntimes);
		feval dydt = new feval(this);
		Odeint<StepperDopr853> ode = new Odeint<StepperDopr853>(ystart, t1, t2, atol, rtol, h1, hmin, out, dydt);
		int nstp = ode.integrate();
		for(int iTime=0; iTime<out.count; iTime++) {
			int iVar = 0;
			for(CBall pBall : BallArray()) {
				pBall.pos.x = out.ysave.get(iVar++,iTime);
				pBall.pos.y = out.ysave.get(iVar++,iTime);
				pBall.pos.z = out.ysave.get(iVar++,iTime);
				pBall.vel.x = out.ysave.get(iVar++,iTime);
				pBall.vel.y = out.ysave.get(iVar++,iTime);
				pBall.vel.z = out.ysave.get(iVar++,iTime);
			}
			// save POV TODO
		}
		return nstp;
	}
	
	public Vector CalculateForces(double t, Vector yode) {	// This function gets called again and again --> not very efficient to import/export every time TODO
		// Read data from y
		int ii=0; 				// Where we are in yode
		for(CBall pBall : BallArray()) {
			pBall.pos.x = 	yode.get(ii++);
			pBall.pos.y = 	yode.get(ii++);
			pBall.pos.z = 	yode.get(ii++);
			pBall.vel.x = 	yode.get(ii++);
			pBall.vel.y = 	yode.get(ii++);
			pBall.vel.z = 	yode.get(ii++);
			pBall.force.x = 0;	// Clear forces for first use
			pBall.force.y = 0;
			pBall.force.z = 0;
		}
		
		// Calculate gravity+bouyancy, normal forces and drag
		for(CBall pBall : BallArray()) {
			// Contact forces
			double y = pBall.pos.y;
			double r = pBall.radius;
			if(y<r){
				pBall.force.y += Kw*(r-y);
			}
			// Gravity and buoyancy
			if(y>r) {			// Only if not already at the floor 
				pBall.force.y += G * ((rho_m-rho_w)/rho_w) * pBall.mass ;  //let the ball fall 
			}
			// Velocity damping
			pBall.force.x -= Kd*pBall.vel.x;
			pBall.force.y -= Kd*pBall.vel.y;
			pBall.force.z -= Kd*pBall.vel.z;
		}
		
		// Return results
		Vector dydx = new Vector(yode.size());
		ii=0;
		for(CBall pBall : BallArray()) {
				double M = pBall.mass;
				dydx.set(ii++,pBall.vel.x);			// dpos/dt = v;
				dydx.set(ii++,pBall.vel.y);
				dydx.set(ii++,pBall.vel.z);
				dydx.set(ii++,pBall.force.x/M);		// dvel/dt = a = f/M
				dydx.set(ii++,pBall.force.y/M);
				dydx.set(ii++,pBall.force.z/M);
		}
		return dydx;
	}
	
	//////////////////
	// Growth stuff //
	//////////////////
	public int GrowCell() {
		int newCell = 0;
		int NCell = cellArray.size();
		for(int iCell=0; iCell<NCell; iCell++){
			CCell pCell = cellArray.get(iCell);
			double mass = pCell.GetMass();

			// Random growth
			mass *= (0.95+rand.Double()/5);
			// Syntrophic growth
			for(CCell pStickCell : pCell.stickCellArray) {
				if((pCell.type==0 && pStickCell.type!=0) || (pCell.type!=0 && pStickCell.type==0)) {
					// The cell types are not different on the other end of the spring
					mass *= 1.2;
					break;
				}
			}
			
			// Cell growth or division
			if(mass>MCellMax) {
				newCell++;
				if(pCell.type==0) {
					// Come up with a nice direction in which to place the new cell
					Vector3d direction = new Vector3d(rand.Double(),rand.Double(),rand.Double());			// TODO make the displacement go into any direction			
					direction.normalise();
					double displacement = pCell.ballArray[0].radius;
					// Make a new, displaced cell
					CCell pNew = new CCell(0,															// Same type as pCell
							pCell.ballArray[0].pos.x - displacement * direction.x,						// The new location is the old one plus some displacement					
							pCell.ballArray[0].pos.y - displacement * direction.y,	
							pCell.ballArray[0].pos.z - displacement * direction.z,
							pCell.filament,this);														// Same filament boolean as pCell and pointer to the model
					// Set mass for both cells
					pNew.SetMass(mass/2);		// Radius is updated in this method
					pCell.SetMass(mass/2);
					// Set properties for new cell
					pNew.ballArray[0].vel = 	pCell.ballArray[0].vel;
					pNew.ballArray[0].force = 	pCell.ballArray[0].force;
					pNew.colour =				pCell.colour;
					pNew.mother = 				pCell;
					// Displace old cell
					pCell.ballArray[0].pos.plus(  direction.times( displacement )  );
					// Contain cells to y dimension of domain
					if(pCell.ballArray[0].pos.y < pCell.ballArray[0].radius) {pCell.ballArray[0].pos.y = pCell.ballArray[0].radius;};
					if(pNew.ballArray[0].pos.y  < pNew.ballArray[0].radius)  {pNew.ballArray[0].pos.y = pNew.ballArray[0].radius;};
					// Set filament springs
					if(pNew.filament) {
						pNew.Stick(pCell);		// but why? TODO
					}
				} else {
					CBall pBall0 = pCell.ballArray[0];
					CBall pBall1 = pCell.ballArray[1];
					//Direction
					Vector3d direction = pBall1.pos.minus( pBall0.pos );
					direction.normalise();
					
					double displacement; 																// Should be improved/made to make sense (TODO)
					if(pCell.type==1) {
						displacement = pBall0.radius*Math.pow(2,-0.666666);							// A very strange formula: compare our radius to the C++ equation for Rpos and you'll see it's the same
					} else {
						displacement = pBall1.radius/2;
					}
					
					// Make a new, displaced cell
					Vector3d middle = pBall1.pos.plus(pBall0.pos).divide(2); 
					CCell pNew = new CCell(pCell.type,													// Same type as pCell
							middle.x+	  displacement*direction.x,										// First ball					
							middle.y+1.01*displacement*direction.y,										// possible TODO, ought to be displaced slightly in original C++ code but is displaced significantly this way (change 1.01 to 2.01)
							middle.z+	  displacement*direction.z,
							pBall1.pos.x,																// Second ball
							pBall1.pos.y,
							pBall1.pos.z,
							pCell.filament,this);														// Same filament boolean as pCell and pointer to the model
					// Set mass for both cells
					pNew.SetMass(mass/2);
					pCell.SetMass(mass/2);
					// Displace old cell, 2nd ball
					pBall1.pos = middle.minus(direction.times(displacement));
					pCell.springArray[0].Reset();
					// Contain cells to y dimension of domain
					for(int iBall=0; iBall<2; iBall++) {
						if(pCell.ballArray[iBall].pos.y < pCell.ballArray[iBall].radius) {pCell.ballArray[0].pos.y = pCell.ballArray[0].radius;};
						if( pNew.ballArray[iBall].pos.y <  pNew.ballArray[iBall].radius) { pNew.ballArray[0].pos.y =  pNew.ballArray[0].radius;};
					}
					// Set properties for new cell
					for(int iBall=0; iBall<2; iBall++) {
						pNew.ballArray[iBall].vel = 	pCell.ballArray[iBall].vel;
						pNew.ballArray[iBall].force = 	pCell.ballArray[iBall].force;
					}
					pNew.colour =	pCell.colour;
					pNew.mother = 	pCell;
					pNew.springArray[0].restLength = pCell.springArray[0].restLength;

					// Set filament springs
					if(pNew.filament) {
						for(CFilSpring pFilSpring : filSpringArray) {
							if( pFilSpring.bigSpring.ballArray[0]== pBall0) {
								pFilSpring.bigSpring.ballArray[0] = pNew.ballArray[0];}
							if( pFilSpring.bigSpring.ballArray[1]== pBall0) {
								pFilSpring.bigSpring.ballArray[1] = pNew.ballArray[0];}
							if( pFilSpring.smallSpring.ballArray[0]== pBall1) {
								pFilSpring.smallSpring.ballArray[0] = pNew.ballArray[1];}
							if( pFilSpring.smallSpring.ballArray[1]== pBall1) {
								pFilSpring.smallSpring.ballArray[1] = pNew.ballArray[1];}
						}
						new CFilSpring(pCell,pNew);
					}
				}

			} else {		
				// Simply increase mass and reset spring
				pCell.SetMass(mass);
				if(pCell.type>0) pCell.springArray[0].Reset();
			}
		}
		return newCell;
	}
	
	////////////////////////////
	// Anchor and Stick stuff //
	////////////////////////////
	public void BreakStick(ArrayList<CStickSpring> breakArray) {
		for(CStickSpring pSpring : breakArray) {
			CCell pCell0 = pSpring.ballArray[0].pCell;
			CCell pCell1 = pSpring.ballArray[1].pCell;
			// Remove cells from each others' stickCellArray 
			pCell0.stickCellArray.remove(pCell1);
			pCell1.stickCellArray.remove(pCell0);
			// Remove springs from model stickSpringArray
			stickSpringArray.remove(pSpring);
			stickSpringArray.removeAll(pSpring.siblingArray);
		}
	}
	
	public int BuildAnchor(ArrayList<CCell> collisionArray) {
		for(CAnchorSpring pSpring : anchorSpringArray) collisionArray.remove(pSpring.pBall.pCell);
		// Anchor the non-stuck, collided cells to the ground
		for(CCell pCell : collisionArray) {pCell.Anchor();}
		return anchorSpringArray.size();
	}
	
	public int BuildStick(ArrayList<CCell> collisionArray) {
		int counter = 0;
		for(int ii=0; ii<collisionArray.size(); ii+=2) {		// For loop works per duo
			boolean setStick = true;
			CCell pCell0 = collisionArray.get(ii);
			CCell pCell1 = collisionArray.get(ii+1);
			// Check if already stuck, don't stick if that is the case
			for(CStickSpring pSpring : stickSpringArray) {		// This one should update automatically after something new has been stuck --> Only new ones are stuck AND, as a result, only uniques are sticked 
				if((pSpring.ballArray[0].equals(pCell0) && pSpring.ballArray[0].equals(pCell1)) || (pSpring.ballArray[0].equals(pCell1) && pSpring.ballArray[0].equals(pCell0))) {
					setStick = false;
					break;										// We've found a duplicate, don't stick this one
				}
			}
			if(setStick) {
				pCell0.Stick(pCell1);
				counter++;}
		}
		return counter;
	}
	
	///////////////////
	// POV-Ray stuff //
	///////////////////
	public void POV_Write() {
		if(!(new File(name)).exists()) {
			new File(name).mkdir();
		}
		if(!(new File(name + "/ouptut")).exists()) {
			new File(name + "/output").mkdir();
		}
		
		try {
			String fileName = String.format("%s/output/pov.%04d.%04d.inc", name, movementIter,growthIter); 
			PrintWriter fid = new PrintWriter(new FileWriter(fileName,true));		// True is for append // Not platform independent TODO
			// Build spheres and rods
			for(int iCell=0; iCell<cellArray.size(); iCell++) {
				CCell pCell = cellArray.get(iCell);
				fid.println("// Cell no. " + iCell);
				if(pCell.type == 0) {
					// Spherical cell
					CBall pBall = pCell.ballArray[0];

					fid.println("sphere\n" + 
							"{\n" + 
							String.format("\t < %10.3f,%10.3f,%10.3f > \n", pBall.pos.x*1e6, pBall.pos.y*1e6, pBall.pos.z*1e6) + 
							String.format("\t%10.3f\n", pBall.radius*1e6) +
							"\ttexture{\n" + 
							"\t\tpigment{\n" +
							String.format("\t\t\tcolor rgb<%10.3f,%10.3f,%10.3f>\n", pCell.colour[0], pCell.colour[1], pCell.colour[2]) +
							"\t\t}\n" +
							"\t\tfinish{\n" +
							"\t\t\tambient .2\n" +
							"\t\t\tdiffuse .6\n" +
							"\t\t}\n" +
							"\t}\n" +
							"}\n");}
				else if(pCell.type == 1 || pCell.type == 2) {	// Rod
					CBall pBall = pCell.ballArray[0];
					CBall pBallNext = pCell.ballArray[1];

					fid.println("cylinder\n" +
							"{\n" +
							String.format("\t<%10.3f,%10.3f,%10.3f>,\n", pBall.pos.x*1e6, pBall.pos.y*1e6, pBall.pos.z*1e6) +
							String.format("\t<%10.3f,%10.3f,%10.3f>,\n", pBallNext.pos.x*1e6, pBallNext.pos.y*1e6, pBallNext.pos.z*1e6) +
							String.format("\t%10.3f\n", pBall.radius*1e6) +
							"\ttexture{\n" +
							"\t\tpigment{\n" +
							String.format("\t\t\tcolor rgb<%10.3f,%10.3f,%10.3f>\n", pCell.colour[0], pCell.colour[1], pCell.colour[2]) +
							"\t\t}\n" +
							"\t\tfinish{\n" +
							"\t\t\tambient .2\n" +
							"\t\t\tdiffuse .6\n" +
							"\t\t}\n" +
							"\t}\n" +
							"}\n" +
							"sphere\n" +		// New sphere
							"{\n" +
							String.format("\t < %10.3f,%10.3f,%10.3f > \n", pBall.pos.x*1e6, pBall.pos.y*1e6, pBall.pos.z*1e6) +
							String.format("\t%10.3f\n", pBall.radius*1e6) +
							"\ttexture{\n" +
							"\t\tpigment{\n" +
							String.format("\t\t\tcolor rgb<%10.3f,%10.3f,%10.3f>\n", pCell.colour[0], pCell.colour[1], pCell.colour[2]) +
							"\t\t}\n" +
							"\t\tfinish{\n" +
							"\t\t\tambient .2\n" +
							"\t\t\tdiffuse .6\n" +
							"\t\t}\n" +
							"\t}\n" +
							"}\n"+
							"sphere\n" +		// New sphere
							"{\n" +
							String.format("\t < %10.3f,%10.3f,%10.3f > \n", pBallNext.pos.x*1e6, pBallNext.pos.y*1e6, pBallNext.pos.z*1e6) +
							String.format("\t%10.3f\n", pBallNext.radius*1e6) +
							"\ttexture{\n" +
							"\t\tpigment{\n" +
							String.format("\t\t\tcolor rgb<%10.3f,%10.3f,%10.3f>\n", pCell.colour[0], pCell.colour[1], pCell.colour[2]) +
							"\t\t}\n" +
							"\t\tfinish{\n" +
							"\t\t\tambient .2\n" +
							"\t\t\tdiffuse .6\n" +
							"\t\t}\n" +
							"\t}\n" +
							"}\n");
				}
			}

			// Build filament springs
			for(int iFil = 0; iFil<filSpringArray.size(); iFil++) {
				for(int springType = 0; springType < 2; springType++) {
					fid.println("// Filament spring no. " + iFil);
					CSpring pSpring;
					double[] colour = new double[3];
					if(springType==0) {		// Set specific things for small spring and big spring
						pSpring = filSpringArray.get(iFil).bigSpring;
						colour[0] = 0; colour[1] = 0; colour[2] = 1;		// Big spring is blue
					} else {
						pSpring = filSpringArray.get(iFil).smallSpring;
						colour[0] = 1; colour[1] = 0; colour[2] = 0;		// Small spring is red
					}

					CBall pBall = pSpring.ballArray[0];
					CBall pBallNext = pSpring.ballArray[1];

					fid.println("cylinder\n" +
							"{\n" +
							String.format("\t<%10.3f,%10.3f,%10.3f>,\n", pBall.pos.x*1e6, pBall.pos.y*1e6, pBall.pos.z*1e6) +
							String.format("\t<%10.3f,%10.3f,%10.3f>,\n", pBallNext.pos.x*1e6, pBallNext.pos.y*1e6, pBallNext.pos.z*1e6) +
							String.format("\t%10.3f\n", pBall.radius*1e5) +
							"\ttexture{\n" +
							"\t\tpigment{\n" +
							String.format("\t\t\tcolor rgb<%10.3f,%10.3f,%10.3f>\n", colour[0], colour[1], colour[2]) +
							"\t\t}\n" +
							"\t\tfinish{\n" +
							"\t\t\tambient .2\n" +
							"\t\t\tdiffuse .6\n" +
							"\t\t}\n" +
							"\t}\n" +
							"}\n");
				}
			}

			// Build stick spring array
			for(int iStick = 0; iStick < stickSpringArray.size(); iStick++) {
				fid.println("// Sticking spring no. " + iStick);
				CStickSpring pSpring = stickSpringArray.get(iStick);
				CBall pBall = pSpring.ballArray[0];
				CBall pBallNext = pSpring.ballArray[1];

				fid.println("cylinder\n" +
						"{\n" +
						String.format("\t<%10.3f,%10.3f,%10.3f>,\n", pBall.pos.x*1e6, pBall.pos.y*1e6, pBall.pos.z*1e6) +
						String.format("\t<%10.3f,%10.3f,%10.3f>,\n", pBallNext.pos.x*1e6, pBallNext.pos.y*1e6, pBallNext.pos.z*1e6) +
						String.format("\t%10.3f\n", pBall.radius*1e5) +
						"\ttexture{\n" +
						"\t\tpigment{\n" +
						String.format("\t\t\tcolor rgb<%10.3f,%10.3f,%10.3f>\n", 0.0, 1.0, 0.0) +		//Sticking springs are green
						"\t\t}\n" +
						"\t\tfinish{\n" +
						"\t\t\tambient .2\n" +
						"\t\t\tdiffuse .6\n" +
						"\t\t}\n" +
						"\t}\n" +
						"}\n");
			}

			//Build anchor spring array
			for(int iAnchor = 1; iAnchor < anchorSpringArray.size(); iAnchor++) {
				fid.println("// Anchor spring no. " + iAnchor);
				CAnchorSpring pSpring = anchorSpringArray.get(iAnchor);
				CBall pBall = pSpring.pBall;

				if (!pSpring.anchor.equals(pBall.pos)) {		// else we get degenerate cylinders (i.e. height==0), POV doesn't like that
					fid.println("cylinder\n" +
							"{\n" +
							String.format("\t<%10.3f,%10.3f,%10.3f>,\n", pBall.pos.x*1e6, pBall.pos.y*1e6, pBall.pos.z*1e6) +
							String.format("\t<%10.3f,%10.3f,%10.3f>,\n", pSpring.anchor.x*1e6, pSpring.anchor.y*1e6, pSpring.anchor.z*1e6) +
							String.format("\t%10.3f\n", pBall.radius*1e5) +	// Note 1e5 instead of 1e6 TODO
							"\ttexture{\n" +
							"\t\tpigment{\n" +
							String.format("\t\t\tcolor rgb<%10.3f,%10.3f,%10.3f>\n", 1.0, 1.0, 0.0) +		//Anchoring springs are yellow
							"\t\t}\n" +
							"\t\tfinish{\n" +
							"\t\t\tambient .2\n" +
							"\t\t\tdiffuse .6\n" +
							"\t\t}\n" +
							"\t}\n" +
							"}\n");
				}
			}
			// Done, clean up and catch errors
			fid.close();
		} catch(IOException E) {
			E.printStackTrace();
		}

	}
	
	public void POV_Plot(boolean boolWaitForFinish, boolean boolEchoCommand) {
		String input = "povray ../pov/tomas_persp_3D_java.pov +W1024 +H768 +K" + String.format("%04d",movementIter) + "." + String.format("%04d",growthIter) + " +O../" + name + "image/pov_" + String.format("m%04dg%04d", movementIter, growthIter) + " +A -J";
		LinuxInteractor.executeCommand("cd " + name + " ; " + input + " ; cd ..", boolWaitForFinish,boolEchoCommand);		// 1st true == wait for process to finish, 2nd true == tell command
	}
	
	public void POV_Plot() {
		POV_Plot(false,false);
	}
}