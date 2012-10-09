package cell;

import interactor.Interactor;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import backbone.Assistant;
import random.rand;
import jmatio.*;
import NR.*;

public class CModel {
	// Model properties
	public static String name = "default";
	public static int randomSeed = 1;
	public static boolean sticking = true;
	public static boolean anchoring = false;
	public static boolean filament = false;
	public static boolean gravity = false;
	// Spring constants
	public static double Kr 	= 1e5;					// internal cell spring (per ball)
	public static double Kf 	= 3e4;					// filament spring (per ball average)
	public static double Kw 	= 2e5;					// wall spring (per ball)
	public static double Kc 	= 1e7;					// collision (per ball)
	public static double Ks 	= 1e4;					// sticking (per ball average)
	public static double Kan	= 1e4;					// anchor (per BALL)
	public static double[] stretchLimAnchor = {0.6, 1.4};			// Maximum tension and compression (1-this value) for anchoring springs
	public static double formLimAnchor = 1.1;			// Multiplication factor for rest length to form anchors. Note that actual rest length is the distance between the two, which could be less
	public static double[] stretchLimStick = {0.6, 1.4};			// Maximum tension and compression (1-this value) for sticking springs
	public static double formLimStick = 1.1; 			// Multiplication factor for rest length to form sticking springs. 
	// Domain properties
	public static double Kd 	= 1e3;						// drag force coefficient (per BALL)
	public static double G		= -9.8;					// [m/s2], acceleration due to gravity
	public static double rhoWater = 1000;				// [kg/m3], density of bulk liquid (water)
	public static double rhoX	= 1020;					// [kg/m3], diatoma density
	public static double MWX 	= 24.6e-3;				// [kg/mol], composition CH1.8O0.5N0.2
	public static Vector3d L 	= new Vector3d(60e-6, 15e-6, 60e-6);	// [m], Dimensions of domain
	// Model biomass properties
	public static int NXComp = 6;						// Types of biomass
	public static int NdComp = 5;						// d for dynamic compound (e.g. total Ac)
	public static int NcComp = 8;						// c for concentration (or virtual compound, e.g. Ac-)
	public static int NAcidDiss = 4; 					// Number of acid dissociation reactions
	public static int NInitCell = 15;					// Initial number of cells
	public static int[] cellType = {1, 5};				// Cell types used by default
//	public static double[] aspect	= {2.0, 2.0, 2.0, 2.0, 2.0, 2.0};	// Aspect ratio of cells
	public static double[] aspect	= {0.0, 0.0, 4.0, 2.0, 5.0, 3.0};	// Aspect ratio of cells (last 2: around 4.0 and 2.0 resp.)
	// Ball properties
	public static double[] nCellInit = {2.66e-16, 1.71e-14, 1.87e-15, 2.88e-14, 1.87e-15, 2.88e-14};		// [Cmol] initial cell, when created at t=0. Factor *0.9 used for initial mass type<4
	public static double[] nBallInit = {nCellInit[0], nCellInit[1], nCellInit[2]/2.0, nCellInit[3]/2.0, nCellInit[4]/2.0, nCellInit[5]/2.0};				// [Cmol] initial mass of one ball in the cell
	public static double[] nCellMax = {nCellInit[0]*2.0, nCellInit[1]*2.0, nCellInit[2]*2.0, nCellInit[3]*2.0, nCellInit[4]*2.0, nCellInit[5]*2.0};		// [Cmol] max mass of cells before division;
	// Progress
	public static double growthTime = 0.0;				// [s] Current time for the growth
	public static double growthTimeStep = 3600.0;		// [s] Time step for growth
	public static int growthIter = 0;					// [-] Counter time iterations for growth
	public static double movementTime = 0.0;			// [s] initial time for movement (for ODE solver)
	public static double movementTimeStep = 2e-2;		// [s] output time step  for movement
	public static double movementTimeStepEnd = 10e-2;	// [s] time interval for movement (for ODE solver), 5*movementTimeStep by default
	public static int movementIter = 0;				// [-] counter time iterations for movement
	// Arrays
	public static ArrayList<CCell> cellArray = new ArrayList<CCell>(NInitCell);
	public static ArrayList<CBall> ballArray = new ArrayList<CBall>(2*NInitCell);
	public static ArrayList<CRodSpring> rodSpringArray = new ArrayList<CRodSpring>(NInitCell);
	public static ArrayList<CStickSpring> stickSpringArray = new ArrayList<CStickSpring>(NInitCell);
	public static ArrayList<CFilSpring> filSpringArray = new ArrayList<CFilSpring>(NInitCell);
	public static ArrayList<CAnchorSpring> anchorSpringArray = new ArrayList<CAnchorSpring>(NInitCell);
	// === COMSOL STUFF ===
	// Biomass, assuming Cmol and composition CH1.8O0.5N0.2 (i.e. MW = 24.6 g/mol)
	//							type 0					type 1					type 2					type 3					type 4					type 5
	// 							m. hungatei				m. hungatei				s. fumaroxidans			s. fumaroxidans			s. fumaroxidans			s. fumaroxidans
	public static double[] SMX = {		7.6e-3/MWX,				7.6e-3/MWX,				2.6e-3/MWX,				2.6e-3/MWX,				2.6e-3/MWX,				2.6e-3/MWX};				// [Cmol X/mol reacted] Biomass yields per flux reaction. All types from Scholten 2000, grown in coculture on propionate
	public static double[] K = {		1e-21, 					1e-21, 					1e-5, 					1e-5, 					1e-5, 					1e-5};						// [microM] FIXME
	public static double[] qMax = {	0.05/(SMX[0]*86400), 	0.05/(SMX[0]*86400), 	0.204*MWX*1e3/86400,	0.204*MWX*1e3/86400,	0.204*MWX*1e3/86400,	0.204*MWX*1e3/86400};		// [mol (Cmol*s)-1] M.h. from Robinson 1984, assuming yield, growth on NaAc in coculture. S.f. from Scholten 2000;
	public static String[] rateEquation = {
			Double.toString(qMax[0]) + "*(c3*d3^4)/(K0+c3*d3^4)",		// type==0
			Double.toString(qMax[1]) + "*(c3*d3^4)/(K1+c3*d3^4)",		// type==1
			Double.toString(qMax[2]) + "*c2/(K2+c2)",					// type==2
			Double.toString(qMax[3]) + "*c2/(K3+c2)",					// type==3
			Double.toString(qMax[4]) + "*c2/(K4+c2)",					// type==4
			Double.toString(qMax[5]) + "*c2/(K5+c2)"};					// type==5
			
	// 	 pH calculations
	//							HPro		CO2			HCO3-		HAc
	//							0,			1,			2,			3
	public static double[] Ka = {		1.34e-5,	4.6e-7,		4.69e-11, 	1.61e-5};								// From Wikipedia 120811. CO2 and H2CO3 --> HCO3- + H+;
	public static String[] pHEquation = {																			// pH calculations
			"c2+c4+c5+c7-c0", 
			"c2*c0/Ka0-c1", 
			"d0-c1-c2", 
			"c4*c0/Ka1-c3", 
			"c5*c0/Ka2-c4", 
			"d1-c3-c4-c5", 
			"c7*c0/Ka3-c6", 
			"d2-c6-c7"}; 	

	// Diffusion
	// 							ProT, 		CO2T,				AcT,				H2, 				CH4
	//							0,    		1,   				2, 					3,   				4
 	public static double[] BCConc = new double[]{
 								13.50,		0.0, 				0.0,				0.0,				0.0	};			// [mol m-3]. equivalent to 1 [kg HPro m-3], neglecting Pro concentration
	public static double[] D = new double[]{	
								1.060e-9,	1.92e-9,			1.21e-9,			4.500e-9,			1.88e-9};		// [m2 s-1]. Diffusion mass transfer Cussler 2nd edition. Methane through Witherspoon 1965
	public static double[][] SMdiffusion = {
							{	0.0,		-1.0,				0.0,				-4.0,				(1.0-SMX[0])*1.0},		// XComp == 0 (small sphere)
							{	0.0,		-1.0,				0.0,				-4.0,				(1.0-SMX[1])*1.0},		// XComp == 1 (big sphere)
							{	-1.0,		(1.0-SMX[2])*1.0,	(1.0-SMX[2])*1.0,	(1.0-SMX[2])*3.0,	0.0				},		// XComp == 2 (small rod, variable W)
							{	-1.0,		(1.0-SMX[3])*1.0,	(1.0-SMX[3])*1.0,	(1.0-SMX[3])*3.0,	0.0				},		// XComp == 3 (big rod, variable W)
							{	-1.0,		(1.0-SMX[4])*1.0,	(1.0-SMX[4])*1.0,	(1.0-SMX[4])*3.0,	0.0				},		// XComp == 4 (small rod, fixed W)
							{	-1.0,		(1.0-SMX[5])*1.0,	(1.0-SMX[5])*1.0,	(1.0-SMX[5])*3.0,	0.0				}};		// XComp == 5 (big rod, fixed W);

	//////////////////////////////////////////////////////////////////////////////////////////
	
	//////////////////
	// Constructors //
	//////////////////
	private CModel(String name) {	// Default constructor, includes default values
		CModel.name  = name;
	}
	
	/////////////////
	// Log writing //
	/////////////////
	public static void Write(String message, String format, boolean suppressFileOutput) {
		// Construct date and time
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		// Extract format from input arguments
		String prefix = "   ";
		String suffix = "";
		if(format.equalsIgnoreCase("iter")) 	{suffix = " (" + growthIter + "/" + movementIter + ")";} 	else
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
				if(!(new File(name + "/output")).exists()) {
					new File(name + "/output").mkdir();
				}
				PrintWriter fid = new PrintWriter(new FileWriter(name + "/" + "logfile.txt",true));		// True is for append
				fid.println(string);
				fid.close();
			} catch(IOException E) {
				E.printStackTrace();
			}
		}
	}
	
	public static void Write(String message, String format) {
		Write(message,format,false);
	}
	
	//////////////////////////
	// Collision detection  //
	//////////////////////////
	public static ArrayList<CCell> DetectFloorCollision(double touchFactor) {				// actual distance < dist*radius--> collision    
		ArrayList<CCell> collisionCell = new ArrayList<CCell>();
		for(CCell cell : cellArray) {
			int NBall = (cell.type<2) ? 1 : 2;	// Figure out number of balls based on type
			for(int iBall=0; iBall<NBall; iBall++) {
				CBall ball = cell.ballArray[iBall];
				if(ball.pos.y - touchFactor*ball.radius < 0) {
					collisionCell.add(cell);
					break;
				}
			}
		}
		return collisionCell;
	}
	
	public static ArrayList<CCell> DetectCellCollision_Simple(double touchFactor) {			// Using ArrayList, no idea how big this one will get
		ArrayList<CCell> collisionCell = new ArrayList<CCell>();
		
		for(int iBall=0; iBall<ballArray.size(); iBall++) {						// If we stick to indexing, it'll be easier to determine which cells don't need to be analysed
			CBall ball = ballArray.get(iBall);
			for(int iBall2 = iBall+1; iBall2<ballArray.size(); iBall2++) {
				CBall ball2 = ballArray.get(iBall2);
				if(ball.cell.Index()!=ball2.cell.Index()) {
					Vector3d diff = ball2.pos.minus(ball.pos);
					if(Math.abs(diff.length()) - touchFactor*(ball.radius+ball2.radius) < 0) {
						collisionCell.add(ball.cell);
						collisionCell.add(ball2.cell);
					}
				}
			}
		}
		return collisionCell;
	}
	
	// Ericson collision detection
	
	private static double Clamp(double n, double min, double max) {
		if(n<min)	return min;
		if(n>max) 	return max;
		return n;
	}
		
	// Collision detection rod-rod
	public static EricsonObject DetectLinesegLineseg(Vector3d p1, Vector3d q1, Vector3d p2, Vector3d q2) {		// This is line segment - line segment collision detection. 
		// Rewritten 120912 because of strange results with the original function
		// Computes closest points C1 and C2 of S1(s) = P1+s*(Q1-P1) and S2(t) = P2+t*(Q2-P2)
		Vector3d d1 = q1.minus(p1);		// Direction of S1
		Vector3d d2 = q2.minus(p2);		// Direction of S2
		Vector3d r = p1.minus(p2);
		double a = d1.dot(d1);			// Squared length of S1, >0
		double e = d2.dot(d2);			// Squared length of S2, >0
		double f = d2.dot(r);
		double c = d1.dot(r);
		double b = d1.dot(d2);
		double denom = a*e-b*b;			// Always >0
		
		// If segments are not parallel, compute closts point on L1 to L2 and clamp to segment S1, otherwise pick arbitrary s (=0)
		double s;
		if(denom!=0.0) {
			s = Clamp((b*f-c*e) /  denom, 0.0, 1.0);
		} else	s = 0.0;
		// Compute point on L2 closest to S1(s) using t = ((P1+D1*s) - P2).dot(D2) / D2.dot(D2) = (b*s + f) / e
		double t = (b*s + f) / e;
		
		// If t is in [0,1] done. Else Clamp(t), recompute s for the new value of t using s = ((P2+D2*t) - P1).dot(D1) / D1.dot(D1) = (t*b - c) / a and clamp s to [0,1]
		if(t<0.0) {
			t = 0.0;
			s = Clamp(-c/a, 0.0, 1.0);
		} else if (t>1.0) {
			t = 1.0;
			s = Clamp((b-c)/a, 0.0, 1.0);
		}
		
		Vector3d c1 = p1.plus(d1.times(s));
		Vector3d c2 = p2.plus(d2.times(t));
		
		// Get the difference of the two closest points
//		Vector3d dP = r.plus(c1.times(s)).minus(c2.times(t));  // = S1(sc) - S2(tc)
		Vector3d dP = c1.minus(c2);  // = S1(sc) - S2(tc)
		
		double dist2 = (c1.minus(c2)).dot(c1.minus(c2));
		
		return new EricsonObject(dP, Math.sqrt(dist2), s, t, c1, c2);
	}
	
	// Collision detection ball-rod
	public static EricsonObject DetectLinesegPoint(Vector3d p1, Vector3d q1, Vector3d p2) {
		Vector3d ab = q1.minus(p1);  	// line
		Vector3d w = p2.minus(p1);	//point-line
		//Project c onto ab, computing parameterized position d(t) = a + t*(b-a)
		double rpos = w.dot(ab)/ab.dot(ab);
		//if outside segment, clamp t and therefore d to the closest endpoint
		if ( rpos<0.0 ) rpos = 0.0;
		if ( rpos>1.0 ) rpos = 1.0;
		//compute projected position from the clamped t
		Vector3d d = p1.plus(ab.times(rpos));
		//calculate the vector p2 --> d
		Vector3d dP = d.minus(p2);
		EricsonObject R = new EricsonObject(dP, dP.length(), rpos);	// Defined at the end of the model class
		return R;
	}
	
	///////////////////////////////
	// Spring breakage detection //
	///////////////////////////////
	public static ArrayList<CAnchorSpring> DetectAnchorBreak(double minStretch, double maxStretch) {
		ArrayList<CAnchorSpring> breakArray = new ArrayList<CAnchorSpring>();
		
		for(CAnchorSpring pSpring : anchorSpringArray) {
			double al = (pSpring.ball.pos.minus(pSpring.anchor)).length();		// al = Actual Length
			if(al < minStretch*pSpring.restLength || al > maxStretch*pSpring.restLength) {
				breakArray.add(pSpring);
			}
		}
		return breakArray;
	}
	
	public static ArrayList<CStickSpring> DetectStickBreak(double minStretch, double maxStretch) {
		ArrayList<CStickSpring> breakArray = new ArrayList<CStickSpring>();
		
		int iSpring = 0;
		while(iSpring < stickSpringArray.size()) {
			CStickSpring spring = stickSpringArray.get(iSpring);
			double al = (spring.ballArray[1].pos.minus(  spring.ballArray[0].pos)  ).length();		// al = Actual Length
			if(al < minStretch*spring.restLength || al > maxStretch*spring.restLength) {
				breakArray.add(spring);
			}
			iSpring += spring.NSibling+1;
		}
		return breakArray;
	} 

	////////////////////
	// Movement stuff //
	////////////////////
	public static int Movement() throws Exception {
		// Reset counter
		Assistant.NAnchorBreak = Assistant.NAnchorForm = Assistant.NStickBreak = Assistant.NStickForm = 0;
		
		int nvar = 6*ballArray.size();
		int ntimes = (int) (movementTimeStepEnd/movementTimeStep);
		double atol = 1.0e-6, rtol = atol;
		double h1 = 0.00001, hmin = 0;
		double t1 = movementTime; 
		double t2 = t1 + movementTimeStepEnd;
		Vector ystart = new Vector(nvar,0.0);

		int ii=0;											// Determine initial value vector
		for(CBall ball : ballArray) { 
			ystart.set(ii++, ball.pos.x);
			ystart.set(ii++, ball.pos.y);
			ystart.set(ii++, ball.pos.z);
			ystart.set(ii++, ball.vel.x);
			ystart.set(ii++, ball.vel.y);
			ystart.set(ii++, ball.vel.z);
		}
		Output<StepperDopr853> out = new Output<StepperDopr853>(ntimes);
		feval dydt = new feval();
		Odeint<StepperDopr853> ode = new Odeint<StepperDopr853>(ystart, t1, t2, atol, rtol, h1, hmin, out, dydt);
		int nstp = ode.integrate();
		for(int iTime=0; iTime<out.nsave; iTime++) {		// Save all intermediate results to the save variables
			int iVar = 0;
			for(CBall ball : ballArray) {
				ball.posSave[iTime].x = out.ysave.get(iVar++,iTime);
				ball.posSave[iTime].y = out.ysave.get(iVar++,iTime);
				ball.posSave[iTime].z = out.ysave.get(iVar++,iTime);
				ball.velSave[iTime].x = out.ysave.get(iVar++,iTime);
				ball.velSave[iTime].y = out.ysave.get(iVar++,iTime);
				ball.velSave[iTime].z = out.ysave.get(iVar++,iTime);
			}
		}
		{int iVar = 0;										// Only the final value is stored in the pos and vel variables
		int iTime = out.nsave;
		for(CBall ball : ballArray) {
			ball.pos.x = out.ysave.get(iVar++,iTime);
			ball.pos.y = out.ysave.get(iVar++,iTime);
			ball.pos.z = out.ysave.get(iVar++,iTime);
			ball.vel.x = out.ysave.get(iVar++,iTime);
			ball.vel.y = out.ysave.get(iVar++,iTime);
			ball.vel.z = out.ysave.get(iVar++,iTime);
		}}
		return nstp;
	}
	
	public static Vector CalculateForces(double t, Vector yode) {	
		// Read data from y
		{int ii=0; 				// Where we are in yode
		for(CBall ball : ballArray) {
			ball.pos.x = 	yode.get(ii++);
			ball.pos.y = 	yode.get(ii++);
			ball.pos.z = 	yode.get(ii++);
			ball.vel.x = 	yode.get(ii++);
			ball.vel.y = 	yode.get(ii++);
			ball.vel.z = 	yode.get(ii++);
			ball.force.x = 0;	// Clear forces for first use
			ball.force.y = 0;
			ball.force.z = 0;
		}}
		// Collision forces
		for(int iCell=0; iCell<cellArray.size(); iCell++) {
			CCell cell0 = cellArray.get(iCell);
			CBall c0b0 = cell0.ballArray[0];
			// Base collision on the cell type
			if(cell0.type<2) {
				// Check for all remaining cells
				for(int jCell=iCell+1; jCell<cellArray.size(); jCell++) {
					CCell cell1 = cellArray.get(jCell);
					CBall c1b0 = cell1.ballArray[0];
					double R2 = c0b0.radius + c1b0.radius;
					Vector3d dirn = c0b0.pos.minus(c1b0.pos);
					double dist = dirn.length();							// Mere estimation for ball-rod
					if(cell1.type<2) {										// The other cell is a ball too
						// do a simple collision detection if close enough
						if(dist<R2) {
							// We have a collision
							dirn.normalise();
							double nBallAvg = (nBallInit[cell0.type]+nBallInit[cell1.type])/2.0;
							Vector3d Fs = dirn.times(Kc*nBallAvg*(R2*1.01-dist));	// Add *1.01 to R2 to give an extra push at collisions (prevent asymptote at touching)
							// Add forces
							c0b0.force = c0b0.force.plus(Fs);
							c1b0.force = c1b0.force.minus(Fs);
						}
					} else {												// this cell is a ball, the other cell is a rod
						double H2 = aspect[cell1.type]*2.0*c1b0.radius + R2;// H2 is maximum allowed distance with still change to collide: R0 + R1 + 2*R1*aspect
						if(dist<H2) {
							// do a sphere-rod collision detection
							CBall c1b1 = cell1.ballArray[1];
							EricsonObject C = DetectLinesegPoint(c1b0.pos, c1b1.pos, c0b0.pos);
							Vector3d dP = C.dP;
							dist = C.dist;									// Make distance more accurate
							double sc = C.sc;
							// Collision detection
							if(dist<R2) {
								double nBallAvg = (nBallInit[cell0.type]+nBallInit[cell1.type])/2.0;
								double f = Kc*nBallAvg / dist*(dist-R2*1.01);
								Vector3d Fs = dP.times(f);
								// Add these elastic forces to the cells
								double sc1 = 1-sc;
								// both balls in rod
								c1b0.force.subtract(Fs.times(sc1));
								c1b1.force.subtract(Fs.times(sc));
								// ball in sphere
								c0b0.force.add(Fs);
							}	
						}
					}
				}
			} else {	// cell.type > 1
				CBall c0b1 = cell0.ballArray[1];
				for(int jCell = iCell+1; jCell<cellArray.size(); jCell++) {
					CCell cell1 = cellArray.get(jCell);
					CBall c1b0 = cell1.ballArray[0];
					double R2 = c0b0.radius + c1b0.radius;
					Vector3d dirn = c0b0.pos.minus(c1b0.pos);
					double dist = dirn.length();							// Mere estimation for ball-rod
					if(cell1.type<2) {										// This cell is a rod, the Next is a ball
						double H2 = aspect[cell0.type]*2.0*c0b0.radius + R2;// H2 is maximum allowed distance with still change to collide: R0 + R1 + 2*R1*aspect
						if(dist<H2) {
							// do a rod-sphere collision detection
							EricsonObject C = DetectLinesegPoint(c0b0.pos, c0b1.pos, c1b0.pos); 
							Vector3d dP = C.dP;
							dist = C.dist;
							double sc = C.sc;
							// Collision detection
							if(dist < R2) {
								double MBallAvg = (nBallInit[cell0.type]+nBallInit[cell1.type])/2.0;
								double f = Kc*MBallAvg / dist*(dist-R2*1.01);
								Vector3d Fs = dP.times(f);
								// Add these elastic forces to the cells
								double sc1 = 1-sc;
								// both balls in rod
								c0b0.force.subtract(Fs.times(sc1));
								c0b1.force.subtract(Fs.times(sc));
								// ball in sphere
								c1b0.force.add(Fs);
							}	
						}
					} else {	// type>1 --> the other cell is a rod too. This is where it gets tricky
						Vector3d c0b0pos = new Vector3d(c0b0.pos);
						Vector3d c0b1pos = new Vector3d(c0b1.pos);
						Vector3d c1b0pos = new Vector3d(c1b0.pos);
						CBall c1b1 = cell1.ballArray[1];
						Vector3d c1b1pos = new Vector3d(c1b1.pos);
						double H2 = aspect[cell0.type]*2.0*c0b0.radius + aspect[cell1.type]*2.0*c1b0.radius + R2;		// aspect0*2*R0 + aspect1*2*R1 + R0 + R1
						if(dist<H2) {
							// calculate the distance between the two diatoma segments
							EricsonObject C = DetectLinesegLineseg(c0b0pos, c0b1pos, c1b0pos, c1b1pos);
							Vector3d dP = C.dP;					// dP is vector from closest point 2 --> 1
							dist = C.dist;
							double sc = C.sc;
							double tc = C.tc;
							if(dist<R2) {
								double MBallAvg = (nBallInit[cell0.type]+nBallInit[cell1.type])/2.0;
								double f = Kc*MBallAvg / dist*(dist-R2*1.01);
								Vector3d Fs = dP.times(f);
								// Add these elastic forces to the cells
								double sc1 = 1-sc;
								double tc1 = 1-tc;
								// both balls in 1st rod
								c0b0.force.subtract(Fs.times(sc1));
								c0b1.force.subtract(Fs.times(sc));
								// both balls in 1st rod
								c1b0.force.add(Fs.times(tc1));
								c1b1.force.add(Fs.times(tc));
							}
						}
					}
				}
			}
		}
		// Calculate gravity+bouyancy, normal forces and drag
		for(CBall ball : ballArray) {
			// Contact forces
			double y = ball.pos.y;
			double r = ball.radius;
			if(y<r){
				ball.force.y += Kw*nBallInit[ball.cell.type]*(r-y);
			}
			// Gravity and buoyancy
			if(gravity) {
				if(y>r*1.1) {			// Only if not already at the floor plus a tiny bit 
					ball.force.y += G * ((rhoX-rhoWater)/rhoWater) * ball.n*MWX ;  //let the ball fall 
				}	
			}
			
			// Velocity damping
			ball.force.subtract(ball.vel.times(Kd*nBallInit[ball.cell.type]));
		}
		
		// Elastic forces between springs within cells (CRodSpring in type>1)
		for(CRodSpring rod : rodSpringArray) {
			CBall ball0 = rod.ballArray[0];
			CBall ball1 = rod.ballArray[1];
			// find difference vector and distance dn between balls (euclidian distance) 
			Vector3d diff = ball1.pos.minus(ball0.pos);
			double dn = diff.length();
			// Get force
			double f = rod.K/dn * (dn - rod.restLength);
			// Hooke's law
			Vector3d Fs = diff.times(f);
			// apply forces on balls
			ball0.force.add(Fs);
			ball1.force.subtract(Fs);
		}
		
		// Apply forces due to anchor springs
		for(CAnchorSpring spring : anchorSpringArray) {
			Vector3d diff = spring.anchor.minus(spring.ball.pos);
			double dn = diff.length();
			// Get force
			double f = spring.K/dn * (dn - spring.restLength);
			// Hooke's law
			Vector3d Fs = diff.times(f);
			// apply forces on balls
			spring.ball.force.add(Fs);

		}
		
		// Apply forces on sticking springs
		for(CStickSpring stick : stickSpringArray) {
			CBall ball0 = stick.ballArray[0];
			CBall ball1 = stick.ballArray[1];
			// find difference vector and distance dn between balls (euclidian distance) 
			Vector3d diff = ball1.pos.minus(ball0.pos);
			double dn = diff.length();
			// Get force
			double f = stick.K/dn * (dn - stick.restLength);
			// Hooke's law
			Vector3d Fs = diff.times(f);
			// apply forces on balls
			ball0.force.add(Fs);
			ball1.force.subtract(Fs);
		}
		
		// Filament spring elastic force (CFilSpring in filSpringArray)
		for(CFilSpring fil : filSpringArray) {
			CBall sb0 = fil.small_ballArray[0];
			CBall sb1 = fil.small_ballArray[1];
			CBall bb0 = fil.big_ballArray[0];
			CBall bb1 = fil.big_ballArray[1];
			// === big spring ===
			{// find difference vector and distance dn between balls (euclidian distance) 
			Vector3d diff = bb1.pos.minus(bb0.pos);
			double dn = diff.length();
			// Get force
			double f = fil.big_K/dn * (dn - fil.big_restLength);
			// Hooke's law
			Vector3d Fs = diff.times(f);
			// apply forces on balls
			bb0.force.add(Fs);
			bb1.force.subtract(Fs);}
			// === small spring ===
			{// find difference vector and distance dn between balls (euclidian distance) 
			Vector3d diff = sb1.pos.minus(sb0.pos);
			double dn = diff.length();
			// Get force
			double f = fil.small_K/dn * (dn - fil.small_restLength);
			// Hooke's law
			Vector3d Fs = diff.times(f);
			// apply forces on balls
			sb0.force.add(Fs);
			sb1.force.subtract(Fs);
			}
		}
		
		// Return results
		Vector dydx = new Vector(yode.size());
		int ii=0;
		for(CBall ball : ballArray) {
				double M = ball.n;
				dydx.set(ii++,ball.vel.x);			// dpos/dt = v;
				dydx.set(ii++,ball.vel.y);
				dydx.set(ii++,ball.vel.z);
				dydx.set(ii++,ball.force.x/M);		// dvel/dt = a = f/M
				dydx.set(ii++,ball.force.y/M);
				dydx.set(ii++,ball.force.z/M);
		}
		return dydx;
	}
	

	public static void AnchorUnAnchor() {
		// See what we need to anchor or break
		for(CCell cell : cellArray) {
			CBall ball0 = cell.ballArray[0];
			CBall ball1 = (cell.type>1) ? ball1 = cell.ballArray[1] : null;

			if(cell.anchorSpringArray.length>0) { 		// This cell is already anchored
				for(CAnchorSpring spring : cell.anchorSpringArray) {
					// Break anchor?
					Vector3d diff = spring.anchor.minus(spring.ball.pos);
					double dn = diff.length();
					if(dn < spring.restLength*stretchLimAnchor[0] || dn > spring.restLength*stretchLimAnchor[1]) {			// too much tension || compression --> break the spring
						Assistant.NAnchorBreak += spring.UnAnchor();
					}
				}
			} else {									// Cell is not yet anchored
				// Form anchor?
				boolean formBall0 = (ball0.pos.y < ball0.radius*formLimAnchor) ? true : false;
				boolean formBall1 = false;
				if(cell.type > 1) 	formBall1 = (ball1.pos.y < ball1.radius*formLimAnchor) ? true : false;			// If ball1 != null
				if(formBall0 || formBall1) {
					Assistant.NAnchorForm += cell.Anchor();					
				}
			}
		}
	}
	
	public static void StickUnStick() {
		for(int ii=0; ii<cellArray.size(); ii++) {
			CCell cell0 = cellArray.get(ii);
			for(int jj=ii+1; jj<cellArray.size(); jj++) {
				CCell cell1 = cellArray.get(jj);
				// Determine current distance
				CBall c0b0 = cell0.ballArray[0];
				CBall c1b0 = cell1.ballArray[0];
				double dist = (c1b0.pos.minus(c0b0.pos)).length();
				// Are these cells stuck to each other?
				boolean stuck = false;
				CStickSpring stickingSpring = null; 
				for(CStickSpring spring : stickSpringArray) {
					if( (spring.ballArray[0].cell.equals(cell0) && spring.ballArray[1].cell.equals(cell1)) || (spring.ballArray[0].cell.equals(cell1) && spring.ballArray[1].cell.equals(cell0)) ) {
						// That's the one containing both cells
						stuck = true;
						stickingSpring = spring;
						break;						
					}
				}
				if(stuck) {					// Stuck --> can we break this spring?
					if(dist < stickingSpring.restLength*stretchLimStick[0] || dist > stickingSpring.restLength*stretchLimStick[1]) 		Assistant.NStickBreak += stickingSpring.UnStick();
				} else {					// Not stuck --> can we stick them?
					double R2 = c0b0.radius + c1b0.radius;
					if(cell0.type<2 && cell1.type<2) {							// both spheres
						if(dist<R2*formLimStick)		Assistant.NStickForm += cell0.Stick(cell1);
					} else if(cell0.type<2) {									// 1st sphere, 2nd rod
						double H2 = aspect[cell1.type]*2.0*c1b0.radius + R2;	// H2 is maximum allowed distance with still change to collide: R0 + R1 + 2*R1*aspect
						if(dist<H2*formLimStick) {
							CBall c1b1 = cell1.ballArray[1];
							// do a sphere-rod collision detection
							EricsonObject C = DetectLinesegPoint(c1b0.pos, c1b1.pos, c0b0.pos);
							dist = C.dist;
							if(dist<R2*formLimStick) 	Assistant.NStickForm += cell0.Stick(cell1);
						}
					} else if (cell1.type<2) {									// 2nd sphere, 1st rod
						double H2 = aspect[cell0.type]*2.0*c0b0.radius + R2;	// H2 is maximum allowed distance with still change to collide: R0 + R1 + 2*R1*aspect
						if(dist<H2*formLimStick) {
							CBall c0b1 = cell0.ballArray[1];
							// do a sphere-rod collision detection
							EricsonObject C = DetectLinesegPoint(c0b0.pos, c0b1.pos, c1b0.pos);
							dist = C.dist;
							if(dist<R2*formLimStick) 	Assistant.NStickForm += cell0.Stick(cell1);		// OPTIMISE by passing dist to Stick
						}
					} else {													// both rod
						double H2 = aspect[cell0.type]*2.0*c0b0.radius + aspect[cell1.type]*2.0*c1b0.radius + R2;		// aspect0*2*R0 + aspect1*2*R1 + R0 + R1
						if(dist<H2*formLimStick) {
							CBall c0b1 = cell0.ballArray[1];
							CBall c1b1 = cell1.ballArray[1];
							// calculate the distance between the two diatoma segments
							EricsonObject C = DetectLinesegLineseg(c0b0.pos, c0b1.pos, c1b0.pos, c1b1.pos);
							dist = C.dist;
							if(dist<R2*formLimStick) 	Assistant.NStickForm += cell0.Stick(cell1);
						}
					}	
				}
			}
		}
		
		ArrayList<CStickSpring> unStickArray = new ArrayList<CStickSpring>(); 
		for(int jj=0; jj<stickSpringArray.size(); jj++) {		// Empty if sticking is disabled, so no need to add an extra if statement for disabled sticking
			CStickSpring stick = stickSpringArray.get(jj);
			CBall ball0 = stick.ballArray[0];
			CBall ball1 = stick.ballArray[1];
			// find difference vector and distance dn between balls (euclidian distance) 
			Vector3d diff = ball1.pos.minus(ball0.pos);
			double dn = diff.length();
			// Break stick?
			if(dn<stick.restLength*stretchLimStick[0] || dn>stick.restLength*stretchLimStick[1]) {
				unStickArray.add(stick);
				for(CStickSpring sibling : stick.siblingArray) {		// Don't worry about duplicates 
					unStickArray.add(sibling);
				}
			}}
		// UnStick() collected array
		for(CStickSpring stick : unStickArray)		Assistant.NStickBreak += stick.UnStick();
}
	
	//////////////////
	// Growth stuff //
	//////////////////
	public static int GrowthSimple() {						// Growth based on a random number, further enhanced by being sticked to a cell of other type (==0 || !=0) 
		int newCell = 0;
		int NCell = cellArray.size();
		for(int iCell=0; iCell<NCell; iCell++){
			CCell mother = cellArray.get(iCell);
			double mass = mother.GetMass();

			// Random growth
			mass *= (0.95+rand.Double()/5.0);
			mother.SetMass(mass);
			
			// Cell growth or division
			if(mother.GetMass()>nCellMax[mother.type]) {
				newCell++;
				GrowCell(mother);
			}	
		}
		return newCell;
	}
	
	public static int GrowthSyntrophy() {						// Growth based on a random number, further enhanced by being sticked to a cell of other type (==0 || !=0) 
		int newCell = 0;
		int NCell = cellArray.size();
		for(int iCell=0; iCell<NCell; iCell++){
			CCell mother = cellArray.get(iCell);
			double mass = mother.GetMass();

			// Random growth
			mass *= (0.95+rand.Double()/5.0);
			// Syntrophic growth
			for(CCell stickCell : mother.stickCellArray) {
				if((mother.type<2 && stickCell.type>1) || (mother.type>1 && stickCell.type<2)) {
					// The cell types are different on the other end of the spring
					mass *= 1.2;
					break;
				}
			}
			mother.SetMass(mass);
			
			// Cell growth or division
			if(mother.GetMass()>nCellMax[mother.type]) {
				newCell++;
				GrowCell(mother);
			}
		}
		return newCell;
	}
	
	public static int GrowthFlux() {
		int newCell=0;
		int NCell = cellArray.size();
		for(int iCell=0; iCell<NCell; iCell++){
			CCell mother = cellArray.get(iCell);
			// Obtain mol increase based on flux
			double molIn = mother.q * mother.GetMass() * growthTimeStep * SMX[mother.type];
			// Grow mother cell
			double newMass = mother.GetMass()+molIn;
			mother.SetMass(newMass);
			// divide mother cell if ready 
			if(mother.GetMass()>newMass) {
				newCell++;
				GrowCell(mother);
			}
		}
		return newCell;
	}
	
	public static CCell GrowCell(CCell mother) {
		double mass = mother.GetMass();
		CCell daughter;
		if(mother.type<2) {
			// Come up with a nice direction in which to place the new cell
			Vector3d direction = new Vector3d(rand.Double()-0.5,rand.Double()-0.5,rand.Double()-0.5);			
			direction.normalise();
			double displacement = mother.ballArray[0].radius;
			// Make a new, displaced cell
			daughter = new CCell(mother.type,																// Same type as cell
					mother.ballArray[0].pos.x - displacement * direction.x,						// The new location is the old one plus some displacement					
					mother.ballArray[0].pos.y - displacement * direction.y,	
					mother.ballArray[0].pos.z - displacement * direction.z,
					mother.filament,
					mother.colour);														// Same filament boolean as cell and pointer to the model
			// Set mass for both cells
			daughter.SetMass(mass/2.0);		// Radius is updated in this method
			mother.SetMass(mass/2.0);
			// Set properties for new cell
			daughter.ballArray[0].vel = 	new Vector3d(mother.ballArray[0].vel);
			daughter.ballArray[0].force = 	new Vector3d(mother.ballArray[0].force);
			daughter.colour =				mother.colour;							// copy of reference
			daughter.mother = 				mother;
			daughter.q = 				mother.q;
			// Displace old cell
			mother.ballArray[0].pos = mother.ballArray[0].pos.plus(  direction.times( displacement )  );
			// Contain cells to y dimension of domain
			if(mother.ballArray[0].pos.y 	< mother.ballArray[0].radius) 		{mother.ballArray[0].pos.y 	= mother.ballArray[0].radius;};
			if(daughter.ballArray[0].pos.y < daughter.ballArray[0].radius)  	{daughter.ballArray[0].pos.y 	= daughter.ballArray[0].radius;};
			// Set filament springs
			if(daughter.filament) {
				daughter.Stick(mother);		// Because there are no filaments for two spherical cells
			}
		} else {
			CBall motherBall0 = mother.ballArray[0];
			CBall motherBall1 = mother.ballArray[1];
			// Direction
			Vector3d direction = motherBall1.pos.minus( motherBall0.pos );
			direction.normalise();
			// Displacement
			double displacement; 																		
//			if(mother.type<4) {
//				displacement = motherBall0.radius*Math.pow(2.0,-0.666666);								// A very strange formula: compare our radius to the C++ equation for Rpos and you'll see it's the same
//			} else {
				displacement = motherBall1.radius/2.0;
//			}
			// Make a new, displaced cell
			Vector3d middle = motherBall1.pos.plus(motherBall0.pos).divide(2.0); 
			daughter = new CCell(mother.type,													// Same type as cell
					middle.x+	  displacement*direction.x,										// First ball. First ball and second ball were swapped in MATLAB and possibly C++					
					middle.y+1.01*displacement*direction.y,										// ought to be displaced slightly in original C++ code but is displaced significantly this way (change 1.01 to 2.01)
					middle.z+	  displacement*direction.z,
					motherBall1.pos.x,															// Second ball
					motherBall1.pos.y,
					motherBall1.pos.z,
					mother.filament,
					mother.colour);																		// Same filament boolean as cell and pointer to the model
			// Set mass for both cells
			daughter.SetMass(mass/2.0);
			mother.SetMass(mass/2.0);
			// Displace old cell, 2nd ball
			motherBall1.pos = middle.minus(direction.times(displacement));
			mother.springArray[0].ResetRestLength();
			// Contain cells to y dimension of domain
			for(int iBall=0; iBall<2; iBall++) {
				if(mother.ballArray[iBall].pos.y 		< mother.ballArray[iBall].radius) 		{mother.ballArray[0].pos.y 	= mother.ballArray[0].radius;};
				if( daughter.ballArray[iBall].pos.y 	< daughter.ballArray[iBall].radius) 	{daughter.ballArray[0].pos.y 	= daughter.ballArray[0].radius;};
			}
			// Set properties for new cell
			for(int iBall=0; iBall<2; iBall++) {
				daughter.ballArray[iBall].vel = 	new Vector3d(mother.ballArray[iBall].vel);
				daughter.ballArray[iBall].force = 	new Vector3d(mother.ballArray[iBall].force);
			}
			daughter.colour =	mother.colour;
			daughter.mother = 	mother;
			daughter.motherIndex = mother.Index();
			daughter.springArray[0].restLength = mother.springArray[0].restLength;

			// Set filament springs
			if(daughter.filament) {
				for(CFilSpring fil : filSpringArray) {
					boolean found=false;
					if( fil.big_ballArray[0] == motherBall0) {
						fil.set(daughter.ballArray[0],0);
						found = true;}
					if( fil.big_ballArray[1] == motherBall0) {
						fil.set(daughter.ballArray[0],1);
						found = true;}
					if( fil.small_ballArray[0] == motherBall1) {
						fil.set(daughter.ballArray[1],2);
						found = true;}
					if( fil.small_ballArray[1] == motherBall1) {
						fil.set(daughter.ballArray[1],3);
						found = true;}
					if(found) {
						fil.ResetSmall();
						fil.ResetBig();
					}
				}
				new CFilSpring(mother,daughter);
			}
		}
		return daughter;
	}
	
	////////////////////////////
	// Anchor and Stick stuff //
	////////////////////////////
	public static void BreakStick(ArrayList<CStickSpring> breakArray) {
		for(CStickSpring pSpring : breakArray) {
			CCell cell0 = pSpring.ballArray[0].cell;
			CCell cell1 = pSpring.ballArray[1].cell;
			// Remove cells from each others' stickCellArray 
			cell0.stickCellArray.remove(cell1);
			cell1.stickCellArray.remove(cell0);
			// Remove springs from model stickSpringArray
			stickSpringArray.remove(pSpring);
			for(int ii=0; ii<pSpring.siblingArray.length; ii++) {
				stickSpringArray.remove(pSpring.siblingArray[ii]);
			}
		}
	}
	
	public static int BuildAnchor(ArrayList<CCell> collisionArray) {
		// Make unique
		for(CAnchorSpring pSpring : anchorSpringArray) collisionArray.remove(pSpring.ball.cell);
		
		// Anchor the non-stuck, collided cells to the ground
		for(CCell cell : collisionArray) cell.Anchor();
		return anchorSpringArray.size();
	}
	
	public static int BuildStick(ArrayList<CCell> collisionArray) {
		int counter = 0;
		for(int ii=0; ii<collisionArray.size(); ii+=2) {		// For loop works per duo
			boolean setStick = true;
			CCell cell0 = collisionArray.get(ii);
			CCell cell1 = collisionArray.get(ii+1);
			// Check if already stuck, don't stick if that is the case
			for(CStickSpring pSpring : stickSpringArray) {		// This one should update automatically after something new has been stuck --> Only new ones are stuck AND, as a result, only uniques are sticked 
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
	
	////////////////////////////////////////////
	// Saving, loading things, reconstructing //
	////////////////////////////////////////////
	public static void Save() {
		MLStructure mlModel = new MLStructure("model", new int[] {1,1});
		int N;
		double[] arrayIndex;
		// Model properties
		mlModel.setField("name",                          new MLChar(null, new String[] {name}, 1));                                      	
		mlModel.setField("randomSeed",                    new MLDouble(null, new double[] {randomSeed}, 1));                              	
		mlModel.setField("sticking",                      new MLDouble(null, new double[] {sticking?1:0}, 1));                            			mlModel.setField("anchoring",                     new MLDouble(null, new double[] {anchoring?1:0}, 1));                           			mlModel.setField("filament",                      new MLDouble(null, new double[] {filament?1:0}, 1));                            			mlModel.setField("gravity",                       new MLDouble(null, new double[] {gravity?1:0}, 1));                             			// Spring constants
		mlModel.setField("Kr",                            new MLDouble(null, new double[] {Kr}, 1));                                      	// internal cell spring (per ball)
		mlModel.setField("Kf",                            new MLDouble(null, new double[] {Kf}, 1));                                      	// filament spring (per ball average)
		mlModel.setField("Kw",                            new MLDouble(null, new double[] {Kw}, 1));                                      	// wall spring (per ball)
		mlModel.setField("Kc",                            new MLDouble(null, new double[] {Kc}, 1));                                      	// collision (per ball)
		mlModel.setField("Ks",                            new MLDouble(null, new double[] {Ks}, 1));                                      	// sticking (per ball average)
		mlModel.setField("Kan",                           new MLDouble(null, new double[] {Kan}, 1));                                     	// anchor (per BALL)
		mlModel.setField("stretchLimAnchor",              new MLDouble(null, stretchLimAnchor, stretchLimAnchor.length));                 	// Maximum tension and compression (1-this value) for anchoring springs
		mlModel.setField("formLimAnchor",                 new MLDouble(null, new double[] {formLimAnchor}, 1));                           	// Multiplication factor for rest length to form anchors. Note that actual rest length is the distance between the two, which could be less
		mlModel.setField("stretchLimStick",               new MLDouble(null, stretchLimStick, stretchLimStick.length));                   	// Maximum tension and compression (1-this value) for sticking springs
		mlModel.setField("formLimStick",                  new MLDouble(null, new double[] {formLimStick}, 1));                            	// Multiplication factor for rest length to form sticking springs.
		// Domain properties
		mlModel.setField("Kd",                            new MLDouble(null, new double[] {Kd}, 1));                                      	// drag force coefficient (per BALL)
		mlModel.setField("G",                             new MLDouble(null, new double[] {G}, 1));                                       	// [m/s2], acceleration due to gravity
		mlModel.setField("rhoWater",                      new MLDouble(null, new double[] {rhoWater}, 1));                                	// [kg/m3], density of bulk liquid (water)
		mlModel.setField("rhoX",                          new MLDouble(null, new double[] {rhoX}, 1));                                    	// [kg/m3], diatoma density
		mlModel.setField("MWX",                           new MLDouble(null, new double[] {MWX}, 1));                                     	// [kg/mol], composition CH1.8O0.5N0.2
		mlModel.setField("L",                             new MLDouble(null, new double[] {L.x, L.y, L.z}, 3));                           	
		// Model biomass properties
		mlModel.setField("NXComp",                        new MLDouble(null, new double[] {NXComp}, 1));                                  	// Types of biomass
		mlModel.setField("NdComp",                        new MLDouble(null, new double[] {NdComp}, 1));                                  	// d for dynamic compound (e.g. total Ac)
		mlModel.setField("NcComp",                        new MLDouble(null, new double[] {NcComp}, 1));                                  	// c for concentration (or virtual compound, e.g. Ac-)
		mlModel.setField("NAcidDiss",                     new MLDouble(null, new double[] {NAcidDiss}, 1));                               	// Number of acid dissociation reactions
		mlModel.setField("NInitCell",                     new MLDouble(null, new double[] {NInitCell}, 1));                               	// Initial number of cells
		//
		double[] DcellType = new double[cellType.length];		for(int ii=0; ii<cellType.length; ii++)		DcellType[ii] = cellType[ii];		mlModel.setField("cellType",                      new MLDouble(null, DcellType, cellType.length));                                	// Cell types used by default
		//
		//	public static double[] aspect	= {2.0, 2.0, 2.0, 2.0, 2.0, 2.0};	// Aspect ratio of cells
		mlModel.setField("aspect",                        new MLDouble(null, aspect, aspect.length));                                     	// Aspect ratio of cells (last 2: around 4.0 and 2.0 resp.)
		// Ball properties
		mlModel.setField("MCellInit",                     new MLDouble(null, nCellInit, nCellInit.length));                               	// [Cmol] initial cell, when created at t=0. Factor *0.9 used for initial mass type<4
		mlModel.setField("MBallInit",                     new MLDouble(null, nBallInit, nBallInit.length));                               	// [Cmol] initial mass of one ball in the cell
		mlModel.setField("MCellMax",                      new MLDouble(null, nCellMax, nCellMax.length));                                 	// [Cmol] max mass of cells before division;
		// Progress
		mlModel.setField("growthTime",                    new MLDouble(null, new double[] {growthTime}, 1));                              	// [s] Current time for the growth
		mlModel.setField("growthTimeStep",                new MLDouble(null, new double[] {growthTimeStep}, 1));                          	// [s] Time step for growth
		mlModel.setField("growthIter",                    new MLDouble(null, new double[] {growthIter}, 1));                              	// [-] Counter time iterations for growth
		mlModel.setField("movementTime",                  new MLDouble(null, new double[] {movementTime}, 1));                            	// [s] initial time for movement (for ODE solver)
		mlModel.setField("movementTimeStep",              new MLDouble(null, new double[] {movementTimeStep}, 1));                        	// [s] output time step  for movement
		mlModel.setField("movementTimeStepEnd",           new MLDouble(null, new double[] {movementTimeStepEnd}, 1));                     	// [s] time interval for movement (for ODE solver), 5*movementTimeStep by default
		mlModel.setField("movementIter",                  new MLDouble(null, new double[] {movementIter}, 1));                            	// [-] counter time iterations for movement
		// Arrays

		// cellArray
		N = cellArray.size();
		MLStructure mlcellArray = new MLStructure(null, new int[] {cellArray.size() ,1});
		for(int ii=0; ii<N; ii++) {
			CCell obj = cellArray.get(ii);
			mlcellArray.setField("type",                      new MLDouble(null, new double[] {obj.type}, 1), ii);                            	
			mlcellArray.setField("filament",                  new MLDouble(null, new double[] {obj.filament?1:0}, 1), ii);                    	
			mlcellArray.setField("colour",                    new MLDouble(null, obj.colour, obj.colour.length), ii);                         	
			
			arrayIndex = new double[obj.ballArray.length];
			for(int jj=0; jj<obj.ballArray.length; jj++)	arrayIndex[jj] = obj.ballArray[jj].Index()+1;
			mlcellArray.setField("ballArray",                 new MLDouble(null, arrayIndex, 1), ii);                                         	// Note that this ballArray has the same name as CModel's
			
			arrayIndex = new double[obj.springArray.length];
			for(int jj=0; jj<obj.springArray.length; jj++)	arrayIndex[jj] = obj.springArray[jj].Index()+1;
			mlcellArray.setField("springArray",               new MLDouble(null, arrayIndex, 1), ii);                                         	
			
			arrayIndex = new double[obj.stickCellArray.size()];
			for(int jj=0; jj<obj.stickCellArray.size(); jj++)	arrayIndex[jj] = obj.stickCellArray.get(jj).Index()+1;
			mlcellArray.setField("stickCellArray",            new MLDouble(null, arrayIndex, 1), ii);                                         	
			
			arrayIndex = new double[obj.stickSpringArray.size()];
			for(int jj=0; jj<obj.stickSpringArray.size(); jj++)	arrayIndex[jj] = obj.stickSpringArray.get(jj).Index()+1;
			mlcellArray.setField("stickSpringArray",          new MLDouble(null, arrayIndex, 1), ii);                                         	
			
			arrayIndex = new double[obj.anchorSpringArray.length];
			for(int jj=0; jj<obj.anchorSpringArray.length; jj++)	arrayIndex[jj] = obj.anchorSpringArray[jj].Index()+1;
			mlcellArray.setField("anchorSpringArray",         new MLDouble(null, arrayIndex, 1), ii);                                         	
			mlcellArray.setField("motherIndex",               new MLDouble(null, new double[] {obj.motherIndex}, 1), ii);                     	
			mlcellArray.setField("q",                         new MLDouble(null, new double[] {obj.q}, 1), ii);                               	// [mol reactions (CmolX * s)-1]
		}
		mlModel.setField("cellArray", mlcellArray);

		// ballArray
		N = ballArray.size();
		MLStructure mlballArray = new MLStructure(null, new int[] {ballArray.size() ,1});
		for(int ii=0; ii<N; ii++) {
			CBall obj = ballArray.get(ii);
			mlballArray.setField("mass",                      new MLDouble(null, new double[] {obj.n}, 1), ii);                            	
			mlballArray.setField("radius",                    new MLDouble(null, new double[] {obj.radius}, 1), ii);                          	// Will put in method
			mlballArray.setField("pos",                       new MLDouble(null, new double[] {obj.pos.x, obj.pos.y, obj.pos.z}, 3), ii);     	
			mlballArray.setField("vel",                       new MLDouble(null, new double[] {obj.vel.x, obj.vel.y, obj.vel.z}, 3), ii);     	
			mlballArray.setField("force",                     new MLDouble(null, new double[] {obj.force.x, obj.force.y, obj.force.z}, 3), ii);	
			mlballArray.setField("cellIndex",                 new MLDouble(null, new double[] {obj.cellIndex}, 1), ii);                       	
		}
		mlModel.setField("ballArray", mlballArray);

		// rodSpringArray
		N = rodSpringArray.size();
		MLStructure mlrodSpringArray = new MLStructure(null, new int[] {rodSpringArray.size() ,1});
		for(int ii=0; ii<N; ii++) {
			CRodSpring obj = rodSpringArray.get(ii);
			
			arrayIndex = new double[obj.ballArray.length];
			for(int jj=0; jj<obj.ballArray.length; jj++)	arrayIndex[jj] = obj.ballArray[jj].Index()+1;
			mlrodSpringArray.setField("ballArray",            new MLDouble(null, arrayIndex, 1), ii);                                         	
			mlrodSpringArray.setField("K",                    new MLDouble(null, new double[] {obj.K}, 1), ii);                               	
			mlrodSpringArray.setField("restLength",           new MLDouble(null, new double[] {obj.restLength}, 1), ii);                      	
		}
		mlModel.setField("rodSpringArray", mlrodSpringArray);

		// stickSpringArray
		N = stickSpringArray.size();
		MLStructure mlstickSpringArray = new MLStructure(null, new int[] {stickSpringArray.size() ,1});
		for(int ii=0; ii<N; ii++) {
			CStickSpring obj = stickSpringArray.get(ii);
			
			arrayIndex = new double[obj.ballArray.length];
			for(int jj=0; jj<obj.ballArray.length; jj++)	arrayIndex[jj] = obj.ballArray[jj].Index()+1;
			mlstickSpringArray.setField("ballArray",          new MLDouble(null, arrayIndex, 1), ii);                                         	
			mlstickSpringArray.setField("K",                  new MLDouble(null, new double[] {obj.K}, 1), ii);                               	
			mlstickSpringArray.setField("restLength",         new MLDouble(null, new double[] {obj.restLength}, 1), ii);                      	
			
			arrayIndex = new double[obj.siblingArray.length];
			for(int jj=0; jj<obj.siblingArray.length; jj++)	arrayIndex[jj] = obj.siblingArray[jj].Index()+1;
			mlstickSpringArray.setField("siblingArray",       new MLDouble(null, arrayIndex, 1), ii);                                         	
			mlstickSpringArray.setField("NSibling",           new MLDouble(null, new double[] {obj.NSibling}, 1), ii);                        	
		}
		mlModel.setField("stickSpringArray", mlstickSpringArray);

		// filSpringArray
		N = filSpringArray.size();
		MLStructure mlfilSpringArray = new MLStructure(null, new int[] {filSpringArray.size() ,1});
		for(int ii=0; ii<N; ii++) {
			CFilSpring obj = filSpringArray.get(ii);
			
			arrayIndex = new double[obj.big_ballArray.length];
			for(int jj=0; jj<obj.big_ballArray.length; jj++)	arrayIndex[jj] = obj.big_ballArray[jj].Index()+1;
			mlfilSpringArray.setField("big_ballArray",        new MLDouble(null, arrayIndex, 1), ii);                                         	
			mlfilSpringArray.setField("big_K",                new MLDouble(null, new double[] {obj.big_K}, 1), ii);                           	
			mlfilSpringArray.setField("big_restLength",       new MLDouble(null, new double[] {obj.big_restLength}, 1), ii);                  	
			
			arrayIndex = new double[obj.small_ballArray.length];
			for(int jj=0; jj<obj.small_ballArray.length; jj++)	arrayIndex[jj] = obj.small_ballArray[jj].Index()+1;
			mlfilSpringArray.setField("small_ballArray",      new MLDouble(null, arrayIndex, 1), ii);                                         	
			mlfilSpringArray.setField("small_K",              new MLDouble(null, new double[] {obj.small_K}, 1), ii);                         	
			mlfilSpringArray.setField("small_restLength",     new MLDouble(null, new double[] {obj.small_restLength}, 1), ii);                	
		}
		mlModel.setField("filSpringArray", mlfilSpringArray);

		// anchorSpringArray
		N = anchorSpringArray.size();
		MLStructure mlanchorSpringArray = new MLStructure(null, new int[] {anchorSpringArray.size() ,1});
		for(int ii=0; ii<N; ii++) {
			CAnchorSpring obj = anchorSpringArray.get(ii);
			mlanchorSpringArray.setField("anchor",            new MLDouble(null, new double[] {obj.anchor.x, obj.anchor.y, obj.anchor.z}, 3), ii);	
			mlanchorSpringArray.setField("K",                 new MLDouble(null, new double[] {obj.K}, 1), ii);                               	
			mlanchorSpringArray.setField("restLength",        new MLDouble(null, new double[] {obj.restLength}, 1), ii);                      	
			
			arrayIndex = new double[obj.siblingArray.length];
			for(int jj=0; jj<obj.siblingArray.length; jj++)	arrayIndex[jj] = obj.siblingArray[jj].Index()+1;
			mlanchorSpringArray.setField("siblingArray",      new MLDouble(null, arrayIndex, 1), ii);                                         	
		}
		mlModel.setField("anchorSpringArray", mlanchorSpringArray);
		// === COMSOL STUFF ===
		// Biomass, assuming Cmol and composition CH1.8O0.5N0.2 (i.e. MW = 24.6 g/mol)
		//							type 0					type 1					type 2					type 3					type 4					type 5
		// 							m. hungatei				m. hungatei				s. fumaroxidans			s. fumaroxidans			s. fumaroxidans			s. fumaroxidans
		mlModel.setField("SMX",                           new MLDouble(null, SMX, SMX.length));                                           	// [Cmol X/mol reacted] Biomass yields per flux reaction. All types from Scholten 2000, grown in coculture on propionate
		mlModel.setField("K",                             new MLDouble(null, K, K.length));                                               	// [microM] FIXME
		mlModel.setField("qMax",                          new MLDouble(null, qMax, qMax.length));                                         	// [mol (Cmol*s)-1] M.h. from Robinson 1984, assuming yield, growth on NaAc. S.f. from Scholten 2000;
		mlModel.setField("rateEquation",                  new MLChar(null, rateEquation));                                                	
		// 	 pH calculations
		//							HPro		CO2			HCO3-		HAc
		//							0,			1,			2,			3
		mlModel.setField("Ka",                            new MLDouble(null, Ka, Ka.length));                                             	// From Wikipedia 120811. CO2 and H2CO3 --> HCO3- + H+;
		mlModel.setField("pHEquation",                    new MLChar(null, pHEquation));                                                  	// pH calculations
		// Diffusion
		// 							ProT, 		CO2T,				AcT,				H2, 				CH4
		//							0,    		1,   				2, 					3,   				4
		mlModel.setField("BCConc",                        new MLDouble(null, BCConc, BCConc.length));                                     	
		mlModel.setField("D",                             new MLDouble(null, D, D.length));                                               	
		mlModel.setField("SMdiffusion",                   new MLDouble(null, SMdiffusion));                                               

		// Create a list and add mlModel
		ArrayList<MLArray> list = new ArrayList<MLArray>(1);
		list.add(mlModel);
		try {
			new MatFileWriter(name + "/output/" + String.format("g%04dm%04d", growthIter, movementIter) + ".mat",list);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public static void Load(String fileName) {
		try {
			MatFileReader mlFile = new MatFileReader(fileName);
			MLStructure mlModel = (MLStructure)mlFile.getMLArray("model");
			
//			aspect 		= ((MLDouble)mlModel.getField("aspect")).getReal(0);
			G 			= ((MLDouble)mlModel.getField("G")).getReal(0);
			growthIter 	= ((MLDouble)mlModel.getField("growthIter")).getReal(0).intValue();
			growthTime 	= ((MLDouble)mlModel.getField("growthTime")).getReal(0);
			growthTimeStep = ((MLDouble)mlModel.getField("growthTimeStep")).getReal(0);
			Kr 			= ((MLDouble)mlModel.getField("Ki")).getReal(0);
			Kan 			= ((MLDouble)mlModel.getField("Ka")).getReal(0);
			Kc 			= ((MLDouble)mlModel.getField("Kc")).getReal(0);
			Kd 			= ((MLDouble)mlModel.getField("Kd")).getReal(0);
			Kf 			= ((MLDouble)mlModel.getField("Kf")).getReal(0);
			Ks 			= ((MLDouble)mlModel.getField("Ks")).getReal(0);
			Kw 			= ((MLDouble)mlModel.getField("Kw")).getReal(0);
			L 			= new Vector3d(
									((MLDouble)mlModel.getField("L")).getReal(0),
									((MLDouble)mlModel.getField("L")).getReal(1),
									((MLDouble)mlModel.getField("L")).getReal(2));
//			MCellInit	= ((MLDouble)mlModel.getField("MCellInit")).getReal(0);
//			MCellMax 	= ((MLDouble)mlModel.getField("MCellMax")).getReal(0);
			K[0] 			= ((MLDouble)mlModel.getField("K")).getReal(0);
			K[1] 			= ((MLDouble)mlModel.getField("K")).getReal(1);
//			K[2] 			= ((MLDouble)mlModel.getField("K")).getReal(2);
//			rMax0 		= ((MLDouble)mlModel.getField("rMax1")).getReal(0);
//			rMax1 		= ((MLDouble)mlModel.getField("rMax2")).getReal(0);
//			BCConc 		= new double[]{	((MLDouble)mlModel.getField("BCConc")).getReal(0),
//							((MLDouble)mlModel.getField("BCConc")).getReal(1),
//							((MLDouble)mlModel.getField("BCConc")).getReal(2)};
//			D 			= new double[]{	((MLDouble)mlModel.getField("D")).getReal(0),
//							((MLDouble)mlModel.getField("D")).getReal(1),
//							((MLDouble)mlModel.getField("D")).getReal(2)};
			movementIter = ((MLDouble)mlModel.getField("movementIter")).getReal(0).intValue();
			movementTime = ((MLDouble)mlModel.getField("movementTime")).getReal(0);
			movementTimeStepEnd = ((MLDouble)mlModel.getField("movementTimeEnd")).getReal(0);
			movementTimeStep = ((MLDouble)mlModel.getField("movementTimeStep")).getReal(0);
			name 		= ((MLChar)mlModel.getField("name")).getString(0);
			NInitCell 	= ((MLDouble)mlModel.getField("NInitCell")).getReal(0).intValue();
			NXComp = ((MLDouble)mlModel.getField("NXComp")).getReal(0).intValue();
			randomSeed 	= ((MLDouble)mlModel.getField("randomSeed")).getReal(0).intValue();
			rhoX 		= ((MLDouble)mlModel.getField("rho_m")).getReal(0);
			rhoWater 		= ((MLDouble)mlModel.getField("rho_w")).getReal(0);
			
			// cellArray
			MLStructure mlCellArray = (MLStructure)mlModel.getField("cellArray");
			int NCell = mlCellArray.getSize();
			cellArray	 	= new ArrayList<CCell>(NCell);
			for(int iCell=0; iCell<NCell; iCell++) {
				CCell cell = new CCell();
				cell.q	= ((MLDouble)mlCellArray.getField("cellArrayIndex", iCell)).getReal(0).doubleValue();
				cell.colour	= new double[]{((MLDouble)mlCellArray.getField("colour", iCell)).getReal(0),
									((MLDouble)mlCellArray.getField("colour", iCell)).getReal(1),
									((MLDouble)mlCellArray.getField("colour", iCell)).getReal(2)};
				cell.filament 	= ((MLDouble)mlCellArray.getField("filament", iCell)).getReal(0)==1 ? true : false;
				cell.type		= ((MLDouble)mlCellArray.getField("type", iCell)).getReal(0).intValue();
				if(mlCellArray.getField("motherIndex", iCell).isEmpty()) {
					cell.mother = null;
				} else {
					cell.mother 	= cellArray.get(((MLDouble)mlCellArray.getField("motherIndex", iCell)).getReal(0).intValue()-1);					
				}
				cellArray.add(cell);
			}
			
			// ballArray
			MLStructure mlBallArray = (MLStructure)mlModel.getField("ballArray");
			int NBall 		= mlBallArray.getSize();
			ballArray	 	= new ArrayList<CBall>(NBall);
			for(int iBall=0; iBall < NBall; iBall++) {
				CBall ball = new CBall();
//				ball.ballArrayIndex = ((MLDouble)mlBallArray.getField("cellBallArrayIndex",iBall)).getReal(0).intValue()-1;
				ball.cell = cellArray.get(((MLDouble)mlBallArray.getField("cellArrayIndex",iBall)).getReal(0).intValue()-1);
				ball.n 	= ((MLDouble)mlBallArray.getField("mass",iBall)).getReal(0);
				ball.radius = ball.Radius();
				ball.pos 	= new Vector3d(
								((MLDouble)mlBallArray.getField("pos", iBall)).getReal(0),
								((MLDouble)mlBallArray.getField("pos", iBall)).getReal(1),
								((MLDouble)mlBallArray.getField("pos", iBall)).getReal(2));
				ball.vel 	= new Vector3d(
								((MLDouble)mlBallArray.getField("vel", iBall)).getReal(0),
								((MLDouble)mlBallArray.getField("vel", iBall)).getReal(1),
								((MLDouble)mlBallArray.getField("vel", iBall)).getReal(2));
				ball.force = new Vector3d();
				// posSave and velSave
				int NSave = (int)(movementTimeStepEnd/movementTimeStep);		// -1 for not last index, +1 for initial values 
				ball.posSave = new Vector3d[NSave];
				ball.velSave = new Vector3d[NSave];
				for(int ii=0; ii<NSave; ii++) {
					ball.posSave[ii] =  new Vector3d(
										((MLDouble)mlBallArray.getField("posSave", iBall)).getReal(ii,0),
										((MLDouble)mlBallArray.getField("posSave", iBall)).getReal(ii,1),
										((MLDouble)mlBallArray.getField("posSave", iBall)).getReal(ii,2));
					ball.velSave[ii] =  new Vector3d(
										((MLDouble)mlBallArray.getField("velSave", iBall)).getReal(ii,0),
										((MLDouble)mlBallArray.getField("velSave", iBall)).getReal(ii,1),
										((MLDouble)mlBallArray.getField("velSave", iBall)).getReal(ii,2));

				}
				ballArray.add(ball);
			}
			
			// rodSpringArray
			int NRod = ((MLStructure)mlModel.getField("rodSpringArray")).getSize();
			rodSpringArray = new ArrayList<CRodSpring>(NRod);
			MLStructure mlRodSpringArray = (MLStructure)mlModel.getField("rodSpringArray");
			for(int iRod=0; iRod<NRod; iRod++) {
				CRodSpring pRod= new CRodSpring();
				// ballArray
				for(int iBall=0; iBall<2; iBall++) {
					int jBall = ((MLDouble)mlRodSpringArray.getField("ballArrayIndex",iRod)).getReal(iBall).intValue()-1;
					pRod.ballArray[iBall] = ballArray.get(jBall);
				}
				pRod.K 		= ((MLDouble)mlRodSpringArray.getField("K", iRod)).getReal(0);
				pRod.restLength = ((MLDouble)mlRodSpringArray.getField("restLength", iRod)).getReal(0);
				rodSpringArray.add(pRod);
			}
			
			// anchorSpringArray
			int NAnchor = ((MLStructure)mlModel.getField("anchorSpringArray")).getSize();
			anchorSpringArray = new ArrayList<CAnchorSpring>(NAnchor);
			MLStructure mlAnchorSpringArray = (MLStructure)mlModel.getField("anchorSpringArray");
			for(int iAnchor=0; iAnchor<NAnchor; iAnchor++) {
				CAnchorSpring spring = new CAnchorSpring();
				spring.anchor 	= new Vector3d(
									((MLDouble)mlAnchorSpringArray.getField("anchor",iAnchor)).getReal(0),
									((MLDouble)mlAnchorSpringArray.getField("anchor",iAnchor)).getReal(1),
									((MLDouble)mlAnchorSpringArray.getField("anchor",iAnchor)).getReal(2));
				spring.K		= ((MLDouble)mlAnchorSpringArray.getField("K",iAnchor)).getReal(0);
				int iBall		= ((MLDouble)mlAnchorSpringArray.getField("ballArrayIndex",iAnchor)).getReal(0).intValue()-1;
				spring.ball 	= ballArray.get(iBall);
				spring.restLength = ((MLDouble)mlAnchorSpringArray.getField("restLength",iAnchor)).getReal(0);
				anchorSpringArray.add(spring);
			}
			for(int iAnchor=0; iAnchor<NAnchor; iAnchor++) {	// Additional for loop to assign siblings
				CAnchorSpring spring = anchorSpringArray.get(iAnchor);
				
				if(spring.ball.cell.type!=0) {
					spring.siblingArray = new CAnchorSpring[1];
					int iSibling = ((MLDouble)mlAnchorSpringArray.getField("siblingArrayIndex", iAnchor)).getReal(0).intValue()-1;
					spring.siblingArray[0] = anchorSpringArray.get(iSibling); 
				}
			}
			// filSpringArray
			MLStructure mlFilSpringArray = (MLStructure)mlModel.getField("filSpringArray");
			int NFil = mlFilSpringArray.getSize();
			filSpringArray = new ArrayList<CFilSpring>(NFil);
			for(int iFil=0; iFil<NFil; iFil++) {
				CFilSpring pFil 		= new CFilSpring();
				// bigSpring
				pFil.big_K 				= ((MLDouble)mlFilSpringArray.getField("big_K", iFil)).getReal(0);
				pFil.big_restLength 	= ((MLDouble)mlFilSpringArray.getField("big_restLength", iFil)).getReal(0);
				for(int iBall=0; iBall<2; iBall++) {
					int jBall = ((MLDouble)mlFilSpringArray.getField("big_ballArrayIndex", iFil)).getReal(iBall).intValue()-1;
					pFil.big_ballArray[iBall] = ballArray.get(jBall);	
				}
				// smallSpring
				pFil.small_K = ((MLDouble)mlFilSpringArray.getField("small_K", iFil)).getReal(0);
				pFil.small_restLength = ((MLDouble)mlFilSpringArray.getField("small_restLength", iFil)).getReal(0);
				for(int iBall=0; iBall<2; iBall++) {
					int jBall = ((MLDouble)mlFilSpringArray.getField("small_ballArrayIndex", iFil)).getReal(iBall).intValue()-1;
					pFil.small_ballArray[iBall] = ballArray.get(jBall);	
				}
				filSpringArray.add(pFil);
			}
			// stickSpringArray
			MLStructure mlStickSpringArray = (MLStructure)mlModel.getField("stickSpringArray");
			int NStick = mlStickSpringArray.getSize();
			stickSpringArray = new ArrayList<CStickSpring>(NStick);
			for(int iStick=0; iStick<NStick; iStick++) {
				CStickSpring pStick = new CStickSpring();
				// ballArray
				for(int iBall=0; iBall<2; iBall++) {
					int jBall = ((MLDouble)mlStickSpringArray.getField("ballArrayIndex", iStick)).getReal(iBall).intValue()-1;
					pStick.ballArray[iBall] = ballArray.get(jBall);
				}
				pStick.K 		= ((MLDouble)mlStickSpringArray.getField("K", iStick)).getReal(0);
				pStick.restLength = ((MLDouble)mlStickSpringArray.getField("restLength", iStick)).getReal(0);
				pStick.NSibling = mlStickSpringArray.getField("siblingArrayIndex", iStick).getSize();
				stickSpringArray.add(pStick);
			}
			// construct each stickSpring's siblingSpringArray
			for(int iStick=0; iStick<NStick; iStick++) {			
				CStickSpring pStick = stickSpringArray.get(iStick);
				pStick.siblingArray = new CStickSpring[3];
				for(int iSibling=0; iSibling < pStick.NSibling; iSibling++) {
					pStick.siblingArray[iSibling] =  stickSpringArray.get(((MLDouble)mlStickSpringArray.getField("siblingArrayIndex", iStick)).getReal(iSibling).intValue()-1);
				}
			}
			// === construct dependent/redundant arrays ===
			// each cell's stickCellArray
			for(CCell cell : cellArray) {
				ArrayList<CCell> stickCellArray = cell.StickCellArray();
				cell.stickCellArray = stickCellArray;
			}
			// each cell's springArray
			for(CRodSpring pRod : rodSpringArray) {
				pRod.ballArray[0].cell.springArray[0] = pRod;
			}
			// each cell's ballArray
//			for(CBall ball : ballArray) {
//				ball.cell.ballArray[ball.ballArrayIndex] = ball;
//			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	///////////////////
	// POV-Ray stuff //
	///////////////////
	public static void POV_Write(boolean plotIntermediate) {
		int NSave;
		if(plotIntermediate) 	NSave = ballArray.get(0).posSave.length;
		else					NSave = 0;
		plotIntermediate = false;		// Don't plot intermediate values for now, will revert later.
		
		//////////////////////////////
		
		double textscale;
		Vector3d pos1;
		Vector3d pos2;
		Vector3d LPOV;
		
		// Define scale
		if(Assistant.POVScale == 1) {
			textscale = 0.7;
			pos1 = new Vector3d(-13.0,15.0,0);
			pos2 = new Vector3d(-13.0,14.0,0);
			LPOV = new Vector3d(20,20,20);
			
		} else {
			textscale = 2.0;
			pos1 = new Vector3d(-40.0,45.0,0);
			pos2 = new Vector3d(-40.0,42.0,0);
			LPOV = new Vector3d(60,60,60);
		}
		
		// Make output folder if it doesn't exist already
		if(!(new File(name + "/output")).exists())	new File(name + "/output").mkdir();
		PrintWriter fid=null;
			for(int ii=0; ii<NSave+1; ii++) {
				try {
					if(ii!=NSave){
						plotIntermediate=true;		// If this is not the first iteration, do the intermediate plotting 
					} else {
						plotIntermediate=false;
					}
					String fileName = String.format("%s/output/pov_g%04dm%04d_%02d.pov", name, growthIter, movementIter, ii);
					// Remove inc file if it already exists
					if((new File(fileName)).exists()) new File(fileName).delete();
					// Write new inc file
					fid = new PrintWriter(new FileWriter(fileName,true));		// True is for append
					
					// Create camera, background and lighting based on L
					fid.println(String.format("#declare Lx = %f;",LPOV.x));
					fid.println(String.format("#declare Ly = %f;",LPOV.y));
					fid.println(String.format("#declare Lz = %f;\n",LPOV.z));
					fid.println("camera {\n" +
			    	"\tlocation <-1*Lx, 0.9*Ly,-1.0*Lz>\n" +
			    	"\tlook_at  <Lx/2, 0, Lz/2>\n" +
			        "\tangle 50\n" +
			    	"}\n");
					fid.println("background { color rgb <1, 1, 1> }\n");
					fid.println("light_source { < Lx/2,  10*Ly,  Lz/2> color rgb <1,1,1> }");		// Why 3x? OPTIMISE
					fid.println("light_source { < Lx/2,  10*Ly,  Lz/2> color rgb <1,1,1> }");
					fid.println("light_source { < Lx/2,  10*Ly,  Lz/2> color rgb <1,1,1> }\n");
					
					// Create plane
					fid.println("union {\n" +
					"\tbox {\n" +
					"\t\t<-Lx, 0, -Lz>,\t\t// Position: in the middle\n" +
				    "\t\t<2*Lx, 0, 2*Lz>\t\t// Size: 2*L\n" +
				    "\t\ttexture {\n" +
				    "\t\t\tpigment{\n" + 
				    "\t\t\t\tcolor rgb <0.2, 0.2, 0.2>\n" +
				    "\t\t\t}\n" +
				    "\t\t\tfinish {\n" +
				    "\t\t\t\tambient .2\n" +
				    "\t\t\t\tdiffuse .6\n" +
				    "\t\t\t}\n" +       
				    "\t\t}\n" +
				    "\t}\n");
					
					// Include text, calibrated for tomas_persp_3D_java.pov
					fid.println("text {\n" +           
					String.format("\tttf \"timrom.ttf\" \"Movement time: %05.2f s\" 0.05, 0.1*x\n",movementIter*movementTimeStepEnd+ii*movementTimeStep) +
				    "\tpigment {color rgb <0.000, 0.000, 0.000>  }\n" +     
			        String.format("\tscale <%f,%f,%f>\n", textscale, textscale, textscale) + 
			        String.format("\ttranslate <%f,%f,%f>\n", pos1.x, pos1.y, pos1.z) +  
			        "\trotate <15,45,0>\n" +
			        "\tno_shadow" +
					"}\n" + 
					"text {\n" +           
					String.format("\tttf \"timrom.ttf\" \"Growth time:     %05.1f h\" 0.05, 0.1*x\n",growthIter*growthTimeStep/3600.0) +		// divided by 3600 to go from [s] --> [h]
				    "\tpigment {color rgb <0.000, 0.000, 0.000>  }\n" +     
				    String.format("\tscale <%f,%f,%f>\n", textscale, textscale, textscale) + 
			        String.format("\ttranslate <%f,%f,%f>\n", pos2.x, pos2.y, pos2.z) +  
			        "\trotate <15,45,0>\n" +
			        "\tno_shadow" +
					"}\n");
					
					// Build spheres and rods
					for(int iCell=0; iCell<cellArray.size(); iCell++) {
						CCell cell = cellArray.get(iCell);
						fid.println("// Cell no. " + iCell);
						if(cell.type<2) {
							// Spherical cell
							CBall ball = cell.ballArray[0];

							fid.println("sphere\n" + 
									"{\n" + 
									(plotIntermediate ? 
										String.format("\t < %10.3f,%10.3f,%10.3f > \n", ball.posSave[ii].x*1e6, ball.posSave[ii].y*1e6, ball.posSave[ii].z*1e6) : 
										String.format("\t < %10.3f,%10.3f,%10.3f > \n", ball.pos.x*1e6, ball.pos.y*1e6, ball.pos.z*1e6)) +  
									String.format("\t%10.3f\n", ball.radius*1e6) +
									"\ttexture{\n" + 
									"\t\tpigment{\n" +
									String.format("\t\t\tcolor rgb<%10.3f,%10.3f,%10.3f>\n", cell.colour[0], cell.colour[1], cell.colour[2]) +
									"\t\t}\n" +
									"\t\tfinish{\n" +
									"\t\t\tambient .2\n" +
									"\t\t\tdiffuse .6\n" +
									"\t\t}\n" +
									"\t}\n" +
									"}\n");}
						else if(cell.type>1) {	// Rod
							CBall ball = cell.ballArray[0];
							CBall ballNext = cell.ballArray[1];

							fid.println("cylinder\n" +		// Sphere-sphere connection
									"{\n" +
									(plotIntermediate ?
										String.format("\t<%10.3f,%10.3f,%10.3f>,\n", ball.posSave[ii].x*1e6, ball.posSave[ii].y*1e6, ball.posSave[ii].z*1e6) +
										String.format("\t<%10.3f,%10.3f,%10.3f>,\n", ballNext.posSave[ii].x*1e6, ballNext.posSave[ii].y*1e6, ballNext.posSave[ii].z*1e6) :
									String.format("\t<%10.3f,%10.3f,%10.3f>,\n", ball.pos.x*1e6, ball.pos.y*1e6, ball.pos.z*1e6) +
									String.format("\t<%10.3f,%10.3f,%10.3f>,\n", ballNext.pos.x*1e6, ballNext.pos.y*1e6, ballNext.pos.z*1e6)) +
									String.format("\t%10.3f\n", ball.radius*1e6) +
									"\ttexture{\n" +
									"\t\tpigment{\n" +
									String.format("\t\t\tcolor rgb<%10.3f,%10.3f,%10.3f>\n", cell.colour[0], cell.colour[1], cell.colour[2]) +
									"\t\t}\n" +
									"\t\tfinish{\n" +
									"\t\t\tambient .2\n" +
									"\t\t\tdiffuse .6\n" +
									"\t\t}\n" +
									"\t}\n" +
									"}\n" +
									"sphere\n" +			// First sphere
									"{\n" +
									(plotIntermediate ?
										String.format("\t < %10.3f,%10.3f,%10.3f > \n", ball.posSave[ii].x*1e6, ball.posSave[ii].y*1e6, ball.posSave[ii].z*1e6) :
										String.format("\t < %10.3f,%10.3f,%10.3f > \n", ball.pos.x*1e6, ball.pos.y*1e6, ball.pos.z*1e6)) +
									String.format("\t%10.3f\n", ball.radius*1e6) +
									"\ttexture{\n" +
									"\t\tpigment{\n" +
									String.format("\t\t\tcolor rgb<%10.3f,%10.3f,%10.3f>\n", cell.colour[0], cell.colour[1], cell.colour[2]) +
									"\t\t}\n" +
									"\t\tfinish{\n" +
									"\t\t\tambient .2\n" +
									"\t\t\tdiffuse .6\n" +
									"\t\t}\n" +
									"\t}\n" +
									"}\n"+
									"sphere\n" +			// Second sphere
									"{\n" +
									(plotIntermediate ? 
										String.format("\t < %10.3f,%10.3f,%10.3f > \n", ballNext.posSave[ii].x*1e6, ballNext.posSave[ii].y*1e6, ballNext.posSave[ii].z*1e6) :
										String.format("\t < %10.3f,%10.3f,%10.3f > \n", ballNext.pos.x*1e6, ballNext.pos.y*1e6, ballNext.pos.z*1e6)) +
									String.format("\t%10.3f\n", ballNext.radius*1e6) +
									"\ttexture{\n" +
									"\t\tpigment{\n" +
									String.format("\t\t\tcolor rgb<%10.3f,%10.3f,%10.3f>\n", cell.colour[0], cell.colour[1], cell.colour[2]) +
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
						CFilSpring pFil = filSpringArray.get(iFil);
						for(int springType = 0; springType < 2; springType++) {
							fid.println("// Filament spring no. " + iFil);
							double[] colour = new double[3];
							CBall ball;
							CBall ballNext;
							if(springType==0) {		// Set specific things for small spring and big spring
								colour[0] = 0; colour[1] = 0; colour[2] = 1;		// Big spring is blue
								ball 	= pFil.big_ballArray[0];
								ballNext = pFil.big_ballArray[1];
							} else {
								colour[0] = 1; colour[1] = 0; colour[2] = 0;		// Small spring is red
								ball 	= pFil.small_ballArray[0];
								ballNext = pFil.small_ballArray[1];
							}

							fid.println("cylinder\n" +
									"{\n" +
									(plotIntermediate ? 
										String.format("\t<%10.3f,%10.3f,%10.3f>,\n", ball.posSave[ii].x*1e6, ball.posSave[ii].y*1e6, ball.posSave[ii].z*1e6) +
										String.format("\t<%10.3f,%10.3f,%10.3f>,\n", ballNext.posSave[ii].x*1e6, ballNext.posSave[ii].y*1e6, ballNext.posSave[ii].z*1e6) :
									String.format("\t<%10.3f,%10.3f,%10.3f>,\n", ball.pos.x*1e6, ball.pos.y*1e6, ball.pos.z*1e6) +
									String.format("\t<%10.3f,%10.3f,%10.3f>,\n", ballNext.pos.x*1e6, ballNext.pos.y*1e6, ballNext.pos.z*1e6)) +
									String.format("\t%10.3f\n", ball.radius*1e5) +
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
						CBall ball = pSpring.ballArray[0];
						CBall ballNext = pSpring.ballArray[1];

						fid.println("cylinder\n" +
								"{\n" +
								(plotIntermediate ?
									String.format("\t<%10.3f,%10.3f,%10.3f>,\n", ball.posSave[ii].x*1e6, ball.posSave[ii].y*1e6, ball.posSave[ii].z*1e6) +
									String.format("\t<%10.3f,%10.3f,%10.3f>,\n", ballNext.posSave[ii].x*1e6, ballNext.posSave[ii].y*1e6, ballNext.posSave[ii].z*1e6) : 
								String.format("\t<%10.3f,%10.3f,%10.3f>,\n", ball.pos.x*1e6, ball.pos.y*1e6, ball.pos.z*1e6) +
								String.format("\t<%10.3f,%10.3f,%10.3f>,\n", ballNext.pos.x*1e6, ballNext.pos.y*1e6, ballNext.pos.z*1e6)) +
								String.format("\t%10.3f\n", ball.radius*1e5) +									// 1e5 == 1/10 of the actual ball radius
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
						CBall ball = pSpring.ball;

						if (!pSpring.anchor.equals(ball.pos)) {		// else we get degenerate cylinders (i.e. height==0), POV doesn't like that
							fid.println("cylinder\n" +
									"{\n" +
									(plotIntermediate ?
										String.format("\t<%10.3f,%10.3f,%10.3f>,\n", ball.posSave[ii].x*1e6, ball.posSave[ii].y*1e6, ball.posSave[ii].z*1e6) :
										String.format("\t<%10.3f,%10.3f,%10.3f>,\n", ball.pos.x*1e6, ball.pos.y*1e6, ball.pos.z*1e6)) +
									String.format("\t<%10.3f,%10.3f,%10.3f>,\n", pSpring.anchor.x*1e6, pSpring.anchor.y*1e6, pSpring.anchor.z*1e6) +
									String.format("\t%10.3f\n", ball.radius*1e5) +	// 1e5 because it is a spring
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
					
					// Finalise the file
					fid.println("\ttranslate <0,0,0>\n" +
					"\trotate <0,0,0>\n");
					fid.println("}\n");						// Yes, we actually need this bracket
					
				} catch(IOException E) {
					E.printStackTrace();
				} finally {
					fid.close();
				}
			}
		
	}
	
	public static void POV_Plot(boolean plotIntermediate) throws Exception {
//		if(growthIter<16) return;
		if(!(new File(name + "/image")).exists()) {
			new File(name + "/image").mkdir();
		}
		int NIter;
		if(plotIntermediate)	NIter = 1+ballArray.get(0).posSave.length;
		else					NIter = 1;
		for(int ii=0; ii<NIter; ii++) {
			String imageName = String.format("pov_g%04dm%04d_%02d", growthIter, movementIter, ii);
			String povName = String.format("pov_g%04dm%04d_%02d.pov", growthIter, movementIter, ii);
			String input = "povray ./output/" + povName + " +W1024 +H768 +O../" + name + "/image/" + imageName + " +A -J";
			Interactor.executeCommand("cd " + name + " ; " + input + " ; rm ./output/" + povName + " ; cd ..", Assistant.waitForFinish,Assistant.echoCommand);
		}
	}
}

class EricsonObject {		// Used for collision detection multiple return
	Vector3d dP;
	double dist;
	double sc;
	double tc;
	Vector3d c1;
	Vector3d c2;
	
	EricsonObject(Vector3d dP, double dist, double sc) {
		this.dP = dP;
		this.dist = dist; 
		this.sc = sc;
	}
	
	EricsonObject(Vector3d dP, double dist, double sc, double tc, Vector3d c1, Vector3d c2) {
		this.dP = dP;
		this.dist = dist; 
		this.sc = sc;
		this.tc = tc;
		this.c1 = c1;
		this.c2 = c2;
	}
}