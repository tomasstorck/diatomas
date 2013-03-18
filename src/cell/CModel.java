package cell;

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

import random.rand;
import NR.Odeint;
import NR.Output;
import NR.StepperDopr853;
import NR.Vector;
import NR.feval;
import NR.Common;
import backbone.Assistant;

public class CModel implements Serializable {
	// Set serializable information
	private static final long serialVersionUID = 1L;
	// Model miscellaneous settings
	public String name = "default";
	public int simulation = 0;					// The simulation type: see Run
	public int randomSeed = 3;
	public boolean comsol = false;
	public int NXType = 6;
	// --> Sticking
	public boolean sticking = false;
	public boolean[][] stickType = new boolean[NXType][NXType];
	// --> Anchoring
	public boolean anchoring = false;
	// --> Filaments
	public boolean filament = false;
	public boolean[] filamentType = new boolean[NXType];
	public boolean sphereStraightFil = false;	// Make streptococci-like structures if true, otherwise staphylococci
	public boolean gravity = false;
	public boolean gravityZ = false;
	// --> Substratum
	public boolean normalForce = true;			// Use normal force to simulate cells colliding with substratum (at y=0)
	public boolean initialAtSubstratum = true;	// All initial balls are positioned at y(t=0) = ball.radius
	// Domain properties
	public Vector3d L 	= new Vector3d(2e-6, 2e-6, 2e-6);
	public double G		= -9.8;					// [m/s2], acceleration due to gravity
	public double rhoWater = 1000;				// [kg/m3], density of bulk liquid (water)
	public double rhoX	= 1010;					// [kg/m3], diatoma density
	public double MWX 	= 24.6e-3;				// [kg/mol], composition CH1.8O0.5N0.2
	// Initial cell properties
	public int[] typeInit = {4};
	public double[] nInit = {1e-15};
	public Vector3d[] directionInit = {new Vector3d(1.0, 0.0, 0.0)};
	public Vector3d[] position0Init = {new Vector3d(0.0, 0.0, 0.0)};
	public Vector3d[] position1Init = {new Vector3d(2e-6, 0.0, 0.0)};
		
	// Spring constants and drag coefficient
	public double Kd 	= 1e-13;				// drag force coefficient
	public double Kc 	= 1e-9;					// cell-cell collision
	public double Kw 	= 5e-10;				// wall(substratum)-cell spring
	public double Kr 	= 5e-11;				// internal cell spring
	public double KfSphere 	= 2e-11;			// filament spring for sphere-sphere filial links
	public double KfRod0 	= 2e-11;			// filament spring for rod-rod filial links, short spring
	public double KfRod1 	= 2e-11;			// filament spring for rod-rod filial links, long sprong
	public double Kan	= 1e-11;				// anchor
	public double Ks 	= 1e-11;				// sticking
	public double anchorStretchLim = 0.5e-6;	// Maximum tension for anchoring springs
	public double anchorFormLim = 0.1e-6;		// Multiplication factor for rest length to form anchors. Note that actual rest length is the distance between the two, which could be less
	public double stickStretchLim = 0.5e-6;		// Maximum tension for sticking springs
	public double stickFormLim = 0.1e-6; 		// Multiplication factor for rest length to form sticking springs. 
	public double filStretchLim = 0.5e-6;		// Maximum tension for sticking springs
	public double filLengthSphere = 1.1;		// How many times R2 the sphere filament's rest length is
	public double[] filLengthRod = {0.5, 1.7};	// How many times R2 the rod filament's [0] short and [1] long spring rest length is
	// Model biomass and growth properties
	public int NdComp = 5;						// d for dynamic compound (e.g. total Ac)
	public int NcComp = 8;						// c for concentration (or virtual compound, e.g. Ac-)
	public int NAcidDiss = 4; 					// Number of acid dissociation reactions
	public int NInitCell = 6;					// Initial number of cells
	public double[] radiusCellMax = new double[NXType];
	public double[] radiusCellMin = new double[NXType];
	public double[] lengthCellMax = new double[NXType];
	public double[] lengthCellMin = new double[NXType];
	public double[] nCellMax =	new double[NXType];
	public double[] nCellMin =	new double[NXType];
	public double[] muAvgSimple = {0.33, 0.33, 0.33, 0.33, 0.33, 0.33};	// [h-1] 0.33  == doubling every 20 minutes. Only used in GrowthSimple!
	public double[] muStDev = {0.25, 0.25, 0.25, 0.25, 0.25, 0.25};	// Standard deviation. Only used in GrowthSimple()!    
	public double syntrophyFactor = 1.0; 		// Accelerated growth if two cells of different types are stuck to each other
	// Attachment
	public double attachmentRate = 0.0;			// [h-1] Number of cells newly attached per hour
	public int attachCellType = 0;				// What cell type the new cell is 
	public int[] attachNotTo = new int[0];		// Which cell types newly attached cells can NOT attach to
	// Progress
	public double growthTime = 0.0;				// [s] Current time for the growth
	public double growthTimeStep = 600.0;		// [s] Time step for growth
	public int growthIter = 0;					// [-] Counter time iterations for growth
	public double relaxationTime = 0.0;			// [s] initial time for relaxation (for ODE solver)
	public double relaxationTimeStepdt = 0.2;	// [s] output time step  for relaxation
	public double relaxationTimeStep = 1.0;		// [s] time interval for relaxation (for ODE solver), 5*relaxationTimeStep by default
	public int relaxationIter = 0;				// [-] counter time iterations for relaxation
	public int relaxationIterSuccessiveMax = 0;	// [-] how many successive iterations we limit relaxation to 
	// Arrays
	public ArrayList<CCell> cellArray = new ArrayList<CCell>(NInitCell);
	public ArrayList<CBall> ballArray = new ArrayList<CBall>(2*NInitCell);
	public ArrayList<CRodSpring> rodSpringArray = new ArrayList<CRodSpring>(0);
	public ArrayList<CStickSpring> stickSpringArray = new ArrayList<CStickSpring>(0);
	public ArrayList<CFilSpring> filSpringArray = new ArrayList<CFilSpring>(0);
	public ArrayList<CAnchorSpring> anchorSpringArray = new ArrayList<CAnchorSpring>(0);
	// === SOLVER STUFF ===
	public double ODEbeta = 0.08;
	public double ODEalpha = 1.0/8.0-ODEbeta*0.2;
	// === COMSOL STUFF ===
	// Biomass, assuming Cmol and composition CH1.8O0.5N0.2 (i.e. MW = 24.6 g/mol)
	//							type 0					type 1					type 2					type 3					type 4					type 5
	// 							m. hungatei				m. hungatei				s. fumaroxidans			s. fumaroxidans			s. fumaroxidans			s. fumaroxidans
	public double[] SMX = {		7.6e-3/MWX,				7.6e-3/MWX,				2.6e-3/MWX,				2.6e-3/MWX,				2.6e-3/MWX,				2.6e-3/MWX};				// [Cmol X/mol reacted] Biomass yields per flux reaction. All types from Scholten 2000, grown in coculture on propionate
	public double[] K = {		1e-21, 					1e-21, 					1e-5, 					1e-5, 					1e-5, 					1e-5};						//
	public double[] qMax = {	0.05/(SMX[0]*86400), 	0.05/(SMX[0]*86400), 	0.204*MWX*1e3/86400,	0.204*MWX*1e3/86400,	0.204*MWX*1e3/86400,	0.204*MWX*1e3/86400};		// [mol (Cmol*s)-1] M.h. from Robinson 1984, assuming yield, growth on NaAc in coculture. S.f. from Scholten 2000;
	public String[] rateEquation = {
			Double.toString(qMax[0]) + "*(c3*d3^4)/(K0+c3*d3^4)",		// type==0
			Double.toString(qMax[1]) + "*(c3*d3^4)/(K1+c3*d3^4)",		// type==1
			Double.toString(qMax[2]) + "*c2/(K2+c2)",					// type==2
			Double.toString(qMax[3]) + "*c2/(K3+c2)",					// type==3
			Double.toString(qMax[4]) + "*c2/(K4+c2)",					// type==4
			Double.toString(qMax[5]) + "*c2/(K5+c2)"};					// type==5
			
	// 	 pH calculations
	//							HPro		CO2			HCO3-		HAc
	//							0,			1,			2,			3
	public double[] Ka = {		1.34e-5,	4.6e-7,		4.69e-11, 	1.61e-5};								// From Wikipedia 120811. CO2 and H2CO3 --> HCO3- + H+;
	public String[] pHEquation = {																			// pH calculations
			"c2+c4+2*c5+c7-c0",											// Has -2 charge 
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
 	public double[] BCConc = new double[]{
 								1.0,		0.0, 				0.0,				0.0,				0.0	};			// [mol m-3]. equivalent to 1 [kg HPro m-3], neglecting Pro concentration
	public double[] D = new double[]{	
								1.060e-9,	1.92e-9,			1.21e-9,			4.500e-9,			1.88e-9};		// [m2 s-1]. Diffusion mass transfer Cussler 2nd edition. Methane through Witherspoon 1965
	public double[][] SMdiffusion = {
							{	0.0,		-1.0,				0.0,				-4.0,				1.0				},		// XComp == 0 (small sphere)
							{	0.0,		-1.0,				0.0,				-4.0,				1.0				},		// XComp == 1 (big sphere)
							{	-1.0,		1.0,				1.0,				3.0,				0.0				},		// XComp == 2 (small rod, variable W)
							{	-1.0,		1.0,				1.0,				3.0,				0.0				},		// XComp == 3 (big rod, variable W)
							{	-1.0,		1.0,				1.0,				3.0,				0.0				},		// XComp == 4 (small rod, fixed W)
							{	-1.0,		1.0,				1.0,				3.0,				0.0				}};		// XComp == 5 (big rod, fixed W);

	//////////////////////////////////////////////////////////////////////////////////////////
	
	/////////////////////////////////////
	// Constructors and initialisation //
	/////////////////////////////////////
	public CModel() {}	// Default constructor, includes default values
	
	public void UpdateAmountCellMax() {	// Updates the nCellMax based on supplied radiusCellMax and lengthCellMax 
		for(int ii = 0; ii<2; ii++) {
			nCellMax[ii] 		= (4.0/3.0*Math.PI * Math.pow(radiusCellMax[ii],3))*rhoX/MWX; 
			nCellMin[ii] 		= 0.5 * nCellMax[ii];
		}
		for(int ii = 2; ii<6; ii++) {
			nCellMax[ii] = (4.0/3.0*Math.PI * Math.pow(radiusCellMax[ii],3) + Math.PI*Math.pow(radiusCellMax[ii],2)*lengthCellMax[ii])*rhoX/MWX;
			nCellMin[ii] = 0.5 * nCellMax[ii];
			if(ii<4) {
				radiusCellMin[ii] = CBall.Radius(nCellMin[ii], ii, this);
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
	
	public void Write(String message, String format) {
		Write(message,format,false,false);
	}
	
	//////////////////////////
	// Collision detection  //
	//////////////////////////
	public ArrayList<CCell> DetectFloorCollision(double touchFactor) {				// actual distance < dist*radius--> collision    
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
	
	public ArrayList<CCell> DetectCellCollision_Simple(double touchFactor) {			// Using ArrayList, no idea how big this one will get
		ArrayList<CCell> collisionCell = new ArrayList<CCell>();
		
		for(int iBall=0; iBall<ballArray.size(); iBall++) {						// If we stick to indexing, it'll be easier to determine which cells don't need to be analysed
			CBall ball = ballArray.get(iBall);
			for(int iBall2 = iBall+1; iBall2<ballArray.size(); iBall2++) {
				CBall ball2 = ballArray.get(iBall2);
				if(ball.cell.Index()!=ball2.cell.Index()) {
					Vector3d diff = ball2.pos.minus(ball.pos);
					if(Math.abs(diff.norm()) - touchFactor*(ball.radius+ball2.radius) < 0) {
						collisionCell.add(ball.cell);
						collisionCell.add(ball2.cell);
					}
				}
			}
		}
		return collisionCell;
	}
	
	public ArrayList<CCell> DetectCellCollision_Proper(double touchFactor) {
		ArrayList<CCell> collisionCell = new ArrayList<CCell>();
		int NCell = cellArray.size();
		for(int ii=0; ii<NCell; ii++) {
			CCell cell0 = cellArray.get(ii);
			for(int jj=ii+1; jj<NCell; jj++) {
				CCell cell1 = cellArray.get(jj);
				if(DetectCellCollision_Proper(cell0, cell1, touchFactor)){
					collisionCell.add(cell0);
					collisionCell.add(cell1);
				}
			}
		}
		return collisionCell;
	}
	
	public boolean DetectCellCollision_Proper(CCell cell0, CCell cell1, double touchFactor) {
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
			CCell rod;	CCell sphere;						// Initialise rod and sphere, should we need it later on (rod-sphere collision detection)
			double dist;
			if(cell0.type > 1 && cell1.type > 1) {			// Rod-rod
				H2 = 1.5*(touchFactor*( lengthCellMax[cell0.type] + lengthCellMax[cell1.type] + R2 ));		// Does not take stretching of the rod spring into account, but should do the trick still
				if(Math.abs(diff.x)<H2 && Math.abs(diff.z)<H2 && Math.abs(diff.y)<H2) {
					// Do good collision detection
					EricsonObject C = DetectLinesegLineseg(cell0.ballArray[0].pos, cell0.ballArray[1].pos, cell1.ballArray[0].pos, cell1.ballArray[1].pos);
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
					EricsonObject C = DetectLinesegPoint(rod.ballArray[0].pos, rod.ballArray[1].pos, sphere.ballArray[0].pos);
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
		
	public EricsonObject DetectLinesegLineseg(Vector3d p1, Vector3d q1, Vector3d p2, Vector3d q2) {		// This is line segment - line segment collision detection. 
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
		
		// If segments are not parallel, compute closest point on L1 to L2 and clamp to segment S1, otherwise pick arbitrary s (=0)
		double s;
		if(denom!=0.0) {
			s = Common.Clamp((b*f-c*e) /  denom, 0.0, 1.0);
		} else	s = 0.0;
		// Compute point on L2 closest to S1(s) using t = ((P1+D1*s) - P2).dot(D2) / D2.dot(D2) = (b*s + f) / e
		double t = (b*s + f) / e;
		
		// If t is in [0,1] (i.e. on S2) we're done. Else Clamp(t), recompute s for the new value of t using s = ((P2+D2*t) - P1).dot(D1) / D1.dot(D1) = (t*b - c) / a and clamp s to [0,1]
		if(t<0.0) {
			t = 0.0;
			s = Common.Clamp(-c/a, 0.0, 1.0);
		} else if (t>1.0) {
			t = 1.0;
			s = Common.Clamp((b-c)/a, 0.0, 1.0);
		}
		
		Vector3d c1 = p1.plus(d1.times(s));	// Collision point on S1
		Vector3d c2 = p2.plus(d2.times(t));	// Collision point on S2
		
		Vector3d dP = c1.minus(c2);  	// = S1(sc) - S2(tc)
		
		double dist2 = (c1.minus(c2)).dot(c1.minus(c2));
		
		return new EricsonObject(dP, Math.sqrt(dist2), s, t, c1, c2);
	}
	
	public EricsonObject DetectLineSegLine(Vector3d p1, Vector3d q1, Vector3d p2, Vector3d q2) {
		// Based on DetectLineSegLineSeg from Ericson
		// Computes closest points C1 and C2 of L1(s) = P1+s*(Q1-P1) and S2(t) = P2+t*(Q2-P2). s is unlimited, t is limited to [0, 1]
		Vector3d d1 = q1.minus(p1);		// Direction of S1
		Vector3d d2 = q2.minus(p2);		// Direction of S2
		Vector3d r = p1.minus(p2);
		double a = d1.dot(d1);			// Squared length of S1, >0
		double e = d2.dot(d2);			// Squared length of S2, >0
		double f = d2.dot(r);
		double c = d1.dot(r);
		double b = d1.dot(d2);
		double denom = a*e-b*b;			// Always >0
		
		// If segments are not parallel, compute closest point on L1 to L2 and clamp to segment S1, otherwise pick arbitrary s (=0)
		double s;
		if(denom!=0.0) {
			s = Common.Clamp((b*f-c*e) /  denom, 0.0, 1.0);
		} else	s = 0.0;
		// Compute point on L2 closest to S1(s) using t = ((P1+D1*s) - P2).dot(D2) / D2.dot(D2) = (b*s + f) / e
		double t = (b*s + f) / e;
		// t is unlimited, so no need to clamp --> we're done
		
		Vector3d c1 = p1.plus(d1.times(s));
		Vector3d c2 = p2.plus(d2.times(t));
		
		Vector3d dP = c1.minus(c2);  	// = S1(sc) - S2(tc)
		
		double dist2 = (c1.minus(c2)).dot(c1.minus(c2));
		
		return new EricsonObject(dP, Math.sqrt(dist2), s, t, c1, c2);
	}
	
	public EricsonObject DetectLinesegPoint(Vector3d p1, Vector3d q1, Vector3d p2) {
		Vector3d ab = q1.minus(p1);  	// line
		Vector3d w = p2.minus(p1);		//point-line
		//Project c onto ab, computing parameterized position d(t) = a + t*(b-a)
		double rpos = w.dot(ab)/ab.dot(ab);
		//if outside segment, clamp t and therefore d to the closest endpoint
		rpos = Common.Clamp(rpos, 0.0, 1.0);
		//compute projected position from the clamped t
		Vector3d d = p1.plus(ab.times(rpos));
		//calculate the vector p2 --> d
		Vector3d dP = d.minus(p2);
		EricsonObject R = new EricsonObject(dP, dP.norm(), rpos);	// Defined at the end of the model class. OPTIMISE: we don't need dP.norm() sometimes and could leave it out
		return R;
	}
	
	public EricsonObject DetectLinePoint(Vector3d p1, Vector3d q1, Vector3d p2) {
		Vector3d ab = q1.minus(p1);  	// vector from p1 to q1 (i.e. the line segment)
		Vector3d w = p2.minus(p1);		// vector from p1 to p2
		//Project c onto ab, computing parameterized position d(t) = a + t*(b-a). rpos can be >1.0, i.e. d can be longer than the line segment
		double rpos = w.dot(ab)/ab.dot(ab);
		// Do not clamp rpos to the range of the line segment, so our line segment becomes a line when projected onto it
		// Compute projected position of p1 --> p2 onto p1 --> q1
		Vector3d d = p1.plus(ab.times(rpos));
		//calculate the vector p2 --> d
		Vector3d dP = d.minus(p2);
		EricsonObject R = new EricsonObject(dP, dP.norm(), rpos);	// Defined at the end of the model class
		return R;
	}

	///////////////////////////////
	// Spring breakage detection //
	///////////////////////////////
	public ArrayList<CAnchorSpring> DetectAnchorBreak(double maxStretch) {
		ArrayList<CAnchorSpring> breakArray = new ArrayList<CAnchorSpring>();
		
		for(CAnchorSpring anchor : anchorSpringArray) {
			double al = (anchor.ballArray[0].pos.minus(anchor.anchorPoint)).norm();		// al = Actual Length
			if(al > maxStretch*anchor.restLength) {
				breakArray.add(anchor);
			}
		}
		return breakArray;
	}
	
	public ArrayList<CSpring> DetectStickBreak(double maxStretch) {
		ArrayList<CSpring> breakArray = new ArrayList<CSpring>();
		
		int iSpring = 0;
		while(iSpring < stickSpringArray.size()) {
			CSpring spring = stickSpringArray.get(iSpring);
			double al = (spring.ballArray[1].pos.minus(  spring.ballArray[0].pos)  ).norm();		// al = Actual Length
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
		for(CBall ball : ballArray) {
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
	public int Relaxation() throws Exception {
		// Reset counter
		Assistant.NAnchorBreak = Assistant.NAnchorForm = Assistant.NFilBreak = Assistant.NStickBreak = Assistant.NStickForm = 0;
		
		int ntimes = (int) (relaxationTimeStep/relaxationTimeStepdt);
		double atol = 1.0e-6, rtol = atol;
		double h1 = 0.00001, hmin = 0;
		double t1 = relaxationTime; 
		double t2 = t1 + relaxationTimeStep;
		Vector ystart = new Vector(6*ballArray.size(),0.0);

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
		feval dydt = new feval(this);
		Odeint<StepperDopr853> ode = new Odeint<StepperDopr853>(ystart, t1, t2, atol, rtol, h1, hmin, out, dydt, this);
		// Update alpha and beta
		ode.s.alpha = ODEalpha;
		ode.s.beta = ODEbeta;
		// Integrate to find solution
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
	
	public Vector CalculateForces(Vector yode) {	
		// Read data from y
		int ii=0; 				// TODO: Completely redundant? Check via StepperBase
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
		}
		// Collision forces
		for(int iCell=0; iCell<cellArray.size(); iCell++) {
			CCell cell0 = cellArray.get(iCell);
			CBall c0b0 = cell0.ballArray[0];
			// Base collision on the cell type
			if(cell0.type<2) {														// cell0 is a ball
				// Check for all remaining cells
				for(int jCell=iCell+1; jCell<cellArray.size(); jCell++) {
					CCell cell1 = cellArray.get(jCell);
					CBall c1b0 = cell1.ballArray[0];
					double R2 = c0b0.radius + c1b0.radius;
					Vector3d dirn = c0b0.pos.minus(c1b0.pos);
					if(cell1.type<2) {												// The other cell1 is a ball too
						double dist = dirn.norm();
						// do a simple collision detection if close enough
						double d = R2*1.01-dist;									// d is the magnitude of the overlap vector, as defined in the IbM paper
						if(d>0.0) {
							// We have a collision
							Vector3d Fs = dirn.normalise().times(Kc*d);
							// Add forces
							c0b0.force = c0b0.force.plus(Fs);
							c1b0.force = c1b0.force.minus(Fs);
						}
					} else if(cell1.type<6) {										// cell0 is a ball, cell1 is a rod
						double H2 = 1.5*(lengthCellMax[cell1.type] + R2);			// H2 is maximum allowed distance with still change to collide: R0 + R1 + 2*R1*aspect. 1.5 is to make it more robust (stretching)
						if(dirn.x<H2 && dirn.z<H2 && dirn.y<H2) {
							// do a sphere-rod collision detection
							CBall c1b1 = cell1.ballArray[1];
							EricsonObject C = DetectLinesegPoint(c1b0.pos, c1b1.pos, c0b0.pos);
							Vector3d dP = C.dP;
							double dist = C.dist;									// Make distance more accurate
							double sc = C.sc;
							// Collision detection
							double d = R2*1.01-dist;								// d is the magnitude of the overlap vector, as defined in the IbM paper
							if(d>0.0) {
								double f = Kc/dist*d;
								Vector3d Fs = dP.times(f);
								// Add these elastic forces to the cells
								// both balls in rod
								c1b0.force = c1b0.force.plus(Fs.times(1.0-sc)); 
								c1b1.force = c1b1.force.plus(Fs.times(sc));
								// ball in sphere
								c0b0.force = c0b0.force.minus(Fs);
							}	
						}
					} else {
						throw new RuntimeException("Unknown cell type");
					}
				}
			} else if (cell0.type<6) {												// cell0.type > 1
				CBall c0b1 = cell0.ballArray[1];
				for(int jCell = iCell+1; jCell<cellArray.size(); jCell++) {
					CCell cell1 = cellArray.get(jCell);
					CBall c1b0 = cell1.ballArray[0];
					double R2 = c0b0.radius + c1b0.radius;
					Vector3d dirn = c0b0.pos.minus(c1b0.pos);
					if(cell1.type<2) {												// cell0 is a rod, the cell1 is a ball
						double H2 = 1.5*(lengthCellMax[cell0.type] + R2);			// H2 is maximum allowed distance with still change to collide: R0 + R1 + 2*R1*aspect
						if(dirn.x<H2 && dirn.z<H2 && dirn.y<H2) {
							// do a rod-sphere collision detection
							EricsonObject C = DetectLinesegPoint(c0b0.pos, c0b1.pos, c1b0.pos); 
							Vector3d dP = C.dP;
							double dist = C.dist;
							double sc = C.sc;
							// Collision detection
							double d = R2*1.01-dist;								// d is the magnitude of the overlap vector, as defined in the IbM paper
							if(d>0.0) {
								double f = Kc/dist*d;
								Vector3d Fs = dP.times(f);
								// Add these elastic forces to the cells
								double sc1 = 1-sc;
								// both balls in rod
								c0b0.force = c0b0.force.plus(Fs.times(sc1));
								c0b1.force = c0b1.force.plus(Fs.times(sc));
								// ball in sphere
								c1b0.force = c1b0.force.minus(Fs);
							}	
						}
					} else if (cell1.type<6){										// type>1 --> the other cell is a rod too. This is where it gets tricky
						Vector3d c0b0pos = new Vector3d(c0b0.pos);
						Vector3d c0b1pos = new Vector3d(c0b1.pos);
						Vector3d c1b0pos = new Vector3d(c1b0.pos);
						CBall c1b1 = cell1.ballArray[1];
						Vector3d c1b1pos = new Vector3d(c1b1.pos);
						double H2 = 1.5*( lengthCellMax[cell0.type] + lengthCellMax[cell1.type] + R2 );		// aspect0*2*R0 + aspect1*2*R1 + R0 + R1
						if(dirn.x<H2 && dirn.z<H2 && dirn.y<H2) {
							// calculate the distance between the segments
							EricsonObject C = DetectLinesegLineseg(c0b0pos, c0b1pos, c1b0pos, c1b1pos);
							Vector3d dP = C.dP;					// dP is vector from closest point 2 --> 1
							double dist = C.dist;
							double sc = C.sc;
							double tc = C.tc;
							double d = R2*1.01-dist;								// d is the magnitude of the overlap vector, as defined in the IbM paper
							if(d>0.0) {
								double f = Kc/dist*d;
								Vector3d Fs = dP.times(f);
								// Add these elastic forces to the cells
								double sc1 = 1-sc;
								double tc1 = 1-tc;
								// both balls in 1st rod
								c0b0.force = c0b0.force.plus(Fs.times(sc1));
								c0b1.force = c0b1.force.plus(Fs.times(sc));
								// both balls in 1st rod
								c1b0.force = c1b0.force.minus(Fs.times(tc1));
								c1b1.force = c1b1.force.minus(Fs.times(tc));
							}
						}
					} else {
						throw new RuntimeException("Unknown cell type");
					}
				}
			} else {
				throw new RuntimeException("Unknown cell type");
			}
		}
		// Calculate gravity+bouyancy, normal forces and drag
		for(CBall ball : ballArray) {
			// Contact forces
			double y = ball.pos.y;
			double r = ball.radius;
			if(normalForce) {
				if(y<r){
					ball.force.y += Kw*(r-y);
				}
			}
			// Gravity and buoyancy
			if(gravity) {
				if(gravityZ) {
					ball.force.z += G * (rhoX-rhoWater) * ball.n*MWX/rhoX;
				} else if(y>r*1.1) {			// Only if not already at the floor plus a tiny bit 
					ball.force.y += G * (rhoX-rhoWater) * ball.n*MWX/rhoX;  //let the ball fall. Note that G is negative 
				}
			}
			
			// Velocity damping
			ball.force = ball.force.minus(ball.vel.times(Kd));			// TODO Should be v^2
		}
		
		// Elastic forces between springs within cells (CSpring in type>1)
		for(CRodSpring rod : rodSpringArray) {
			CBall ball0 = rod.ballArray[0];
			CBall ball1 = rod.ballArray[1];
			// find difference vector and distance dn between balls (euclidian distance) 
			Vector3d diff = ball1.pos.minus(ball0.pos);
			double dn = diff.norm();
			// Get force
			double f = rod.K/dn * (dn - rod.restLength);
			// Hooke's law
			Vector3d Fs = diff.times(f);
			// apply forces on balls
			ball0.force = ball0.force.plus(Fs);
			ball1.force = ball1.force.minus(Fs);
		}
		
		// Apply forces due to anchor springs
		for(CAnchorSpring anchor : anchorSpringArray) {
			Vector3d diff = anchor.anchorPoint.minus(anchor.ballArray[0].pos);
			double dn = diff.norm();
			// Get force
			double f = anchor.K/dn * (dn - anchor.restLength);
			// Hooke's law
			Vector3d Fs = diff.times(f);
			// apply forces on balls
			anchor.ballArray[0].force = anchor.ballArray[0].force.plus(Fs);

		}
		
		// Apply forces on sticking springs
		for(CStickSpring stick : stickSpringArray) {
			CBall ball0 = stick.ballArray[0];
			CBall ball1 = stick.ballArray[1];
			// find difference vector and distance dn between balls (euclidian distance) 
			Vector3d diff = ball1.pos.minus(ball0.pos);
			double dn = diff.norm();
			// Get force
			double f = stick.K/dn * (dn - stick.restLength);
			// Hooke's law
			Vector3d Fs = diff.times(f);
			// apply forces on balls
			ball0.force = ball0.force.plus(Fs);
			ball1.force = ball1.force.minus(Fs);
		}
		
		// Filament spring elastic force (CSpring in filSpringArray)
		for(CSpring fil : filSpringArray) {
			CBall ball0 = fil.ballArray[0];
			CBall ball1 = fil.ballArray[1];
			{// find difference vector and distance dn between balls (euclidian distance) 
			Vector3d diff = ball1.pos.minus(ball0.pos);
			double dn = diff.norm();
			// Get force
			double f = fil.K/dn * (dn - fil.restLength);
			// Hooke's law
			Vector3d Fs = diff.times(f);
			// apply forces on balls
			ball0.force = ball0.force.plus(Fs);
			ball1.force = ball1.force.minus(Fs);
			}
		}
		
		// Return results
		Vector dydx = new Vector(yode.size());
		int jj=0;
		for(CBall ball : ballArray) {
			double m = ball.n*MWX;	
			dydx.set(jj++,ball.vel.x);						// dpos/dt = v;
			dydx.set(jj++,ball.vel.y);
			dydx.set(jj++,ball.vel.z);
			dydx.set(jj++,ball.force.x/m);					// dvel/dt = a = f/M
			dydx.set(jj++,ball.force.y/m);
			dydx.set(jj++,ball.force.z/m);
		}
		return dydx;
	}
	
	public void FormBreak() {								// Breaks and forms sticking, filament springs when needed. Used during Relaxation()
		for(int ii=0; ii<cellArray.size(); ii++) {
			CCell cell0 = cellArray.get(ii);
			// Anchoring
			if(anchoring) {
				CBall ball0 = cell0.ballArray[0];
				CBall ball1 = (cell0.type>1) ? ball1 = cell0.ballArray[1] : null;

				if(cell0.anchorSpringArray.size()>0) { 		// This cell is already anchored
					ArrayList<CAnchorSpring> breakArray = new ArrayList<CAnchorSpring>();
					for(CAnchorSpring anchor : cell0.anchorSpringArray) {
						// Break anchor?
						Vector3d diff = anchor.anchorPoint.minus(anchor.ballArray[0].pos);
						double dn = diff.norm();
						if(dn > anchor.restLength+anchorStretchLim) {	// too much tension --> break the spring
							breakArray.add(anchor);
						}
					}
					for(CAnchorSpring anchor : breakArray)		Assistant.NAnchorBreak += anchor.Break();
				} else {									// Cell is not yet anchored
					// Form anchor?
					boolean formBall0 = (ball0.pos.y < anchorFormLim+ball0.radius) ? true : false;
					boolean formBall1 = false;
					if(cell0.type > 1) 	formBall1 = (ball1.pos.y < anchorFormLim+ball1.radius) ? true : false;			// If ball1 != null
					if(formBall0 || formBall1) {
						Assistant.NAnchorForm += cell0.Anchor();
					}
				}
			}
			// Sticking and filial links
			for(int jj=ii+1; jj<cellArray.size(); jj++) {	// Only check OTHER cells not already checked in a different order (i.e. factorial elimination)
				CCell cell1 = cellArray.get(jj);
				// Are these cells connected to each other, either through sticking spring or filament?
				boolean isStuck = false, isFilament = false;
				CSpring stickingSpring = null, filamentSpring = null; 
				for(CSpring fil : cell0.filSpringArray) {	// Will be empty if filaments are disabled --> no need to add further if statements 
					if(fil.ballArray[0].cell.equals(cell1) || fil.ballArray[1].cell.equals(cell1))  {		// We already know it is a filial spring with cell0
						// That's the one containing both cells
						isFilament = true;
						filamentSpring = fil;
						break;								// That is all we need: only one set of filial springs exists between two cells
					}
				}
				for(CSpring stick : cell0.stickSpringArray) { 
					if(stick.ballArray[0].cell.equals(cell1) || stick.ballArray[1].cell.equals(cell1)) {
						isStuck = true;
						stickingSpring = stick;				// Only one set of sticking springs exists between two cells
						break;						
					}
				}
				if(isFilament) {							// Can only be true if filaments are enabled 
					// Don't stick this. It shouldn't be stuck so don't check if we can break sticking springs. Instead, see if we can break the filial link 
					double distance = filamentSpring.ballArray[0].pos.minus(filamentSpring.ballArray[1].pos).norm();
					// Check if we can break this spring
					if(distance>filamentSpring.restLength+filStretchLim) {
						Assistant.NFilBreak += filamentSpring.Break();	// Also breaks its siblings
					}
				} else if (sticking){						// Check if we want to do sticking, or break the sticking spring
					// Determine current distance, required for formation and breaking
					CBall c0b0 = cell0.ballArray[0];
					CBall c1b0 = cell1.ballArray[0];				
					if(isStuck) {							// Stuck --> can we break this spring (and its siblings)?
						double dist = (c1b0.pos.minus(c0b0.pos)).norm();
						if(dist > stickingSpring.restLength+stickStretchLim) 		Assistant.NStickBreak += stickingSpring.Break();
					} else {								// Not stuck --> can we stick them? We have already checked if they are linked through filaments, not the case
						double R2 = c0b0.radius + c1b0.radius;
						Vector3d dirn = (c1b0.pos.minus(c0b0.pos));
						double dist;
						if(cell0.type<2 && cell1.type<2) {	// both spheres
							if(stickType[cell0.type][cell1.type]) { 
								dist = (c1b0.pos.minus(c0b0.pos)).norm();
							} else continue;
						} else if(cell0.type<2) {			// 1st sphere, 2nd rod
							double H2f =  1.5*(stickFormLim+(lengthCellMax[cell1.type] + R2));	// H2 is maximum allowed distance with still change to collide: R0 + R1 + 2*R1*aspect
							if(stickType[cell0.type][cell1.type] && dirn.x<H2f && dirn.z<H2f && dirn.y<H2f) {
								CBall c1b1 = cell1.ballArray[1];
								// do a sphere-rod collision detection
								EricsonObject C = DetectLinesegPoint(c1b0.pos, c1b1.pos, c0b0.pos);
								dist = C.dist;
							} else continue;
						} else if(cell1.type<2) {			// 2nd sphere, 1st rod
							double H2f = 1.5*(stickFormLim+(lengthCellMax[cell0.type] + R2));	// H2 is maximum allowed distance with still change to collide: R0 + R1 + 2*R1*aspect
							if(stickType[cell0.type][cell1.type] && dirn.x<H2f && dirn.z<H2f && dirn.y<H2f) {
								CBall c0b1 = cell0.ballArray[1];
								// do a sphere-rod collision detection
								EricsonObject C = DetectLinesegPoint(c0b0.pos, c0b1.pos, c1b0.pos);
								dist = C.dist;
							} else continue;
						} else if(cell0.type<6 && cell1.type<6) {  	// both rod
							double H2f = 1.5*(stickFormLim+(lengthCellMax[cell0.type] + lengthCellMax[cell1.type] + R2));
							if(stickType[cell0.type][cell1.type] && dirn.x<H2f && dirn.z<H2f && dirn.y<H2f) {
								CBall c0b1 = cell0.ballArray[1];
								CBall c1b1 = cell1.ballArray[1];
								// calculate the distance between the two segments
								EricsonObject C = DetectLinesegLineseg(c0b0.pos, c0b1.pos, c1b0.pos, c1b1.pos);
								dist = C.dist;
							} else continue;
						} else {
							throw new IndexOutOfBoundsException("Cell types: " + cell0.type + " and " + cell1.type);
						}
						// Stick if distance is small enough
						if(dist<R2+stickFormLim) 	Assistant.NStickForm += cell0.Stick(cell1);
					}
				}
			}
		}
	}
	
	//////////////////
	// Growth stuff //
	//////////////////
	public ArrayList<CCell> GrowthSimple() throws Exception {									// Growth based on a random number, further enhanced by being sticked to a cell of other type (==0 || !=0) 
		int NCell = cellArray.size();
		ArrayList<CCell> dividedCell = new ArrayList<CCell>(); 
		for(int iCell=0; iCell<NCell; iCell++){
			CCell mother = cellArray.get(iCell);
			double amount = mother.GetAmount();

			// Random growth, with syntrophy if required
			double mu = muAvgSimple[mother.type] + (muStDev[mother.type] * rand.Gaussian());	// Come up with a mu for this cell, this iteration
			double growthAcceleration = 1.0;
			for(CCell stickCell : mother.stickCellArray) {
				if(mother.type != stickCell.type) {
					// The cell types are different on the other end of the spring
					growthAcceleration *= syntrophyFactor;
					break;
				}
			}
			amount *= Math.exp(mu*growthAcceleration*growthTimeStep/3600.0);					// We need growthTimeStep s-1 --> h-1

			// Syntrophic growth for sticking cells
			mother.SetAmount(amount);
		}
		
		return dividedCell;
	}
	
	public ArrayList<CCell> GrowthFlux() throws Exception {
		int NCell = cellArray.size();
		ArrayList<CCell> dividedCell = new ArrayList<CCell>();
		for(int iCell=0; iCell<NCell; iCell++){
			CCell mother = cellArray.get(iCell);
			// Obtain mol increase based on flux
			double molIn = mother.q * mother.GetAmount() * growthTimeStep * SMX[mother.type];
			// Grow mother cell
			double newAmount = mother.GetAmount()+molIn;
			mother.SetAmount(newAmount);
		}
		return dividedCell;
	}
	
	public CCell DivideCell(CCell c0) {
		// Nomenclature: c0 == mother, c1 == daughter
		double n = c0.GetAmount();
		CCell c1;
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
			c1 = new CCell(c0.type,												// Same type as cell
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
				ArrayList<CCell> copyCellArray = new ArrayList<CCell>(cellArray);
				copyCellArray.remove(c0);		copyCellArray.remove(c1);
				boolean overlap = false;
				if(DetectCellCollision_Proper(c0, c1, 1.0))		overlap = true; 
				for(CCell cell : copyCellArray) {
					if(DetectCellCollision_Proper(c0, cell, 1.0) || DetectCellCollision_Proper(c1, cell, 1.0)) {
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
			c1.q = 					c0.q;
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
			CBall c0b0 = c0.ballArray[0];
			CBall c0b1 = c0.ballArray[1];
			// Determine displacement
			double radius = c0b0.radius;
			Vector3d middle = c0b1.pos.minus(c0b0.pos).divide(2.0);				// Vector from c0b0 --> halfway c0b1
			double L = middle.norm();
			Vector3d ball1Vector = middle.times((L-radius)/L);					// Vector from c0b0 --> new c0b1 position (halfway with radius subtracted)
			// Make a new, displaced cell
			c1 = new CCell(c0.type,												// Same type as cell
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
		for(CCell cell : c0.stickCellArray) {														// We want to check each other cell
			for(CStickSpring stick : c0.stickSpringArray) {												// And find the correct spring, attached to c0 and cell
				if(stick.ballArray[0].equals(c1) || stick.ballArray[1].equals(c1)) {				
					if(c1.GetDistance(cell) < c0.GetDistance(cell)) {								// If c1 is closer, move sticking spring to c1
						stick.Break();																// Break this spring and its siblings. OPTIMISE: We could restick it, but then we need to find the correct ball to Stick() to 
						c1.Stick(cell);
						break;
					} else {																		// If c0 is closer, just reset rest length of this spring and its siblings
						stick.ResetRestLength();
						for(CSpring sibling : stick.siblingArray)		sibling.ResetRestLength();		
					}
				}
				// Create new sticking spring
			}
		}
		// Done, return daughter cell
		return c1;
	}
	
	public void TransferFilament(CCell c0, CCell c1) {
		if(c0.filament) {
			if(c0.type < 2 && c1.type < 2) {
				if(sphereStraightFil) {											// Reorganise if we want straight fils, otherwise just attach resulting in random structures
					CBall motherBall0 = c0.ballArray[0];
					CBall daughterBall0 = c1.ballArray[0];
					ArrayList<CFilSpring> donateFilArray = new ArrayList<CFilSpring>();
					for(CFilSpring fil : c0.filSpringArray) {
						boolean found=false;
						if( fil.ballArray[0] == motherBall0) {					// Only replace half the balls for daughter's
							fil.ballArray[0] = daughterBall0;
							found = true;}
						if(found) {
							// Mark filament spring for donation from mother to daughter
							donateFilArray.add(fil);
						}
					}
					for(CFilSpring fil : donateFilArray) {
						c1.filSpringArray.add(fil);
						c0.filSpringArray.remove(fil);
						// Reset rest lengths. Spring constant won't change because it depends on cell type
						fil.ResetRestLength();
					}
				}
				new CFilSpring(c0.ballArray[0], c1.ballArray[0], 3);
			} else if(c0.type < 6 && c1.type < 6) {
				CBall c0b0 = c0.ballArray[0];
				CBall c0b1 = c0.ballArray[1];
				CBall c1b0 = c1.ballArray[0];
				CBall c1b1 = c1.ballArray[1];
				ArrayList<CFilSpring> donateFilArray = new ArrayList<CFilSpring>();
				for(CFilSpring fil : c0.filSpringArray) {
					boolean found=false;
					if( fil.type == 4 && fil.ballArray[0] == c0b1) {
						fil.ballArray[0] = 	c1b1;
						found = true;}
					if( fil.type == 4 && fil.ballArray[1] == c0b1) {
						fil.ballArray[1] = 	c1b1;
						found = true;}
					if( fil.type == 5 && fil.ballArray[0] == c0b0) {
						fil.ballArray[0] = 	c1b0;
						found = true;}
					if( fil.type == 5 && fil.ballArray[1] == c0b0) {
						fil.ballArray[1] = 	c1b0;
						found = true;}
					if(found) {
						// Mark filament spring for donation from mother to daughter
						donateFilArray.add(fil);
					}
				}
				for(CFilSpring fil : donateFilArray) {
					c1.filSpringArray.add(fil);
					c0.filSpringArray.remove(fil);
					// Reset rest lengths
					fil.ResetRestLength();
				}
				// Make new filial link between mother and daughter
				CFilSpring filSmall = 	new CFilSpring(c1.ballArray[0], c0.ballArray[1], 4);							// type==4 --> Small spring
				CFilSpring filBig = 	new CFilSpring(c1.ballArray[1], c0.ballArray[0], 5);							// type==5 --> Big spring
				filSmall.siblingArray.add(filBig);
				filBig.siblingArray.add(filSmall);
			}
		}
	}
	
	public void Attachment(int NNew) {
		for(int iA=0; iA<NNew; iA++) {
			// Define the cell we will attach
			final int typeNew = attachCellType; 
			final double nNew = nCellMin[typeNew] * (1.0 + rand.Double());
			final boolean filNew = filament && filamentType[typeNew];
			final double rNew = CBall.Radius(nNew, typeNew, this); 
			// Create array of balls in non-spherical cells 
			ArrayList<CBall> ballArrayRod = new ArrayList<CBall>(ballArray.size());
			for(CBall ball : ballArray) 	if(ball.cell.type>1) 	ballArrayRod.add(ball);
			// Find a random rod's ball position dest(ination) and move the ball there from dirn ("along the path") until we find a particle
			Vector3d firstPos = new Vector3d(0.0, 0.0, 0.0);
			// Create and position the new cell to this champion ball. Position it in the direction of dirn
			CCell newCell = new CCell(typeNew, nNew, firstPos, new Vector3d(), filNew, this);
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
				CBall firstBall = ballArray.get(0);
				double firstDist = 0.0;
				boolean success = false;
				
				// Find if the new cell can attach to a spherical cell
				for(CBall ball : ballArray) {
					EricsonObject E = DetectLinePoint(dest, dest.plus(dirn), ball.pos);			// Detect distance line-point, with line the path and point the ball.pos
					// Check if ball.pos is close enough to path to touch the attaching particle by analysing the distance obtained from Ericsson
					if( E.dist < rNew+ball.radius ) {
						// Good, if the attaching particle would be moved along path from dirn to dest it would collide with other 
						// Now check if it is the first ball that the newly attached cell would encounter, i.e. if the distance from dest is the largest yet 
						if(E.sc > firstDist) {													// sc is the multiplier for the vector that denotes the line to get the segment: since dirn.norm() == 1 we don't need to multiply with the length
							// Set this ball to be first to be encountered by the attaching particle
							firstDist = E.sc;
							firstBall = ball;
							firstPos = ball.pos.plus(dirn.times(rNew+ball.radius));		// Position where the new cell will attach after colliding
						}
					}
				}
				// After checking all balls, check all springs in the rods
				for(CRodSpring spring : rodSpringArray) {
					// Find the distance between the path of the particle (a line) and the rod spring (a line segment)
					CBall ball0 = spring.ballArray[0];
					CBall ball1 = spring.ballArray[1];
					EricsonObject E = DetectLineSegLine(ball0.pos, ball1.pos, dest, dest.plus(dirn));							// Detect distance line-point, with line the path and point the ball.pos
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
				if(!success)						continue;
				// Check if it is valid in case we have a substratum
				if(normalForce && firstPos.y<rNew)	continue;	// the new cell went through the plane to get to this point
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
					CCell cell = cellArray.get(iCell);
					if(DetectCellCollision_Proper(newCell, cell, 1.0))	continue tryloop;
				}
				// Congratulations!
				break;
			}
			// It will stick/anchor when needed during movement, so we're done
		}
	}
	
	////////////////////////////
	// Anchor and Stick stuff //
	////////////////////////////
	public void BreakStick(ArrayList<CStickSpring> breakArray) {
		for(CStickSpring spring : breakArray) {
			CCell cell0 = spring.ballArray[0].cell;
			CCell cell1 = spring.ballArray[1].cell;
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
	
	public int BuildAnchor(ArrayList<CCell> collisionArray) {
		// Make unique
		for(CAnchorSpring pSpring : anchorSpringArray) collisionArray.remove(pSpring.ballArray[0].cell);
		
		// Anchor the non-stuck, collided cells to the ground
		for(CCell cell : collisionArray) cell.Anchor();
		return anchorSpringArray.size();
	}
	
	public int BuildStick(ArrayList<CCell> collisionArray) {
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
	
	////////////
	// Saving //
	////////////
	public void Save() {		// Save as serialised file, later to be converted to .mat file
		FileOutputStream fos = null;
		GZIPOutputStream gz = null;
		ObjectOutputStream oos = null;
		try {
			fos = new FileOutputStream(String.format("%s/output/g%04dr%04d.ser", name, growthIter, relaxationIter));
			gz = new GZIPOutputStream(fos);
			oos = new ObjectOutputStream(gz);
			oos.writeObject(this);
			oos.close();
		} catch (IOException ex) {
			ex.printStackTrace();
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