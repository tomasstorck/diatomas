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
//			int[] type = new int[]{4,4,4};
//			model.cellRadiusMax[4] = 0.25e-6;
//			model.cellLengthMax[4] = 2.5e-6;
//			model.UpdateAmountCellMax();
//			model.NInitCell = 3;
//			double restLength = 0.75*model.cellLengthMax[4];			// 0.75 is a fair estimate
//			Vector3d[] direction = new Vector3d[]{
//					new Vector3d(0.3,		0.0,					0.2).normalise(),
//					new Vector3d(1.0,		0.0,					-0.2).normalise(),
//					new Vector3d(-0.5,		0.0,					1.0).normalise()};
//			Vector3d[] position0 = new Vector3d[]{
//					new Vector3d(0.6e-6,	model.cellRadiusMax[4],	-0.2e-6),
//					new Vector3d(-0.5e-6,	model.cellRadiusMax[4],	-0.1e-6),
//					new Vector3d(0.1e-6,	model.cellRadiusMax[4],	0.3e-6),
//					new Vector3d(0.4e-6,	model.cellRadiusMax[0],	0.4e-6),
//					new Vector3d(-0.3e-6,	model.cellRadiusMax[0],	-0.3e-6),
//					new Vector3d(0.0e-6,	model.cellRadiusMax[0],	0.0e-6)};
//			Vector3d[] position1 = new Vector3d[]{
//					position0[0].plus(direction[0].times(restLength)),
//					position0[1].plus(direction[1].times(restLength)),
//					position0[2].plus(direction[2].times(restLength)),
//					new Vector3d(),
//					new Vector3d(),
//					new Vector3d()};
//			model.muAvgSimple[4] = 0.33;
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
			
			/////////////
			// DENTAL  //
			/////////////
			int[] type = new int[]{4,4,4,0,0,0};
			model.cellRadiusMax[0] = 0.25e-6 * 1.25;
			model.cellRadiusMax[4] = 0.25e-6;
			model.cellLengthMax[4] = 2.5e-6;
			model.UpdateAmountCellMax();
			model.NInitCell = 6;
			double restLength = 0.75*model.cellLengthMax[4];			// 0.75 is a fair estimate
			Vector3d[] direction = new Vector3d[]{
					new Vector3d(0.3,		1.0,					0.2).normalise(),
					new Vector3d(1.0,		0.1,					-0.2).normalise(),
					new Vector3d(-0.5,		0.4,					1.0).normalise()};
			Vector3d[] position0 = new Vector3d[]{
					new Vector3d(0.6e-6,	model.cellRadiusMax[4],	-0.2e-6),
					new Vector3d(-0.5e-6,	model.cellRadiusMax[4],	-0.1e-6),
					new Vector3d(0.1e-6,	model.cellRadiusMax[4],	0.3e-6),
					new Vector3d(0.4e-6,	model.cellRadiusMax[0],	0.4e-6),
					new Vector3d(-0.3e-6,	model.cellRadiusMax[0],	-0.3e-6),
					new Vector3d(0.0e-6,	model.cellRadiusMax[0],	0.0e-6)};
			Vector3d[] position1 = new Vector3d[]{
					position0[0].plus(direction[0].times(restLength)),
					position0[1].plus(direction[1].times(restLength)),
					position0[2].plus(direction[2].times(restLength)),
					new Vector3d(),
					new Vector3d(),
					new Vector3d()};
			model.muAvgSimple[0] = 0.33;
			model.muAvgSimple[4] = 0.15;
			model.sticking = true;
			model.stickRodRod = false;
			model.stickSphereSphere = false;
			model.filament = true;
			model.filSphere = false;
			model.anchoring = false;
			model.initialAtSubstratum = false;
			model.normalForce = true;
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
			model.limOverlap = new double[]{5e-3, 1e-2};
			model.syntrophyFactor = 2.0;
			model.attachmentRate = 3.0;
			
			// COMSOL was here
			
			// Create initial cells, not overlapping
			rand.Seed(model.randomSeed);							// Reinitialise random seed, below shouldn't depend on positions above
			for(int iCell = 0; iCell < model.NInitCell; iCell++){
				double n = 0.5 * model.nCellMax[type[iCell]] * (1.0 + rand.Double());
				boolean filament = false;
				if(model.filament) {
					if(type[iCell]<2) 		filament = model.filSphere; 
					else if(type[iCell]<6)	filament = model.filRod;
					else throw new IndexOutOfBoundsException("Cell type: " + type); 
				}
				@SuppressWarnings("unused")
				CCell cell = new CCell(type[iCell], 				// Type of biomass
						n,											// Initial cell mass is random between initial and max
						position0[iCell].x,
						position0[iCell].y,
						position0[iCell].z,
						position1[iCell].x,
						position1[iCell].y,
						position1[iCell].z,
						filament,									// With capability to form filaments?
						model.colour[iCell],
						model);
			}
			model.Write(model.cellArray.size() + " initial cells created","iter");
			
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
			model.attachmentStack += (model.growthTimeStep/3600.0 * model.attachmentRate);
			int NNew = (int) model.attachmentStack;
			model.attachmentStack -= NNew;
			model.Write("Attaching " + NNew + " new cells", "iter");
			model.Attachment(NNew);
						
			// Relaxation
			model.Write("Starting relaxation calculations","iter");
			int nstp = model.Relaxation();
			model.relaxationIter++;
			model.relaxationTime += model.relaxationTimeStep;
			model.Write("Relaxation finished in " + nstp + " solver steps","iter");
			model.Write("Anchor springs broken/formed: " + Assistant.NAnchorBreak + "/" + Assistant.NAnchorForm + ", net " + (Assistant.NAnchorForm-Assistant.NAnchorBreak) + ", total " + model.anchorSpringArray.size(), "iter");
			model.Write("Filament springs broken: " + Assistant.NFilBreak + ", total " + model.filSpringArray.size(), "iter");
			model.Write("Stick springs broken/formed: " + Assistant.NStickBreak + "/" + Assistant.NStickForm + ", net " + (Assistant.NStickForm-Assistant.NStickBreak) + ", total " + model.stickSpringArray.size(), "iter");
			// Lower beta in ODE solver if too many steps
			if(nstp>4000) {
				model.ODEbeta *= 0.75;
				model.ODEalpha = 1.0/8.0-model.ODEbeta*0.2;		// alpha is per default a function of beta
				model.Write("Lowered ODE beta to " + model.ODEbeta +  " for next relaxation iteration","warning");
			}
			// Check if we can continue growth, or need to relax a bit longer
			ArrayList<CCell> overlapCellArray = model.DetectCellCollision_Proper(1.0);
			if(model.allowOverlapDuringGrowth || overlapCellArray.isEmpty()) {
				allowGrowth = true;
			} else {
				model.Write(overlapCellArray.size() + " overlapping cells detected, growth delayed","iter");
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