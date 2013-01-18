package backbone;

import java.util.ArrayList;

import NR.Vector3d;

import random.rand;
import ser2mat.ser2mat;
import cell.CBall;
import cell.CCell;
import cell.CModel;
import cell.CSpring;

public class WithoutComsol {

	public static void Run(CModel model) throws Exception{				
		if(model.growthIter==0 && model.relaxationIter==0) {
			// Set parameters. This overwrites both CModel and supplied arguments
			
//			/////////////
//			// E. COLI //
//			/////////////
//			model.cellRadiusMax[4] = 0.25e-6;
//			model.cellLengthMax[4] = 2.5e-6;
//			model.muAvgSimple[4] = 0.33;
//			model.UpdateAmountCellMax();
//			model.NInitCell = 3;
//			int[] type = new int[]{4,4,4};
//			model.sticking = false;
//			model.filament = false;
//			model.gravity = false;
//			model.initialAtSubstratum = true;
//			model.normalForce = true;
//			model.limOverlap = new double[]{1e-3, 1e-2};
//			model.Kd 	= 1e-13;
//			model.Kc 	= 1e-9;
//			model.Kw 	= 5e-10;
//			model.Kr 	= 5e-11;
//			model.Kf 	= 2e-11;
//			model.Kan	= 1e-11;
//			model.Ks 	= 1e-11;
//			// Set initial positions
//			rand.Seed(model.randomSeed+1000000);					// Make new random seed to use
//			Vector3d[] position = new Vector3d[model.NInitCell];
//			for(int ii=0; ii<position.length; ii++) {
//				position[ii] = new Vector3d(
//						(0.2*rand.Double()-0.1)*model.L.x,			// Anywhere between -0.1*Lx and 0.1*Lx
//						(0.2*rand.Double()+0.9)*model.L.y,			// Anywhere between 0.9*Ly and 1.1*Ly
//						(0.2*rand.Double()-0.1)*model.L.z);			// Anywhere between -0.1*Lz and 0.1*Lz
//			}
			
			/////////////
			// DENTAL  //
			/////////////
			model.randomSeed = 3;
			model.cellRadiusMax[0] = 0.25e-6 * 1.25;
			model.muAvgSimple[0] = 0.33;
			model.cellRadiusMax[4] = 0.25e-6;
			model.cellLengthMax[4] = 2.5e-6;
			model.muAvgSimple[4] = 0.15;
			model.UpdateAmountCellMax();
			model.NInitCell = 6;
			int[] type = new int[]{4,4,4,0,0,0};
			model.sticking = true;
			model.stickRodRod = false;
			model.stickSphereSphere = false;
			model.stretchLimStick = 1.6;
			model.filament = true;
			model.filSphere = false;
			model.anchoring = false;
			model.initialAtSubstratum = false;
			model.normalForce = true;
			model.limOverlap = new double[]{5e-3, 1e-2};
			model.syntrophyFactor = 2.0;
			model.L.y 	= model.cellRadiusMax[4];
			model.Kd 	= 1e-13;
			model.Kc 	= 1e-9;
			model.Kw 	= 5e-10;
			model.Kr 	= 5e-11;
			model.Kf 	= 2e-11;
			model.Kan	= 1e-11;
			model.Ks 	= 1e-11;
			model.allowOverlapDuringGrowth = true;
			model.relaxationTimeStep *= 2.0;
			model.relaxationTimeStepEnd *= 2.0;
			// Set initial positions
			rand.Seed(model.randomSeed+1000000);					// Make new random seed to use
			Vector3d[] position = new Vector3d[model.NInitCell];
			for(int ii=0; ii<position.length; ii++) {
				position[ii] = new Vector3d(
						(0.2*rand.Double()-0.1)*model.L.x,			// Anywhere between -0.1*Lx and 0.1*Lx
						(0.2*rand.Double()+0.9)*model.L.y,			// Anywhere between 0.9*Ly and 1.1*Ly
						(0.2*rand.Double()-0.1)*model.L.z);			// Anywhere between -0.1*Lz and 0.1*Lz
			}
			
			
			
//			// Create cell positions
//			// Defined
//			Vector3d[] position = new Vector3d[]{
//					new Vector3d(0.0,0.0,1.0).times(1e-6),
//					new Vector3d(2.0,0.0,-1.0).times(1e-6),
//					new Vector3d(-2.0,0.0,1.0).times(1e-6),
//					new Vector3d(0.0,0.0,-1.0).times(1e-6),
//					new Vector3d(2.0,0.0,1.0).times(1e-6),
//					new Vector3d(-2.0,0.0,-1.0).times(1e-6)};
			
			// COMSOL was here
			
			// Create initial cells, not overlapping
			rand.Seed(model.randomSeed);							// Reinitialise random seed, below shouldn't depend on positions above
			for(int iCell = 0; iCell < model.NInitCell; iCell++){
				double n = 0.5 * model.nCellMax[type[iCell]] * (1.0 + rand.Double());
				boolean filament = false;
				if(model.filament) {
					if(type[iCell]<2) {
						filament = model.filSphere; 
					} else if(type[iCell]<6){
						filament = model.filRod;
					} else {
						throw new IndexOutOfBoundsException("Cell type: " + type); 
					}
				}
				
				CCell cell = new CCell(type[iCell], 				// Type of biomass
						n,											// Initial cell mass is random between initial and max
						position[iCell].x,
						position[iCell].y,
						position[iCell].z,
						filament,									// With capability to form filaments?
						model.colour[iCell],
						model);
				// Set cell boundary concentration to initial value
				cell.q = 0.0;
				// Lower cell to the substratum if desired (INITIALATSUBSTRATUM)
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
			
			// Try to save and convert the file
			model.Save();
			ser2mat.Convert(model);	
		}
		
		boolean allowGrowth;
		if(model.allowOverlapDuringGrowth || model.DetectCellCollision_Proper(1.0).isEmpty())	allowGrowth = true;
		else 	allowGrowth=false;
		
		while(true) {
			// Reset the random seed
			rand.Seed((model.randomSeed+1)*(model.growthIter+1)*(model.relaxationIter+1));			// + something because if growthIter == 0, randomSeed doesn't matter.

			// COMSOL was here
			
			// Grow cells
			if(allowGrowth) {
				model.Write("Growing cells", "iter");
				ArrayList<CCell> dividedCellArray = model.GrowthSimple();
				
				// Advance growth
				model.growthIter++;
				model.growthTime += model.growthTimeStep;
				if(dividedCellArray.size()>0) {
					String cellNumber = "" + dividedCellArray.get(0).Index();
					for(int ii=1; ii<dividedCellArray.size(); ii++) 	cellNumber += ", " + dividedCellArray.get(ii).Index();
					model.Write(dividedCellArray.size() + " new cells grown, total " + model.cellArray.size() + " cells","iter");
					model.Write("Cells grown: " + cellNumber,"iter");
				}

				model.Write("Resetting springs","iter");
				for(CSpring rod : model.rodSpringArray) {
					rod.ResetRestLength();
				}
				for(CSpring fil : model.filSpringArray) 	{
					fil.ResetRestLength();
				}
			}
			
			// Attach new cells
			final int NNew = (int) (model.growthTimeStep/3600.0 * model.rateAttachment);
			model.Write("Attaching " + NNew + " new cells", "iter");
			model.Attachment(NNew);
			
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
			if(model.allowOverlapDuringGrowth || overlapCellArray.isEmpty()) {
				allowGrowth = true;
			} else {
				model.Write(overlapCellArray.size() + " overlapping cells detected, growth delayed","warning");
				String cellNumber = "" + overlapCellArray.get(0).Index();
				for(int ii=1; ii<overlapCellArray.size(); ii++) 	cellNumber += ", " + overlapCellArray.get(ii).Index();
				model.Write("Cells overlapping: " + cellNumber,"iter");
				allowGrowth = false;
			}

			// And finally: save stuff
			model.Write("Saving model as serialised file", "iter");
			model.Save();
			ser2mat.Convert(model);
		}
	}
}