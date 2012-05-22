package cell;

// Import Java stuff
import java.io.File;
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
	double Ki;
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
	double movementTime;
	double movementTimeStep;
	double movementTimeStepEnd;
	int movementIter;
	// Counters
	static int NBall;
	// Arrays
	ArrayList<CCell> cellArray;
	ArrayList<CBall> ballArray;
	ArrayList<CSpring> rodSpringArray;
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
		randomSeed = 1;
		// Spring constants
		Ki 		= 0.5e-2;			// internal cell spring
		Kf 		= 0.1e-4;			// filament spring
		Kw 		= 0.5e-5;			// wall spring
		Kc 		= 0.1e-4;			// collision
		Ks 		= 0.5e-5;			// sticking
		Ka 		= 0.5e-5;			// anchor
		Kd 		= 0.1e-7;			// drag force coefficient
		// Domain properties
		G		= -9.8;				// [m/s2], acceleration due to gravity
		rho_w	= 1000;				// [kg/m3], density of bulk liquid (water)
		rho_m	= 1100;				// [kg/m3], diatoma density
		L 		= new Vector3d(1200e-6, 300e-6, 1200e-6);	// [m], Dimensions of domain
		// Cell properties
		NType 	= 2;				// Types of cell
		NInitCell = 15;				// Initial number of cells
		aspect	= 2;				// Aspect ratio of cells
		// Ball properties
		MCellInit = 1e-11;			// kg
		MCellMax = 2e-11; 			// max mass of cells before division
		// Progress
		growthTime = 0;				// [h] Current time for the growth
		growthTimeStep = 1.0;		// [h] Time step for growth
		growthIter = 0;				// [-] Counter time iterations for growth
		movementTime = 0;			// [s] initial time for movement (for ODE solver)
		movementTimeStep = 2e-2;	// [s] output time step  for movement
		movementTimeStepEnd	= 10e-2;// [s] time interval for movement (for ODE solver), 5*movementTimeStep by default
		movementIter = 0;			// [-] counter time iterations for movement
		// Counters
		NBall 	= 0;
		// Arrays
		ballArray = new ArrayList<CBall>(2*NInitCell);
		cellArray = new ArrayList<CCell>(NInitCell);
		rodSpringArray = new ArrayList<CSpring>(NInitCell);
		stickSpringArray = new ArrayList<CStickSpring>(NInitCell);
		filSpringArray = new ArrayList<CFilSpring>(NInitCell);
		anchorSpringArray = new ArrayList<CAnchorSpring>(NInitCell);
	}
	
	///////////////////////
	// Get and Set stuff //
	///////////////////////
//	public CBall[] BallArray() {
//		CBall[] ballArray = new CBall[NBall];
//		int iBall = 0;
//		for(int iCell=0; iCell < cellArray.size(); iCell++) {
//			CCell cell = cellArray.get(iCell);
//			int NBallInCell = (cell.type==0) ? 1 : 2;
//			for(int iBallInCell=0; iBallInCell < NBallInCell; iBallInCell++) {
//				ballArray[iBall] = cell.ballArray[iBallInCell];
//				iBall++;
//			}
//		}
//		return ballArray;
//	}
	
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
				if(!(new File(name + "/output")).exists()) {
					new File(name + "/output").mkdir();
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
		for(CCell cell : cellArray) {
			int NBall = (cell.type==0) ? 1 : 2;	// Figure out number of balls based on type
			for(int iBall=0; iBall<NBall; iBall++) {
				CBall ball = cell.ballArray[iBall];
				if(ball.pos.y - ball.radius < 0) {
					collisionCell.add(cell);
					break;
				}
			}
		}
		return collisionCell;
	}
	
	public ArrayList<CCell> DetectCellCollision_Simple() {				// Using ArrayList, no idea how big this one will get
		ArrayList<CCell> collisionCell = new ArrayList<CCell>();
		
		for(int iBall=0; iBall<NBall; iBall++) {						// If we stick to indexing, it'll be easier to determine which cells don't need to be analysed
			CBall ball = ballArray.get(iBall);
			for(int iBall2 = iBall+1; iBall2<NBall; iBall2++) {
				CBall ball2 = ballArray.get(iBall2);
				if(ball.cell.arrayIndex!=ball2.cell.arrayIndex) {
					Vector3d diff = ball2.pos.minus(ball.pos);
					if(Math.abs(diff.length()) - ball.radius - ball2.radius < 0) {
						collisionCell.add(ball.cell);
						collisionCell.add(ball2.cell);
					}
				}
			}
		}
		return collisionCell;
	}
	
	public returnObject DetectCellCollision_Ericson(Vector3d S1_P0, Vector3d S1_P1, Vector3d S2_P0, Vector3d S2_P1) {
	    Vector3d   u = S1_P1.minus(S1_P0);		// d1(Ericson)
	    Vector3d   v = S2_P1.minus(S2_P0);		// d2(Ericson)
	    Vector3d   w = S1_P0.minus(S2_P0);		// r (Ericson)

	    double    a = u.dot(u);			// always >= 0	Called a(Ericson)
	    double    b = u.dot(v);			//				Called b(Ericson)
	    double    c = v.dot(v);			// always >= 0	Called e(Ericson)
	    double    d = u.dot(w);			//				Called c(Ericson)
	    double    e = v.dot(w);			//				Called f(Ericson)
	    
		double    D = a*c - b*b;		// always >= 0	Called denom(Ericson)

		Vector3d c1, c2;
		double sc, tc;
		double n, tnom;

		if (D != 0.0){
			n = (b*e - d*c)/D;
			if (n<0.0)
				sc = 0.0;
			else if (n>1.0)
				sc = 1.0;
			else sc = n;
		} else sc = 0.0; 			//Arbitrary sc

		tc = (b*sc + e)/c;
		tnom = b*sc + e;

		if (tnom < 0.0){
			tc = 0.0;
			
			n = -d/a;
			if (n<0.0)
				sc = 0.0;
			else if (n>1.0)
				sc = 1.0;
			else sc = n;

		} else if (tnom > c) {
			tc = 1.0;

			n = b - d;
			if (n<0.0)
				sc = 0.0;
			else if (n>1.0)
				sc = 1.0;
			else sc = n;

		} else tc = tnom/c;

		// Get the difference of the two closest points
		Vector3d   dP = w.plus(u.times(sc)).minus(v.times(tc));  // = S1(sc) - S2(tc)

		c1 = S1_P0.plus(u.times(sc));	// NOT NECESSARY... Just to check!!
		c2 = S2_P0.plus(v.times(tc)); 	// NOT NECESSARY... Just to check!!

		returnObject R = new returnObject(dP, dP.length(), sc, tc, c1, c2);
		return R;
	}
	
	public returnObject DetectCellCollision_Ericson(Vector3d S1_P0, Vector3d S1_P1, Vector3d S2_P0) {
		Vector3d ab = S1_P1.minus(S1_P0);  	// line
		Vector3d w = S2_P0.minus(S1_P0);	//point-line
		//Project c onto ab, computing parameterized position d(t) = a + t*(b-a)
		double rpos = w.dot(ab)/ab.dot(ab);
		//if outside segment, clamp t and therefore d to the closest endpoint
		if ( rpos<0.0f ) rpos = 0.0f;
		if ( rpos>1.0f ) rpos = 1.0f;
		//compute projected position from the clamped t
		Vector3d d = S1_P0.plus(ab.times(rpos));
		//calculate the vector c --> d
		Vector3d dP = d.minus(S2_P0);
		returnObject R = new returnObject(dP, dP.length(), rpos);	// Defined at the end of the model class
		return R;
	}
	
	///////////////////////////////
	// Spring breakage detection //
	///////////////////////////////
	public ArrayList<CAnchorSpring> DetectAnchorBreak() {
		double maxStretch = 1.2; 
		double minStretch = 0.8;
		ArrayList<CAnchorSpring> breakArray = new ArrayList<CAnchorSpring>();
		
		for(CAnchorSpring pSpring : anchorSpringArray) {
			double al = (pSpring.ball.pos.minus(pSpring.anchor)).length();		// al = Actual Length
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
			iSpring += pSpring.NSibling+1;
		}
		return breakArray;
	} 

	////////////////////
	// Movement stuff //
	////////////////////
	public int Movement() throws Exception {
		int nvar = 6*CModel.NBall;
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
		feval dydt = new feval(this);
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
	
	public Vector CalculateForces(double t, Vector yode) {	// This function gets called again and again --> not very efficient to import/export every time TODO
		// Read data from y
		int ii=0; 				// Where we are in yode
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
		double R2, H2;			// rest length, check distance 
		for(int iCell=0; iCell<cellArray.size(); iCell++) {
			CCell cell = cellArray.get(iCell);
			CBall ball = cell.ballArray[0];
			// Base collision on the cell type
			if(cell.type==0) {
				// Check for all remaining cells
				for(int jCell=iCell+1; jCell<cellArray.size(); jCell++) {
					Vector3d S1_P0 = new Vector3d(ball.pos);
					CCell cellNext = cellArray.get(jCell);
					// Check for a sticking spring, if there is one let it do the work
					if(!cell.stickCellArray.equals(cellNext)) {
						CBall ballNext = cellNext.ballArray[0];
						R2 = ball.radius + ballNext.radius;
						H2 = aspect * R2 + R2;
						if(cellNext.type==0) {		// The other cell is a ball too
							// do a simple collision detection
							Vector3d S2_P0 = new Vector3d(ballNext.pos);
							Vector3d dirn = S1_P0.minus(S2_P0);
							double rpos = dirn.length();
							if(rpos<R2) {
								// We have a collision
								dirn.normalise();
								rpos = R2-rpos;
								dirn = dirn.times(Kc*rpos);
								// Add forces
								ball.force.plus(dirn);
								ballNext.force.minus(dirn);
							}
						} else {					// type != 0 --> the other cell is a rod
							// do a sphere-rod collision detection
							CBall ballNext2 = cellNext.ballArray[1];
							returnObject R = DetectCellCollision_Ericson(ballNext.pos, ballNext2.pos, ball.pos);
							Vector3d dP = R.dP;
							double dist = R.dist;
							double sc = R.sc;
							
							if(dist<R2) {	// Collision
								// don't stick, done during growth
								double f = Kc/dist*(dist-R2);
								double Fsx = f*dP.x;
								double Fsy = f*dP.y;
								double Fsz = f*dP.z;
								// Add these elastic forces to the cells
								double sc1 = 1-sc;
								// both balls in rod
								ballNext.force.x -= sc1*Fsx;
								ballNext.force.y -= sc1*Fsy;
								ballNext.force.z -= sc1*Fsz;
								ballNext2.force.x -= sc*Fsx;
								ballNext2.force.y -= sc*Fsy;
								ballNext2.force.z -= sc*Fsz;
								// ball in sphere
								ball.force.x += Fsx;
								ball.force.y += Fsy;
								ball.force.z += Fsz;
							}
						}
					}
				}
			} else {	// cell.type != 0
				CBall ball2 = cell.ballArray[1];
				for(int jCell = iCell+1; jCell<cellArray.size(); jCell++) {
					CCell cellNext = cellArray.get(jCell);
					// check for sticking spring and let it do the work later on if it's there
					if(!cell.stickCellArray.equals(cellNext)) {
						CBall ballNext = cellNext.ballArray[0];
						R2 = ball.radius + ballNext.radius; 
						if(cellNext.type==0) {
							// do a sphere-rod collision detection
							returnObject R = DetectCellCollision_Ericson(ball.pos, ball2.pos, ballNext.pos); 
							Vector3d dP = R.dP;
							double dist = R.dist;
							double sc = R.sc;
							
							if(dist < R2) {
								double f = Kc/dist*(dist-R2);
								double Fsx = f*dP.x;
								double Fsy = f*dP.y;
								double Fsz = f*dP.z;
								// Add these elastic forces to the cells
								double sc1 = 1-sc;
								// both balls in rod
								ball.force.x -= sc1*Fsx;
								ball.force.y -= sc1*Fsy;
								ball.force.z -= sc1*Fsz;
								ball2.force.x -= sc*Fsx;
								ball2.force.y -= sc*Fsy;
								ball2.force.z -= sc*Fsz;
								// ball in sphere
								ballNext.force.x += Fsx;
								ballNext.force.y += Fsy;
								ballNext.force.z += Fsz;
							}
						} else {	// type != 0 --> the other cell is a rod. This is where it gets tricky
							Vector3d S1_P0 = new Vector3d(ball.pos);
							Vector3d S1_P1 = new Vector3d(ball2.pos);
							CBall ballNext2 = cellNext.ballArray[1];
							Vector3d S2_P0 = new Vector3d(ballNext.pos);
							Vector3d S2_P1 = new Vector3d(ballNext2.pos);
							H2 = aspect*R2*3;
							if(Math.abs(S2_P0.x - S1_P0.x) < H2 && Math.abs(S2_P0.y - S1_P0.y) < H2 && Math.abs(S2_P0.z - S1_P0.z) < H2) {
								// calculate the distance between the two diatoma segments
								returnObject R = DetectCellCollision_Ericson(S1_P0, S1_P1, S2_P0, S2_P1);
								Vector3d dP = R.dP;
								double dist = R.dist;
								double sc = R.sc;
								double tc = R.tc;
								
								if(dist<R2 && !cell.IsFilament(cellNext)) {
									double f = Kc/dist*(dist-R2);
									double Fsx = f*dP.x;
									double Fsy = f*dP.y;
									double Fsz = f*dP.z;
									// Add these elastic forces to the cells
									double sc1 = 1-sc;
									double tc1 = 1-tc;
									// both balls in 1st rod
									ball.force.x -= sc1*Fsx;
									ball.force.y -= sc1*Fsy;
									ball.force.z -= sc1*Fsz;
									ball2.force.x -= sc*Fsx;
									ball2.force.y -= sc*Fsy;
									ball2.force.z -= sc*Fsz;
									// both balls in 1st rod
									ballNext.force.x += tc1*Fsx;
									ballNext.force.y += tc1*Fsy;
									ballNext.force.z += tc1*Fsz;
									ballNext2.force.x += tc*Fsx;
									ballNext2.force.y += tc*Fsy;
									ballNext2.force.z += tc*Fsz;
								}
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
				ball.force.y += Kw*(r-y);
			}
//			// Gravity and buoyancy
//			if(y>r) {			// Only if not already at the floor 
//				ball.force.y += G * ((rho_m-rho_w)/rho_w) * ball.mass ;  //let the ball fall 
//			}
			// Velocity damping
			ball.force.x -= Kd*ball.vel.x;
			ball.force.y -= Kd*ball.vel.y;
			ball.force.z -= Kd*ball.vel.z;
		}
		
		// Elastic forces between springs within cells (CSpring in type>0)
		for(CSpring pSpring : rodSpringArray) {
			// find difference vector and distance dn between balls (euclidian distance) 
			Vector3d diff = pSpring.ballArray[1].pos.minus(pSpring.ballArray[0].pos);
			double dn = diff.length();
			// Get force
			double f = pSpring.K/dn * (dn - pSpring.restLength);
			// Hooke's law
			double Fsx = f*diff.x;
			double Fsy = f*diff.y;
			double Fsz = f*diff.z;
			// apply forces on balls
			pSpring.ballArray[0].force.x += Fsx;
			pSpring.ballArray[0].force.y += Fsy;
			pSpring.ballArray[0].force.z += Fsz;
			pSpring.ballArray[1].force.x -= Fsx;
			pSpring.ballArray[1].force.y -= Fsy;
			pSpring.ballArray[1].force.z -= Fsz;
		}
		
		// Sticking springs elastic forces (CStickSpring in stickSpringArray)
		for(CStickSpring pStick : stickSpringArray) {
			// find difference vector and distance dn between balls (euclidian distance) 
			Vector3d diff = pStick.ballArray[1].pos.minus(pStick.ballArray[0].pos);
			double dn = diff.length();
			// Get force
			double f = pStick.K/dn * (dn - pStick.restLength);
			// Hooke's law
			double Fsx = f*diff.x;
			double Fsy = f*diff.y;
			double Fsz = f*diff.z;
			// apply forces on balls
			pStick.ballArray[0].force.x += Fsx;
			pStick.ballArray[0].force.y += Fsy;
			pStick.ballArray[0].force.z += Fsz;
			pStick.ballArray[1].force.x -= Fsx;
			pStick.ballArray[1].force.y -= Fsy;
			pStick.ballArray[1].force.z -= Fsz;
		}
		
		// Filament spring elastic force (CFilSpring in filSpringArray)
		for(CFilSpring pFil : filSpringArray) {
			// === big spring ===
			{// find difference vector and distance dn between balls (euclidian distance) 
			Vector3d diff = pFil.big_ballArray[1].pos.minus(pFil.big_ballArray[0].pos);
			double dn = diff.length();
			// Get force
			double f = pFil.big_K/dn * (dn - pFil.big_restLength);
			// Hooke's law
			double Fsx = f*diff.x;
			double Fsy = f*diff.y;
			double Fsz = f*diff.z;
			// apply forces on balls
			pFil.big_ballArray[0].force.x += Fsx;
			pFil.big_ballArray[0].force.y += Fsy;
			pFil.big_ballArray[0].force.z += Fsz;
			pFil.big_ballArray[1].force.x -= Fsx;
			pFil.big_ballArray[1].force.y -= Fsy;
			pFil.big_ballArray[1].force.z -= Fsz;}
			// === small spring ===
			{// find difference vector and distance dn between balls (euclidian distance) 
			Vector3d diff = pFil.small_ballArray[1].pos.minus(pFil.small_ballArray[0].pos);
			double dn = diff.length();
			// Get force
			double f = pFil.small_K/dn * (dn - pFil.small_restLength);
			// Hooke's law
			double Fsx = f*diff.x;
			double Fsy = f*diff.y;
			double Fsz = f*diff.z;
			// apply forces on balls
			pFil.small_ballArray[0].force.x += Fsx;
			pFil.small_ballArray[0].force.y += Fsy;
			pFil.small_ballArray[0].force.z += Fsz;
			pFil.small_ballArray[1].force.x -= Fsx;
			pFil.small_ballArray[1].force.y -= Fsy;
			pFil.small_ballArray[1].force.z -= Fsz;}
		}
		
		// Return results
		Vector dydx = new Vector(yode.size());
		ii=0;
		for(CBall ball : ballArray) {
				double M = ball.mass;
				dydx.set(ii++,ball.vel.x);			// dpos/dt = v;
				dydx.set(ii++,ball.vel.y);
				dydx.set(ii++,ball.vel.z);
				dydx.set(ii++,ball.force.x/M);		// dvel/dt = a = f/M
				dydx.set(ii++,ball.force.y/M);
				dydx.set(ii++,ball.force.z/M);
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
			CCell pMother = cellArray.get(iCell);
			double mass = pMother.GetMass();

			// Random growth
			mass *= (0.95+rand.Double()/5.0);
			// Syntrophic growth
			for(CCell pStickCell : pMother.stickCellArray) {
				if((pMother.type==0 && pStickCell.type!=0) || (pMother.type!=0 && pStickCell.type==0)) {
					// The cell types are different on the other end of the spring
					mass *= 1.2;
					break;
				}
			}
			
			// Cell growth or division
			if(mass>MCellMax) {
				newCell++;
				if(pMother.type==0) {
					// Come up with a nice direction in which to place the new cell
					Vector3d direction = new Vector3d(rand.Double(),rand.Double(),rand.Double());			// TODO make the displacement go into any direction			
					direction.normalise();
					double displacement = pMother.ballArray[0].radius;
					// Make a new, displaced cell
					CCell pDaughter = new CCell(0,															// Same type as cell
							pMother.ballArray[0].pos.x - displacement * direction.x,						// The new location is the old one plus some displacement					
							pMother.ballArray[0].pos.y - displacement * direction.y,	
							pMother.ballArray[0].pos.z - displacement * direction.z,
							pMother.filament,this);														// Same filament boolean as cell and pointer to the model
					// Set mass for both cells
					pDaughter.SetMass(mass/2.0);		// Radius is updated in this method
					pMother.SetMass(mass/2.0);
					// Set properties for new cell
					pDaughter.ballArray[0].vel = 	new Vector3d(pMother.ballArray[0].vel);
					pDaughter.ballArray[0].force = 	new Vector3d(pMother.ballArray[0].force);
					pDaughter.colour =				pMother.colour;
					pDaughter.mother = 				pMother;
					// Displace old cell
					pMother.ballArray[0].pos.plus(  direction.times( displacement )  );
					// Contain cells to y dimension of domain
					if(pMother.ballArray[0].pos.y 	< pMother.ballArray[0].radius) 		{pMother.ballArray[0].pos.y 	= pMother.ballArray[0].radius;};
					if(pDaughter.ballArray[0].pos.y < pDaughter.ballArray[0].radius)  	{pDaughter.ballArray[0].pos.y 	= pDaughter.ballArray[0].radius;};
					// Set filament springs
					if(pDaughter.filament) {
						pDaughter.Stick(pMother);		// but why? TODO
					}
				} else {
					CBall pMotherBall0 = pMother.ballArray[0];
					CBall pMotherBall1 = pMother.ballArray[1];
					// Direction
					Vector3d direction = pMotherBall1.pos.minus( pMotherBall0.pos );
					direction.normalise();
					// Displacement
					double displacement; 																		// Should be improved/made to make sense (TODO)
					if(pMother.type==1) {
						displacement = pMotherBall0.radius*Math.pow(2.0,-0.666666);								// A very strange formula: compare our radius to the C++ equation for Rpos and you'll see it's the same
					} else {
						displacement = pMotherBall1.radius/2.0;
					}
					// Make a new, displaced cell
					Vector3d middle = pMotherBall1.pos.plus(pMotherBall0.pos).divide(2.0); 
					CCell pDaughter = new CCell(pMother.type,													// Same type as cell
							middle.x+	  displacement*direction.x,												// First ball. First ball and second ball were swapped in MATLAB and possibly C++					
							middle.y+1.01*displacement*direction.y,												// possible TODO, ought to be displaced slightly in original C++ code but is displaced significantly this way (change 1.01 to 2.01)
							middle.z+	  displacement*direction.z,
							pMotherBall1.pos.x,																	// Second ball
							pMotherBall1.pos.y,
							pMotherBall1.pos.z,
							pMother.filament,this);																// Same filament boolean as cell and pointer to the model
					// Set mass for both cells
					pDaughter.SetMass(mass/2.0);
					pMother.SetMass(mass/2.0);
					// Displace old cell, 2nd ball
					pMotherBall1.pos = middle.minus(direction.times(displacement));
					pMother.springArray[0].ResetRestLength();
					// Contain cells to y dimension of domain
					for(int iBall=0; iBall<2; iBall++) {
						if(pMother.ballArray[iBall].pos.y 		< pMother.ballArray[iBall].radius) 		{pMother.ballArray[0].pos.y 	= pMother.ballArray[0].radius;};
						if( pDaughter.ballArray[iBall].pos.y 	< pDaughter.ballArray[iBall].radius) 	{pDaughter.ballArray[0].pos.y 	= pDaughter.ballArray[0].radius;};
					}
					// Set properties for new cell
					for(int iBall=0; iBall<2; iBall++) {
						pDaughter.ballArray[iBall].vel = 	new Vector3d(pMother.ballArray[iBall].vel);
						pDaughter.ballArray[iBall].force = 	new Vector3d(pMother.ballArray[iBall].force);
					}
					pDaughter.colour =	pMother.colour;
					pDaughter.mother = 	pMother;
					pDaughter.springArray[0].restLength = pMother.springArray[0].restLength;

					// Set filament springs
					if(pDaughter.filament) {
						for(CFilSpring pFil : filSpringArray) {
							if( pFil.big_ballArray[0]== pMotherBall0) {
								pFil.big_ballArray[0] = pDaughter.ballArray[0];}
							if( pFil.big_ballArray[1]== pMotherBall0) {
								pFil.big_ballArray[1] = pDaughter.ballArray[0];}
							if( pFil.small_ballArray[0]== pMotherBall1) {
								pFil.small_ballArray[0] = pDaughter.ballArray[1];}
							if( pFil.small_ballArray[1]== pMotherBall1) {
								pFil.small_ballArray[1] = pDaughter.ballArray[1];}
						}
						new CFilSpring(pMother,pDaughter);
					}
				}

			} else {		
				// Simply increase mass and reset spring
				pMother.SetMass(mass);
				if(pMother.type>0) pMother.springArray[0].ResetRestLength();
			}
		}
		return newCell;
	}
	
	////////////////////////////
	// Anchor and Stick stuff //
	////////////////////////////
	public void BreakStick(ArrayList<CStickSpring> breakArray) {
		for(CStickSpring pSpring : breakArray) {
			CCell cell0 = pSpring.ballArray[0].cell;
			CCell cell1 = pSpring.ballArray[1].cell;
			// Remove cells from each others' stickCellArray 
			cell0.stickCellArray.remove(cell1);
			cell1.stickCellArray.remove(cell0);
			// Remove springs from model stickSpringArray
			stickSpringArray.remove(pSpring);
			stickSpringArray.remove(pSpring.siblingArray[0]);
			stickSpringArray.remove(pSpring.siblingArray[1]);
			stickSpringArray.remove(pSpring.siblingArray[2]);
		}
	}
	
	public int BuildAnchor(ArrayList<CCell> collisionArray) {
		// Make unique
		for(CAnchorSpring pSpring : anchorSpringArray) collisionArray.remove(pSpring.ball.cell);
		
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
	
	////////////////////////////
	// Saving, loading things //
	////////////////////////////
	public void Save() {
		MLStructure mlModel = new MLStructure("model", new int[] {1,1});
		mlModel.setField("aspect", 				new MLDouble(null, new double[] {aspect}, 1));
		mlModel.setField("G", 					new MLDouble(null, new double[] {G}, 1));
		mlModel.setField("growthIter", 			new MLDouble(null, new double[] {growthIter}, 1));
		mlModel.setField("growthTime",			new MLDouble(null, new double[] {growthTime}, 1));
		mlModel.setField("growthTimeStep",		new MLDouble(null, new double[] {growthTimeStep}, 1));
		mlModel.setField("K1",					new MLDouble(null, new double[] {Ki}, 1));
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
		mlModel.setField("movementTimeEnd",		new MLDouble(null, new double[] {movementTimeStepEnd}, 1));
		mlModel.setField("movementTimeStep",	new MLDouble(null, new double[] {movementTimeStep}, 1));
		mlModel.setField("name",				new MLChar(null, new String[] {name}, name.length()));
		mlModel.setField("NBall",				new MLDouble(null, new double[] {NBall}, 1));
		mlModel.setField("NInitCell",			new MLDouble(null, new double[] {NInitCell}, 1));
		mlModel.setField("NType",				new MLDouble(null, new double[] {NType}, 1));
		mlModel.setField("randomSeed",			new MLDouble(null, new double[] {randomSeed}, 1));
		mlModel.setField("rho_m",				new MLDouble(null, new double[] {rho_m}, 1));
		mlModel.setField("rho_w",				new MLDouble(null, new double[] {rho_w}, 1));
		
		// anchorSpringArray
		int NAnchor = anchorSpringArray.size();
		MLStructure mlAnchorSpringArray = new MLStructure(null, new int[] {NAnchor,1});
		for(int iAnchor=0; iAnchor<NAnchor; iAnchor++) {
			CAnchorSpring pAnchor = anchorSpringArray.get(iAnchor);
			mlAnchorSpringArray.setField("anchor", 			new MLDouble(null, new double[] {pAnchor.anchor.x, pAnchor.anchor. y, pAnchor.anchor.z}, 3),iAnchor);
			mlAnchorSpringArray.setField("ballArrayIndex", 	new MLDouble(null, new double[] {pAnchor.ball.arrayIndex+1}, 1),iAnchor);		// +1 for 0 vs 1 based indexing in Java vs MATLAB
			mlAnchorSpringArray.setField("K",				new MLDouble(null, new double[] {pAnchor.K}, 1),iAnchor);
			mlAnchorSpringArray.setField("restLength",		new MLDouble(null, new double[] {pAnchor.restLength}, 1),iAnchor);
			mlAnchorSpringArray.setField("arrayIndex", new MLDouble(null, new double[] {anchorSpringArray.indexOf(pAnchor)+1}, 1),iAnchor);
			if(pAnchor.ball.cell.type!=0) {
				mlAnchorSpringArray.setField("siblingArrayIndex", new MLDouble(null, new double[] {anchorSpringArray.indexOf(pAnchor.siblingArray[0])+1}, 1),iAnchor);
			} else {
				mlAnchorSpringArray.setField("siblingArrayIndex", new MLDouble(null, new double[] {}, 1),iAnchor);
			}
		}
		mlModel.setField("anchorSpringArray", mlAnchorSpringArray);
		// cellArray
		int Ncell = cellArray.size();
		MLStructure mlCellArray = new MLStructure(null, new int[] {Ncell,1});
		for(int iCell=0; iCell<Ncell; iCell++) {
			CCell cell = cellArray.get(iCell);
			mlCellArray.setField("cellArrayIndex", 	new MLDouble(null, new double[] {cell.arrayIndex+1}, 1), iCell);
			mlCellArray.setField("colour", 			new MLDouble(null, new double[] {cell.colour[0], cell.colour[1], cell.colour[2]}, 3), iCell);
			mlCellArray.setField("filament", 		new MLDouble(null, new double[] {cell.filament?1:0}, 1), iCell);
			mlCellArray.setField("type", 			new MLDouble(null, new double[] {cell.type}, 1), iCell);
			if(cell.mother!=null) {
				mlCellArray.setField("motherIndex", 	new MLDouble(null, new double[] {cell.mother.arrayIndex+1}, 1), iCell);
			} else {
				mlCellArray.setField("motherIndex", 	new MLDouble(null, new double[] {}, 1), iCell);
			}
		}
		mlModel.setField("cellArray", mlCellArray);
		
		// ballArray
		int NBall = ballArray.size();
		MLStructure mlBallArray = new MLStructure(null,new int[] {NBall,1});
		for(int iBall=0; iBall<NBall; iBall++) {
			CBall ball = ballArray.get(iBall);
			mlBallArray.setField("pos", 		new MLDouble(null, new double[] {ball.pos.x, ball.pos.y, ball.pos.z}, 3), iBall);
			mlBallArray.setField("vel", 		new MLDouble(null, new double[] {ball.vel.x, ball.vel.y, ball.vel.z}, 3), iBall);
//			mlBallArray.setField("force",	 	new MLDouble(null, new double[] {ball.force.x, ball.force.y, ball.force.z}, 1), iBall);
			// posSave and velSave
			int NSave = (int)(movementTimeStepEnd/movementTimeStep)-1;
			double[] posSave = new double[NSave*3];
			double[] velSave = new double[NSave*3];
			for(int ii=0; ii<NSave; ii++) {
				posSave[ii] 		= ball.posSave[ii].x; 	velSave[ii] 		= ball.velSave[ii].x;
				posSave[ii+NSave] 	= ball.posSave[ii].y; 	velSave[ii+NSave] 	= ball.velSave[ii].y;
				posSave[ii+2*NSave] = ball.posSave[ii].z; 	velSave[ii+2*NSave] = ball.velSave[ii].z;
			}
			mlBallArray.setField("posSave", 	new MLDouble(null, posSave, NSave), iBall);
			mlBallArray.setField("velSave", 	new MLDouble(null, velSave, NSave), iBall);
			//
			mlBallArray.setField("arrayIndex", new MLDouble(null, new double[] {ball.arrayIndex+1}, 1), iBall);
			mlBallArray.setField("cellArrayIndex", new MLDouble(null, new double[] {ball.cell.arrayIndex+1}, 1), iBall);
			mlBallArray.setField("cellBallArrayIndex", new MLDouble(null, new double[] {ball.cellBallArrayIndex+1}, 1), iBall);
			mlBallArray.setField("mass", 		new MLDouble(null, new double[] {ball.mass}, 1), iBall);
//			mlBallArray.setField("radius", 		new MLDouble(null, new double[] {ball.radius}, 1), iBall);
		}
		mlModel.setField("ballArray", mlBallArray);
		
		// rodSpringArray
		int NRod = rodSpringArray.size();
		MLStructure mlRodSpringArray = new MLStructure(null, new int[] {NRod,1});
		for(int iRod=0; iRod<NRod; iRod++) {
			CSpring pRod = rodSpringArray.get(iRod);
			mlRodSpringArray.setField("ballArrayIndex",	new MLDouble(null, new double[] {pRod.ballArray[0].arrayIndex+1, pRod.ballArray[1].arrayIndex+1}, 2),iRod);
			mlRodSpringArray.setField("K",				new MLDouble(null, new double[] {pRod.K}, 1),iRod);
			mlRodSpringArray.setField("restLength",		new MLDouble(null, new double[] {pRod.restLength}, 1),iRod);	
		}
		mlModel.setField("rodSpringArray", mlRodSpringArray);
		
		// filSpringArray
		int NFil 	= filSpringArray.size();
		MLStructure mlFilSpringArray  = new MLStructure(null, new int[] {NFil,1});
		for(int iFil=0; iFil<NFil; iFil++) {
			CFilSpring pFil = filSpringArray.get(iFil);
			mlFilSpringArray.setField("arrayIndex", 		new MLDouble(null, new double[] {pFil.arrayIndex+1}, 1),iFil);
			mlFilSpringArray.setField("big_K", 				new MLDouble(null, new double[] {pFil.big_K}, 1),iFil);
			mlFilSpringArray.setField("big_restLength", 	new MLDouble(null, new double[] {pFil.big_restLength}, 1),iFil);
			mlFilSpringArray.setField("big_ballArrayIndex", new MLDouble(null, new double[] {pFil.big_ballArray[0].arrayIndex+1, 	pFil.big_ballArray[1].arrayIndex+1}, 2),iFil);
			mlFilSpringArray.setField("small_K", 			new MLDouble(null, new double[] {pFil.small_K}, 1),iFil);
			mlFilSpringArray.setField("small_restLength", 	new MLDouble(null, new double[] {pFil.small_restLength}, 1),iFil);
			mlFilSpringArray.setField("small_ballArrayIndex",new MLDouble(null, new double[] {pFil.small_ballArray[0].arrayIndex+1, pFil.small_ballArray[1].arrayIndex+1}, 2),iFil);
		}
		mlModel.setField("filSpringArray", mlFilSpringArray);
		// stickSpringArray
		int NStick = stickSpringArray.size();
		MLStructure mlStickSpringArray = new MLStructure(null, new int[] {NStick,1});
		for(int iStick=0; iStick<NStick; iStick++) {
			CStickSpring pStick = stickSpringArray.get(iStick);
			mlStickSpringArray.setField("ballArrayIndex", 	new MLDouble(null, new double[] {pStick.ballArray[0].arrayIndex+1, pStick.ballArray[1].arrayIndex+1}, 2), iStick);
			mlStickSpringArray.setField("K",				new MLDouble(null, new double[] {pStick.K}, 1), iStick);
			mlStickSpringArray.setField("restLength",		new MLDouble(null, new double[] {pStick.restLength}, 1), iStick);
			mlStickSpringArray.setField("stickArrayIndex", 	new MLDouble(null, new double[] {stickSpringArray.indexOf(pStick)+1}, 1), iStick);
			if(pStick.ballArray[0].cell.type==0 && pStick.ballArray[1].cell.type==0) {
				mlStickSpringArray.setField("siblingArrayIndex", new MLDouble(null, new double[] {}, 1), iStick);	// only spheres, no siblings
			} else if(pStick.ballArray[0].cell.type==0 ^ pStick.ballArray[1].cell.type==0) {	// exactly one (XOR) rod one sphere, so 1 sibling
				mlStickSpringArray.setField("siblingArrayIndex", new MLDouble(null, new double[] {stickSpringArray.indexOf(pStick.siblingArray[0])+1}, 1), iStick);
			} else {																			// both are two rod, so  3 siblings
				mlStickSpringArray.setField("siblingArrayIndex", new MLDouble(null, new double[] {stickSpringArray.indexOf(pStick.siblingArray[0])+1, 
																									stickSpringArray.indexOf(pStick.siblingArray[1])+1, 
																									stickSpringArray.indexOf(pStick.siblingArray[2])+1}, 3), iStick);
			}
		}
		mlModel.setField("stickSpringArray", mlStickSpringArray);
		// Create a list and add mlModel
		ArrayList<MLArray> list = new ArrayList<MLArray>(1);
		list.add(mlModel);
		
		try {
			new MatFileWriter(name + "/output/" + String.format("m%04dg%04d", movementIter, growthIter) + ".mat",list);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void Load(String fileName) {
		try {
			MatFileReader mlFile = new MatFileReader(fileName);
			MLStructure mlModel = (MLStructure)mlFile.getMLArray("model");
			
			aspect 		= ((MLDouble)mlModel.getField("aspect")).getReal(0);
			G 			= ((MLDouble)mlModel.getField("G")).getReal(0);
			growthIter 	= ((MLDouble)mlModel.getField("growthIter")).getReal(0).intValue();
			growthTime 	= ((MLDouble)mlModel.getField("growthTime")).getReal(0);
			growthTimeStep = ((MLDouble)mlModel.getField("growthTimeStep")).getReal(0);
			Ki 			= ((MLDouble)mlModel.getField("K1")).getReal(0);
			Ka 			= ((MLDouble)mlModel.getField("Ka")).getReal(0);
			Kc 			= ((MLDouble)mlModel.getField("Kc")).getReal(0);
			Kd 			= ((MLDouble)mlModel.getField("Kd")).getReal(0);
			Kf 			= ((MLDouble)mlModel.getField("Kf")).getReal(0);
			Ks 			= ((MLDouble)mlModel.getField("Ks")).getReal(0);
			Kw 			= ((MLDouble)mlModel.getField("Kw")).getReal(0);
			L 			= new Vector3d(
									((MLDouble)mlModel.getField("L")).getReal(0),
									((MLDouble)mlModel.getField("L")).getReal(1),
									((MLDouble)mlModel.getField("L")).getReal(2));
			MCellInit	= ((MLDouble)mlModel.getField("MCellInit")).getReal(0);
			MCellMax 	= ((MLDouble)mlModel.getField("MCellMax")).getReal(0);
			movementIter = ((MLDouble)mlModel.getField("movementIter")).getReal(0).intValue();
			movementTime = ((MLDouble)mlModel.getField("movementTime")).getReal(0);
			movementTimeStepEnd = ((MLDouble)mlModel.getField("movementTimeEnd")).getReal(0);
			movementTimeStep = ((MLDouble)mlModel.getField("movementTimeStep")).getReal(0);
			name 		= ((MLChar)mlModel.getField("name")).getString(0);
			NBall		= ((MLDouble)mlModel.getField("NBall")).getReal(0).intValue();
			NInitCell 	= ((MLDouble)mlModel.getField("NInitCell")).getReal(0).intValue();
			NType 		= ((MLDouble)mlModel.getField("NType")).getReal(0).intValue();
			randomSeed 	= ((MLDouble)mlModel.getField("randomSeed")).getReal(0).intValue();
			rho_m 		= ((MLDouble)mlModel.getField("rho_m")).getReal(0);
			rho_w 		= ((MLDouble)mlModel.getField("rho_w")).getReal(0);
			
			// cellArray
			MLStructure mlCellArray = (MLStructure)mlModel.getField("cellArray");
			int NCell = mlCellArray.getSize();
			cellArray	 	= new ArrayList<CCell>(NCell);
			for(int iCell=0; iCell<NCell; iCell++) {
				CCell cell = new CCell();
				cell.arrayIndex = ((MLDouble)mlCellArray.getField("cellArrayIndex", iCell)).getReal(0).intValue()-1;
				cell.colour	= new double[]{((MLDouble)mlCellArray.getField("colour", iCell)).getReal(0),
									((MLDouble)mlCellArray.getField("colour", iCell)).getReal(1),
									((MLDouble)mlCellArray.getField("colour", iCell)).getReal(2)};
				cell.filament 	= ((MLDouble)mlCellArray.getField("filament", iCell)).getReal(0)==1 ? true : false;
				cell.pModel	= this;
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
				ball.arrayIndex = ((MLDouble)mlBallArray.getField("arrayIndex",iBall)).getReal(0).intValue()-1;
				ball.cellBallArrayIndex = ((MLDouble)mlBallArray.getField("cellBallArrayIndex",iBall)).getReal(0).intValue()-1;
				ball.cell = cellArray.get(((MLDouble)mlBallArray.getField("cellArrayIndex",iBall)).getReal(0).intValue()-1);
				ball.mass 	= ((MLDouble)mlBallArray.getField("mass",iBall)).getReal(0);
				ball.radius = ball.Radius();
				ball.pos 	= new Vector3d(
								((MLDouble)mlBallArray.getField("pos", iBall)).getReal(0),
								((MLDouble)mlBallArray.getField("pos", iBall)).getReal(1),
								((MLDouble)mlBallArray.getField("pos", iBall)).getReal(2));
				ball.vel 	= new Vector3d(
								((MLDouble)mlBallArray.getField("vel", iBall)).getReal(0),
								((MLDouble)mlBallArray.getField("vel", iBall)).getReal(1),
								((MLDouble)mlBallArray.getField("vel", iBall)).getReal(2));
//				ball.force = new Vector3d(
//								((MLDouble)mlBallArray.getField("force", iBall)).getReal(0),
//								((MLDouble)mlBallArray.getField("force", iBall)).getReal(1),
//								((MLDouble)mlBallArray.getField("force", iBall)).getReal(2));
				ball.force = new Vector3d();
				// posSave and velSave
				int NSave = (int)(movementTimeStepEnd/movementTimeStep-1);
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
			rodSpringArray = new ArrayList<CSpring>(NRod);
			MLStructure mlRodSpringArray = (MLStructure)mlModel.getField("rodSpringArray");
			for(int iRod=0; iRod<NRod; iRod++) {
				CSpring pRod= new CSpring();
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
				CAnchorSpring pAnchor = new CAnchorSpring();
				pAnchor.anchor 	= new Vector3d(
									((MLDouble)mlAnchorSpringArray.getField("anchor",iAnchor)).getReal(0),
									((MLDouble)mlAnchorSpringArray.getField("anchor",iAnchor)).getReal(1),
									((MLDouble)mlAnchorSpringArray.getField("anchor",iAnchor)).getReal(2));
				pAnchor.K		= ((MLDouble)mlAnchorSpringArray.getField("K",iAnchor)).getReal(0);
				int iBall		= ((MLDouble)mlAnchorSpringArray.getField("ballArrayIndex",iAnchor)).getReal(0).intValue()-1;
				pAnchor.ball 	= ballArray.get(iBall);
				pAnchor.restLength = ((MLDouble)mlAnchorSpringArray.getField("restLength",iAnchor)).getReal(0);
				anchorSpringArray.add(pAnchor);
			}
			for(int iAnchor=0; iAnchor<NAnchor; iAnchor++) {	// Additional for loop to assign siblings
				CAnchorSpring pAnchor = anchorSpringArray.get(iAnchor);
				if(pAnchor.ball.cell.type!=0) {
					int iSibling = ((MLDouble)mlAnchorSpringArray.getField("siblingArrayIndex", iAnchor)).getReal(0).intValue()-1;
					pAnchor.siblingArray[0] = anchorSpringArray.get(iSibling); 
				}
			}
			// filSpringArray
			MLStructure mlFilSpringArray = (MLStructure)mlModel.getField("filSpringArray");
			int NFil = mlFilSpringArray.getSize();
			filSpringArray = new ArrayList<CFilSpring>(NFil);
			for(int iFil=0; iFil<NFil; iFil++) {
				CFilSpring pFil 		= new CFilSpring();
				pFil.arrayIndex 		= ((MLDouble)mlFilSpringArray.getField("arrayIndex", iFil)).getReal(0).intValue()-1;
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
			for(CSpring pRod : rodSpringArray) {
				pRod.ballArray[0].cell.springArray[0] = pRod;
			}
			// each cell's ballArray
			for(CBall ball : ballArray) {
				ball.cell.ballArray[ball.cellBallArrayIndex] = ball;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	///////////////////
	// POV-Ray stuff //
	///////////////////
	public void POV_Write(boolean plotIntermediate) {
		int NSave;
		if(plotIntermediate) 	NSave = ballArray.get(0).posSave.length;
		else					NSave = 0;
		plotIntermediate = false;		// Don't plot intermediate values for now, will revert later.
		
		//////////////////////////////
		
		// Make output folder if it doesn't exist already
		if(!(new File(name + "/output")).exists())	new File(name + "/output").mkdir();
		PrintWriter fid=null;
			for(int ii=0; ii<NSave+1; ii++) {
				try {
					if(ii>0)	plotIntermediate=true;		// If this is not the first iteration, do the intermediate plotting 
					String fileName = String.format("%s/output/pov.%04d.%04d.inc", name, movementIter-ii,growthIter);
					// Remove inc file if it already exists
					if((new File(fileName)).exists()) new File(fileName).delete();
					// Write new inc file
					fid = new PrintWriter(new FileWriter(fileName,true));		// True is for append // Not platform independent TODO
					// Include text, calibrated for tomas_persp_3D_java.pov 
					fid.println("text \n{\n" +           
					String.format("\tttf \"timrom.ttf\" \"Movement time: %05.2f s\" 0.05, 0.1*x\n",(movementIter-ii)*movementTimeStep) +
				    "\tpigment {color rgb <0.000, 0.000, 0.000>  }\n" +     
			        "\tscale <60,60,60>\n" + 
			        "\ttranslate <-1200,1340,-75>\n" +  
			        "\trotate <15,45,0>\n" +
			        "\tno_shadow" +
					"}\n" + 
					"text \n{\n" +           
					String.format("\tttf \"timrom.ttf\" \"Growth time:     %05.1f h\" 0.05, 0.1*x\n",growthIter*growthTimeStep) +
				    "\tpigment {color rgb <0.000, 0.000, 0.000>  }\n" +     
			        "\tscale <60,60,60>\n" + 
			        "\ttranslate <-1200,1260,-75>\n" +  
			        "\trotate <15,45,0>\n" +
			        "\tno_shadow" +
					"}\n");
					
					// Build spheres and rods
					for(int iCell=0; iCell<cellArray.size(); iCell++) {
						CCell cell = cellArray.get(iCell);
						fid.println("// Cell no. " + iCell);
						if(cell.type == 0) {
							// Spherical cell
							CBall ball = cell.ballArray[0];

							fid.println("sphere\n" + 
									"{\n" + 
									(plotIntermediate ? 
										String.format("\t < %10.3f,%10.3f,%10.3f > \n", ball.posSave[NSave-ii].x*1e6, ball.posSave[NSave-ii].y*1e6, ball.posSave[NSave-ii].z*1e6) : 
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
						else if(cell.type == 1 || cell.type == 2) {	// Rod
							CBall ball = cell.ballArray[0];
							CBall ballNext = cell.ballArray[1];

							fid.println("cylinder\n" +		// Sphere-sphere connection
									"{\n" +
									(plotIntermediate ?
										String.format("\t<%10.3f,%10.3f,%10.3f>,\n", ball.posSave[NSave-ii].x*1e6, ball.posSave[NSave-ii].y*1e6, ball.posSave[NSave-ii].z*1e6) +
										String.format("\t<%10.3f,%10.3f,%10.3f>,\n", ballNext.posSave[NSave-ii].x*1e6, ballNext.posSave[NSave-ii].y*1e6, ballNext.posSave[NSave-ii].z*1e6) :
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
										String.format("\t < %10.3f,%10.3f,%10.3f > \n", ball.posSave[NSave-ii].x*1e6, ball.posSave[NSave-ii].y*1e6, ball.posSave[NSave-ii].z*1e6) :
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
										String.format("\t < %10.3f,%10.3f,%10.3f > \n", ballNext.posSave[NSave-ii].x*1e6, ballNext.posSave[NSave-ii].y*1e6, ballNext.posSave[NSave-ii].z*1e6) :
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
										String.format("\t<%10.3f,%10.3f,%10.3f>,\n", ball.posSave[NSave-ii].x*1e6, ball.posSave[NSave-ii].y*1e6, ball.posSave[NSave-ii].z*1e6) +
										String.format("\t<%10.3f,%10.3f,%10.3f>,\n", ballNext.posSave[NSave-ii].x*1e6, ballNext.posSave[NSave-ii].y*1e6, ballNext.posSave[NSave-ii].z*1e6) :
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
									String.format("\t<%10.3f,%10.3f,%10.3f>,\n", ball.posSave[NSave-ii].x*1e6, ball.posSave[NSave-ii].y*1e6, ball.posSave[NSave-ii].z*1e6) +
									String.format("\t<%10.3f,%10.3f,%10.3f>,\n", ballNext.posSave[NSave-ii].x*1e6, ballNext.posSave[NSave-ii].y*1e6, ballNext.posSave[NSave-ii].z*1e6) : 
								String.format("\t<%10.3f,%10.3f,%10.3f>,\n", ball.pos.x*1e6, ball.pos.y*1e6, ball.pos.z*1e6) +
								String.format("\t<%10.3f,%10.3f,%10.3f>,\n", ballNext.pos.x*1e6, ballNext.pos.y*1e6, ballNext.pos.z*1e6)) +
								String.format("\t%10.3f\n", ball.radius*1e5) +
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
										String.format("\t<%10.3f,%10.3f,%10.3f>,\n", ball.posSave[NSave-ii].x*1e6, ball.posSave[NSave-ii].y*1e6, ball.posSave[NSave-ii].z*1e6) :
										String.format("\t<%10.3f,%10.3f,%10.3f>,\n", ball.pos.x*1e6, ball.pos.y*1e6, ball.pos.z*1e6)) +
									String.format("\t<%10.3f,%10.3f,%10.3f>,\n", pSpring.anchor.x*1e6, pSpring.anchor.y*1e6, pSpring.anchor.z*1e6) +
									String.format("\t%10.3f\n", ball.radius*1e5) +	// Note 1e5 instead of 1e6 TODO
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
					// Done with this time interval
				} catch(IOException E) {
					E.printStackTrace();
				} finally {
					fid.close();
				}
			}
		
	}
	
	public void POV_Plot(boolean plotIntermediate) {
		if(!(new File(name + "/image")).exists()) {
			new File(name + "/image").mkdir();
		}
		int NIter;
		if(plotIntermediate)	NIter = 1+ballArray.get(0).posSave.length;
		else					NIter = 1;
		for(int ii=0; ii<NIter; ii++) {
			String imageName = String.format("pov_m%04dg%04d", movementIter-ii, growthIter);
			String incName = String.format("pov.%04d.%04d.inc", movementIter-ii,growthIter);
			String input = "povray ../pov/tomas_persp_3D_java.pov +W1024 +H768 +K" + String.format("%04d",movementIter-ii) + "." + String.format("%04d",growthIter) + " +O../" + name + "/image/" + imageName + " +A -J";
			LinuxInteractor.executeCommand("cd " + name + " ; " + input + " ; rm ./output/" + incName + " ; cd ..", setting.waitForFinish,setting.echoCommand);
		}
	}
}

class returnObject {		// Used for collision detection multiple return
	Vector3d dP;
	double dist;
	double sc;
	double tc;
	Vector3d c1;
	Vector3d c2;
	
	returnObject(Vector3d dP, double dist, double sc) {
		this.dP = dP;
		this.dist = dist; 
		this.sc = sc;
	}
	
	returnObject(Vector3d dP, double dist, double sc, double tc, Vector3d c1, Vector3d c2) {
		this.dP = dP;
		this.dist = dist; 
		this.sc = sc;
		this.tc = tc;
		this.c1 = c1;
		this.c2 = c2;
	}
}