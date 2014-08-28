package ibm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince54Integrator;
import org.apache.commons.math3.ode.sampling.StepHandler;
import org.apache.commons.math3.ode.sampling.StepInterpolator;

import random.rand;

public class Model implements Serializable {
	// Set serializable information
	private static final long serialVersionUID = 1L;
	// Model miscellaneous settings
	public String name = "default";
	public int simulation = 0;					// The simulation type: see Run
	public int randomSeed = 1;
	public boolean comsol = false;
	// Domain properties
	public Vector3d L 	= new Vector3d(2e-6, 2e-6, 2e-6);
	public double rhoWater = 1000;				// [kg/m3], density of bulk liquid (water)
	public double rhoX	= 1010;					// [kg/m3], diatoma density
	public double MWX 	= 24.6e-3;				// [kg/mol], composition CH1.8O0.5N0.2
	public int NXType = 6;
	// --> Intracellular (rod) springs
	public double Kr 	= 5e-11;				// internal cell spring
	// --> Sticking
	public boolean sticking = false;
	public boolean[][] stickType = new boolean[NXType][NXType];
	public double Ks 	= 1e-11;
	public double stickStretchLim = 1e-6;		// Maximum tension for sticking springs
	public double stickFormLim = 0.5e-6; 		// Added to rest length to check if we should form sticking springs
	// --> Anchoring
	public boolean anchoring = false;
	public double Kan	= 1e-11;				// anchor
	public double anchorStretchLim = 1e-6;	// Maximum tension for anchoring springs
	public double anchorFormLim = 0.5e-6;		// Multiplication factor for rest length to form anchors. Note that actual rest length is the distance between the two, which could be less
	// --> Filaments
	public boolean filament = false;
	public boolean[] filType = new boolean[NXType];
	public double KfSphere 	= 2e-11;			// filament spring for sphere-sphere filial links
	public double[] KfRod 	= {2e-11, 2e-11};	// filament spring for rod-rod filial links {short spring, long spring}
	public boolean filSphereStraightFil = false;// Make streptococci-like structures if true, otherwise staphylococci
	public double filRodBranchFrequency = 0.0;// Which fraction of daughter cells form a branching filial link instead of a straight
	public double filStretchLim = 2e-6;		// Maximum tension for sticking springs
	public double filLengthSphere = 1.1;		// How many times R2 the sphere filament's rest length is
	public double[] filLengthRod = {0.5, 1.7};	// How many times R2 the rod filament's [0] short and [1] long spring rest length is
	// --> Gravity/buoyancy, drag and electrostatics
	public double G		= -9.8;					// [m/s2], acceleration due to gravity
	public boolean gravity = false;
	public boolean gravityZ = false;
	public double Kd 	= 1e-13;				// drag force coefficient
	public boolean electrostatic = false;
	public double kappa = 1.0/(20e-9);			// [1/m], inverse Debye length for ionic concentration of ... [Hermansson 1999]
	public double Ces	= 1e-22;				// Electrostatic grouped constants (excl. kappa)
	public double Cvdw  = 1e-31;				// van der Waals grouped constants 
	public double dlimFactor = 6.0;				// Multiplication factor to determine minimum distance in DLVO (dlimFactor*1/kappa)
	// --> Substratum and normal forces
	public boolean normalForce = false;			// Use normal force to simulate cells colliding with substratum (at y=0)
	public boolean initialAtSubstratum = false;	// All initial balls are positioned at y(t=0) = ball.radius
	// --> Collision forces
	public double Kc 	= 1e-10;					// cell-cell collision
	public double Kw 	= 1e-10;				// wall(substratum)-cell spring
	// Model biomass and growth properties
	public int NdComp = 5;						// d for dynamic compound (e.g. total Ac)
	public int NcComp = 8;						// c for concentration (or virtual compound, e.g. Ac-)
	public int NAcidDiss = 4; 					// Number of acid dissociation reactions
	public int NCellInit = 6;					// Initial number of cells
	public int NColoniesInit = 1;				// Initial number of colonies, in total containing NCellInit cells
	public double[] radiusCellMax = new double[NXType];
	public double[] radiusCellMin = new double[NXType];
	public double[] lengthCellMax = new double[NXType];
	public double[] lengthCellMin = new double[NXType];
	public double[] radiusCellStDev = new double[NXType];
	public double[] nCellMax =	new double[NXType];
	public double[] nCellMin =	new double[NXType];
	public double[] muAvgSimple = {0.33, 0.33, 0.33, 0.33, 0.33, 0.33};	// [h-1] 0.33  == doubling every 20 minutes. Only used in GrowthSimple!
	public double[] muStDev = {0.25, 0.25, 0.25, 0.25, 0.25, 0.25};	// Standard deviation. Only used in GrowthSimple()!    
	public double syntrophyFactor = 1.0; 		// Accelerated growth if two cells of different types are stuck to each other
	// Attachment
	public double attachmentRate = 0.0;			// [h-1] Number of cells newly attached per hour
	public int attachCellType = 0;				// What cell type the new cell is 
	public int[] attachNotTo = new int[0];		// Which cell types newly attached cells can NOT attach to
	public double attachCounter = 0.0;			// How many cells we will attach in this iteration
	// Progress
	public double growthTime = 0.0;				// [s] Current time for the growth
	public double growthTimeStep = 600.0;		// [s] Time step for growth
	public int growthIter = 0;					// [-] Counter time iterations for growth
	public int growthIterMax = Integer.MAX_VALUE;	// [-] Run infinitely long
	public double relaxationTime = 0.0;			// [s] initial time for relaxation (for ODE solver)
	public double relaxationTimeStepdt = 0.2;	// [s] output time step  for relaxation
	public double relaxationTimeStep = 1.0;		// [s] time interval for relaxation (for ODE solver), 5*relaxationTimeStep by default
	public int relaxationIter = 0;				// [-] counter time iterations for relaxation
	public int relaxationIterSuccessiveMax = 0;	// [-] how many successive iterations we limit relaxation to
	public boolean allowMovement = true;				// Whether we allow cells to continue moving or we keep relaxing them until relaxationIterSuccessiveMax is reached
	public boolean allowOverlap = true;			// Whether we allow cells to overlap or we keep relaxing them until relaxationIterSuccessiveMax is reached 
	public int relaxationIterMax = Integer.MAX_VALUE;	// [-] Number of iterations before model is finished
	// Arrays
	public ArrayList<Cell> cellArray = new ArrayList<Cell>(NCellInit);
	public ArrayList<Ball> ballArray = new ArrayList<Ball>(2*NCellInit);
	public ArrayList<SpringRod> rodSpringArray = new ArrayList<SpringRod>(0);
	public ArrayList<SpringStick> stickSpringArray = new ArrayList<SpringStick>(0);
	public ArrayList<SpringFil> filSpringArray = new ArrayList<SpringFil>(0);
	public ArrayList<SpringAnchor> anchorSpringArray = new ArrayList<SpringAnchor>(0);
	// ODE settings
	public double ODETol = 1e-7;
	// === AS STUFF ===
	public int flocF = 4;
	public int filF = 5;
	// === COMSOL STUFF ===
	public double[] yieldXS = new double[]{2.6/24.6, 7.6/24.6, 2.6/24.6, 7.6/24.6, 2.6/24.6, 7.6/24.6};		// [Cmol X/mol reaction] yield of biomass. Reactions are normalised to mol substrate 
	//////////////////////////////////////////////////////////////////////////////////////////
	
	/////////////////////////////////////
	// Constructors and initialisation //
	/////////////////////////////////////
	public Model() {}	// Default constructor, includes default values
	
	public void UpdateAmountCellMax() {	// Updates the nCellMax based on supplied radiusCellMax and lengthCellMax 
		for(int ii = 0; ii<2; ii++) {
			nCellMax[ii] 		= (4.0/3.0*Math.PI * Math.pow(radiusCellMax[ii],3))*rhoX/MWX; 
			nCellMin[ii] 		= 0.5 * nCellMax[ii];
		}
		for(int ii = 2; ii<6; ii++) {
			nCellMax[ii] = (4.0/3.0*Math.PI * Math.pow(radiusCellMax[ii],3) + Math.PI*Math.pow(radiusCellMax[ii],2)*lengthCellMax[ii])*rhoX/MWX;
			nCellMin[ii] = 0.5 * nCellMax[ii];
			if(ii<4) {
				radiusCellMin[ii] = Ball.Radius(nCellMin[ii], ii, this);
				lengthCellMin[ii] = radiusCellMin[ii] * lengthCellMax[ii]/radiusCellMax[ii];		// min radius times constant aspect ratio	
			} else {
				radiusCellMin[ii] = radiusCellMax[ii];
				lengthCellMin[ii] = nCellMin[ii]*MWX/(Math.PI*rhoX*Math.pow(radiusCellMin[ii],2.0)) - 4.0/3.0*radiusCellMin[ii];	
			}
			
		}
	}
	
	
	/////////////////
	// Log writing //
	/////////////////
	public void Write(String message, String format, boolean suppressFileOutput, boolean suppressConsoleOutput) {
		// Construct date and time
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		// Extract format from input arguments
		String prefix = "   ";
		String suffix = "";
		if(format.equalsIgnoreCase("iter")) 			{suffix = " (" + growthIter + "/" + relaxationIter + ")";} 	else
			if(format.equalsIgnoreCase("warning")) 		{prefix = " WARNING: ";} 									else
				if(format.equalsIgnoreCase("error")) 	{prefix = " ERROR: ";}
		String string = dateFormat.format(cal.getTime()) + prefix + message + suffix;
		// Write to console
		if(!suppressConsoleOutput) {
			System.out.println(string);
		}
		// Write to file
		final String simulationPath = "results/" + name; 
		if(!suppressFileOutput) {
			try {
				if(!(new File("results")).exists()) {
					new File("results").mkdir();
				}
				if(!(new File(simulationPath)).exists()) {
					new File(simulationPath).mkdir();
				}
				if(!(new File(simulationPath + "/output")).exists()) {
					new File(simulationPath + "/output").mkdir();
				}
				PrintWriter fid = new PrintWriter(new FileWriter(simulationPath + "/" + "logfile.txt",true));		// True is for append
				fid.println(string);
				fid.close();
			} catch(IOException E) {
				E.printStackTrace();
			}
		}
	}
	
	public void Write(String message, String format) {
		Write(message,format,false,false);
	}
	
	//////////////////////////
	// Collision detection  //
	//////////////////////////
	public ArrayList<Cell> DetectFloorCollision(double touchFactor) {				// actual distance < dist*radius--> collision    
		ArrayList<Cell> collisionCell = new ArrayList<Cell>();
		for(Cell cell : cellArray) {
			int NBall = (cell.type<2) ? 1 : 2;	// Figure out number of balls based on type
			for(int iBall=0; iBall<NBall; iBall++) {
				Ball ball = cell.ballArray[iBall];
				if(ball.pos.y - touchFactor*ball.radius < 0) {
					collisionCell.add(cell);
					break;
				}
			}
		}
		return collisionCell;
	}
	
	public ArrayList<Cell> DetectCollisionCellArray(double touchFactor) {
		ArrayList<Cell> collisionCell = new ArrayList<Cell>();
		int NCell = cellArray.size();
		for(int ii=0; ii<NCell; ii++) {
			Cell cell0 = cellArray.get(ii);
			for(int jj=ii+1; jj<NCell; jj++) {
				Cell cell1 = cellArray.get(jj);
				if(DetectCollisionCellCell(cell0, cell1, touchFactor)){
					collisionCell.add(cell0);
					collisionCell.add(cell1);
				}
			}
		}
		return collisionCell;
	}
	
	public boolean DetectCollisionCellCell(Cell cell0, Cell cell1, double touchFactor) {
		double R2 = cell0.ballArray[0].radius + cell1.ballArray[0].radius; 
		if(cell0.type < 2 && cell1.type < 2) {				// Ball-ball. Nice and simple
			double dist = cell1.ballArray[0].pos.minus(cell0.ballArray[0].pos).norm();
			if(dist<R2*touchFactor) {
				return true;
			} else {
				return false;
			}
		} else {
			double H2;
			Vector3d diff = cell1.ballArray[0].pos.minus(cell0.ballArray[0].pos);
			Cell rod;	Cell sphere;						// Initialise rod and sphere, should we need it later on (rod-sphere collision detection)
			double dist;
			if(cell0.type > 1 && cell1.type > 1) {			// Rod-rod
				H2 = 1.5*(touchFactor*( lengthCellMax[cell0.type] + lengthCellMax[cell1.type] + R2 ));		// Does not take stretching of the rod spring into account, but should do the trick still
				if(Math.abs(diff.x)<H2 && Math.abs(diff.z)<H2 && Math.abs(diff.y)<H2) {
					// Do good collision detection
					ericson.ReturnObject C = ericson.DetectCollision.LinesegLineseg(cell0.ballArray[0].pos, cell0.ballArray[1].pos, cell1.ballArray[0].pos, cell1.ballArray[1].pos);
					dist = C.dist;							// Then check if dist is small enough (end of method)
				} else {
					return false;							// Not close enough, won't overlap
				}
			} else if(cell0.type<6 && cell1.type<6) {		// Rod-ball
				if(cell0.type<2) {
					rod=cell1;
					sphere=cell0;
				} else {
					rod=cell0;
					sphere=cell1;
				}
				H2 = 1.5*(touchFactor*( lengthCellMax[rod.type] + R2 ));// H2 is maximum allowed distance with still change to collide: R0 + R1 + 2*R1*aspect
				if(Math.abs(diff.x)<H2 && Math.abs(diff.z)<H2 && Math.abs(diff.y)<H2) {
					// Do good collision detection
					ericson.ReturnObject C = ericson.DetectCollision.LinesegPoint(rod.ballArray[0].pos, rod.ballArray[1].pos, sphere.ballArray[0].pos);
					dist = C.dist;							// Then check if dist is small enough (end of method)
				} else {
					return false;							// Not close enough, won't overlap
				}
			} else {
				throw new IndexOutOfBoundsException("Cell type: " + cell0.type + "/" + cell1.type);
			}
			// Add cells to collision array if they are close enough after proper collision detection
			if(dist<R2*touchFactor)	{
				return true;
			} else {
				return false;
			}
		}
	}
		
	///////////////////////////////
	// Spring breakage detection //
	///////////////////////////////
	public ArrayList<SpringAnchor> DetectAnchorBreak(double maxStretch) {
		ArrayList<SpringAnchor> breakArray = new ArrayList<SpringAnchor>();
		
		for(SpringAnchor anchor : anchorSpringArray) {
			double al = (anchor.GetL()).norm();		// al = Actual Length
			if(al > maxStretch*anchor.restLength) {
				breakArray.add(anchor);
			}
		}
		return breakArray;
	}
	
	public ArrayList<SpringStick> DetectStickBreak(double maxStretch) {
		ArrayList<SpringStick> breakArray = new ArrayList<SpringStick>();
		
		int iSpring = 0;
		while(iSpring < stickSpringArray.size()) {
			SpringStick spring = stickSpringArray.get(iSpring);				// TODO: replace name with stick for consistency
			double al = spring.GetL().norm();		// al = Actual Length
			if(al > maxStretch*spring.restLength) {
				breakArray.add(spring);
			}
			iSpring += spring.siblingArray.size()+1;
		}
		return breakArray;
	} 
	
	public Vector3d[] GetBallSpread() {
		Vector3d max = new Vector3d(-1.0, -1.0, -1.0); 
		Vector3d min = new Vector3d(1.0, 1.0, 1.0);
		for(Ball ball : ballArray) {
			if(ball.pos.x>max.x)		max.x = ball.pos.x;
			if(ball.pos.y>max.y)		max.y = ball.pos.y;
			if(ball.pos.z>max.z)		max.z = ball.pos.z;
			if(ball.pos.x<min.x)		min.x = ball.pos.x;
			if(ball.pos.y<min.y)		min.y = ball.pos.y;
			if(ball.pos.z<min.z)		min.z = ball.pos.z;
		}
		return new Vector3d[]{min, max};
	}

	//////////////////////
	// Relaxation stuff //
	//////////////////////
	public int[] Relaxation() throws RuntimeException {
		final FirstOrderIntegrator odeIntegrator = new DormandPrince54Integrator(0, relaxationTimeStepdt, ODETol, ODETol); 	// (minStep, maxStep, absTol, relTol)
//		final FirstOrderIntegrator odeIntegrator = new MidpointIntegrator(ODETol); 											// (stepSize)
		final RelaxationODE ode = new RelaxationODE(this); 			// Subclass of FirstOrderDifferentialEquations in Apache Commons
		StepHandler stepHandler = new StepHandler() {
			public void init(double t0, double[] y0, double t) {}
			// Let the solver (1) count time steps and (2) form and break springs after each successful iteration
			public void handleStep(StepInterpolator interpolator, boolean isLast) {
				int[] springChanges = FormBreak();
				ode.NStep++;
				ode.NAnchorForm += springChanges[0]; 
				ode.NAnchorBreak += springChanges[1];
				ode.NStickForm += springChanges[2];
				ode.NStickBreak += springChanges[3];
				ode.NFilBreak += springChanges[4];
			}
		};
		odeIntegrator.addStepHandler(stepHandler);
		
		// Define initial conditions
		double[] y = new double[ballArray.size()*6];
		int ii=0;											// Determine initial value vector
		for(Ball ball : ballArray) { 
			y[ii++] = ball.pos.x;
			y[ii++] = ball.pos.y;
			y[ii++] = ball.pos.z;
			y[ii++] = ball.vel.x;
			y[ii++] = ball.vel.y;
			y[ii++] = ball.vel.z;
		}
		// Set up solver
		odeIntegrator.integrate(ode, 0.0, y, relaxationTimeStepdt, y); 	// y will contain solution

		ii = 0; 												// TODO This is probably redundant, already transferred in calculateDerivative 
		for(Ball ball : ballArray) {
			ball.pos.x = y[ii++];
			ball.pos.y = y[ii++];
			ball.pos.z = y[ii++];
			ball.vel.x = y[ii++];
			ball.vel.y = y[ii++];
			ball.vel.z = y[ii++];
		}
		return new int[]{ode.NStep, ode.NAnchorBreak, ode.NAnchorForm, ode.NStickBreak, ode.NStickForm, ode.NFilBreak};
//		return new int[]{0,0,0,0,0,0};
		
		
//		int ntimes = (int) (relaxationTimeStep/relaxationTimeStepdt);
//		double atol = 1.0e-6, rtol = atol;
//		double h1 = 0.00001, hmin = 0;
//		double t1 = relaxationTime; 
//		double t2 = t1 + relaxationTimeStep;
//		Vector ystart = new Vector(6*ballArray.size(),0.0);
//
//		int ii=0;											// Determine initial value vector
//		for(CBall ball : ballArray) { 
//			ystart.set(ii++, ball.pos.x);
//			ystart.set(ii++, ball.pos.y);
//			ystart.set(ii++, ball.pos.z);
//			ystart.set(ii++, ball.vel.x);
//			ystart.set(ii++, ball.vel.y);
//			ystart.set(ii++, ball.vel.z);
//		}
//		Output<StepperDopr853> out = new Output<StepperDopr853>(ntimes);
//		feval dydt = new feval(this);
//		Odeint<StepperDopr853> ode = new Odeint<StepperDopr853>(ystart, t1, t2, atol, rtol, h1, hmin, out, dydt, this);
//		// Update alpha and beta
//		ode.s.alpha = ODEalpha;
//		ode.s.beta = ODEbeta;
//		// Integrate to find solution
//		int nstp = ode.integrate();
//		for(int iTime=0; iTime<out.nsave; iTime++) {		// Save all intermediate results to the save variables
//			int iVar = 0;
//			for(CBall ball : ballArray) {
//				ball.posSave[iTime].x = out.ysave.get(iVar++,iTime);
//				ball.posSave[iTime].y = out.ysave.get(iVar++,iTime);
//				ball.posSave[iTime].z = out.ysave.get(iVar++,iTime);
//				ball.velSave[iTime].x = out.ysave.get(iVar++,iTime);
//				ball.velSave[iTime].y = out.ysave.get(iVar++,iTime);
//				ball.velSave[iTime].z = out.ysave.get(iVar++,iTime);
//			}
//		}
//		{int iVar = 0;										// Only the final value is stored in the pos and vel variables
//		int iTime = out.nsave;
//		for(CBall ball : ballArray) {
//			ball.pos.x = out.ysave.get(iVar++,iTime);
//			ball.pos.y = out.ysave.get(iVar++,iTime);
//			ball.pos.z = out.ysave.get(iVar++,iTime);
//			ball.vel.x = out.ysave.get(iVar++,iTime);
//			ball.vel.y = out.ysave.get(iVar++,iTime);
//			ball.vel.z = out.ysave.get(iVar++,iTime);
//		}}
//		return new int[]{nstp, ode.NAnchorBreak, ode.NAnchorForm, ode.NStickBreak, ode.NStickForm, ode.NFilBreak};
	}
	
	public int[] FormBreak() {								// Breaks and forms sticking, filament springs when needed. Used during Relaxation()
		int NAnchorBreak= 0;
		int NAnchorForm = 0;
		int NStickBreak = 0;
		int NStickForm 	= 0;
		int NFilBreak 	= 0;
		
		for(int ii=0; ii<cellArray.size(); ii++) {
			Cell cell0 = cellArray.get(ii);
			// Anchoring
			if(anchoring) {
				Ball ball0 = cell0.ballArray[0];
				Ball ball1 = (cell0.type>1) ? ball1 = cell0.ballArray[1] : null;

				if(cell0.anchorSpringArray.size()>0) { 		// This cell is already anchored
					ArrayList<SpringAnchor> breakArray = new ArrayList<SpringAnchor>();
					for(SpringAnchor anchor : cell0.anchorSpringArray) {
						// Break anchor?
						Vector3d diff = anchor.GetL();
						double dn = diff.norm();
						if(dn > anchor.restLength+anchorStretchLim) {	// too much tension --> break the spring
							breakArray.add(anchor);
						}
					}
					for(SpringAnchor anchor : breakArray)		NAnchorBreak += anchor.Break();
				} else {									// Cell is not yet anchored
					// Form anchor?
					boolean formBall0 = (ball0.pos.y < anchorFormLim+ball0.radius) ? true : false;
					boolean formBall1 = false;
					if(cell0.type > 1) 	formBall1 = (ball1.pos.y < anchorFormLim+ball1.radius) ? true : false;			// If ball1 != null
					if(formBall0 || formBall1) {
						NAnchorForm += cell0.Anchor();
					}
				}
			}
			// Sticking and filial links
			for(int jj=ii+1; jj<cellArray.size(); jj++) {	// Only check OTHER cells not already checked in a different order (i.e. factorial elimination)
				Cell cell1 = cellArray.get(jj);
				// Are these cells connected to each other, either through sticking spring or filament?
				boolean isStuck = false, isFilament = false;
				Spring stickingSpring = null, filamentSpring = null; 
				for(Spring fil : cell0.filSpringArray) {	// Will be empty if filaments are disabled --> no need to add further if statements 
					if(fil.ballArray[0].cell.equals(cell1) || fil.ballArray[1].cell.equals(cell1))  {		// We already know it is a filial spring with cell0
						// That's the one containing both cells
						isFilament = true;
						filamentSpring = fil;
						break;								// That is all we need: only one set of filial springs exists between two cells
					}
				}
				for(Spring stick : cell0.stickSpringArray) { 
					if(stick.ballArray[0].cell.equals(cell1) || stick.ballArray[1].cell.equals(cell1)) {
						isStuck = true;
						stickingSpring = stick;				// Only one set of sticking springs exists between two cells
						break;						
					}
				}
				if(isFilament) {							// Can only be true if filaments are enabled 
					// Don't stick this. It shouldn't be stuck so don't check if we can break sticking springs. Instead, see if we can break the filial link 
					double distance = filamentSpring.GetL().norm();
					// Check if we can break this spring
					if(distance>filamentSpring.restLength+filStretchLim) {
						NFilBreak += filamentSpring.Break();	// Also breaks its siblings
					}
				} else if (sticking){						// Check if we want to do sticking, or break the sticking spring
					// Determine current distance, required for formation and breaking
					Ball c0b0 = cell0.ballArray[0];
					Ball c1b0 = cell1.ballArray[0];				
					if(isStuck) {							// Stuck --> can we break this spring (and its siblings)?
						double dist = stickingSpring.GetL().norm();
						if(dist > stickingSpring.restLength+stickStretchLim) 		NStickBreak += stickingSpring.Break();
					} else {								// Not stuck --> can we stick them? We have already checked if they are linked through filaments, not the case
						double R2 = c0b0.radius + c1b0.radius;
						Vector3d dirn = (c1b0.pos.minus(c0b0.pos));
						double dist;
						if(cell0.type<2 && cell1.type<2) {	// both spheres
							if(stickType[cell0.type][cell1.type]) { 
								dist = (c1b0.pos.minus(c0b0.pos)).norm();						// Not a spring, so can't use GetL() yet
							} else continue;
						} else if(cell0.type<2) {			// 1st sphere, 2nd rod
							double H2f =  1.5*(stickFormLim+(lengthCellMax[cell1.type] + R2));	// H2 is maximum allowed distance with still change to collide: R0 + R1 + 2*R1*aspect
							if(stickType[cell0.type][cell1.type] && dirn.x<H2f && dirn.z<H2f && dirn.y<H2f) {
								Ball c1b1 = cell1.ballArray[1];
								// do a sphere-rod collision detection
								ericson.ReturnObject C = ericson.DetectCollision.LinesegPoint(c1b0.pos, c1b1.pos, c0b0.pos);
								dist = C.dist;
							} else continue;
						} else if(cell1.type<2) {			// 2nd sphere, 1st rod
							double H2f = 1.5*(stickFormLim+(lengthCellMax[cell0.type] + R2));	// H2 is maximum allowed distance with still change to collide: R0 + R1 + 2*R1*aspect
							if(stickType[cell0.type][cell1.type] && dirn.x<H2f && dirn.z<H2f && dirn.y<H2f) {
								Ball c0b1 = cell0.ballArray[1];
								// do a sphere-rod collision detection
								ericson.ReturnObject C = ericson.DetectCollision.LinesegPoint(c0b0.pos, c0b1.pos, c1b0.pos);
								dist = C.dist;
							} else continue;
						} else if(cell0.type<6 && cell1.type<6) {  	// both rod
							double H2f = 1.5*(stickFormLim+(lengthCellMax[cell0.type] + lengthCellMax[cell1.type] + R2));
							if(stickType[cell0.type][cell1.type] && dirn.x<H2f && dirn.z<H2f && dirn.y<H2f) {
								Ball c0b1 = cell0.ballArray[1];
								Ball c1b1 = cell1.ballArray[1];
								// calculate the distance between the two segments
								ericson.ReturnObject C = ericson.DetectCollision.LinesegLineseg(c0b0.pos, c0b1.pos, c1b0.pos, c1b1.pos);
								dist = C.dist;
							} else continue;
						} else {
							throw new IndexOutOfBoundsException("Cell types: " + cell0.type + " and " + cell1.type);
						}
						// Stick if distance is small enough
						if(dist<R2+stickFormLim) 	NStickForm += cell0.Stick(cell1);
					}
				}
			}
		}
		return new int[]{NAnchorForm, NAnchorBreak, NStickForm, NStickBreak, NFilBreak};
	}
	
	//////////////////
	// Growth stuff //
	//////////////////
	public ArrayList<Cell> GrowthSimple() throws RuntimeException {									// Growth based on a random number, further enhanced by being sticked to a cell of other type (==0 || !=0) 
		int NCell = cellArray.size();
		ArrayList<Cell> dividedCell = new ArrayList<Cell>(); 
		for(int iCell=0; iCell<NCell; iCell++){
			Cell mother = cellArray.get(iCell);
			double amount = mother.GetAmount();

			// Random growth, with syntrophy if required
			double mu = muAvgSimple[mother.type] + (muStDev[mother.type] * rand.Gaussian());	// Come up with a mu for this cell, this iteration
			double growthAcceleration = 1.0;
			for(Cell stickCell : mother.stickCellArray) {
				if(mother.type != stickCell.type) {
					// The cell types are different on the other end of the spring
					growthAcceleration *= syntrophyFactor;
					break;
				}
			}
			amount *= Math.exp(mu*growthAcceleration*growthTimeStep/3600.0);					// We need growthTimeStep s --> h

			// Syntrophic growth for sticking cells
			mother.SetAmount(amount);
		}
		
		return dividedCell;
	}
	
	public ArrayList<Cell> GrowthFlux() throws RuntimeException {
		int NCell = cellArray.size();
		ArrayList<Cell> dividedCell = new ArrayList<Cell>();
		for(int iCell=0; iCell<NCell; iCell++){
			Cell mother = cellArray.get(iCell);
			// Obtain mol increase based on flux FIXME
			// Grow mother cell
			double newAmount = mother.GetAmount() + mother.Rx * growthTimeStep * yieldXS[mother.type];
			mother.SetAmount(newAmount);
		}
		return dividedCell;
	}
	
	public Cell DivideCell(Cell c0) {
		// Nomenclature: c0 == mother, c1 == daughter
		double n = c0.GetAmount();
		Cell c1;
		if(c0.type<2) {
			///////
			// Original cell:
			//     (     )
			// New cells:
			//    (   )(   )
			///////
			// Set amount for both cells
			c0.SetAmount(0.5*n);
			// Make a new cell
			Vector3d posOld = new Vector3d(c0.ballArray[0].pos);
			c1 = new Cell(c0.type,												// Same type as cell
					c0.GetAmount(),
					posOld,					
					new Vector3d(),
					c0.filament,
					this);														// Same filament boolean as cell and pointer to the model
			// Displace new and old cell. Rods won't need this while loop, because they'll just be cut in half 
			int overlapIter = 0;
			while(true) {
				overlapIter++;
				// Come up with a nice direction in which to place the new cell
				Vector3d direction = new Vector3d(rand.Double()-0.5,rand.Double()-0.5,rand.Double()-0.5);
				direction = direction.normalise();
				double displacement = c0.ballArray[0].radius;						// Displacement is done for both balls --> total of 1.0*radius displacement
				// Displace
				c0.ballArray[0].pos = posOld.plus(  direction.times(displacement) );
				c1.ballArray[0].pos = posOld.minus( direction.times(displacement) );
				// Contain cells to y dimension of domain
				if(normalForce) {
					c0.ballArray[0].pos.y = Math.max(c0.ballArray[0].radius, c0.ballArray[0].pos.y);					
					c1.ballArray[0].pos.y = Math.max(c1.ballArray[0].radius, c1.ballArray[0].pos.y);
				}
				// Check if all went well: collision detection
				// Create a copy of cellArray and remove c0 and c1 from it
				ArrayList<Cell> copyCellArray = new ArrayList<Cell>(cellArray);
				copyCellArray.remove(c0);		copyCellArray.remove(c1);
				boolean overlap = false;
				if(DetectCollisionCellCell(c0, c1, 1.0))		overlap = true; 
				for(Cell cell : copyCellArray) {
					if(DetectCollisionCellCell(c0, cell, 1.0) || DetectCollisionCellCell(c1, cell, 1.0)) {
						overlap = true; 
						break;
					}
				}
				// See if we can continue with these positions now
				if(!overlap)	break;
				// Continue the while loop if no proper direction was found
				if(overlapIter>100) {
//					Write("Cell " + c0.Index() + " or " + c1.Index() + " will overlap after growth","warning");
					break;
				}
			}
						
			// Set properties for new cell
			c1.ballArray[0].vel = 	new Vector3d(c0.ballArray[0].vel);
			c1.ballArray[0].force = new Vector3d(c0.ballArray[0].force);
			c1.mother = 			c0;
			c1.Rx = 					c0.Rx;
		} else if (c0.type<6) {
			///////
			// Original cell:
			// (00)~~~~~~~~~~~~(01)
			// New cells:
			// (00)~~(01)(10)~~(11)
			///////
			// Half mass of mother cell
			c0.SetAmount(0.5*c0.GetAmount());
			// Define balls
			Ball c0b0 = c0.ballArray[0];
			Ball c0b1 = c0.ballArray[1];
			// Determine displacement
			double radius = c0b0.radius;
			Vector3d middle = c0b1.pos.minus(c0b0.pos).divide(2.0);				// Vector from c0b0 --> halfway c0b1
			double L = middle.norm();
			Vector3d ball1Vector = middle.times((L-radius)/L);					// Vector from c0b0 --> new c0b1 position (halfway with radius subtracted)
			// Make a new, displaced cell
			c1 = new Cell(c0.type,												// Same type as cell
					c0.GetAmount(),												// Same mass as (already slimmed down) mother cell
					c0b1.pos.minus(ball1Vector),								// First ball. First ball and second ball were swapped in MATLAB and possibly C++					
					c0b1.pos,
					c0.filament,
					this);														// Same filament boolean as cell and pointer to the model
			// Displace old cell, 2nd ball (1st ball stays in place)
			c0b1.pos = c0b0.pos.plus(ball1Vector);
			c0b1.pos.z += 1e-8;													// WORKAROUND: Move in z direction by 0.01 micron. Required to prevent deadlock 
			c0.rodSpringArray.get(0).ResetRestLength();
			// Contain cells to y dimension of domain
			if(normalForce) {
				for(int iBall=0; iBall<2; iBall++) {
					c0.ballArray[iBall].pos.y = Math.max(c0.ballArray[iBall].radius, c0.ballArray[iBall].pos.y);					
					c1.ballArray[iBall].pos.y = Math.max(c1.ballArray[iBall].radius, c1.ballArray[iBall].pos.y);
				}
			}
			// Set properties for new cell
			for(int iBall=0; iBall<2; iBall++) {
				c1.ballArray[iBall].vel = 	new Vector3d(c0.ballArray[iBall].vel);
				c1.ballArray[iBall].force = new Vector3d(c0.ballArray[iBall].force);
			}
			c1.mother = c0;
			c1.rodSpringArray.get(0).restLength = c0.rodSpringArray.get(0).restLength;

		} else {
			throw new IndexOutOfBoundsException("Cell type: " + c0.type);
		}
		// Set sticking springs
		for(Cell cell : c0.stickCellArray) {														// We want to check each other cell
			for(SpringStick stick : c0.stickSpringArray) {												// And find the correct spring, attached to c0 and cell
				if(stick.ballArray[0].equals(c1) || stick.ballArray[1].equals(c1)) {				
					if(c1.GetDistance(cell) < c0.GetDistance(cell)) {								// If c1 is closer, move sticking spring to c1
						stick.Break();																// Break this spring and its siblings. OPTIMISE: We could restick it, but then we need to find the correct ball to Stick() to 
						c1.Stick(cell);
						break;
					} else {																		// If c0 is closer, just reset rest length of this spring and its siblings
						stick.ResetRestLength();
						for(Spring sibling : stick.siblingArray)		sibling.ResetRestLength();		
					}
				}
			}
		}
		// Done, return daughter cell
		return c1;
	}
	
	public void CreateFilament(Cell c0, Cell c1) {
		if(c0.type==c1.type && c0.type<2)
			new SpringFil(c0.ballArray[0], c1.ballArray[0], 3);
		else if (c0.type==c1.type && c0.type<6) {
			// Make new filial link between mother and daughter
			SpringFil filSmall = 	new SpringFil(c1.ballArray[0], c0.ballArray[1], 4);							// type==4 --> Small spring
			SpringFil filBig = 	new SpringFil(c1.ballArray[1], c0.ballArray[0], 5);							// type==? --> Big spring
			filSmall.siblingArray.add(filBig);
			filBig.siblingArray.add(filSmall);
		} else {
			throw new IndexOutOfBoundsException("Cell type: " + c0.type + "/" + c1.type);
		}
	}
	
	public void CreateFilament(Cell daughter, Cell mother, Cell neighbour) {
		if(mother.type==daughter.type && mother.type<2)
			new SpringFil(mother.ballArray[0], daughter.ballArray[0], 3);
		else if (mother.type==daughter.type && mother.type<6) {
			// Make new filial link between mother and daughter
			SpringFil filSmallDM = 	new SpringFil(daughter.ballArray[0], mother.ballArray[1], 6);
			SpringFil filBigDM = 		new SpringFil(daughter.ballArray[1], mother.ballArray[0], 7);
			SpringFil filSmallDN = 	new SpringFil(daughter.ballArray[0], neighbour.ballArray[0], 6);
			SpringFil filBigDN = 		new SpringFil(daughter.ballArray[1], neighbour.ballArray[1], 7);
			filSmallDM.siblingArray.add(filBigDM);
			filBigDM.siblingArray.add(filSmallDM);
			filSmallDN.siblingArray.add(filBigDN);
			filBigDN.siblingArray.add(filSmallDN);
		} else {
			throw new IndexOutOfBoundsException("Cell type: " + daughter.type + "/" + mother.type + "/" + neighbour.type);
		}
	}
	
	public void TransferFilament(Cell mother, Cell daughter) {
		if(mother.type < 2 && daughter.type < 2) {
			Ball motherBall0 = mother.ballArray[0];
			Ball daughterBall0 = daughter.ballArray[0];
			ArrayList<SpringFil> donateFilArray = new ArrayList<SpringFil>();
			for(SpringFil fil : mother.filSpringArray) {
				boolean found=false;
				if( fil.ballArray[0] == motherBall0 ) {					// Only replace half the balls for daughter's
					fil.ballArray[0] = daughterBall0;
					found = true;}
				if(found) {
					// Mark filament spring for donation from mother to daughter
					donateFilArray.add(fil);
				}
			}
			for(SpringFil fil : donateFilArray) {
				daughter.filSpringArray.add(fil);
				mother.filSpringArray.remove(fil);
				// Reset rest lengths. Spring constant won't change because it depends on cell type
				fil.ResetRestLength();
			}
		} else if(mother.type < 6 && daughter.type < 6) {
			Ball c0b0 = mother.ballArray[0];
			Ball c0b1 = mother.ballArray[1];
			Ball c1b0 = daughter.ballArray[0];
			Ball c1b1 = daughter.ballArray[1];
			ArrayList<SpringFil> donateFilArray = new ArrayList<SpringFil>();
			for(SpringFil fil : mother.filSpringArray) {	// OPTIMISE
				boolean found=false;
				if(fil.type == 4) {							// Short spring
					if(fil.ballArray[0] == c0b1) {
						for(SpringFil sibling : fil.siblingArray) {
							if(sibling.type == 5) {
								fil.ballArray[0] = 	c1b1;
								found = true;
							}
						}
					} else if(fil.ballArray[1] == c0b1) {
						for(SpringFil sibling : fil.siblingArray) {
							if(sibling.type == 5) {
								fil.ballArray[1] = 	c1b1;
								found = true;
							}
						}
					}					
				} else if(fil.type == 5) {	// Long spring (straight fil and branched fil resp.)
					if(fil.ballArray[0] == c0b0) {
						for(SpringFil sibling : fil.siblingArray) {
							if(sibling.type == 4) {
								fil.ballArray[0] = 	c1b0;
								found = true;
							}
						}
					} else if(fil.ballArray[1] == c0b0) {
						for(SpringFil sibling : fil.siblingArray) {
							if(sibling.type == 4) {
								fil.ballArray[1] = 	c1b0;
								found = true;
							}
						}
					}
				}
				// We don't want to transfer fil.type==6
				if(found)
					donateFilArray.add(fil);
			}
			for(SpringFil fil : donateFilArray) {
				daughter.filSpringArray.add(fil);
				mother.filSpringArray.remove(fil);
				// Reset rest lengths
				fil.ResetRestLength();
			}
		} else
			throw new IndexOutOfBoundsException("Cell type: " + mother.type + '/' + daughter.type);
	}

	public void Attachment(int NNew) {
		for(int iA=0; iA<NNew; iA++) {
			// Define the cell we will attach
			final int typeNew = attachCellType; 
			final double nNew = nCellMin[typeNew] * (1.0 + rand.Double());
			final boolean filNew = filament && filType[typeNew];
			final double rNew = Ball.Radius(nNew, typeNew, this); 
			// Create array of balls in non-spherical cells 
			ArrayList<Ball> ballArrayRod = new ArrayList<Ball>(ballArray.size());
			for(Ball ball : ballArray) 	if(ball.cell.type>1) 	ballArrayRod.add(ball);
			// Find a random rod's ball position dest(ination) and move the ball there from dirn ("along the path") until we find a particle
			Vector3d firstPos = new Vector3d(0.0, 0.0, 0.0);
			// Create and position the new cell to this champion ball. Position it in the direction of dirn
			Cell newCell = new Cell(typeNew, nNew, firstPos, new Vector3d(), filNew, this);
			tryloop:while(true) {
				// Find dest(ination) based on random position within range of the domain
				Vector3d[] spread = GetBallSpread();
				Vector3d dest = new Vector3d(
						spread[0].x + rand.Double()*(spread[1].x-spread[0].x),
						spread[0].y + rand.Double()*(spread[1].y-spread[0].y),
						spread[0].z + rand.Double()*(spread[1].z-spread[0].z));
				// Find dirn, any direction away from dest (we take care of substratum blocking later)
				Vector3d dirn = new Vector3d(rand.Double()-0.5, rand.Double()-0.5, rand.Double()-0.5).normalise();
				// Check how far all (incl. spherical) balls are from dest and in the correct dirn. Also select our winner (any cell type)
				Ball firstBall = ballArray.get(0);
				double firstDist = 0.0;
				boolean success = false;
				
				// Find if the new cell can attach to a spherical cell
				for(Ball ball : ballArray) {
					ericson.ReturnObject E = ericson.DetectCollision.LinePoint(dest, dest.plus(dirn), ball.pos);			// Detect distance line-point, with line the path and point the ball.pos
					// Check if ball.pos is close enough to path to touch the attaching particle by analysing the distance obtained from Ericsson
					if( E.dist < rNew+ball.radius ) {
						// Good, if the attaching particle would be moved along path from dirn to dest it would collide with other 
						// Now check if it is the first ball that the newly attached cell would encounter, i.e. if the distance from dest is the largest yet 
						if(E.sc > firstDist) {													// sc is the multiplier for the vector that denotes the line to get the segment: since dirn.norm() == 1 we don't need to multiply with the length
							success = true;
							// Set this ball to be first to be encountered by the attaching particle
							firstDist = E.sc;
							firstBall = ball;
							firstPos = ball.pos.plus(dirn.times(rNew+ball.radius));		// Position where the new cell will attach after colliding
						}
					}
				}
				// After checking all balls, check all springs in the rods
				for(SpringRod spring : rodSpringArray) {
					// Find the distance between the path of the particle (a line) and the rod spring (a line segment)
					Ball ball0 = spring.ballArray[0];
					Ball ball1 = spring.ballArray[1];
					ericson.ReturnObject E = ericson.DetectCollision.LineSegLine(ball0.pos, ball1.pos, dest, dest.plus(dirn));							// Detect distance line-point, with line the path and point the ball.pos
					if( E.dist < rNew+ball0.radius ) {
						// Good, if the attaching particle would be moved along path from dirn to dest it would collide with the rod
						// Now check if it is the first ball that the newly attached cell would encounter, i.e. if the distance from dest is the largest yet
						double distFromDest = dest.plus(dirn.times(E.tc)).norm();
						if(distFromDest > firstDist) {
							success = true;
							// Set this ball to be first to be encountered by the attaching particle
							firstDist = distFromDest;
							firstBall = ball0;													// Could also be ball1, but doesn't matter here
							Vector3d away;
							if(E.sc==0.0 || E.sc==1.0)										// Otherwise, away will be null vector, so choose another direction
								away = dirn;
							else
								away = E.c2.minus(E.c1).normalise();							// Vector pointing away from the collision
							// The point on the path where the collision is closest, moving the ball away from there until it no longer overlapping
							firstPos = E.c1.plus(away.times(rNew+ball0.radius));
						}
					}
				}
				// Get new position. 
				// Check if we actually had a collision
				if(!success)						
					continue tryloop;
				// Check if it is valid in case we have a substratum
				if(normalForce && firstPos.y<rNew)	
					continue tryloop;	// the new cell went through the plane to get to this point
				// If a cell of the correct type wins, we're happy
				for(int ii=0; ii<attachNotTo.length; ii++)
					if(firstBall.cell.type == attachNotTo[ii])
						continue tryloop;
				// Reposition the cell
				newCell.ballArray[0].pos = firstPos;
				if(typeNew>1 && typeNew<6) {
					newCell.ballArray[1].pos = firstPos.plus( dirn.times(newCell.rodSpringArray.get(0).restLength) );
				} else if (typeNew>6)
					throw new IndexOutOfBoundsException("Cell type: " + typeNew);
				// Check if it is not overlapping with any other cells
				for(int iCell=0; iCell<cellArray.size()-1; iCell++) {
					Cell cell = cellArray.get(iCell);
					if(DetectCollisionCellCell(newCell, cell, 1.0))	
						continue tryloop;
				}
//				// See if within range of origin
//				Vector3d nul = new Vector3d(0,0,0);
//				double range = 4e-6;
//				if(firstPos.minus(nul).norm()>range)
//					continue tryloop;
				// Congratulations!
				break;
			}
			// It will stick/anchor when needed during movement, so we're done
		}
	}
	
	////////////////////////////
	// Anchor and Stick stuff //
	////////////////////////////
	public void BreakStick(ArrayList<SpringStick> breakArray) {
		for(SpringStick spring : breakArray) {
			Cell cell0 = spring.ballArray[0].cell;
			Cell cell1 = spring.ballArray[1].cell;
			// Remove cells from each others' stickCellArray 
			cell0.stickCellArray.remove(cell1);
			cell1.stickCellArray.remove(cell0);
			// Remove springs from model stickSpringArray
			stickSpringArray.remove(spring);
			for(int ii=0; ii<spring.siblingArray.size(); ii++) {
				stickSpringArray.remove(spring.siblingArray.get(ii));
			}
		}
	}
	
	public int BuildAnchor(ArrayList<Cell> collisionArray) {
		// Make unique
		for(SpringAnchor pSpring : anchorSpringArray) collisionArray.remove(pSpring.ballArray[0].cell);
		
		// Anchor the non-stuck, collided cells to the ground
		for(Cell cell : collisionArray) cell.Anchor();
		return anchorSpringArray.size();
	}
	
	public int BuildStick(ArrayList<Cell> collisionArray) {
		int counter = 0;
		for(int ii=0; ii<collisionArray.size(); ii+=2) {		// For loop works per duo
			boolean setStick = true;
			Cell cell0 = collisionArray.get(ii);
			Cell cell1 = collisionArray.get(ii+1);
			// Check if already stuck, don't stick if that is the case
			for(SpringStick pSpring : stickSpringArray) {		// This one should update automatically after something new has been stuck --> Only new ones are stuck AND, as a result, only uniques are sticked 
				if((pSpring.ballArray[0].cell.equals(cell0) && pSpring.ballArray[1].cell.equals(cell1)) || (pSpring.ballArray[0].cell.equals(cell1) && pSpring.ballArray[1].cell.equals(cell0))) {
					setStick = false;
				}
			}
			if(setStick) {
				cell0.Stick(cell1);
				counter++;
			}
		}
		return counter;
	}
	
	////////////
	// Saving //
	////////////
	public void Save() {		// Save as serialised file, later to be converted to .mat file
		FileOutputStream fos = null;
		GZIPOutputStream gz = null;
		ObjectOutputStream oos = null;
		try {
			fos = new FileOutputStream(String.format("results/%s/output/g%04dr%04d.ser", name, growthIter, relaxationIter));
			gz = new GZIPOutputStream(fos);
			oos = new ObjectOutputStream(gz);
			oos.writeObject(this);
			oos.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}