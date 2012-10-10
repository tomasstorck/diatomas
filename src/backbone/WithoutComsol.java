package backbone;

import java.util.ArrayList;

import NR.Vector3d;

import cell.*;

import random.rand;

public class WithoutComsol {

	public static void Run() throws Exception{
		// Change default parameters
		/////
//		model.L.y = 4e-7;
//		setting.POVScale = 1;
		/////
		CModel.randomSeed = 4;		// Results in 7 rods, 8 spheres
		/////
		CModel.sticking = true;
//		CModel.filament = true;
//		CModel.gravity = true;
//		CModel.anchoring = true;
		/////
//		// Cristian
//		CModel.Kan = 2e7;
//		CModel.Kc = 4e7;
//		CModel.Kd = 4e4;
//		CModel.Kf = 4e7;
//		CModel.Kr = 1.23e7;
//		CModel.Ks = 2e7;
//		CModel.Kw = 2e7;
		/////
//		model.Kr *= 0.01;
		
		// Initialise random seed
		rand.Seed(CModel.randomSeed);

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
		if(CModel.growthIter==0 && CModel.movementIter==0) {
			// Create initial cells, not overlapping
			for(int iCell = 0; iCell < CModel.NInitCell; iCell++){
				int type = rand.IntChoose(CModel.cellType);
				double n = CModel.nCellInit[type]+(CModel.nCellMax[type]-CModel.nCellInit[type])*rand.Double();
				CCell cell = new CCell(type, 						// Type of biomass
						n,											// Initial cell mass is random between initial and max
						(0.2*(rand.Double()+0.4))*CModel.L.x, 		// Anywhere between 0.4*Lx and 0.6*Lx
						(0.2*(rand.Double()+0.4))*CModel.L.y, 		// Anywhere between 0.4*Ly and 0.6*Ly
						(0.2*(rand.Double()+0.4))*CModel.L.z,		// Anywhere between 0.4*Lz and 0.6*Lz
						CModel.filament,								// With filament?
						colour[iCell]);
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
				CModel.Movement();
				// We want to save the number of springs formed and broken
				NSpring[0] += Assistant.NAnchorBreak;
				NSpring[1] += Assistant.NAnchorForm;
				NSpring[2] += Assistant.NStickBreak;
				NSpring[3] += Assistant.NStickForm;
				if(CModel.DetectCellCollision_Simple(1.0).isEmpty()) 	overlap = false;
			}
			CModel.Write(CModel.cellArray.size() + " initial non-overlapping cells created","iter");
			CModel.Write((NSpring[1]-NSpring[0]) + " anchor and " + (NSpring[3]-NSpring[2]) + " sticking springs formed", "iter");
		}
		
		boolean overlap = false;
		
		while(true) {
			// Reset the random seed
			rand.Seed((CModel.randomSeed+1)*(CModel.growthIter+1)*(CModel.movementIter+1));			// + something because if growthIter == 0, randomSeed doesn't matter.

			// COMSOL was here
			
			// Grow cells
			if(!overlap) {
				CModel.Write("Growing cells", "iter");
				int newCell = CModel.GrowthSimple();
				
				// Advance growth
				CModel.growthIter++;
				CModel.growthTime += CModel.growthTimeStep;
				
				CModel.Write(newCell + " new cells grown, total " + CModel.cellArray.size() + " cells","iter");

				CModel.Write("Resetting springs","iter");
				for(CRodSpring rod : CModel.rodSpringArray) {
					rod.ResetRestLength();
				}
				for(CFilSpring fil : CModel.filSpringArray) 	{
//					fil.ResetSmall();
					fil.ResetBig();
				}
				
			}
			
			// Movement
			CModel.Write("Starting movement calculations","iter");
			int nstp = CModel.Movement();
			CModel.movementIter++;
			CModel.movementTime += CModel.movementTimeStep;
			CModel.Write("Movement finished in " + nstp + " solver steps","iter");
			CModel.Write("Anchor springs broken/formed: " + Assistant.NAnchorBreak + "/" + Assistant.NAnchorForm + ", net " + (Assistant.NAnchorForm-Assistant.NAnchorBreak) + ", total " + CModel.anchorSpringArray.size(), "iter");
			CModel.Write("Stick springs broken/formed: " + Assistant.NStickBreak + "/" + Assistant.NStickForm + ", net " + (Assistant.NStickForm-Assistant.NStickBreak) + ", total " + CModel.stickSpringArray.size(), "iter");
			ArrayList<CCell> overlapCellArray = CModel.DetectCellCollision_Simple(1.0);
			if(!overlapCellArray.isEmpty()) {
				CModel.Write(overlapCellArray.size() + " overlapping cells detected, growth delayed","warning");
				String cellNumber = "" + overlapCellArray.get(0).Index();
				for(int ii=1; ii<overlapCellArray.size(); ii++) 	cellNumber += " & " + overlapCellArray.get(ii).Index();
				CModel.Write("Cell numbers " + cellNumber,"iter");
				overlap = true;
			} else {
				overlap = false;
			}

			// Plot
			if(Assistant.plot) {
				CModel.Write("Writing and rendering POV files","iter");
				CModel.POV_Write(Assistant.plotIntermediate);
				CModel.POV_Plot(Assistant.plotIntermediate); 
			}
			
			// And finally: save stuff
			CModel.Write("Saving model as .mat file", "iter");
			CModel.Save();
		}
	}
}