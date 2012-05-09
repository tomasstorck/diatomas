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
//	ArrayList<CBall> ballArray;
//	ArrayList<CSpring> springArray;
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
		randomSeed = 0;
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
		double dist = dP.length();

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
			iSpring += pSpring.NSibling+1;
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
		for(int iTime=0; iTime<out.nsave-1; iTime++) {		// Save all intermediate results to the save variables
			int iVar = 0;
			for(CBall pBall : BallArray()) {
				pBall.posSave[iTime].x = out.ysave.get(iVar++,iTime);
				pBall.posSave[iTime].y = out.ysave.get(iVar++,iTime);
				pBall.posSave[iTime].z = out.ysave.get(iVar++,iTime);
				pBall.velSave[iTime].x = out.ysave.get(iVar++,iTime);
				pBall.velSave[iTime].y = out.ysave.get(iVar++,iTime);
				pBall.velSave[iTime].z = out.ysave.get(iVar++,iTime);
			}
		}
		{int iVar = 0;										// Only the final value is stored in the pos and vel variables
		int iTime = out.nsave;
		for(CBall pBall : BallArray()) {
			pBall.pos.x = out.ysave.get(iVar++,iTime);
			pBall.pos.y = out.ysave.get(iVar++,iTime);
			pBall.pos.z = out.ysave.get(iVar++,iTime);
			pBall.vel.x = out.ysave.get(iVar++,iTime);
			pBall.vel.y = out.ysave.get(iVar++,iTime);
			pBall.vel.z = out.ysave.get(iVar++,iTime);
		}}
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
		// Collision forces
		double R2, H2;			// rest length, check distance 
		for(int iCell=0; iCell<cellArray.size(); iCell++) {
			CCell pCell = cellArray.get(iCell);
			CBall pBall = pCell.ballArray[0];
			// Base collision on the cell type
			if(pCell.type==0) {
				// Check for all remaining cells
				for(int jCell=iCell+1; jCell<cellArray.size(); jCell++) {
					Vector3d S1_P0 = new Vector3d(pBall.pos);
					CCell pCellNext = cellArray.get(jCell);
					// Check for a sticking spring, if there is one let it do the work
					if(!pCell.stickCellArray.equals(pCellNext)) {
						CBall pBallNext = pCellNext.ballArray[0];
						R2 = pBall.radius + pBallNext.radius;
						H2 = aspect * R2 + R2;
						if(pCellNext.type==0) {		// The other cell is a ball too
							// do a simple collision detection
							Vector3d S2_P0 = new Vector3d(pBallNext.pos);
							Vector3d dirn = S1_P0.minus(S2_P0);
							double rpos = dirn.length();
							if(rpos<R2) {
								// We have a collision
								dirn.normalise();
								rpos = R2-rpos;
								dirn = dirn.times(Kc*rpos);
								// Add forces
								pBall.force.plus(dirn);
								pBallNext.force.minus(dirn);
							}
						} else {					// type != 0 --> the other cell is a rod
							// do a sphere-rod collision detection
							CBall pBallNext2 = pCellNext.ballArray[1];
							returnObject R = DetectCellCollision_Ericson(pBallNext.pos, pBallNext2.pos, pBall.pos);
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
								pBallNext.force.x -= sc1*Fsx;
								pBallNext.force.y -= sc1*Fsy;
								pBallNext.force.z -= sc1*Fsz;
								pBallNext2.force.x -= sc*Fsx;
								pBallNext2.force.y -= sc*Fsy;
								pBallNext2.force.z -= sc*Fsz;
								// ball in sphere
								pBall.force.x += Fsx;
								pBall.force.y += Fsy;
								pBall.force.z += Fsz;
							}
						}
					}
				}
			} else {	// pCell.type != 0
				CBall pBall2 = pCell.ballArray[1];
				for(int jCell = iCell+1; jCell<cellArray.size(); jCell++) {
					CCell pCellNext = cellArray.get(jCell);
					// check for sticking spring and let it do the work later on if it's there
					if(!pCell.stickCellArray.equals(pCellNext)) {
						CBall pBallNext = pCellNext.ballArray[0];
						R2 = pBall.radius + pBallNext.radius; 
						if(pCellNext.type==0) {
							// do a sphere-rod collision detection
							returnObject R = DetectCellCollision_Ericson(pBall.pos, pBall2.pos, pBallNext.pos); 
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
								pBall.force.x -= sc1*Fsx;
								pBall.force.y -= sc1*Fsy;
								pBall.force.z -= sc1*Fsz;
								pBall2.force.x -= sc*Fsx;
								pBall2.force.y -= sc*Fsy;
								pBall2.force.z -= sc*Fsz;
								// ball in sphere
								pBallNext.force.x += Fsx;
								pBallNext.force.y += Fsy;
								pBallNext.force.z += Fsz;
							}
						} else {	// type != 0 --> the other cell is a rod. This is where it gets tricky
							Vector3d S1_P0 = new Vector3d(pBall.pos);
							Vector3d S1_P1 = new Vector3d(pBall2.pos);
							CBall pBallNext2 = pCellNext.ballArray[1];
							Vector3d S2_P0 = new Vector3d(pBallNext.pos);
							Vector3d S2_P1 = new Vector3d(pBallNext2.pos);
							H2 = aspect*R2*3;
							if(Math.abs(S2_P0.x - S1_P0.x) < H2 && Math.abs(S2_P0.y - S1_P0.y) < H2 && Math.abs(S2_P0.z - S1_P0.z) < H2) {
								// calculate the distance between the two diatoma segments
								returnObject R = DetectCellCollision_Ericson(S1_P0, S1_P1, S2_P0, S2_P1);
								Vector3d dP = R.dP;
								double dist = R.dist;
								double sc = R.sc;
								double tc = R.tc;
								
								if(dist<R2 && !pCell.IsFilament(pCellNext)) {
									double f = Kc/dist*(dist-R2);
									double Fsx = f*dP.x;
									double Fsy = f*dP.y;
									double Fsz = f*dP.z;
									// Add these elastic forces to the cells
									double sc1 = 1-sc;
									double tc1 = 1-tc;
									// both balls in 1st rod
									pBall.force.x -= sc1*Fsx;
									pBall.force.y -= sc1*Fsy;
									pBall.force.z -= sc1*Fsz;
									pBall2.force.x -= sc*Fsx;
									pBall2.force.y -= sc*Fsy;
									pBall2.force.z -= sc*Fsz;
									// both balls in 1st rod
									pBallNext.force.x += tc1*Fsx;
									pBallNext.force.y += tc1*Fsy;
									pBallNext.force.z += tc1*Fsz;
									pBallNext2.force.x += tc*Fsx;
									pBallNext2.force.y += tc*Fsy;
									pBallNext2.force.z += tc*Fsz;
								}
							}
						}
					}
				}
			}
		}
//		// Calculate gravity+bouyancy, normal forces and drag
//		for(CBall pBall : BallArray()) {
//			// Contact forces
//			double y = pBall.pos.y;
//			double r = pBall.radius;
//			if(y<r){
//				pBall.force.y += Kw*(r-y);
//			}
//			// Gravity and buoyancy
//			if(y>r) {			// Only if not already at the floor 
//				pBall.force.y += G * ((rho_m-rho_w)/rho_w) * pBall.mass ;  //let the ball fall 
//			}
//			// Velocity damping
//			pBall.force.x -= Kd*pBall.vel.x;
//			pBall.force.y -= Kd*pBall.vel.y;
//			pBall.force.z -= Kd*pBall.vel.z;
//		}
//		
		// Elastic forces between springs within cells (CSpring in type>0)
		for(CCell pCell : cellArray) {
			if(pCell.type!=0) {
				CSpring pSpring = pCell.springArray[0];
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
		}
//		
//		// Sticking springs elastic forces (CStickSpring in stickSpringArray)
//		for(CStickSpring pStick : stickSpringArray) {
//				// find difference vector and distance dn between balls (euclidian distance) 
//				Vector3d diff = pStick.ballArray[1].pos.minus(pStick.ballArray[0].pos);
//				double dn = diff.length();
//				// Get force
//				double f = pStick.K/dn * (dn - pStick.restLength);
//				// Hooke's law
//				double Fsx = f*diff.x;
//				double Fsy = f*diff.y;
//				double Fsz = f*diff.z;
//				// apply forces on balls
//				pStick.ballArray[0].force.x += Fsx;
//				pStick.ballArray[0].force.y += Fsy;
//				pStick.ballArray[0].force.z += Fsz;
//				pStick.ballArray[1].force.x -= Fsx;
//				pStick.ballArray[1].force.y -= Fsy;
//				pStick.ballArray[1].force.z -= Fsz;
//		}
		
//		// Filament spring elastic force (CFilSpring in filSpringArray)
//		for(CFilSpring pFil : filSpringArray) {
//			for(CSpring pSpring : new CSpring[]{pFil.bigSpring, pFil.smallSpring}) {
//				// find difference vector and distance dn between balls (euclidian distance) 
//				Vector3d diff = pSpring.ballArray[1].pos.minus(pSpring.ballArray[0].pos);
//				double dn = diff.length();
//				// Get force
//				double f = pSpring.K/dn * (dn - pSpring.restLength);
//				// Hooke's law
//				double Fsx = f*diff.x;
//				double Fsy = f*diff.y;
//				double Fsz = f*diff.z;
//				// apply forces on balls
//				pSpring.ballArray[0].force.x += Fsx;
//				pSpring.ballArray[0].force.y += Fsy;
//				pSpring.ballArray[0].force.z += Fsz;
//				pSpring.ballArray[1].force.x -= Fsx;
//				pSpring.ballArray[1].force.y -= Fsy;
//				pSpring.ballArray[1].force.z -= Fsz;
//			}
//		}
		
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
			mass *= (0.95+rand.Double()/5.0);
			// Syntrophic growth
			for(CCell pStickCell : pCell.stickCellArray) {
				if((pCell.type==0 && pStickCell.type!=0) || (pCell.type!=0 && pStickCell.type==0)) {
					// The cell types are different on the other end of the spring
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
					pNew.SetMass(mass/2.0);		// Radius is updated in this method
					pCell.SetMass(mass/2.0);
					// Set properties for new cell
					pNew.ballArray[0].vel = 	new Vector3d(pCell.ballArray[0].vel);
					pNew.ballArray[0].force = 	new Vector3d(pCell.ballArray[0].force);
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
						displacement = pBall0.radius*Math.pow(2.0,-0.666666);							// A very strange formula: compare our radius to the C++ equation for Rpos and you'll see it's the same
					} else {
						displacement = pBall1.radius/2.0;
					}
					
					// Make a new, displaced cell
					Vector3d middle = pBall1.pos.plus(pBall0.pos).divide(2.0); 
					CCell pNew = new CCell(pCell.type,													// Same type as pCell
							middle.x+	  displacement*direction.x,										// First ball					
							middle.y+1.01*displacement*direction.y,										// possible TODO, ought to be displaced slightly in original C++ code but is displaced significantly this way (change 1.01 to 2.01)
							middle.z+	  displacement*direction.z,
							pBall1.pos.x,																// Second ball
							pBall1.pos.y,
							pBall1.pos.z,
							pCell.filament,this);														// Same filament boolean as pCell and pointer to the model
					// Set mass for both cells
					pNew.SetMass(mass/2.0);
					pCell.SetMass(mass/2.0);
					// Displace old cell, 2nd ball
					pBall1.pos = middle.minus(direction.times(displacement));
					pCell.springArray[0].ResetRestLength();
					// Contain cells to y dimension of domain
					for(int iBall=0; iBall<2; iBall++) {
						if(pCell.ballArray[iBall].pos.y < pCell.ballArray[iBall].radius) {pCell.ballArray[0].pos.y = pCell.ballArray[0].radius;};
						if( pNew.ballArray[iBall].pos.y <  pNew.ballArray[iBall].radius) { pNew.ballArray[0].pos.y =  pNew.ballArray[0].radius;};
					}
					// Set properties for new cell
					for(int iBall=0; iBall<2; iBall++) {
						pNew.ballArray[iBall].vel = 	new Vector3d(pCell.ballArray[iBall].vel);
						pNew.ballArray[iBall].force = 	new Vector3d(pCell.ballArray[iBall].force);
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
				if(pCell.type>0) pCell.springArray[0].ResetRestLength();
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
			stickSpringArray.remove(pSpring.siblingArray[0]);
			stickSpringArray.remove(pSpring.siblingArray[1]);
			stickSpringArray.remove(pSpring.siblingArray[2]);
		}
	}
	
	public int BuildAnchor(ArrayList<CCell> collisionArray) {
		for(CAnchorSpring pSpring : anchorSpringArray) collisionArray.remove(pSpring.pBall.pCell);
		// Anchor the non-stuck, collided cells to the ground
		for(CCell pCell : collisionArray) pCell.Anchor();
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
				if((pSpring.ballArray[0].pCell.equals(pCell0) && pSpring.ballArray[0].pCell.equals(pCell1)) || (pSpring.ballArray[0].pCell.equals(pCell1) && pSpring.ballArray[0].pCell.equals(pCell0))) {
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
	
	////////////////////////////
	// Saving, loading things //
	////////////////////////////
	public void Save() {
		MLStructure mlModel = new MLStructure("model", new int[] {1,1});
		mlModel.setField("aspect", 				new MLDouble(null, new double[] {aspect}, 1));
		mlModel.setField("G", 					new MLDouble(null, new double[] {G}, 1));
		mlModel.setField("growthIter", 			new MLDouble(null, new double[] {growthIter}, 1));
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
		mlModel.setField("L",					new MLDouble(null, new double[] {L.x, L.y, L.z}, 1));
		mlModel.setField("MCellInit",			new MLDouble(null, new double[] {MCellInit}, 1));
		mlModel.setField("MCellMax",			new MLDouble(null, new double[] {MCellMax}, 1));
		mlModel.setField("movementIter",		new MLDouble(null, new double[] {movementIter}, 1));
		mlModel.setField("movementTime",		new MLDouble(null, new double[] {movementTime}, 1));
		mlModel.setField("movementTimeEnd",		new MLDouble(null, new double[] {movementTimeEnd}, 1));
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
			mlAnchorSpringArray.setField("cellArrayIndex", 	new MLDouble(null, new double[] {pAnchor.pBall.pCell.cellArrayIndex+1}, 1),iAnchor);		// +1 for 0 vs 1 based indexing in Java vs MATLAB
			mlAnchorSpringArray.setField("ballArrayIndex", 	new MLDouble(null, new double[] {pAnchor.pBall.ballArrayIndex+1}, 1),iAnchor);
			mlAnchorSpringArray.setField("K",				new MLDouble(null, new double[] {pAnchor.K}, 1),iAnchor);
			mlAnchorSpringArray.setField("restLength",		new MLDouble(null, new double[] {pAnchor.restLength}, 1),iAnchor);
			mlAnchorSpringArray.setField("anchorArrayIndex", new MLDouble(null, new double[] {pAnchor.anchorArrayIndex+1}, 1),iAnchor);
			if(pAnchor.pBall.pCell.type!=0) {
				mlAnchorSpringArray.setField("siblingArrayIndex", new MLDouble(null, new double[] {pAnchor.anchorArrayIndex+1}, 1),iAnchor);
			} else {
				mlAnchorSpringArray.setField("siblingArrayIndex", new MLDouble(null, new double[] {}, 1),iAnchor);
			}
		}
		mlModel.setField("anchorSpringArray", mlAnchorSpringArray);
		// cellArray
		int Ncell = cellArray.size();
		MLStructure mlCellArray = new MLStructure(null, new int[] {Ncell,1});
		for(int iCell=0; iCell<Ncell; iCell++) {
			CCell pCell = cellArray.get(iCell);
			mlCellArray.setField("cellArrayIndex", 	new MLDouble(null, new double[] {pCell.cellArrayIndex+1}, 1), iCell);
			mlCellArray.setField("colour", 			new MLDouble(null, new double[] {pCell.colour[0], pCell.colour[1], pCell.colour[2]}, 3), iCell);
			mlCellArray.setField("filament", 		new MLDouble(null, new double[] {pCell.filament?1:0}, 1), iCell);
			mlCellArray.setField("type", 			new MLDouble(null, new double[] {pCell.type}, 1), iCell);
			if(pCell.mother!=null) {
				mlCellArray.setField("motherIndex", 	new MLDouble(null, new double[] {pCell.mother.cellArrayIndex+1}, 1), iCell);
			} else {
				mlCellArray.setField("motherIndex", 	new MLDouble(null, new double[] {}, 1), iCell);
			}
			// ballArray
			int Nball = (pCell.type==0 ? 1 : 2);
			MLStructure mlBallArray = new MLStructure(null,new int[] {Nball,1});
			for(int iBall=0; iBall<Nball; iBall++) {
				CBall pBall = pCell.ballArray[iBall];
				mlBallArray.setField("pos", 		new MLDouble(null, new double[] {pBall.pos.x, pBall.pos.y, pBall.pos.z}, 3), iBall);
				mlBallArray.setField("vel", 		new MLDouble(null, new double[] {pBall.vel.x, pBall.vel.y, pBall.vel.z}, 3), iBall);
//				mlBallArray.setField("force",	 	new MLDouble(null, new double[] {pBall.force.x, pBall.force.y, pBall.force.z}, 1), iBall);
				// posSave and velSave
				int NSave = (int)(movementTimeEnd/movementTimeStep)-1;
				double[] posSave = new double[NSave*3];
				double[] velSave = new double[NSave*3];
				for(int ii=0; ii<NSave; ii++) {
					posSave[ii] 		= pBall.posSave[ii].x; 	velSave[ii] 		= pBall.velSave[ii].x;
					posSave[ii+NSave] 	= pBall.posSave[ii].y; 	velSave[ii+NSave] 	= pBall.velSave[ii].y;
					posSave[ii+2*NSave] = pBall.posSave[ii].z; 	velSave[ii+2*NSave] = pBall.velSave[ii].z;
				}
				mlBallArray.setField("posSave", 	new MLDouble(null, posSave, NSave), iBall);
				mlBallArray.setField("velSave", 	new MLDouble(null, velSave, NSave), iBall);
				//
				mlBallArray.setField("ballArrayIndex", new MLDouble(null, new double[] {pBall.ballArrayIndex+1}, 1), iBall);
				mlBallArray.setField("mass", 		new MLDouble(null, new double[] {pBall.mass}, 1), iBall);
//				mlBallArray.setField("radius", 		new MLDouble(null, new double[] {pBall.radius}, 1), iBall);
			}
			mlCellArray.setField("ballArray", mlBallArray, iCell);
			// springArray
			MLStructure mlSpringArray;
			if(pCell.type!=0) {
				mlSpringArray = new MLStructure(null,new int[] {1,1});
				CSpring pSpring = pCell.springArray[0];
				mlSpringArray.setField("cellArrayIndex",	new MLDouble(null, new double[] {pSpring.ballArray[0].pCell.cellArrayIndex+1, pSpring.ballArray[1].pCell.cellArrayIndex+1}, 2),0);
				mlSpringArray.setField("ballArrayIndex",	new MLDouble(null, new double[] {pSpring.ballArray[0].ballArrayIndex+1, pSpring.ballArray[1].ballArrayIndex+1}, 2),0);
				mlSpringArray.setField("K",				new MLDouble(null, new double[] {pSpring.K}, 1),0);
				mlSpringArray.setField("restLength",		new MLDouble(null, new double[] {pSpring.restLength}, 1),0);	
			} else {
				mlSpringArray = new MLStructure(null,new int[] {0,0});
			}
			mlCellArray.setField("springArray", mlSpringArray,iCell);
		}
		mlModel.setField("cellArray", mlCellArray);
		// filSpringArray
		int NFil 	= filSpringArray.size();
		MLStructure mlFilSpringArray  = new MLStructure(null, new int[] {NFil,1});
		for(int iFil=0; iFil<NFil; iFil++) {
			CFilSpring pFil = filSpringArray.get(iFil);
//			mlFilSpringArray.setField("K", 					new MLDouble(null, new double[] {pFil.K}, 1), iFil);
			MLStructure mlBigSpring  = new MLStructure(null, new int[] {1,1});
			MLStructure mlSmallSpring  = new MLStructure(null, new int[] {1,1});
			mlBigSpring.setField("K", 						new MLDouble(null, new double[] {pFil.bigSpring.K}, 1));
			mlBigSpring.setField("restLength", 				new MLDouble(null, new double[] {pFil.bigSpring.restLength}, 1));
			mlBigSpring.setField("cellArrayIndex", 			new MLDouble(null, new double[] {pFil.bigSpring.ballArray[0].pCell.cellArrayIndex+1, 	pFil.bigSpring.ballArray[1].pCell.cellArrayIndex+1}, 2));
			mlBigSpring.setField("ballArrayIndex", 			new MLDouble(null, new double[] {pFil.bigSpring.ballArray[0].ballArrayIndex+1, 			pFil.bigSpring.ballArray[1].ballArrayIndex+1}, 2));
			mlSmallSpring.setField("K", 					new MLDouble(null, new double[] {pFil.smallSpring.K}, 1));
			mlSmallSpring.setField("restLength", 			new MLDouble(null, new double[] {pFil.smallSpring.restLength}, 1));
			mlSmallSpring.setField("cellArrayIndex", 		new MLDouble(null, new double[] {pFil.smallSpring.ballArray[0].pCell.cellArrayIndex+1, 	pFil.smallSpring.ballArray[1].pCell.cellArrayIndex+1}, 2));
			mlSmallSpring.setField("ballArrayIndex", 		new MLDouble(null, new double[] {pFil.smallSpring.ballArray[0].ballArrayIndex+1, 		pFil.smallSpring.ballArray[1].ballArrayIndex+1}, 2));
			mlFilSpringArray.setField("bigSpring", mlBigSpring, iFil);
			mlFilSpringArray.setField("smallSpring", mlSmallSpring, iFil);
		}
		mlModel.setField("filSpringArray", mlFilSpringArray);
		// stickSpringArray
		int NStick = stickSpringArray.size();
		MLStructure mlStickSpringArray = new MLStructure(null, new int[] {NStick,1});
		for(int iStick=0; iStick<NStick; iStick++) {
			CStickSpring pStick = stickSpringArray.get(iStick);
			mlStickSpringArray.setField("cellArrayIndex", 	new MLDouble(null, new double[] {pStick.ballArray[0].pCell.cellArrayIndex+1, pStick.ballArray[1].pCell.cellArrayIndex+1}, 2), iStick);
			mlStickSpringArray.setField("ballArrayIndex", 	new MLDouble(null, new double[] {pStick.ballArray[0].ballArrayIndex+1, pStick.ballArray[1].ballArrayIndex+1}, 2), iStick);
			mlStickSpringArray.setField("K",				new MLDouble(null, new double[] {pStick.K}, 1), iStick);
			mlStickSpringArray.setField("restLength",		new MLDouble(null, new double[] {pStick.restLength}, 1), iStick);
			mlStickSpringArray.setField("stickArrayIndex", 	new MLDouble(null, new double[] {pStick.stickArrayIndex+1}, 1), iStick);
			if(pStick.ballArray[0].pCell.type==0 && pStick.ballArray[1].pCell.type==0) {
				mlStickSpringArray.setField("siblingArrayIndex", new MLDouble(null, new double[] {}, 1), iStick);
			} else if(pStick.ballArray[0].pCell.type==0 ^ pStick.ballArray[1].pCell.type==0) {		// exactly one rod one sphere, so 1 siblings
				mlStickSpringArray.setField("siblingArrayIndex", new MLDouble(null, new double[] {pStick.siblingArray[0].stickArrayIndex+1}, 1), iStick);
			} else {																			// both are two rod, so  3 siblings
				mlStickSpringArray.setField("siblingArrayIndex", new MLDouble(null, new double[] {pStick.siblingArray[0].stickArrayIndex+1, pStick.siblingArray[1].stickArrayIndex+1, pStick.siblingArray[2].stickArrayIndex+1}, 3), iStick);
			}
			
			if(pStick.ballArray[0].pCell.type!=0 || pStick.ballArray[1].pCell.type!=0) {		// at least one rod, so >0 siblings
				if(pStick.ballArray[0].pCell.type==0 || pStick.ballArray[1].pCell.type==0) { 	// one ball one rod, so  1 sibling  
					mlStickSpringArray.setField("siblingArrayIndex", new MLDouble(null, new double[] {pStick.siblingArray[0].stickArrayIndex+1}, 1), iStick);
				} else {																		// both are two rod, so  3 siblings
					mlStickSpringArray.setField("siblingArrayIndex", new MLDouble(null, new double[] {pStick.siblingArray[0].stickArrayIndex+1, pStick.siblingArray[1].stickArrayIndex+1, pStick.siblingArray[2].stickArrayIndex+1}, 3), iStick);
				}
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
			growthMaxIter = ((MLDouble)mlModel.getField("growthMaxIter")).getReal(0);
			growthTime 	= ((MLDouble)mlModel.getField("growthTime")).getReal(0);
			growthTimeStep = ((MLDouble)mlModel.getField("growthTimeStep")).getReal(0);
			K1 			= ((MLDouble)mlModel.getField("K1")).getReal(0);
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
			movementTimeEnd = ((MLDouble)mlModel.getField("movementTimeEnd")).getReal(0);
			movementTimeStep = ((MLDouble)mlModel.getField("movementTimeStep")).getReal(0);
			name 		= ((MLChar)mlModel.getField("name")).getString(0);
			NBall		= ((MLDouble)mlModel.getField("NBall")).getReal(0).intValue();
			NInitCell 	= ((MLDouble)mlModel.getField("NInitCell")).getReal(0).intValue();
			NType 		= ((MLDouble)mlModel.getField("NType")).getReal(0).intValue();
			randomSeed 	= ((MLDouble)mlModel.getField("randomSeed")).getReal(0).intValue();
			rho_m 		= ((MLDouble)mlModel.getField("rho_m")).getReal(0);
			rho_w 		= ((MLDouble)mlModel.getField("rho_w")).getReal(0);
			// cellArray
			int NCell = ((MLStructure)mlModel.getField("cellArray")).getSize();
			cellArray	 	= new ArrayList<CCell>(NCell);
			MLStructure mlCellArray = (MLStructure)mlModel.getField("cellArray");
			for(int iCell=0; iCell<NCell; iCell++) {
				CCell pCell = new CCell();
				// ballArray
				int NBall 		= ((MLDouble)mlCellArray.getField("type", iCell)).getReal(0).intValue() == 0 ? 1 : 2;
				MLStructure mlBallArray = (MLStructure) mlCellArray.getField("ballArray", iCell);
				pCell.cellArrayIndex = ((MLDouble)mlCellArray.getField("cellArrayIndex", iCell)).getReal(0).intValue()-1;
				pCell.colour	= new double[]{((MLDouble)mlCellArray.getField("colour", iCell)).getReal(0),
									((MLDouble)mlCellArray.getField("colour", iCell)).getReal(1),
									((MLDouble)mlCellArray.getField("colour", iCell)).getReal(2)};
				pCell.filament 	= ((MLDouble)mlCellArray.getField("filament", iCell)).getReal(0)==1 ? true : false;
				pCell.pModel	= this;
				pCell.type		= ((MLDouble)mlCellArray.getField("type", iCell)).getReal(0).intValue();
				if(mlCellArray.getField("motherIndex", iCell).isEmpty()) {
					pCell.mother = null;
				} else {
					pCell.mother 	= cellArray.get(((MLDouble)mlCellArray.getField("motherIndex", iCell)).getReal(0).intValue()-1);					
				}
				for(int iBall=0; iBall < NBall; iBall++) {
					CBall pBall = new CBall();
					pBall.ballArrayIndex = ((MLDouble)mlBallArray.getField("ballArrayIndex")).getReal(0).intValue()-1;
					pBall.pCell = pCell;
					pBall.mass 	= ((MLDouble)mlBallArray.getField("mass")).getReal(0);
					pBall.radius = pBall.Radius();
					pBall.pos 	= new Vector3d(
									((MLDouble)mlBallArray.getField("pos", iBall)).getReal(0),
									((MLDouble)mlBallArray.getField("pos", iBall)).getReal(1),
									((MLDouble)mlBallArray.getField("pos", iBall)).getReal(2));
					pBall.vel 	= new Vector3d(
									((MLDouble)mlBallArray.getField("vel", iBall)).getReal(0),
									((MLDouble)mlBallArray.getField("vel", iBall)).getReal(1),
									((MLDouble)mlBallArray.getField("vel", iBall)).getReal(2));
//					pBall.force = new Vector3d(
//									((MLDouble)mlBallArray.getField("force", iBall)).getReal(0),
//									((MLDouble)mlBallArray.getField("force", iBall)).getReal(1),
//									((MLDouble)mlBallArray.getField("force", iBall)).getReal(2));
					pBall.force = new Vector3d();
					// posSave and velSave
					int NSave = (int)(movementTimeEnd/movementTimeStep-1);
					pBall.posSave = new Vector3d[NSave];
					pBall.velSave = new Vector3d[NSave];
					for(int ii=0; ii<NSave; ii++) {
						pBall.posSave[ii] =  new Vector3d(
											((MLDouble)mlBallArray.getField("posSave", iBall)).getReal(ii,0),
											((MLDouble)mlBallArray.getField("posSave", iBall)).getReal(ii,1),
											((MLDouble)mlBallArray.getField("posSave", iBall)).getReal(ii,2));
						pBall.velSave[ii] =  new Vector3d(
											((MLDouble)mlBallArray.getField("velSave", iBall)).getReal(ii,0),
											((MLDouble)mlBallArray.getField("velSave", iBall)).getReal(ii,1),
											((MLDouble)mlBallArray.getField("velSave", iBall)).getReal(ii,2));

					}
					pCell.ballArray[iBall] = pBall;
				}
				// springArray
				CSpring pSpring = new CSpring();
				MLStructure mlSpringArray = (MLStructure)mlCellArray.getField("springArray", iCell);
				if(pCell.type>0) {
					for(int iBall=0; iBall<2; iBall++) pSpring.ballArray[iBall] = pCell.ballArray[iBall];	// replace by System.arraycopy? TODO
					pSpring.K 		= ((MLDouble)mlSpringArray.getField("K")).getReal(0);
					pSpring.restLength = ((MLDouble)mlSpringArray.getField("restLength")).getReal(0);
					pCell.springArray[0] = pSpring;
					// stickCellArray will be constructed based on stickSpringArray
				}
				cellArray.add(pCell);
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
				pAnchor.anchorArrayIndex = ((MLDouble)mlAnchorSpringArray.getField("anchorArrayIndex",iAnchor)).getReal(0).intValue()-1;
				pAnchor.K		= ((MLDouble)mlAnchorSpringArray.getField("K",iAnchor)).getReal(0);
				int iCell 		= ((MLDouble)mlAnchorSpringArray.getField("cellArrayIndex",iAnchor)).getReal(0).intValue()-1;
				int iBall		= ((MLDouble)mlAnchorSpringArray.getField("ballArrayIndex",iAnchor)).getReal(0).intValue()-1;
				pAnchor.pBall 	= cellArray.get(iCell).ballArray[iBall];
				pAnchor.restLength = ((MLDouble)mlAnchorSpringArray.getField("restLength",iAnchor)).getReal(0);
				anchorSpringArray.add(pAnchor);
			}
			for(int iAnchor=0; iAnchor<NAnchor; iAnchor++) {	// Additional for loop to assign siblings
				CAnchorSpring pAnchor = anchorSpringArray.get(iAnchor);
				if(pAnchor.pBall.pCell.type!=0) {
					int iSibling = ((MLDouble)mlAnchorSpringArray.getField("siblingArrayIndex", iAnchor)).getReal(0).intValue()-1;
					pAnchor.siblingArray[0] = anchorSpringArray.get(iSibling); 
				}
			}
			// filSpringArray
			MLStructure mlFilSpringArray = (MLStructure)mlModel.getField("filSpringArray");
			int NFil = mlFilSpringArray.getSize();
			filSpringArray = new ArrayList<CFilSpring>(NFil);
			for(int iFil=0; iFil<NFil; iFil++) {
				CFilSpring pFil = new CFilSpring();
				// bigSpring
				CSpring bigSpring = new CSpring();
				MLStructure mlBigSpring = (MLStructure)mlFilSpringArray.getField("bigSpring",iFil);
				bigSpring.K = ((MLDouble)mlBigSpring.getField("K")).getReal(0);
				bigSpring.restLength = ((MLDouble)mlBigSpring.getField("restLength")).getReal(0);
				for(int iBall=0; iBall<2; iBall++) {
					int jCell = ((MLDouble)mlBigSpring.getField("cellArrayIndex")).getReal(iBall).intValue()-1;
					int jBall = ((MLDouble)mlBigSpring.getField("ballArrayIndex")).getReal(iBall).intValue()-1;
					bigSpring.ballArray[0] = cellArray.get(jCell).ballArray[jBall];	
				}
				pFil.bigSpring = bigSpring;
				// smallSpring
				CSpring smallSpring = new CSpring();
				MLStructure mlSmallSpring = (MLStructure)mlFilSpringArray.getField("smallSpring",iFil);
				smallSpring.K = ((MLDouble)mlSmallSpring.getField("K")).getReal(0);
				smallSpring.restLength = ((MLDouble)mlSmallSpring.getField("restLength")).getReal(0);
				for(int iBall=0; iBall<2; iBall++) {
					int jCell = ((MLDouble)mlSmallSpring.getField("cellArrayIndex")).getReal(iBall).intValue()-1;
					int jBall = ((MLDouble)mlSmallSpring.getField("ballArrayIndex")).getReal(iBall).intValue()-1;
					smallSpring.ballArray[0] = cellArray.get(jCell).ballArray[jBall];	
				}
				pFil.smallSpring = smallSpring;
			}
			// stickSpringArray
			MLStructure mlStickSpringArray = (MLStructure)mlModel.getField("stickSpringArray");
			int NStick = mlStickSpringArray.getSize();
			stickSpringArray = new ArrayList<CStickSpring>(NStick);
			for(int iStick=0; iStick<NStick; iStick++) {
				CStickSpring pStick = new CStickSpring();
				// ballArray
				for(int iBall=0; iBall<2; iBall++) {
					int jCell = ((MLDouble)mlStickSpringArray.getField("cellArrayIndex", iStick)).getReal(iBall).intValue()-1;
					int jBall = ((MLDouble)mlStickSpringArray.getField("ballArrayIndex", iStick)).getReal(iBall).intValue()-1;
					pStick.ballArray[iBall] = cellArray.get(jCell).ballArray[jBall];
				}
				pStick.K 		= ((MLDouble)mlStickSpringArray.getField("K", iStick)).getReal(0);
				pStick.restLength = ((MLDouble)mlStickSpringArray.getField("restLength", iStick)).getReal(0);
				pStick.stickArrayIndex = ((MLDouble)mlStickSpringArray.getField("stickArrayIndex", iStick)).getReal(0).intValue()-1;
				stickSpringArray.add(pStick);
			}
			for(int iStick=0; iStick<NStick; iStick++) {			// construct stickSpring's siblingSpringArray
				CStickSpring pStick = stickSpringArray.get(iStick);
				if(pStick.ballArray[0].pCell.type != 0 || pStick.ballArray[1].pCell.type != 0) {		// at least one rod 
					if(pStick.ballArray[0].pCell.type == 0 || pStick.ballArray[1].pCell.type == 0) {	// exactly one rod, one sphere --> 1 sibling
						pStick.siblingArray = new CStickSpring[]{	stickSpringArray.get(((MLDouble)mlStickSpringArray.getField("stickArrayIndex", 0)).getReal(0).intValue()-1)};
					} else {																			// two rods --> 3 siblings
						pStick.siblingArray = new CStickSpring[]{	stickSpringArray.get(((MLDouble)mlStickSpringArray.getField("stickArrayIndex", 0)).getReal(0).intValue()-1),
																	stickSpringArray.get(((MLDouble)mlStickSpringArray.getField("stickArrayIndex", 1)).getReal(0).intValue()-1),
																	stickSpringArray.get(((MLDouble)mlStickSpringArray.getField("stickArrayIndex", 2)).getReal(0).intValue()-1)};
					}
				}
			}
			// construct each cell's stickCellArray
			for(CCell pCell : cellArray) {
				ArrayList<CCell> stickCellArray = pCell.StickCellArray();
				pCell.stickCellArray = stickCellArray;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	///////////////////
	// POV-Ray stuff //
	///////////////////
	public void POV_Write() {
		String fileName = String.format("%s/output/pov.%04d.%04d.inc", name, movementIter,growthIter);
		// Make output folder if it doesn't exist already
		if(!(new File(name + "/output")).exists())	new File(name + "/output").mkdir();
		PrintWriter fid=null;
		try {
			// Remove inc file if it already exists
			if((new File(fileName)).exists()) new File(fileName).delete();
			// Write new inc file
			fid = new PrintWriter(new FileWriter(fileName,true));		// True is for append // Not platform independent TODO
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
		} catch(IOException E) {
			E.printStackTrace();
		} finally {
			fid.close();
			if((new File(fileName)).exists()) new File(fileName).delete();
		}

	}
	
	public void POV_Plot() {
		if(!(new File(name + "/image")).exists()) {
			new File(name + "/image").mkdir();
		}
		String input = "povray ../pov/tomas_persp_3D_java.pov +W1024 +H768 +K" + String.format("%04d",movementIter) + "." + String.format("%04d",growthIter) + " +O../" + name + "/image/pov_" + String.format("m%04dg%04d", movementIter, growthIter) + " +A -J";
		LinuxInteractor.executeCommand("cd " + name + " ; " + input + " ; cd ..", setting.waitForFinish,setting.echoCommand);		// 1st true == wait for process to finish, 2nd true == tell command
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