package comsol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import cell.*;
import com.comsol.model.*;
import com.comsol.model.util.*;
import com.comsol.util.exceptions.FlException;


public class Comsol {
	Model model;				// The COMSOL model
	
	int NSphere = 0;
	int NRod = 0;
	ArrayList<String> cellList = new ArrayList<String>();   
	
	public Comsol() {}
	
	//////////////////////////////////
	
	public void Initialise() throws FlException {
		// Create model, initialise geometry, mesh, study and physics
		ModelUtil.initStandalone(false);
		model = ModelUtil.create("Model");
	    model.modelPath("/home/tomas/Desktop");
	    model.modelNode().create("mod1");
	    model.geom().create("geom1", 3);
	    	    
	    // Make list with parameters
	    for(int ii=0; ii<CModel.NXComp; ii++) {		// Set acid dissociation constants, Ka[]
	    	model.param().set("K" + Integer.toString(ii), Double.toString(CModel.K[ii]));
	    }
	    for(int ii=0; ii<CModel.NAcidDiss; ii++) {		// Set acid dissociation constants, Ka[]
	    	model.param().set("Ka" + Integer.toString(ii), Double.toString(CModel.Ka[ii]));
	    }
	    for(int ii=0; ii<CModel.NdComp; ii++) {		// Set diffusion coefficients, D[]
	    	model.param().set("D" + Integer.toString(ii), Double.toString(CModel.D[ii]));
	    }
	    
	    // Make list with variables (simple functions)			// UPDATE
	    model.variable().create("var1");
	    model.variable("var1").model("mod1");
	    for(int ii=0; ii<CModel.NXComp; ii++) {
	    	model.variable("var1").set("q" + Integer.toString(ii), CModel.rateEquation[ii]);
	    }
	    
	    // Create mesh
	    model.mesh().create("mesh1", "geom1");
	    model.mesh("mesh1").automatic(true);
		model.mesh("mesh1").autoMeshSize(7);		// 4 == fine, 5 == normal, 7 == coarser		
		model.mesh("mesh1").run();	
		
		// Define physics
		String dString[][] = new String[1][CModel.NdComp];
		for(int ii=0; ii<CModel.NdComp; ii++) {
			dString[0][ii] = "d" + Integer.toString(ii);
		}
		model.physics()
        	 .create("chds", "DilutedSpecies", "geom1", dString);
	    model.study().create("std1");
	    model.study("std1").feature().create("stat", "Stationary");
	    model.geom("geom1").lengthUnit("m");			// metre. micrometre == "\u00b5m" 
	    model.geom("geom1").geomRep("comsol");			// Alternative is CAD
	    model.geom("geom1").repairTol(1.0E-10);			// Change default repair tolerance from 1.0E-6
	    model.geom("geom1").run();

	    // Disable convection, set diffusion coefficients and set initial concentrations
	    model.physics("chds").prop("Convection").set("Convection", 1, "0");				// Disable convection
	    for(int ii=0; ii<CModel.NdComp; ii++) {	 
	    	model.physics("chds").feature("cdm1")
	        	 .set("D_" + Integer.toString(ii), new String[]{"D" + Integer.toString(ii), "0", "0", "0", "D" + Integer.toString(ii), "0", "0", "0", "D" + Integer.toString(ii)});		// Set diffusion coefficients. Note underscore, so careful with further simplification
	    	model.physics("chds").feature("init1").set("d" + Integer.toString(ii), 1, Double.toString(CModel.BCConc[ii]));		// Set initial concentrations
	    }
	    
	    // Define ODE/DAE equations for acid dissociation
	    String CName[] = new String[CModel.NcComp];
	    String CfList[][] = new String[CModel.NcComp][1];
	    for(int ii=0; ii<CModel.NcComp; ii++) {
	    	CName[ii] = "c" + Integer.toString(ii);
	    	CfList[ii][0] = "0";
	    }
	    model.physics().create("dode", "DomainODE", "geom1");
	    model.physics("dode").feature().create("aleq1", "AlgebraicEquation", 3);
	    model.physics("dode").feature("aleq1").selection().all();
	    model.physics("dode").field("dimensionless")
         	.component(CName);
	    model.physics("dode").prop("Units").set("CustomSourceTermUnit", "1");
   		model.physics("dode").feature("dode1")
   			.set("f", CfList);
   		
		// Set initial values (TODO this code is not well automated and assumes only d0[0] is non-zero) 	// UPDATE
		model.physics("dode").feature("init1").set("c0", "sqrt(" + Double.toString(CModel.BCConc[0]) + "*Ka0)");
		model.physics("dode").feature("init1").set("c1", "1-sqrt(" + Double.toString(CModel.BCConc[0]) + "*Ka0)");
		model.physics("dode").feature("init1").set("c2", "sqrt(" + Double.toString(CModel.BCConc[0]) + "*Ka0)");
   		
   		// Set equations 		// UPDATE
   		model.physics("dode").feature("aleq1")
   			.set("f", CModel.pHEquation);

	    String[][] odef = new String[CModel.NcComp][1];
	    for(int iRow=0; iRow<CModel.NcComp; iRow++) {
	    	odef[iRow][0] = "0";
	    }	    
	    model.physics("dode").feature("dode1")						// Disable ODE stuff (1/2)
	         .set("f", odef);

	    // Create alternative solver (direct, PARDISO, fully coupled, 25 iterations allowed, adjusted tolerance)
	    model.sol().create("sol1");
	    model.sol("sol1").study("std1");
	    model.sol("sol1").attach("std1");
	    model.sol("sol1").feature().create("st1", "StudyStep");
	    model.sol("sol1").feature().create("v1", "Variables");
	    model.sol("sol1").feature().create("s1", "Stationary");
	    model.sol("sol1").feature("s1").feature().create("fc1", "FullyCoupled");		// Create fully coupled solver
	    model.sol("sol1").feature("s1").feature().remove("fcDef");
	    model.sol("sol1").feature("st1").name("Compile Equations: Stationary");
	    model.sol("sol1").feature("st1").set("studystep", "stat");
	    model.sol("sol1").feature("v1").set("control", "stat");
	    model.sol("sol1").feature("s1").set("control", "stat");
	    model.sol("sol1").feature("s1").set("stol", "1e-6");							// Define relative tolerance
	    model.sol("sol1").feature("s1").feature("dDef")									// Set PARDISO as solver
	         .set("linsolver", "pardiso");
	    model.sol("sol1").feature("s1").feature("fc1").set("maxiter", "25");			// Set maximum iterations to 25 (default)
    }
	
	public void CreateSphere(CCell cell) {
		// Pure geometry
		String name = "sph" + Integer.toString(cell.Index());
	    model.geom("geom1").feature().create(name, "Sphere");
	    model.geom("geom1").feature(name).set("r", Double.toString(cell.ballArray[0].radius*0.9));			// FIXME Note radiusFactor
	    model.geom("geom1").feature(name).set("createselection", "on");		// Allows us to add something by selection name
	    model.geom("geom1").feature(name).set("pos", new String[]{Double.toString(cell.ballArray[0].pos.x), Double.toString(cell.ballArray[0].pos.y), Double.toString(cell.ballArray[0].pos.z)});

	    // Update the model information
	    NSphere++;
	    cellList.add(name);
	}
	
	public void CreateRod(CCell cell) {
		// Create points for constructing WP
		String pointName = "pt" + Integer.toString(3*cell.Index());
	    model.geom("geom1").feature().create(pointName, "Point");
	    model.geom("geom1").feature(pointName).set("p", new String[][]{{Double.toString(cell.ballArray[0].pos.x)},{Double.toString(cell.ballArray[0].pos.y)},{Double.toString(cell.ballArray[0].pos.z)}});
	    pointName = "pt" + Integer.toString(3*cell.Index()+1);
	    model.geom("geom1").feature().create(pointName, "Point");
	    model.geom("geom1").feature(pointName).set("p", new String[][]{{Double.toString(cell.ballArray[1].pos.x)},{Double.toString(cell.ballArray[1].pos.y)},{Double.toString(cell.ballArray[1].pos.z)}});
	    pointName = "pt" + Integer.toString(3*cell.Index()+2);
	    model.geom("geom1").feature().create(pointName, "Point");
	    model.geom("geom1").feature(pointName).set("p", new String[][]{{Double.toString(cell.ballArray[1].pos.x)},{Double.toString(cell.ballArray[1].pos.y)},{Double.toString(cell.ballArray[1].pos.z+cell.ballArray[0].radius*0.9)}});
	    
	    // Create WP
	    String wpName = "wp" + Integer.toString(cell.Index());
	    model.geom("geom1").feature().create(wpName, "WorkPlane");
	    model.geom("geom1").feature(wpName).set("planetype", "vertices");
	    model.geom("geom1").feature(wpName).selection("vertex1")
	         .set("pt" + Integer.toString(3*cell.Index()) + "(1)", new int[]{1});
	    model.geom("geom1").feature(wpName).selection("vertex2")
	         .set("pt" + Integer.toString(3*cell.Index()+1) + "(1)", new int[]{1});
	    model.geom("geom1").feature(wpName).selection("vertex3")
	         .set("pt" + Integer.toString(3*cell.Index()+2) + "(1)", new int[]{1});
	    
	    // Create rectangle in WP
	    String rectName = "rect" + Integer.toString(cell.Index());
	    double cellWidth = (cell.ballArray[1].pos.minus(cell.ballArray[0].pos)).length() + 2*cell.ballArray[0].radius*0.9;
	    model.geom("geom1").feature(wpName).geom().feature()
	         .create(rectName, "Rectangle");
	    model.geom("geom1").feature(wpName).geom().feature(rectName)
	         .setIndex("size", Double.toString(cellWidth), 0);
	    model.geom("geom1").feature(wpName).geom().feature(rectName)
	         .setIndex("size", Double.toString(cell.ballArray[0].radius*0.9), 1);			// We're revolving --> half the actual height
	    model.geom("geom1").feature(wpName).geom().feature(rectName)
	         .setIndex("pos", Double.toString(-cell.ballArray[0].radius*0.9), 0);			// Move the cell on the x axis, so that the centre of the ball is aligned with the origin 

	    // Fillet the rectangle
	    String filName = "fil" + Integer.toString(cell.Index());
	    model.geom("geom1").feature(wpName).geom().feature()
	         .create(filName, "Fillet");
	    model.geom("geom1").feature(wpName).geom().feature(filName)
	         .selection("point").set(rectName + "(1)", new int[]{3, 4});
	    model.geom("geom1").feature(wpName).geom().feature(filName)
	         .set("radius", Double.toString(cell.ballArray[0].radius*0.9));

	    // Revolve WP around X axis
	    String name = "rod" + Integer.toString(cell.Index());
	    model.geom("geom1").feature().create(name, "Revolve");
	    model.geom("geom1").feature(name).set("angtype", "full");
	    model.geom("geom1").feature(name).setIndex("axis", "1", 0); 	// Revolve around axis x==1
	    model.geom("geom1").feature(name).setIndex("axis", "0", 1);		// ... and y==0
	    model.geom("geom1").feature(name).selection("input")
        	 .set(new String[]{wpName});
	    model.geom("geom1").feature(name).set("createselection", "on");	// Make sure we can select this object later on
	
	    // Update model information
	    NRod++;
	    cellList.add(name);
	}
	
	public void CreateBCBox() {
		double BCMultiplier = 3.0;
		
	    model.geom("geom1").feature().create("blk1", "Block");

	    // Find extremes
	    double minX = 10;
	    double maxX = 0;
	    double minY = 10;
	    double maxY = 0;
	    double minZ = 10;
	    double maxZ = 0;
	    for(CBall ball : CModel.ballArray) {
	    	if(ball.pos.x < minX) 	minX = ball.pos.x - ball.radius;		// Using radius because initially balls might be in the same plane
	    	if(ball.pos.x > maxX) 	maxX = ball.pos.x + ball.radius;
	    	if(ball.pos.y < minY) 	minY = ball.pos.y - ball.radius;
	    	if(ball.pos.y > maxY) 	maxY = ball.pos.y + ball.radius;
	    	if(ball.pos.z < minZ) 	minZ = ball.pos.z - ball.radius;
	    	if(ball.pos.z > maxZ) 	maxZ = ball.pos.z + ball.radius;
	    }
	    model.geom("geom1").feature("blk1").setIndex("pos", "(" + Double.toString(maxX) + "+" + Double.toString(minX) + ")" + "/2.0", 0);
	    model.geom("geom1").feature("blk1").setIndex("pos", "(" + Double.toString(maxY) + "+" + Double.toString(minY) + ")" + "/2.0", 1);
	    model.geom("geom1").feature("blk1").setIndex("pos", "(" + Double.toString(maxZ) + "+" + Double.toString(minZ) + ")" + "/2.0", 2);
	    
//	    // Additional code to take the max of the max (i.e. box of Lmax x Lmax x Lmax instead of Lxmax x Lymax x Lzmax), remove if you want to undo
//	    if(maxX - minX < maxY - minY)		minX = minY; maxX = maxY;
//	    if(maxX - minX < maxZ - minZ)		minX = minZ; maxX = maxZ;
//	    if(maxY - minY < maxX - minX)		minY = minX; maxY = maxX;
//	    if(maxY - minY < maxZ - minZ)		minY = minZ; maxY = maxZ;
//	    if(maxZ - minZ < maxX - minX)		minZ = minX; maxZ = maxX;
//	    if(maxZ - minZ < maxY - minY)		minZ = minY; maxZ = maxY;
	    
	    model.geom("geom1").feature("blk1").setIndex("size", Double.toString(BCMultiplier) + "*(" + Double.toString(maxX) + "-" + Double.toString(minX) + ") + 10.0e-6", 0);
	    model.geom("geom1").feature("blk1").setIndex("size", Double.toString(BCMultiplier) + "*(" + Double.toString(maxY) + "-" + Double.toString(minY) + ") + 10.0e-6", 1);
	    model.geom("geom1").feature("blk1").setIndex("size", Double.toString(BCMultiplier) + "*(" + Double.toString(maxZ) + "-" + Double.toString(minZ) + ") + 10.0e-6", 2);
	    model.geom("geom1").feature("blk1").set("base", "center");
	    model.geom("geom1").feature("blk1").set("createselection", "on");

	    model.physics("chds").feature().create("conc1", "Concentration", 2);
	    model.physics("chds").feature("conc1").selection()
	         .named("geom1_blk1_bnd");
	    for(int ii=0; ii<CModel.NdComp; ii++) {		// -NType because biomass is not modelled in COMSOL
	    	model.physics("chds").feature("conc1").set("species", ii+1, "1");
	    	model.physics("chds").feature("conc1").set("c0", ii+1, Double.toString(CModel.BCConc[ii]));
	    }
	    
	    // Subtract cells from this block
	    String[] cellArray = Arrays.copyOf(cellList.toArray(), cellList.size(), String[].class);	// Convert cellList from ArrayList<String> to String[]. Typecast doesn't work for some reason
	    model.geom("geom1").feature().create("dif1", "Difference");		// Subtract the cells from the block:
	    model.geom("geom1").feature("dif1").selection("input")			// Add this block
	         .set(new String[]{"blk1"});
	    model.geom("geom1").feature("dif1").selection("input2")			// Subtract all cells from cellList
	    	 .set(cellArray);	         //.set(new String[]{"sph0","sph2","sph6"});
	    model.geom("geom1").feature("dif1").set("createselection", "on");
//	    model.geom("geom1").run("dif1");
	}
	
	public void BuildGeometry() {
		model.geom("geom1").run();
	}
	
	//////////////////////////////////
	
	public void SetFlux(CCell cell) {
//		double[] rate = cell.GetRate();
		String name;
		String flName;
		
		if(cell.type<2) {
			name = "sph" + Integer.toString(cell.Index());
		} else {
			name = "rod" + Integer.toString(cell.Index());
		}
		
		flName = "fl" + Integer.toString(cell.Index());
		model.physics("chds").feature().create(flName, "Fluxes", 2);
	    model.physics("chds").feature(flName).selection().named("geom1_" + name + "_bnd");
	    for(int ii=0; ii<CModel.NdComp; ii++) {
	    	if(CModel.SMdiffusion[cell.type][ii]!=0.0) {
	    		model.physics("chds").feature(flName).set("species", ii+1, "1");
		    	model.physics("chds").feature(flName).set("N0", ii+1, "q" + Integer.toString(cell.type) + " * " + Double.toString(cell.GetAmount()) + "/" + Double.toString(cell.SurfaceArea()) + " * " + CModel.SMdiffusion[cell.type][ii]);	
	    	}
	    }
	}
	
	//////////////////////////////////

	public double GetParameter(CCell cell, String parameter) {
		return GetParameter(cell, parameter, parameter);
	}
	
	public double GetParameter(CCell cell, String parameter, String name) {
		String avName = "av" + Integer.toString(cell.Index()) + "_" + name;											// e.g. av0_c0
		String cellName = (cell.type<2 ? "sph" : "rod") + cell.Index();							// We named it either sphere or rod + the cell's number  
		model.result().numerical().create(avName,"AvSurface");										// Determine the average surface value...
		model.result().numerical(avName).selection().named("geom1_" + cellName + "_bnd");			// ... of the cell's area's... (if a selection was made, this last part allows us to select its boundaries)
		model.result().numerical(avName).set("expr", parameter);									// ... parameter (e.g. concentration 1, "c0") 
		return model.result().numerical(avName).getReal()[0][0];									// Return the value's [0][0] (getReal returns a double[][])
	}
	
	//////////////////////////////////
	
	public void Run() {
		model.sol("sol1").runAll();
	}
	
	public void Save() throws IOException {
		model.save(System.getProperty("user.dir") + "/" + CModel.name + "/output/" + String.format("g%04dm%04d", CModel.growthIter, CModel.movementIter));		// No 2nd arguments --> save as .mph
	}
	
	//////////////////////////////////
	
	public void RemoveModel() {
		ModelUtil.remove("model1");
	}
}
