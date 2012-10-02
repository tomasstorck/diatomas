package backbone;

import java.util.ArrayList;

import comsol.*;
import cell.*;

import random.rand;

public class WithComsol {

	//OUTDATED FIXME
	
	public static void Run(CModel model) throws Exception{
		// Change default parameters
//		model.cellType = new int[]{1,5};
		/////
//		model.L 	= new Vector3d(20e-6, 5e-6, 20e-6);		// [m], Dimensions of domain
//		setting.POVScale = 1;
		/////
		model.randomSeed = 1;
		/////
		model.sticking = true;
		model.filament = true;
		model.gravity = true;
		model.anchoring = true;
		/////
//		model.Ks = 10.0*model.Ks;
//		model.Kan = 1000.0*model.Kan;
//		model.rhoX = 1020;
//		model.Kd = 50.0*model.Kd;
//		model.Kr = 0.1*model.Kr;
		/////
		
		// Initialise random seed
		rand.Seed(model.randomSeed);

		// Create cells
		double[][] colour = new double[][]{
				{0.3,0.3,0.3},
				{0.3,0.3,1.0},
				{0.3,1.0,0.3},
				{0.3,1.0,1.0},
				{1.0,0.3,0.3},
				{1.0,0.3,1.0},
				{1.0,1.0,0.3},
				{1.0,1.0,1.0},
				{0.1,0.1,0.1},
				{0.1,0.1,0.4},
				{0.1,0.4,0.1},
				{0.1,0.4,0.4},
				{0.4,0.1,0.1},
				{0.4,0.1,0.4},
				{0.4,0.4,0.1}};
		if(model.growthIter==0 && model.movementIter==0) {
			// Create initial cells, not overlapping
			for(int iCell = 0; iCell < model.NInitCell; iCell++){
				CCell cell = new CCell(rand.IntChoose(model.cellType), 	// 0, 1 or 2 by default (specified number is exclusive)
						(0.2*(rand.Double()+0.4))*model.L.x, 		// Anywhere between 0.4*Lx and 0.6*Lx
						(0.2*(rand.Double()+0.4))*model.L.y, 		// Anywhere between 0.4*Ly and 0.6*Ly
						(0.2*(rand.Double()+0.4))*model.L.z,		// Anywhere between 0.4*Lz and 0.6*Lz
						model.filament,								// With filament?
						colour[iCell],
						model);										// And a pointer to the model
				// Set cell boundary concentration to initial value
				cell.q = 0.0;
//				for(int ii=0; ii<(cell.type<2?1:2); ii++) {
//					cell.ballArray[ii].pos.y=cell.ballArray[ii].radius;
//				}
//				cell.Anchor();
			}
			boolean overlap = true;
			while(overlap) {
				model.Movement();
				if(model.DetectCellCollision_Simple(1.0).isEmpty()) 	overlap = false;
			}
			model.Write(model.cellArray.size() + " initial non-overlapping cells created","iter");
		}
		
		boolean overlap = false;
		
		// Start server and connect
		model.Write("\tStarting server and connecting model to localhost:" + setting.port, "iter");
//		Server.Stop(false);
		Server.Start(setting.port);
		Server.Connect(setting.port);
		
		while(true) {
			// Reset the random seed
			rand.Seed((model.randomSeed+1)*(model.growthIter+1)*(model.movementIter+1));			// + something because if growthIter == 0, randomSeed doesn't matter.

			if(model.anchoring) {
				// Break anchor springs
				// {} to make sure objects are destroyed when we're done (aka scope)
				ArrayList<CAnchorSpring> breakArray = model.DetectAnchorBreak(0.6,1.4);	// Returns lonely anchors, without their siblings
				int counter = 0;
				for(CAnchorSpring anchor : breakArray) {
					counter += anchor.UnAnchor();
				}
				model.Write(counter + " anchor springs broken","iter");
				// Build anchor springs
				model.Write("Detecting cell-floor collisions","iter");
				ArrayList<CCell> collisionArray = model.DetectFloorCollision(1.1);		// Returns already anchored cells
				int NNewAnchor = model.BuildAnchor(collisionArray);
				model.Write(NNewAnchor + " anchor springs built","iter");
			}

			if(model.sticking) {
				// Break stick springs
				ArrayList<CStickSpring> breakArray = model.DetectStickBreak(0.6,1.4);		// Returns all springs that'll be broken (<rl*first argument, >rl*second argument). Should not contain any duplicates in the form of siblingsprings
				model.BreakStick(breakArray);
				model.Write(breakArray.size() + " cell pairs broken","iter");
				// Build stick springs
				model.Write("Detecting cell-cell collisions","iter");
				ArrayList<CCell> collisionArray = model.DetectCellCollision_Simple(1.1);	 // Note that this one returns already stuck and duplicate cells
				model.Write("Building new sticking springs","iter");
				int NNewStick = model.BuildStick(collisionArray);
				model.Write(NNewStick + " cell pairs sticked","iter");				// Divided by two, as array is based on origin and other cell (see for loop)
			}
			
			// Do COMSOL things
			model.Write("Calculating cell steady state concentrations (COMSOL)","iter");
			// Make the model
			Comsol comsol = new Comsol(model);
			model.Write("\tInitialising geometry", "iter");
			comsol.Initialise();
			model.Write("\tCreating cells", "iter");
			// Create cells in the COMSOL model
			for(CCell cell : model.cellArray) {
				if(cell.type<2) 	comsol.CreateSphere(cell);
				else				comsol.CreateRod(cell);
			}
			comsol.CreateBCBox();					// Create a large box where we set the "bulk" conditions
			comsol.BuildGeometry();
			// Set fluxes
			for(CCell cell : model.cellArray) {
				comsol.SetFlux(cell);
			}
			model.Write("\tSaving model", "iter");
			comsol.Save();							// Save .mph file
			// Calculate and extract the results
			model.Write("\tRunning model", "iter");
			comsol.Run();							// Run model to calculate concentrations
			model.Write("\tCalculating cell surface concentrations", "iter");
			for(CCell cell : model.cellArray) {
				cell.q = comsol.GetParameter(cell, "q" + Integer.toString(cell.type));
			}
			// Clean up after ourselves 
			model.Write("\tCleaning model from server", "iter");
			comsol.RemoveModel();

			// Grow cells
			if(!overlap) {
				model.Write("Growing cells", "iter");
				int newCell = model.GrowthFlux();
				model.Write(newCell + " new cells grown, total " + model.cellArray.size() + " cells","iter");

				model.Write("Resetting springs","iter");
				for(CRodSpring rod : model.rodSpringArray) {
					rod.ResetRestLength();
				}
				for(CFilSpring fil : model.filSpringArray) 	{
					//								fil.ResetSmall();
					fil.ResetBig();
				}
				// Advance growth
				model.growthIter++;
				model.growthTime += model.growthTimeStep;
			}

			// Movement
			model.Write("Starting movement calculations","iter");
			int nstp = model.Movement();
			model.movementIter++;
			model.movementTime += model.movementTimeStep;
			model.Write("Movement finished in " + nstp + " solver steps","iter");
			ArrayList<CCell> overlapCellArray = model.DetectCellCollision_Simple(1.0);
			if(!overlapCellArray.isEmpty()) {
				model.Write(overlapCellArray.size() + " overlapping cells detected, growth delayed","warning");
				String cellNumber = "" + overlapCellArray.get(0).Index();
				for(int ii=1; ii<overlapCellArray.size(); ii++) 	cellNumber += " & " + overlapCellArray.get(ii).Index();
				model.Write("Cell numbers " + cellNumber,"iter");
				overlap = true;
			} else {
				overlap = false;
			}

			// Plot
			if(setting.plot) {
				model.Write("Writing and rendering POV files","iter");
				model.POV_Write(setting.plotIntermediate);
				model.POV_Plot(setting.plotIntermediate); 
			}

			// And finally: save stuff
			model.Write("Saving model as .mat file", "iter");
			model.Save();
		}
//		Server.Stop();
	}
}