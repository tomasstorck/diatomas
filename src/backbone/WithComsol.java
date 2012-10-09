package backbone;

import java.util.ArrayList;

import comsol.*;
import cell.*;

import random.rand;

public class WithComsol {

	public static void Run(CModel model) throws Exception{
		// Change default parameters
		/////
//		model.L.y = 4e-7;
//		setting.POVScale = 1;
		/////
		model.randomSeed = 1;
		/////
		model.sticking = true;
		model.filament = false;
		model.gravity = false;
		model.anchoring = false;
		/////
//		model.Kan *= 100.0;
//		model.Kc *= 100.0;
//		model.Kd *= 100.0;
//		model.Kf *= 100.0;
//		model.Kr *= 100.0;
//		model.Ks *= 100.0;
//		model.Kw *= 100.0;
//		model.rhoX = 1020;
		/////
//		model.Kr *= 0.01;
		model.growthTimeStep = 24.0*3600.0;
		
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
			int[] NSpring = {0,0,0,0};
			while(overlap) {
				model.Movement();
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
		
		boolean overlap = false;
		
		// Start server and connect
		model.Write("Starting server and connecting model to localhost:" + Assistant.port, "iter");
//		Server.Stop(false);
		Server.Start(Assistant.port);
		Server.Connect(Assistant.port);
		
		
		while(true) {
			// Reset the random seed
			rand.Seed((model.randomSeed+1)*(model.growthIter+1)*(model.movementIter+1));			// + something because if growthIter == 0, randomSeed doesn't matter.

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
				for(CRodSpring rod : model.rodSpringArray) {
					rod.ResetRestLength();
				}
				for(CFilSpring fil : model.filSpringArray) 	{
//					fil.ResetSmall();
					fil.ResetBig();
				}
			}

			// Movement
			model.Write("Starting movement calculations","iter");
			int nstp = model.Movement();
			model.movementIter++;
			model.movementTime += model.movementTimeStep;
			model.Write("Movement finished in " + nstp + " solver steps","iter");
			model.Write("Anchor springs broken/formed: " + Assistant.NAnchorBreak + "/" + Assistant.NAnchorForm + ", net " + (Assistant.NAnchorForm-Assistant.NAnchorBreak) + ", total " + model.anchorSpringArray.size(), "iter");
			model.Write("Stick springs broken/formed: " + Assistant.NStickBreak + "/" + Assistant.NStickForm + ", net " + (Assistant.NStickForm-Assistant.NStickBreak) + ", total " + model.stickSpringArray.size(), "iter");
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
			if(Assistant.plot) {
				model.Write("Writing and rendering POV files","iter");
				model.POV_Write(Assistant.plotIntermediate);
				model.POV_Plot(Assistant.plotIntermediate); 
			}
			
			// And finally: save stuff
			model.Write("Saving model as .mat file", "iter");
			model.Save();
		}
	}
}