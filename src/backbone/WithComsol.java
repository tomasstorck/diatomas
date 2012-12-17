package backbone;

import java.util.ArrayList;

import comsol.*;
import cell.*;

import random.rand;
import ser2mat.ser2mat;

public class WithComsol {

	public static void Run(CModel model) throws Exception{
		// Change default parameters
		/////
		model.randomSeed = 4;		// Results in 7 rods, 8 spheres
//		model.cellType = new int[]{1,3};
//		// Cristian
//		model.Kan = 2e7;
//		model.Kc = 4e7;
//		model.Kd = 4e4;
//		model.Kf = 4e7;
//		model.Kr = 1.23e7;
//		model.Ks = 2e7;
//		model.Kw = 2e7;
		/////
		model.growthTimeStep = 8.0*3600.0;
		/////

		// Start server and connect
		model.Write("Starting server and connecting model to localhost:" + Assistant.port, "iter");
//		Server.Stop(false);
		Server.Start(Assistant.port);
		Server.Connect(Assistant.port);
		
		// Initialise random seed
		rand.Seed(model.randomSeed);

		// Create cells
		double[][] colour = new double[][]{
				{1.0,0.7,0.7},
				{0.1,1.0,0.1},
				{0.1,0.1,0.4},
				{1.0,1.0,0.7},
				{0.1,1.0,1.0},
				{0.4,0.1,0.4},
				{0.4,0.1,0.1},
				{0.4,1.0,0.4},
				{0.1,0.1,1.0},
				{0.4,0.4,0.1},
				{0.4,1.0,1.0},
				{1.0,0.1,1.0}};
				
		if(model.growthIter==0 && model.relaxationIter==0) {
			// Create initial cells, not overlapping
			for(int iCell = 0; iCell < model.NInitCell; iCell++){
				int type = rand.IntChoose(model.cellType);
				double n = model.nCellInit[type]+(model.nCellMax[type]-model.nCellInit[type])*rand.Double();
				CCell cell = new CCell(type, 						// Type of biomass
						n,											// Initial cell mass is random between initial and max
						(0.2*rand.Double()-0.1)*model.L.x, 			// Anywhere between -0.1*Lx and 0.1*Lx
						(0.2*rand.Double()+0.9)*model.L.y, 			// Anywhere between 0.9*Ly and 1.1*Ly
						(0.2*rand.Double()-0.1)*model.L.z,			// Anywhere between -0.1*Lz and 0.1*Lz
						model.filament,								// With filament?
						colour[iCell],
						model);
				// Set cell boundary concentration to initial value
				cell.q = 0.0;
				// Lower cell to the substratum if desired
				if(model.initialAtSubstratum) {
					for(CBall ball : cell.ballArray) 	ball.pos.y = ball.radius;
				}
			}
			boolean overlap = true;
			int[] NSpring = {0,0,0,0};
			while(overlap) {
				model.Relaxation();
				// We want to save the number of springs formed and broken
				NSpring[0] += Assistant.NAnchorBreak;
				NSpring[1] += Assistant.NAnchorForm;
				NSpring[2] += Assistant.NStickBreak;
				NSpring[3] += Assistant.NStickForm;
				if(model.DetectCellCollision_Simple(1.0).isEmpty()) 	overlap = false;
			}
			model.Write(model.cellArray.size() + " initial non-overlapping cells created","iter");
			model.Write((NSpring[1]-NSpring[0]) + " anchor and " + (NSpring[3]-NSpring[2]) + " sticking springs formed", "iter");
		}
		
		model.Save();
		ser2mat.Convert(model);
		
		boolean overlap = false;
		while(true) {
			// Reset the random seed
			rand.Seed((model.randomSeed+1)*(model.growthIter+1)*(model.relaxationIter+1));			// + something because if growthIter == 0, randomSeed doesn't matter.

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
				
				// Advance growth
				model.growthIter++;
				model.growthTime += model.growthTimeStep;

				model.Write(newCell + " new cells grown, total " + model.cellArray.size() + " cells","iter");

				model.Write("Resetting springs","iter");
				for(CSpring rod : model.rodSpringArray) {
					rod.ResetRestLength();
				}
				for(CSpring fil : model.filSpringArray) 	{
					fil.ResetRestLength();
				}
			}
			
			// Below code is only required if sticking/anchoring is not done in the ODE solver
//			if(model.anchoring) {
//				// Break anchor springs
//				// {} to make sure objects are destroyed when we're done (aka scope)
//				ArrayList<CAnchorSpring> breakArray = model.DetectAnchorBreak(0.6,1.4);	// Returns lonely anchors, without their siblings
//				int counter = 0;
//				for(CAnchorSpring anchor : breakArray) {
//					counter += anchor.UnAnchor();
//				}
//				model.Write(counter + " anchor springs broken","iter");
//				// Build anchor springs
//				model.Write("Detecting cell-floor collisions","iter");
//				ArrayList<CCell> collisionArray = model.DetectFloorCollision(1.1);		// Returns already anchored cells
//				int NNewAnchor = model.BuildAnchor(collisionArray);
//				model.Write(NNewAnchor + " anchor springs built","iter");
//			}

//			if(model.sticking) {
//				// Break stick springs
//				ArrayList<CStickSpring> breakArray = model.DetectStickBreak(0.6,1.4);		// Returns all springs that'll be broken (<rl*first argument, >rl*second argument). Should not contain any duplicates in the form of siblingsprings
//				model.BreakStick(breakArray);
//				model.Write(breakArray.size() + " sticking springs broken","iter");
//				// Build stick springs
//				model.Write("Detecting cell-cell collisions","iter");
//				ArrayList<CCell> collisionArray = model.DetectCellCollision_Simple(1.1);	 // Note that this one returns already stuck and duplicate cells
//				model.Write("Building new sticking springs","iter");
//				int NNewStick = model.BuildStick(collisionArray);
//				model.Write(NNewStick + " cell pairs sticked","iter");				// Divided by two, as array is based on origin and other cell (see for loop)
//			}
			
			// Relaxation
			model.Write("Starting relaxation calculations","iter");
			int nstp = model.Relaxation();
			model.relaxationIter++;
			model.relaxationTime += model.relaxationTimeStep;
			model.Write("Relaxation finished in " + nstp + " solver steps","iter");
			model.Write("Anchor springs broken/formed: " + Assistant.NAnchorBreak + "/" + Assistant.NAnchorForm + ", net " + (Assistant.NAnchorForm-Assistant.NAnchorBreak) + ", total " + model.anchorSpringArray.size(), "iter");
			model.Write("Filament springs broken: " + Assistant.NFilBreak + ", total " + model.filSpringArray.size(), "iter");
			model.Write("Stick springs broken/formed: " + Assistant.NStickBreak + "/" + Assistant.NStickForm + ", net " + (Assistant.NStickForm-Assistant.NStickBreak) + ", total " + model.stickSpringArray.size(), "iter");
			ArrayList<CCell> overlapCellArray = model.DetectCellCollision_Proper(1.0);
			if(!overlapCellArray.isEmpty()) {
				model.Write(overlapCellArray.size() + " overlapping cells detected, growth delayed","warning");
				String cellNumber = "" + overlapCellArray.get(0).Index();
				for(int ii=1; ii<overlapCellArray.size(); ii++) 	cellNumber += " & " + overlapCellArray.get(ii).Index();
				model.Write("Cell numbers " + cellNumber,"iter");
				overlap = true;
			} else {
				overlap = false;
			}

			// And finally: save stuff
			model.Write("Saving model as serialised file", "iter");
			model.Save();
			ser2mat.Convert(model);
		}
	}
}