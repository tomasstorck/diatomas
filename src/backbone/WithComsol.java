package backbone;

import java.util.ArrayList;

import NR.Matrix;

import comsol.*;
import cell.*;

import random.rand;

public class WithComsol {

	//OUTDATED FIXME
	
	public static void Run(CModel model) throws Exception{
		// Change default parameters
		
		// Initialise random seed
		rand.Seed(model.randomSeed);

		{int attempts = 0;
		if(model.growthIter==0 && model.movementIter==0) {
			// Create initial cells, not overlapping
			boolean overlap = true; 
			while(overlap) {
				for(int iCell = 0; iCell < model.NInitCell; iCell++){
					@SuppressWarnings("unused") 
					CCell cell = new CCell(rand.Int(model.NXComp), 	// 0, 1 or 2 by default (specified number is exclusive)
							(0.2*(rand.Double()+0.4))*model.L.x, 		// Anywhere between 0.4*Lx and 0.6*Lx
							(0.2*(rand.Double()+0.4))*model.L.y, 		// Anywhere between 0.4*Ly and 0.6*Ly
							(0.2*(rand.Double()+0.4))*model.L.z,		// Anywhere between 0.4*Lz and 0.6*Lz
							true,										// With filament
							new double[]{rand.Double(), rand.Double(), rand.Double()},
							model);										// And a pointer to the model
				}
				attempts++;
				if(model.DetectCellCollision_Simple(1.0).isEmpty()) {		// TODO: Replace with a new mechanism, based on vector projections onto the rods
					overlap = false;
				} else {
					// Clear model
					model.cellArray.clear();	
					model.ballArray.clear();
					model.rodSpringArray.clear();
				}
			}

			model.Write(model.cellArray.size() + " initial non-overlapping cells created in " + attempts + " attempts","iter");
		}}

		while(true) {
			// Reset the random seed
			rand.Seed((model.randomSeed+1)*(model.growthIter+1));				// + something because if growthIter == 0, randomSeed doesn't matter.

			// Redistribute filament springs
			for(CFilSpring fil : model.filSpringArray) {
				fil.ResetSmall();
				fil.ResetBig();
			}
			
			// Break anchor springs
			// {} to make sure objects are destroyed when we're done (aka scope)
			{ArrayList<CAnchorSpring> breakArray = model.DetectAnchorBreak(0.8,1.2);	// Returns lonely anchors, without their siblings
			int counter = 0;
			for(CAnchorSpring pAnchor : breakArray) {
				counter += pAnchor.UnAnchor();
			}
			model.Write(counter + " anchor springs broken","iter");
			// Build anchor springs
			model.Write("Detecting cell-floor collisions","iter");
			ArrayList<CCell> collisionArray = model.DetectFloorCollision(1.0);		// Returns already anchored cells
			int NNewAnchor = model.BuildAnchor(collisionArray);
			model.Write(NNewAnchor + " anchor springs built","iter");}

			// Break stick springs
			{ArrayList<CStickSpring> breakArray = model.DetectStickBreak(0.8,1.2);		// Returns all springs that'll be broken. Should not contain any duplicates in the form of siblingsprings
			model.BreakStick(breakArray);
			model.Write(breakArray.size() + " cell pairs broken","iter");
			// Build stick springs
			model.Write("Detecting cell-cell collisions","iter");
			ArrayList<CCell> collisionArray = model.DetectCellCollision_Simple(1.0);	 // Note that this one returns already stuck and duplicate cells
			model.Write("Building new sticking springs","iter");
			int NNewStick = model.BuildStick(collisionArray);
			model.Write(NNewStick + " cell pairs sticked","iter");}				// Divided by two, as array is based on origin and other cell (see for loop)

			// Do COMSOL things
			model.Write("Calculating cell steady state concentrations (COMSOL)","iter");
//			// Start server and connect
//			model.Write("\tStarting server and connecting model to localhost:2036", "iter");
//			Server.Stop(false);
//			Server.Start();
//			Server.Connect();
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
				cell.q = comsol.GetParameter(cell, "r" + Integer.toString(cell.type));
			}
			// Clean up after ourselves 
			model.Write("\tCleaning model from server", "iter");
			comsol.RemoveModel();
			
			// Grow cells
			model.Write("Growing cells", "iter");
			int newCell = model.GrowthFlux();
			model.Write(newCell + " new cells grown, total " + model.cellArray.size() + " cells","iter");

			// Advance growth
			model.growthIter++;
			model.growthTime += model.growthTimeStep;

			// Movement
			model.Write("Starting movement calculations","iter");
			boolean overlap = true;
			while(overlap) {
				int nstp = model.Movement();
				model.movementIter += (int)(model.movementTimeStepEnd/model.movementTimeStep);
				model.movementTime += model.movementTimeStep;
				ArrayList<CCell> overlapCellArray = model.DetectCellCollision_Simple(1.0);
				model.Write("Movement finished in " + nstp + " solver steps","iter");
				if(!overlapCellArray.isEmpty()) {
					model.Write(overlapCellArray.size() + " overlapping cells detected","warning");
				} else {
					overlap = false;
				}
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