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
import NR.Vector3d;
import NR.feval;
import backbone.Assistant;

public class CModel implements Serializable {
	// Set serializable information
	private static final long serialVersionUID = 1L;
	// Model properties
	public String name = "default";
	public int randomSeed = 1;
	public boolean withComsol = false;
	public boolean sticking = true;
	public boolean stickSphereSphere = true;
	public boolean stickSphereRod = true;
	public boolean stickRodRod = true;
	public boolean anchoring = false;
	public boolean filament = false;
	public boolean gravity = false;
	public boolean gravityZ = false;
	public boolean sphereStraightFil = false;	// Make streptococci-like structures if true, otherwise staphylococci
	public boolean normalForce = true;			// Use normal force to simulate cells colliding with substratum (at y=0)
	public boolean initialAtSubstratum = true;	// All initial balls are positioned at y(t=0) = ball.radius
	public double syntrophyFactor = 1.0; 	// Accelerated growth if two cells of different types are stuck to each other
	// Domain properties
	public double G		= -9.8;					// [m/s2], acceleration due to gravity
	public double rhoWater = 1000;				// [kg/m3], density of bulk liquid (water)
	public double rhoX	= 1010;					// [kg/m3], diatoma density
	public double MWX 	= 24.6e-3;				// [kg/mol], composition CH1.8O0.5N0.2
	public Vector3d L 	= new Vector3d(20e-6, 20e-6, 20e-6);	// [m], Dimensions of domain
	// Spring constants and drag ceoefficient
	// Fast simulations/poor gravity
	public double Kd 	= 1e-13;				// drag force coefficient
	public double Kc 	= 1e-9;					// cell-cell collision
	public double Kw 	= 5e-10;				// wall(substratum)-cell spring
	public double Kr 	= 5e-11;				// internal cell spring
	public double Kf 	= 2e-11;				// filament spring
	public double Kan	= 1e-11;				// anchor
	public double Ks 	= 1e-11;				// sticking
	public double stretchLimAnchor = 1.4;// Maximum tension and compression (1-this value) for anchoring springs
	public double formLimAnchor = 1.1;			// Multiplication factor for rest length to form anchors. Note that actual rest length is the distance between the two, which could be less
	public double stretchLimStick = 1.4;// Maximum tension and compression (1-this value) for sticking springs
	public double formLimStick = 1.1; 			// Multiplication factor for rest length to form sticking springs. 
	public double stretchLimFil = 1.6;	// Maximum tension and compression (1-this value) for sticking springs
	// Model biomass properties
	public int NXComp = 6;						// Types of biomass
	public int NdComp = 5;						// d for dynamic compound (e.g. total Ac)
	public int NcComp = 8;						// c for concentration (or virtual compound, e.g. Ac-)
	public int NAcidDiss = 4; 					// Number of acid dissociation reactions
	public int NInitCell = 6;					// Initial number of cells
	public int[] cellType = {1, 5};				// Cell types used by default
	public double[] cellRadiusMax = {0.125e-6,	0.5e-6, 	0.125e-6, 	0.375e-6, 	0.125e-6, 	0.375e-6};
	public double[] cellLengthMax = {0.0,		0.0,		1e-6,		1.5e-6,		1e-6,		1.5e-6};
	public double[] nCellMax =	new double[6];
	public double muAvgSimple = 0.33;			// Doubling every 20 minutes. Only used in GrowthSimple!
	// Progress
	public double growthTime = 0.0;				// [s] Current time for the growth
	public double growthTimeStep = 600.0;		// [s] Time step for growth
	public int growthIter = 0;					// [-] Counter time iterations for growth
	public double relaxationTime = 0.0;			// [s] initial time for relaxation (for ODE solver)
	public double relaxationTimeStep = 2e-2;		// [s] output time step  for relaxation
	public double relaxationTimeStepEnd = 10e-2;	// [s] time interval for relaxation (for ODE solver), 5*relaxationTimeStep by default
	public int relaxationIter = 0;				// [-] counter time iterations for relaxation
	// Arrays
	public ArrayList<CCell> cellArray = new ArrayList<CCell>(NInitCell);
	public ArrayList<CBall> ballArray = new ArrayList<CBall>(2*NInitCell);
	public ArrayList<CSpring> rodSpringArray = new ArrayList<CSpring>(0);
	public ArrayList<CSpring> stickSpringArray = new ArrayList<CSpring>(0);
	public ArrayList<CSpring> filSpringArray = new ArrayList<CSpring>(0);
	public ArrayList<CSpring> anchorSpringArray = new ArrayList<CSpring>(0);
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
	public CModel(String name) {	// Default constructor, includes default values
		this.name = name;
	}
	
	public void UpdateAmountCellMax() {	// Updates the nCellMax based on the supplied parameters 
		for(int ii = 0; ii<2; ii++) {
			nCellMax[ii] = (4.0/3.0*Math.PI * Math.pow(cellRadiusMax[ii],3))*rhoX/MWX; 
		}
		for(int ii = 2; ii<6; ii++) {
			nCellMax[ii] = (4.0/3.0*Math.PI * Math.pow(cellRadiusMax[ii],3) + Math.PI*Math.pow(cellRadiusMax[ii],2)*cellLengthMax[ii])*rhoX/MWX; 
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
				double R2 = cell0.ballArray[0].radius + cell1.ballArray[0].radius; 
				if(cell0.type < 2 && cell1.type < 2) {					// Ball-ball
					double dist = cell1.ballArray[0].pos.minus(cell0.ballArray[0].pos).norm();
					if(dist<R2*touchFactor) {
						collisionCell.add(cell0); 
						collisionCell.add(cell1);
					}
				} else {
					double H2;
					Vector3d diff = cell1.ballArray[0].pos.minus(cell0.ballArray[0].pos);;
					if(cell0.type > 1 && cell1.type > 1) {				// Rod-rod
						H2 = 1.5*(touchFactor*( cellLengthMax[cell0.type] + cellLengthMax[cell1.type] + R2 ));		// Does not take stretching of the rod spring into account, but should do the trick still  
					} else {											// Rod-ball
						CCell rod;
						if(cell0.type<2) {
							rod=cell1;
						} else {
							rod=cell0;
						}
						H2 = 1.5*(touchFactor*( cellLengthMax[rod.type] + R2 ));// H2 is maximum allowed distance with still change to collide: R0 + R1 + 2*R1*aspect
					}
					if(Math.abs(diff.x)<H2 && Math.abs(diff.z)<H2 && Math.abs(diff.y)<H2) {
						double dist = diff.norm();
						if(dist<R2*touchFactor)	{
							collisionCell.add(cell0);
							collisionCell.add(cell1);
						}
					}
				}
			}
		}
		return collisionCell;
	}
	/////////////////////////////////
	// Ericson collision detection //
	/////////////////////////////////
	
	private double Clamp(double n, double min, double max) {
		if(n<min)	return min;
		if(n>max) 	return max;
		return n;
	}
		
	// Collision detection rod-rod
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
	public EricsonObject DetectLinesegPoint(Vector3d p1, Vector3d q1, Vector3d p2) {
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
		EricsonObject R = new EricsonObject(dP, dP.norm(), rpos);	// Defined at the end of the model class
		return R;
	}
	
	///////////////////////////////
	// Spring breakage detection //
	///////////////////////////////
	public ArrayList<CSpring> DetectAnchorBreak(double maxStretch) {
		ArrayList<CSpring> breakArray = new ArrayList<CSpring>();
		
		for(CSpring anchor : anchorSpringArray) {
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

	//////////////////////
	// Relaxation stuff //
	//////////////////////
	public int Relaxation() throws Exception {
		// Reset counter
		Assistant.NAnchorBreak = Assistant.NAnchorForm = Assistant.NFilBreak = Assistant.NStickBreak = Assistant.NStickForm = 0;
		
		int nvar = 6*ballArray.size();
		int ntimes = (int) (relaxationTimeStepEnd/relaxationTimeStep);
		double atol = 1.0e-6, rtol = atol;
		double h1 = 0.00001, hmin = 0;
		double t1 = relaxationTime; 
		double t2 = t1 + relaxationTimeStepEnd;
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
		feval dydt = new feval(this);
		Odeint<StepperDopr853> ode = new Odeint<StepperDopr853>(ystart, t1, t2, atol, rtol, h1, hmin, out, dydt, this);
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
			if(cell0.type<2) {											// cell0 is a ball
				// Check for all remaining cells
				for(int jCell=iCell+1; jCell<cellArray.size(); jCell++) {
					CCell cell1 = cellArray.get(jCell);
					CBall c1b0 = cell1.ballArray[0];
					double R2 = c0b0.radius + c1b0.radius;
					Vector3d dirn = c0b0.pos.minus(c1b0.pos);
					if(cell1.type<2) {									// The other cell1 is a ball too
						double dist = dirn.norm();
						// do a simple collision detection if close enough
						if(dist<R2) {
							// We have a collision
							dirn.normalise();
							Vector3d Fs = dirn.times(Kc*(R2*1.01-dist));	// Add *1.01 to R2 to give an extra push at collisions (prevent asymptote at touching)
							// Add forces
							c0b0.force = c0b0.force.plus(Fs);
							c1b0.force = c1b0.force.minus(Fs);
						}
					} else {												// cell0 is a ball, cell1 is a rod
						double H2 = 1.5*(cellLengthMax[cell1.type] + R2);	// H2 is maximum allowed distance with still change to collide: R0 + R1 + 2*R1*aspect. 1.5 is to make it more robust (stretching)
						if(dirn.x<H2 && dirn.z<H2 && dirn.y<H2) {
							// do a sphere-rod collision detection
							CBall c1b1 = cell1.ballArray[1];
							EricsonObject C = DetectLinesegPoint(c1b0.pos, c1b1.pos, c0b0.pos);
							Vector3d dP = C.dP;
							double dist = C.dist;						// Make distance more accurate
							double sc = C.sc;
							// Collision detection
							if(dist<R2) {
								double f = Kc/dist*(dist-R2*1.01);
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
			} else {	// cell0.type > 1
				CBall c0b1 = cell0.ballArray[1];
				for(int jCell = iCell+1; jCell<cellArray.size(); jCell++) {
					CCell cell1 = cellArray.get(jCell);
					CBall c1b0 = cell1.ballArray[0];
					double R2 = c0b0.radius + c1b0.radius;
					Vector3d dirn = c0b0.pos.minus(c1b0.pos);
					if(cell1.type<2) {										// cell0 is a rod, the cell1 is a ball
						double H2 = 1.5*(cellLengthMax[cell0.type] + R2);	// H2 is maximum allowed distance with still change to collide: R0 + R1 + 2*R1*aspect
						if(dirn.x<H2 && dirn.z<H2 && dirn.y<H2) {
							// do a rod-sphere collision detection
							EricsonObject C = DetectLinesegPoint(c0b0.pos, c0b1.pos, c1b0.pos); 
							Vector3d dP = C.dP;
							double dist = C.dist;
							double sc = C.sc;
							// Collision detection
							if(dist < R2) {
								double f = Kc/dist*(dist-R2*1.01);
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
						double H2 = 1.5*( cellLengthMax[cell0.type] + cellLengthMax[cell1.type] + R2 );		// aspect0*2*R0 + aspect1*2*R1 + R0 + R1
						if(dirn.x<H2 && dirn.z<H2 && dirn.y<H2) {
							// calculate the distance between the two diatoma segments
							EricsonObject C = DetectLinesegLineseg(c0b0pos, c0b1pos, c1b0pos, c1b1pos);
							Vector3d dP = C.dP;					// dP is vector from closest point 2 --> 1
							double dist = C.dist;
							double sc = C.sc;
							double tc = C.tc;
							if(dist<R2) {
								double f = Kc/dist*(dist-R2*1.01);
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
			ball.force.subtract(ball.vel.times(Kd));			// TODO Should be v^2
		}
		
		// Elastic forces between springs within cells (CSpring in type>1)
		for(CSpring rod : rodSpringArray) {
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
			ball0.force.add(Fs);
			ball1.force.subtract(Fs);
		}
		
		// Apply forces due to anchor springs
		for(CSpring anchor : anchorSpringArray) {
			Vector3d diff = anchor.anchorPoint.minus(anchor.ballArray[0].pos);
			double dn = diff.norm();
			// Get force
			double f = anchor.K/dn * (dn - anchor.restLength);
			// Hooke's law
			Vector3d Fs = diff.times(f);
			// apply forces on balls
			anchor.ballArray[0].force.add(Fs);

		}
		
		// Apply forces on sticking springs
		for(CSpring stick : stickSpringArray) {
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
			ball0.force.add(Fs);
			ball1.force.subtract(Fs);
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
			ball0.force.add(Fs);
			ball1.force.subtract(Fs);
			}
		}
		
		// Return results
		Vector dydx = new Vector(yode.size());
		int ii=0;
		for(CBall ball : ballArray) {
				double m = ball.n*MWX;
				dydx.set(ii++,ball.vel.x);			// dpos/dt = v;
				dydx.set(ii++,ball.vel.y);
				dydx.set(ii++,ball.vel.z);
				dydx.set(ii++,ball.force.x/m);		// dvel/dt = a = f/M
				dydx.set(ii++,ball.force.y/m);
				dydx.set(ii++,ball.force.z/m);
		}
		return dydx;
	}
	
	public void FormBreak() {									// Breaks and forms sticking, filament springs when needed. Used during Relaxation()
		for(int ii=0; ii<cellArray.size(); ii++) {
			CCell cell0 = cellArray.get(ii);
			// Anchoring
			if(anchoring) {
				CBall ball0 = cell0.ballArray[0];
				CBall ball1 = (cell0.type>1) ? ball1 = cell0.ballArray[1] : null;

				if(cell0.anchorSpringArray.size()>0) { 		// This cell is already anchored
					ArrayList<CSpring> breakArray = new ArrayList<CSpring>();
					for(CSpring anchor : cell0.anchorSpringArray) {
						// Break anchor?
						Vector3d diff = anchor.anchorPoint.minus(anchor.ballArray[0].pos);
						double dn = diff.norm();
						if(dn > anchor.restLength*stretchLimAnchor) {			// too much tension --> break the spring
							breakArray.add(anchor);
						}
					}
					for(CSpring anchor : breakArray)		Assistant.NAnchorBreak += anchor.Break();
				} else {									// Cell is not yet anchored
					// Form anchor?
					boolean formBall0 = (ball0.pos.y < ball0.radius*formLimAnchor) ? true : false;
					boolean formBall1 = false;
					if(cell0.type > 1) 	formBall1 = (ball1.pos.y < ball1.radius*formLimAnchor) ? true : false;			// If ball1 != null
					if(formBall0 || formBall1) {
						Assistant.NAnchorForm += cell0.Anchor();					
					}
				}
			}
			// Sticking and filial links
			for(int jj=ii+1; jj<cellArray.size(); jj++) {		// Only check OTHER cells not already checked in a different order (i.e. factorial elimination)
				CCell cell1 = cellArray.get(jj);
				// Are these cells connected to each other, either through sticking spring or filament?
				boolean stuck = false, filament = false;
				CSpring stickingSpring = null, filamentSpring = null; 
				for(CSpring fil : cell0.filSpringArray) {		// Will be empty if filaments are disabled --> no need to add further if statements 
					if(fil.ballArray[0].cell.equals(cell1) || fil.ballArray[1].cell.equals(cell1))  {		// We already know it is a filial spring with cell0
						// That's the one containing both cells
						filament = true;
						filamentSpring = fil;
						break;									// That is all we need: only one set of filial springs exists between two cells
					}
				}
				for(CSpring stick : cell0.stickSpringArray) {	// Will be empty if sticking springs are disabled --> no need to add further if statements 
					if(stick.ballArray[0].cell.equals(cell1) || stick.ballArray[1].cell.equals(cell1)) {	// We already know it is stick to cell0
						stuck = true;
						stickingSpring = stick;					// That is all we need: only one set of sticking springs exists between two cells
						break;						
					}
				}
				if(filament) {									// Can only be true if filaments are enabled 
					// Don't stick this. It shouldn't be stuck so don't check if we can break sticking springs. Instead, see if we can break the filial link 
					double distance = filamentSpring.ballArray[0].pos.minus(filamentSpring.ballArray[1].pos).norm();
					// Check if we can break this spring
					if(distance>filamentSpring.restLength*stretchLimFil) {
						Assistant.NFilBreak += filamentSpring.Break();				// Also breaks its siblings
					}
				} else if (sticking){							// Check if we want to do sticking
					// Determine current distance, required for formation and breaking
					CBall c0b0 = cell0.ballArray[0];
					CBall c1b0 = cell1.ballArray[0];				
					if(stuck) {					// Stuck --> can we break this spring (and its siblings)?
						double dist = (c1b0.pos.minus(c0b0.pos)).norm();
						if(dist > stickingSpring.restLength*stretchLimStick) 		Assistant.NStickBreak += stickingSpring.Break();
					} else {					// Not stuck --> can we stick them? We have already checked if they are linked through filaments, not the case
						double R2 = c0b0.radius + c1b0.radius;
						Vector3d dirn = (c1b0.pos.minus(c0b0.pos));
						double dist;
						if(cell0.type<2 && cell1.type<2) {			// both spheres
							if(stickSphereSphere) { 
								dist = (c1b0.pos.minus(c0b0.pos)).norm();
							} else continue;
						} else if(cell0.type<2) {					// 1st sphere, 2nd rod
							double H2f =  1.5*(formLimStick*(cellLengthMax[cell1.type] + R2));	// H2 is maximum allowed distance with still change to collide: R0 + R1 + 2*R1*aspect
							if(stickSphereRod && dirn.x<H2f && dirn.z<H2f && dirn.y<H2f) {
								CBall c1b1 = cell1.ballArray[1];
								// do a sphere-rod collision detection
								EricsonObject C = DetectLinesegPoint(c1b0.pos, c1b1.pos, c0b0.pos);
								dist = C.dist;
							} else continue;
						} else if(cell1.type<2) {					// 2nd sphere, 1st rod
							double H2f = 1.5*(formLimStick*(cellLengthMax[cell0.type] + R2));	// H2 is maximum allowed distance with still change to collide: R0 + R1 + 2*R1*aspect
							if(stickSphereRod && dirn.x<H2f && dirn.z<H2f && dirn.y<H2f) {
								CBall c0b1 = cell0.ballArray[1];
								// do a sphere-rod collision detection
								EricsonObject C = DetectLinesegPoint(c0b0.pos, c0b1.pos, c1b0.pos);
								dist = C.dist;
							} else continue;
						} else if(cell0.type<6 && cell1.type<6) {  	// both rod
							double H2f = 1.5*(formLimStick*(cellLengthMax[cell0.type] + cellLengthMax[cell1.type] + R2));
							if(stickRodRod && dirn.x<H2f && dirn.z<H2f && dirn.y<H2f) {
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
						if(dist<R2*formLimStick) 	Assistant.NStickForm += cell0.Stick(cell1);
					}
				}
			}
		}
	}
	
	//////////////////
	// Growth stuff //
	//////////////////
	public int GrowthSimple() throws Exception {						// Growth based on a random number, further enhanced by being sticked to a cell of other type (==0 || !=0) 
		int NCell = cellArray.size();
		int newCell = 0;
		for(int iCell=0; iCell<NCell; iCell++){
			CCell mother = cellArray.get(iCell);
			double amount = mother.GetAmount();

			// Random growth, with syntrophy if required
			double mu = muAvgSimple + (rand.Double()-0.25)/5.0;					// Come up with a mu for this cell, this iteration, between 0.95 and 1.15
			double syntrophyAcceleration = 1.0;
			for(CCell stickCell : mother.stickCellArray) {
				if(mother.type != stickCell.type) {
					// The cell types are different on the other end of the spring
					syntrophyAcceleration *= syntrophyFactor;
					break;
				}
			}
			amount *= Math.exp(mu*syntrophyAcceleration*growthTimeStep/3600.0);							// We need growthTimeStep s-1 --> h-1

			// Syntrophic growth for sticking cells
			mother.SetAmount(amount);
			
			// Cell growth or division
			if(mother.GetAmount()>nCellMax[mother.type]) {
				newCell++;
				GrowCell(mother);
			}	
		}
		return newCell;
	}
	
	public int GrowthFlux() throws Exception {
		int newCell=0;
		int NCell = cellArray.size();
		for(int iCell=0; iCell<NCell; iCell++){
			CCell cell = cellArray.get(iCell);
			// Obtain mol increase based on flux
			double molIn = cell.q * cell.GetAmount() * growthTimeStep * SMX[cell.type];
			// Grow mother cell
			double newAmount = cell.GetAmount()+molIn;
			cell.SetAmount(newAmount);
			// divide mother cell if ready 
			if(newAmount>nCellMax[cell.type]) {
				newCell++;
				GrowCell(cell);
			}
		}
		return newCell;
	}
	
	public CCell GrowCell(CCell c0) {
		// Nomenclature: c0 == mother, c1 == daughter
		double n = c0.GetAmount();
		CCell c1;
		if(c0.type<2) {
			// Set amount for both cells
			c0.SetAmount(0.5*n);
			// Come up with a nice direction in which to place the new cell
			Vector3d direction = new Vector3d(rand.Double()-0.5,rand.Double()-0.5,rand.Double()-0.5);			
			direction.normalise();
			double displacement = c0.ballArray[0].radius;						// Displacement is done for both balls --> total of 1.0*radius displacement
			// Make a new, displaced cell
			c1 = new CCell(c0.type,											// Same type as cell
					c0.GetAmount(),
					c0.ballArray[0].pos.x - displacement * direction.x,				// The new location is the old one plus some displacement					
					c0.ballArray[0].pos.y - displacement * direction.y,	
					c0.ballArray[0].pos.z - displacement * direction.z,
					c0.filament,
					c0.colour,
					this);														// Same filament boolean as cell and pointer to the model
			// Set properties for new cell
			c1.ballArray[0].vel = 	new Vector3d(c0.ballArray[0].vel);
			c1.ballArray[0].force = new Vector3d(c0.ballArray[0].force);
			c1.colour =				c0.colour;							// copy of reference
			c1.mother = 			c0;
			c1.q = 					c0.q;
			// Displace old cell
			c0.ballArray[0].pos = c0.ballArray[0].pos.plus(  direction.times( displacement )  );
			// Contain cells to y dimension of domain
			if(c0.ballArray[0].pos.y < c0.ballArray[0].radius) 		{c0.ballArray[0].pos.y 	= c0.ballArray[0].radius;};
			if(c1.ballArray[0].pos.y < c1.ballArray[0].radius)  	{c1.ballArray[0].pos.y 	= c1.ballArray[0].radius;};
			// Set filament springs
			if(c1.filament) {
				if(sphereStraightFil) {								// Reorganise if we want straight fils, otherwise just attach resulting in random structures
					CBall motherBall0 = c0.ballArray[0];
					CBall daughterBall0 = c1.ballArray[0];
					ArrayList<CSpring> donateFilArray = new ArrayList<CSpring>();
					for(CSpring fil : c0.filSpringArray) {
						boolean found=false;
						if( fil.ballArray[0] == motherBall0) {		// Only replace half the balls for daughter's
							fil.ballArray[0] = daughterBall0;
							found = true;}
						if(found) {
							// Mark filament spring for donation from mother to daughter
							donateFilArray.add(fil);
						}
					}
					for(CSpring fil : donateFilArray) {
						c1.filSpringArray.add(fil);
						c0.filSpringArray.remove(fil);
						// Reset rest lengths. Spring constant won't change because it depends on cell type
						fil.ResetRestLength();
					}
				}
				new CSpring(c0.ballArray[0], c1.ballArray[0], 2);
			}
		} else if (c0.type<6) {
			///////
			// Original cell:
			// (00)============(01)
			// New cells:
			// (00)==(01)(10)==(11)
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
					c0.colour,
					this);														// Same filament boolean as cell and pointer to the model
			// Displace old cell, 2nd ball (1st ball stays in place)
			c0b1.pos = c0b0.pos.plus(ball1Vector);
			c0b1.pos.y *= 1.01;													// Required to prevent deadlock! 
			c0.rodSpringArray.get(0).ResetRestLength();
			// Contain cells to y dimension of domain
			for(int iBall=0; iBall<2; iBall++) {
				if(c0.ballArray[iBall].pos.y 	< c0.ballArray[iBall].radius) 	{c0.ballArray[0].pos.y 	= c0.ballArray[0].radius;};
				if(c1.ballArray[iBall].pos.y 	< c1.ballArray[iBall].radius) 	{c1.ballArray[0].pos.y 	= c1.ballArray[0].radius;};
			}
			// Set properties for new cell
			for(int iBall=0; iBall<2; iBall++) {
				c1.ballArray[iBall].vel = 	new Vector3d(c0.ballArray[iBall].vel);
				c1.ballArray[iBall].force = new Vector3d(c0.ballArray[iBall].force);
			}
			c1.colour =	c0.colour;
			c1.mother = c0;
			c1.motherIndex = c0.Index();
			c1.rodSpringArray.get(0).restLength = c0.rodSpringArray.get(0).restLength;

			// Set filament springs
			if(c1.filament) {
				CBall c1b0 = c1.ballArray[0];
				CBall c1b1 = c1.ballArray[1];
				ArrayList<CSpring> donateFilArray = new ArrayList<CSpring>();
				for(CSpring fil : c0.filSpringArray) {
					boolean found=false;
					if( fil.type == 2 && fil.ballArray[0] == c0b1) {
						fil.ballArray[0] = 	c1b1;
						found = true;}
					if( fil.type == 2 && fil.ballArray[1] == c0b1) {
						fil.ballArray[1] = 	c1b1;
						found = true;}
					if( fil.type == 3 && fil.ballArray[0] == c0b0) {
						fil.ballArray[0] = 	c1b0;
						found = true;}
					if( fil.type == 3 && fil.ballArray[1] == c0b0) {
						fil.ballArray[1] = 	c1b0;
						found = true;}
					if(found) {
						// Mark filament spring for donation from mother to daughter
						donateFilArray.add(fil);
					}
				}
				for(CSpring fil : donateFilArray) {
					c1.filSpringArray.add(fil);
					c0.filSpringArray.remove(fil);
					// Reset rest lengths
					fil.ResetRestLength();
				}
				// Make new filial link between mother and daughter
				CSpring filSmall = new CSpring(c1.ballArray[0], c0.ballArray[1], 2);		// type==2 --> Small spring
				CSpring filBig = new CSpring(c1.ballArray[1], c0.ballArray[0], 3, new CSpring[]{filSmall});		// type==3 --> Big spring
				filSmall.siblingArray.add(filBig);
			}
		} else {
			throw new IndexOutOfBoundsException("Cell type: " + c0.type);
		}
		return c1;
	}
	
	////////////////////////////
	// Anchor and Stick stuff //
	////////////////////////////
	public void BreakStick(ArrayList<CSpring> breakArray) {
		for(CSpring spring : breakArray) {
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
		for(CSpring pSpring : anchorSpringArray) collisionArray.remove(pSpring.ballArray[0].cell);
		
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
			for(CSpring pSpring : stickSpringArray) {		// This one should update automatically after something new has been stuck --> Only new ones are stuck AND, as a result, only uniques are sticked 
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
			fos = new FileOutputStream(String.format("%s/output/g%04dm%04d.ser", name, growthIter, relaxationIter));
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