package comsol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import NR.Vector3d;
import cell.*;
import com.comsol.model.*;
import com.comsol.model.util.*;
import com.comsol.util.exceptions.FlException;

public class Comsol {
	Model comsol;				// The COMSOL model
	CModel java;
	
	final double dimensionFactor = 0.75;
	
	int NSphere = 0;
	int NRod = 0;
	ArrayList<String> cellList = new ArrayList<String>();   
	
	// Settings for model
	static int meshSize = 8;
	
	public Comsol(CModel java) {
		this.java = java;  
	}
	
	//////////////////////////////////
	
	public void Initialise() throws FlException {
		// Create model, initialise geometry, mesh, study and physics
		ModelUtil.initStandalone(false);
		ModelUtil.showProgress(false);								// enabling this causes COMSOL to run something SWT/graphical --> crash
		ModelUtil.showProgress(java.name + "/logfile_comsol.txt");
		comsol = ModelUtil.create("Model");
	    comsol.modelPath("/home/tomas/Desktop");
	    comsol.modelNode().create("mod1");
	    comsol.geom().create("geom1", 3);
	    	    
	    // Make list with parameters
	    for(int ii=0; ii<java.NXComp; ii++) {		// Set Monod constants, K[]
	    	comsol.param().set("K" + Integer.toString(ii), Double.toString(java.K[ii]));
	    }
	    for(int ii=0; ii<java.NAcidDiss; ii++) {		// Set acid dissociation constants, Ka[]
	    	comsol.param().set("Ka" + Integer.toString(ii), Double.toString(java.Ka[ii]));
	    }
	    for(int ii=0; ii<java.NdComp; ii++) {		// Set diffusion coefficients, D[]
	    	comsol.param().set("D" + Integer.toString(ii), Double.toString(java.D[ii]));
	    }
	    
	    // Make list with variables (simple functions)			// UPDATE
	    comsol.variable().create("var1");
	    comsol.variable("var1").model("mod1");
	    for(int ii=0; ii<java.NXComp; ii++) {
	    	comsol.variable("var1").set("q" + Integer.toString(ii), java.rateEquation[ii]);
	    }
	    
	    // Create mesh
	    comsol.mesh().create("mesh1", "geom1");
	    comsol.mesh("mesh1").automatic(true);
		comsol.mesh("mesh1").autoMeshSize(meshSize);		// 4 == fine, 5 == normal, 7 == coarser, 9 == extremely coarse (max)
		comsol.mesh("mesh1").run();	
		
		// Define physics
		String dString[][] = new String[1][java.NdComp];
		for(int ii=0; ii<java.NdComp; ii++) {
			dString[0][ii] = "d" + Integer.toString(ii);
		}
		comsol.physics()
        	 .create("chds", "DilutedSpecies", "geom1", dString);
	    comsol.study().create("std1");
	    comsol.study("std1").feature().create("stat", "Stationary");
	    comsol.geom("geom1").lengthUnit("m");			// metre. micrometre == "\u00b5m" 
	    comsol.geom("geom1").geomRep("comsol");			// Alternative is CAD
	    comsol.geom("geom1").repairTol(1.0E-10);			// Change default repair tolerance from 1.0E-6
	    comsol.geom("geom1").run();

	    // Disable convection, set diffusion coefficients and set initial concentrations
	    comsol.physics("chds").prop("Convection").set("Convection", 1, "0");				// Disable convection
	    for(int ii=0; ii<java.NdComp; ii++) {	 
	    	comsol.physics("chds").feature("cdm1")
	        	 .set("D_" + Integer.toString(ii), new String[]{"D" + Integer.toString(ii), "0", "0", "0", "D" + Integer.toString(ii), "0", "0", "0", "D" + Integer.toString(ii)});		// Set diffusion coefficients. Note underscore, so careful with further simplification
	    	comsol.physics("chds").feature("init1").set("d" + Integer.toString(ii), 1, Double.toString(java.BCConc[ii]));		// Set initial concentrations
	    }
	    
	    // Define ODE/DAE equations for acid dissociation
	    String CName[] = new String[java.NcComp];
	    String CfList[][] = new String[java.NcComp][1];
	    for(int ii=0; ii<java.NcComp; ii++) {
	    	CName[ii] = "c" + Integer.toString(ii);
	    	CfList[ii][0] = "0";
	    }
	    comsol.physics().create("dode", "DomainODE", "geom1");
	    comsol.physics("dode").feature().create("aleq1", "AlgebraicEquation", 3);
	    comsol.physics("dode").feature("aleq1").selection().all();
	    comsol.physics("dode").field("dimensionless")
         	.component(CName);
	    comsol.physics("dode").prop("Units").set("CustomSourceTermUnit", "1");
   		comsol.physics("dode").feature("dode1")
   			.set("f", CfList);
   		
		// Set initial values (TODO this code is not well automated and assumes only d0[0] is non-zero) 	// UPDATE
		comsol.physics("dode").feature("init1").set("c0", Double.toString(java.BCConc[0]) + "*sqrt(Ka0)");
		comsol.physics("dode").feature("init1").set("c1", Double.toString(java.BCConc[0]) + "*(1-sqrt(Ka0))");
		comsol.physics("dode").feature("init1").set("c2", Double.toString(java.BCConc[0]) + "*sqrt(Ka0)");
   		
   		// Set equations 		// UPDATE
   		comsol.physics("dode").feature("aleq1")
   			.set("f", java.pHEquation);

	    String[][] odef = new String[java.NcComp][1];
	    for(int iRow=0; iRow<java.NcComp; iRow++) {
	    	odef[iRow][0] = "0";
	    }	    
	    comsol.physics("dode").feature("dode1")						// Disable ODE stuff (1/2)
	         .set("f", odef);

	    // Create alternative solver (direct, PARDISO, fully coupled, 25 iterations allowed, adjusted tolerance)
	    comsol.sol().create("sol1");
	    comsol.sol("sol1").clearSolution();												// Added to make sure previous stuff is cleared
	    comsol.sol("sol1").study("std1");
	    comsol.sol("sol1").attach("std1");
	    comsol.sol("sol1").feature().create("st1", "StudyStep");
	    comsol.sol("sol1").feature().create("v1", "Variables");
	    comsol.sol("sol1").feature().create("s1", "Stationary");
	    comsol.sol("sol1").feature("s1").feature().create("fc1", "FullyCoupled");		// Create fully coupled solver
	    comsol.sol("sol1").feature("s1").feature().remove("fcDef");
	    comsol.sol("sol1").feature("st1").name("Compile Equations: Stationary");
	    comsol.sol("sol1").feature("st1").set("studystep", "stat");
	    comsol.sol("sol1").feature("v1").set("control", "stat");
	    comsol.sol("sol1").feature("s1").set("control", "stat");
	    comsol.sol("sol1").feature("s1").set("stol", "1e-6");							// Define relative tolerance
	    comsol.sol("sol1").feature("s1").feature("dDef")									// Set PARDISO as solver
	         .set("linsolver", "pardiso");
	    comsol.sol("sol1").feature("s1").feature("fc1").set("maxiter", "25");			// Set maximum iterations to 25 (default)
    }
	
	public void CreateSphere(CCell cell) throws FlException {
		// Pure geometry
		String name = "sph" + Integer.toString(cell.Index());
	    comsol.geom("geom1").feature().create(name, "Sphere");
	    comsol.geom("geom1").feature(name).set("r", Double.toString(cell.ballArray[0].radius*dimensionFactor));
	    comsol.geom("geom1").feature(name).set("createselection", "on");		// Allows us to add something by selection name
	    comsol.geom("geom1").feature(name).set("pos", new String[]{Double.toString(cell.ballArray[0].pos.x), Double.toString(cell.ballArray[0].pos.y), Double.toString(cell.ballArray[0].pos.z)});

	    // Update the model information
	    NSphere++;
	    cellList.add(name);
	}
	
	public void CreateRod(CCell cell) throws FlException {
		// Create points for constructing WP
		String pointName = "pt" + Integer.toString(3*cell.Index());
		double cellHT = ( (cell.ballArray[1].pos.minus(cell.ballArray[0].pos)).length() + 2.0*cell.ballArray[0].radius )*dimensionFactor;		// HT = Head-Tail
		Vector3d pos0 = cell.ballArray[0].pos.plus(cell.ballArray[1].pos.minus(cell.ballArray[0].pos).times((1.0-dimensionFactor)*0.5));
		Vector3d pos1 = cell.ballArray[1].pos.minus(cell.ballArray[1].pos.minus(cell.ballArray[0].pos).times((1.0-dimensionFactor)*0.5));
		
	    comsol.geom("geom1").feature().create(pointName, "Point");
	    comsol.geom("geom1").feature(pointName).set("p", new String[][]{{Double.toString(pos0.x)},{Double.toString(pos0.y)},{Double.toString(pos0.z)}});
	    pointName = "pt" + Integer.toString(3*cell.Index()+1);
	    comsol.geom("geom1").feature().create(pointName, "Point");
	    comsol.geom("geom1").feature(pointName).set("p", new String[][]{{Double.toString(pos1.x)},{Double.toString(pos1.y)},{Double.toString(pos1.z)}});
	    pointName = "pt" + Integer.toString(3*cell.Index()+2);
	    comsol.geom("geom1").feature().create(pointName, "Point");
	    comsol.geom("geom1").feature(pointName).set("p", new String[][]{{Double.toString(pos1.x)},{Double.toString(pos1.y)},{Double.toString(pos1.z+cell.ballArray[0].radius*dimensionFactor)}});
	    
	    // Create WP
	    String wpName = "wp" + Integer.toString(cell.Index());
	    comsol.geom("geom1").feature().create(wpName, "WorkPlane");
	    comsol.geom("geom1").feature(wpName).set("planetype", "vertices");
	    comsol.geom("geom1").feature(wpName).selection("vertex1")
	         .set("pt" + Integer.toString(3*cell.Index()) + "(1)", new int[]{1});
	    comsol.geom("geom1").feature(wpName).selection("vertex2")
	         .set("pt" + Integer.toString(3*cell.Index()+1) + "(1)", new int[]{1});
	    comsol.geom("geom1").feature(wpName).selection("vertex3")
	         .set("pt" + Integer.toString(3*cell.Index()+2) + "(1)", new int[]{1});
	    
	    // Create rectangle in WP
	    String rectName = "rect" + Integer.toString(cell.Index());
	    comsol.geom("geom1").feature(wpName).geom().feature()
	         .create(rectName, "Rectangle");
	    comsol.geom("geom1").feature(wpName).geom().feature(rectName)
	         .setIndex("size", Double.toString(cellHT), 0);
	    comsol.geom("geom1").feature(wpName).geom().feature(rectName)
	         .setIndex("size", Double.toString(cell.ballArray[0].radius*dimensionFactor), 1);			// We're revolving --> half the actual height
	    comsol.geom("geom1").feature(wpName).geom().feature(rectName)
	         .setIndex("pos", Double.toString(-cell.ballArray[0].radius*dimensionFactor), 0);			// Move the cell on the x axis, so that the centre of the ball is aligned with the origin 

	    // Fillet the rectangle
	    String filName = "fil" + Integer.toString(cell.Index());
	    comsol.geom("geom1").feature(wpName).geom().feature()
	         .create(filName, "Fillet");
	    comsol.geom("geom1").feature(wpName).geom().feature(filName)
	         .selection("point").set(rectName + "(1)", new int[]{3, 4});
	    comsol.geom("geom1").feature(wpName).geom().feature(filName)
	         .set("radius", Double.toString(cell.ballArray[0].radius*dimensionFactor));

	    // Revolve WP around X axis
	    String name = "rod" + Integer.toString(cell.Index());
	    comsol.geom("geom1").feature().create(name, "Revolve");
	    comsol.geom("geom1").feature(name).set("angtype", "full");
	    comsol.geom("geom1").feature(name).setIndex("axis", "1", 0); 	// Revolve around axis x==1
	    comsol.geom("geom1").feature(name).setIndex("axis", "0", 1);		// ... and y==0
	    comsol.geom("geom1").feature(name).selection("input")
        	 .set(new String[]{wpName});
	    comsol.geom("geom1").feature(name).set("createselection", "on");	// Make sure we can select this object later on
	
	    // Update model information
	    NRod++;
	    cellList.add(name);
	}
	
	public void CreateBCBox() throws FlException {
		double BCMultiplier = 3.0;
		
	    comsol.geom("geom1").feature().create("blk1", "Block");

	    // Find extremes
	    double minX = 10;
	    double maxX = 0;
	    double minY = 10;
	    double maxY = 0;
	    double minZ = 10;
	    double maxZ = 0;
	    for(CBall ball : java.ballArray) {
	    	if(ball.pos.x < minX) 	minX = ball.pos.x - ball.radius;		// Using radius because initially balls might be in the same plane
	    	if(ball.pos.x > maxX) 	maxX = ball.pos.x + ball.radius;
	    	if(ball.pos.y < minY) 	minY = ball.pos.y - ball.radius;
	    	if(ball.pos.y > maxY) 	maxY = ball.pos.y + ball.radius;
	    	if(ball.pos.z < minZ) 	minZ = ball.pos.z - ball.radius;
	    	if(ball.pos.z > maxZ) 	maxZ = ball.pos.z + ball.radius;
	    }
	    comsol.geom("geom1").feature("blk1").setIndex("pos", "(" + Double.toString(maxX) + "+" + Double.toString(minX) + ")" + "/2.0", 0);
	    comsol.geom("geom1").feature("blk1").setIndex("pos", "(" + Double.toString(maxY) + "+" + Double.toString(minY) + ")" + "/2.0", 1);
	    comsol.geom("geom1").feature("blk1").setIndex("pos", "(" + Double.toString(maxZ) + "+" + Double.toString(minZ) + ")" + "/2.0", 2);
	    
//	    // Additional code to take the max of the max (i.e. box of Lmax x Lmax x Lmax instead of Lxmax x Lymax x Lzmax), remove if you want to undo
//	    if(maxX - minX < maxY - minY)		minX = minY; maxX = maxY;
//	    if(maxX - minX < maxZ - minZ)		minX = minZ; maxX = maxZ;
//	    if(maxY - minY < maxX - minX)		minY = minX; maxY = maxX;
//	    if(maxY - minY < maxZ - minZ)		minY = minZ; maxY = maxZ;
//	    if(maxZ - minZ < maxX - minX)		minZ = minX; maxZ = maxX;
//	    if(maxZ - minZ < maxY - minY)		minZ = minY; maxZ = maxY;
	    
	    comsol.geom("geom1").feature("blk1").setIndex("size", Double.toString(BCMultiplier) + "*(" + Double.toString(maxX) + "-" + Double.toString(minX) + ") + 10.0e-6", 0);
	    comsol.geom("geom1").feature("blk1").setIndex("size", Double.toString(BCMultiplier) + "*(" + Double.toString(maxY) + "-" + Double.toString(minY) + ") + 10.0e-6", 1);
	    comsol.geom("geom1").feature("blk1").setIndex("size", Double.toString(BCMultiplier) + "*(" + Double.toString(maxZ) + "-" + Double.toString(minZ) + ") + 10.0e-6", 2);
	    comsol.geom("geom1").feature("blk1").set("base", "center");
	    comsol.geom("geom1").feature("blk1").set("createselection", "on");

	    comsol.physics("chds").feature().create("conc1", "Concentration", 2);
	    comsol.physics("chds").feature("conc1").selection()
	         .named("geom1_blk1_bnd");
	    for(int ii=0; ii<java.NdComp; ii++) {		// -NType because biomass is not modelled in COMSOL
	    	comsol.physics("chds").feature("conc1").set("species", ii+1, "1");
	    	comsol.physics("chds").feature("conc1").set("c0", ii+1, Double.toString(java.BCConc[ii]));
	    }
	    
	    // Subtract cells from this block
	    String[] cellArray = Arrays.copyOf(cellList.toArray(), cellList.size(), String[].class);	// Convert cellList from ArrayList<String> to String[]. Typecast doesn't work for some reason
	    comsol.geom("geom1").feature().create("dif1", "Difference");		// Subtract the cells from the block:
	    comsol.geom("geom1").feature("dif1").selection("input")			// Add this block
	         .set(new String[]{"blk1"});
	    comsol.geom("geom1").feature("dif1").selection("input2")			// Subtract all cells from cellList
	    	 .set(cellArray);	         //.set(new String[]{"sph0","sph2","sph6"});
	    comsol.geom("geom1").feature("dif1").set("createselection", "on");
//	    model.geom("geom1").run("dif1");
	}
	
	public void BuildGeometry() throws FlException {
		comsol.geom("geom1").run();
	}
	
	//////////////////////////////////
	
	public void SetFlux(CCell cell) throws FlException {
//		double[] rate = cell.GetRate();
		String name;
		String flName;
		
		if(cell.type<2) {
			name = "sph" + Integer.toString(cell.Index());
		} else {
			name = "rod" + Integer.toString(cell.Index());
		}
		
		flName = "fl" + Integer.toString(cell.Index());
		comsol.physics("chds").feature().create(flName, "Fluxes", 2);
	    comsol.physics("chds").feature(flName).selection().named("geom1_" + name + "_bnd");
	    for(int ii=0; ii<java.NdComp; ii++) {
	    	if(java.SMdiffusion[cell.type][ii]!=0.0) {
	    		comsol.physics("chds").feature(flName).set("species", ii+1, "1");
		    	comsol.physics("chds").feature(flName).set("N0", ii+1, "q" + Integer.toString(cell.type) + " * " + Double.toString(cell.GetAmount()) + "/" + Double.toString(cell.SurfaceArea()) + " * " + java.SMdiffusion[cell.type][ii]);	
	    	}
	    }
	}
	
	//////////////////////////////////

	public double GetParameter(CCell cell, String parameter) throws FlException {
		return GetParameter(cell, parameter, parameter);
	}
	
	public double GetParameter(CCell cell, String parameter, String name) throws FlException {
		String avName = "av" + Integer.toString(cell.Index()) + "_" + name;											// e.g. av0_c0
		String cellName = (cell.type<2 ? "sph" : "rod") + cell.Index();							// We named it either sphere or rod + the cell's number  
		comsol.result().numerical().create(avName,"AvSurface");										// Determine the average surface value...
		comsol.result().numerical(avName).selection().named("geom1_" + cellName + "_bnd");			// ... of the cell's area's... (if a selection was made, this last part allows us to select its boundaries)
		comsol.result().numerical(avName).set("expr", parameter);									// ... parameter (e.g. concentration 1, "c0") 
		return comsol.result().numerical(avName).getReal()[0][0];									// Return the value's [0][0] (getReal returns a double[][])
	}
	
	//////////////////////////////////
	
	public void Run(){																			// Includes detailed error handling using try/catch
		boolean solved = false;
		while(!solved) {
			try {
				comsol.sol("sol1").runAll();			
				// No problems? Continue then
				solved = true;
			} catch(FlException E) {
				String message = E.toString();
				if(message.contains("Out of memory LU factorization")) {
					java.Write("\tOut of memory during LU factorisation","warning");
					if(meshSize<9) {
						java.Write("\tIncreasing mesh size by 1 to " + ++meshSize + " and re-running", "iter");
						comsol.mesh("mesh1").autoMeshSize(meshSize);	// Add 1 to meshSize, enter that value		
						comsol.mesh("mesh1").run();					// Run mesh again
						continue;									// Try solving again
					} else {
						java.Write("\tCannot increase mesh size any further", "warning");
					}
				} else if(message.contains("Failed to respect boundary element edge on geometry face")) {
					java.Write("\tBoundary element edge meshing problem","warning");
					if(meshSize>1) {
						java.Write("Decreasing mesh size by 1 to " + --meshSize + " and re-running", "iter");
						comsol.mesh("mesh1").autoMeshSize(meshSize);	// Add 1 to meshSize, enter that value		
						comsol.mesh("mesh1").run();						// Run mesh again
						continue;
					} else 		java.Write("\tCannot increase mesh size any further", "warning");
				} else if(message.contains("Mean operator requires an adjacent domain of higher dimension")) {
					java.Write("\tMean operator domain size problem","warning");
					if(meshSize>1) {
						java.Write("Decreasing mesh size by 1 to " + --meshSize + " and re-running", "iter");
						comsol.mesh("mesh1").autoMeshSize(meshSize);	// Add 1 to meshSize, enter that value		
						comsol.mesh("mesh1").run();						// Run mesh again
						continue;
					} else 		java.Write("\tCannot increase mesh size any further", "warning");
				}
				// If we're still here, throw error
				java.Write(message,"");
				java.Write("Don't know how to deal with error above, exiting", "error");
				throw new FlException("Don't know how to deal with error above, exiting");
			}
		}
				
	}
	
	public void Save() throws IOException {
		comsol.save(System.getProperty("user.dir") + "/" + java.name + "/output/" + String.format("g%04dm%04d", java.growthIter, java.movementIter));		// No 2nd arguments --> save as .mph
	}
	
	//////////////////////////////////
	
	public void RemoveModel() {
		ModelUtil.remove("model1");
	}
}
