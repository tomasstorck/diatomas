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

// Import NR stuff
import NR.*;

public class CModel {
	// Model properties
	String name;
	String pathOutput;
	String pathImage;
	// Arrays
	ArrayList<CCell> cellArray = new ArrayList<CCell>();
	ArrayList<CBall> ballArray = new ArrayList<CBall>();
	//springArray = CSpring.empty;
	ArrayList<CStickSpring> stickSpringArray = new ArrayList<CStickSpring>();
	//modelSpringArray = CModel.empty;
	//stickingIndexArray = [];
	ArrayList<CFilSpring> filSpringArray = new ArrayList<CFilSpring>();
	//ballArray = CBall.empty;
	ArrayList<CAnchorSpring> anchorSpringArray = new ArrayList<CAnchorSpring>();
	// Spring constants
	double K1 = 0.5e-2;			// Cell spring
	double Kf = 0.1e-4;			// filament spring
	double Kw = 0.5e-5;			// wall spring
	double Kc = 0.1e-4;			// collision
	double Ks = 0.5e-5;			// sticking
	double Ka = 0.5e-5;			// anchor
	double Kd = 1e-9;			// drag force coefficient
	// Domain properties
	double G		= -9.8;		// [m/s2], acceleration due to gravity
	double rho_w	= 1000;		// [kg/m3], density of bulk liquid (water)
	double rho_m	= 1100;		// [kg/m3], diatoma density
	Vector3d L = new Vector3d(1200e-6, 300e-6, 1200e-6);	// [m], Dimensions of domain
	int randomSeed= 1;
	// Cell properties
	int NType 		= 2;		// Types of cell
	int NInitCell	= 15;		// Initial number of cells
	double aspect	= 2;		// Aspect ratio of cells
	// Ball properties
	double MCellInit 		= 1e-11;	// kg
	double MCellMax			= 2e-11; 	// max mass of cells before division
	// Progress
	double growthTime		= 0;	// [s] Current time for the growth
	double growthTimeStep	= 900;	// [s] Time step for growth
	int  growthIter			= 0;	// [-] Counter time iterations for growth
	double growthMaxIter	= 672;	// [-] where we'll stop
	double movementTime		= 0;	// [s] initial time for movement (for ODE solver)
	double movementTimeStep	= 2e-2; // [s] output time step  for movement
	double movementTimeEnd	= 10e-2;// [s] time interval for movement (for ODE solver), 5*movementTimeStep by default
	int movementIter		= 0;	// [-] counter time iterations for movement
	// Counters
	static int NBall 		= 0;
	//		NBall; NCell; NSpring; NFilament; <--- disabled, more reliable and fast enough with length(), used in MEX though, reconstructed there for speed and to prevent errors

	//////////////////////////////////////////////////////////////////////////////////////////
	
	//////////////////
	// Constructors //
	//////////////////
	
	public CModel(String name) {
		this.name  = name;
		pathImage  = name + "/image";
		pathOutput = name + "/output";
		
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
	// TODO
	
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
	
	public ArrayList<CCell> DetectCellCollision_Simple() {				// Using ArrayList, no idea how big this one will get. For some stupid reason, it won't take int
		ArrayList<CCell> collisionCell = new ArrayList<CCell>();
		
		CBall[] ballArray = BallArray();
		for(int iBall=0; iBall<NBall; iBall++) {		// If we stick to indexing, it'll be easier to determine which cells don't need to be analysed
			CBall pBall = ballArray[iBall];
			for(int iBall2 = iBall+1; iBall2<NBall; iBall2++) {
				CBall pBall2 = ballArray[iBall2];
				if(pBall.pCell.cellArrayIndex!=pBall2.pCell.cellArrayIndex) {
					Vector3d diff = pBall2.pos.minus(pBall.pos);
					double distance = Math.abs(diff.length());
					if(distance - pBall.radius - pBall2.radius < 0) {
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
				breakArray.addAll(pSpring.siblingArray);
			}
			iSpring += pSpring.siblingArray.size()+1; 
		}
		return breakArray;
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
			// Syntrophic growth			// TODO: Horribly inefficient
			for(CStickSpring pSpring : stickSpringArray) {
				if(pSpring.ballArray[0].pCell.equals(pCell) || pSpring.ballArray[1].pCell.equals(pCell)) {
					// We found the cell
					if(pSpring.ballArray[0].pCell.type == pSpring.ballArray[1].pCell.type) {
						// The cell types are not different on the other end of the spring
						mass *= 1.2;
						break;	// That'll do, pig
					}
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
	
	public int BuildAnchor(ArrayList<CCell> collisionArray) {
		for(CAnchorSpring pSpring : anchorSpringArray) {collisionArray.remove(pSpring.pBall.pCell);}
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
		String input = "povray ../pov/tomas_persp_3D_java.pov +W1024 +H768 +K" + String.format("%04d",movementIter) + "." + String.format("%04d",growthIter) + " +O../" + pathImage + "/pov_" + String.format("m%04dg%04d", movementIter, growthIter) + " +A -J";
		LinuxInteractor.executeCommand("cd " + name + " ; " + input + " ; cd ..", boolWaitForFinish,boolEchoCommand);		// 1st true == wait for process to finish, 2nd true == tell command
	}
	
	public void POV_Plot() {
		POV_Plot(false,false);
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
}