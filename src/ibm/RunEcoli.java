package ibm;

import java.util.ArrayList;

import random.rand;
import ser2mat.ser2mat;

public class RunEcoli extends Run { 	// simulation == 0
	Model model;
	
	public RunEcoli(Model model) {
		this.model = model;
	}
	
	public void Initialise() {				
		rand.Seed(model.randomSeed);			// Set seed
		// Load default parameters
		model.Write("Loading parameters for E. coli","");
//		model.activeCellType = new int[]{4};
		model.shapeX[0] = 2; 				// E. coli is a fixed radius rod (==1)
		model.MWX[0] = 24.6e-3;
		model.rhoX[0] =1010; 				// [kg m-3]
		model.randomSeed = 9; 				// As in paper.
		model.L = new Vector3d(30e-6, 30e-6, 30e-6);
		model.Linit = new Vector3d(1e-6, 1e-6, 1e-6);
		model.radiusCellMax[0] = 0.375e-6;	// m. From Pierucci, 1978
		model.lengthCellMax[0] = 5.0e-6;	// m. From Pierucci, 1978. Theirs is initial cell length, so including 1*D
		model.radiusCellStDev[0] = model.radiusCellMax[0]*0.05;	// [m] Standard deviation from radiusCellMax. Note that this will work only with fixed radius rods! Spheres and var. radius rods must have a "free" radius
		model.NCellInit = 1;
		model.normalForce = true;
		model.Ks[0][0] = 1e-11;
		model.KfRod = new double[]{5e-13, 5e-13};
		model.filStretchLim = 1e-6;
		model.filType[0] = true;
		model.anchorStretchLim = 1e-6;		// Bit longer than initial to work with DLVO forces 
		model.sticking = model.filament = false;
		model.electrostatic = true; 		// Default enabled for E. coli. As in paper.
		model.muAvgSimple[0] = 1.23;		// h-1, i.e. doubling every 33 minutes. Koch & Wang, 1982
		model.muStDev[0] = 0.277;			// h-1. Képès, 1986
		model.growthTimeStep = 240.0;		// s, i.e. 4 minutes
	}
	
	public void Start() throws Exception {
		model.UpdateDependentParameters();
		// Initialise model if we are starting a new simulation
		if(model.growthIter == 0 && model.relaxationIter == 0) {			// First time we run this simulation, didn't load it
			// Set initial cell parameters based on model
			rand.Seed(model.randomSeed);
			int[] typeInit = new int[model.NCellInit];
			double[] nInit = new double[model.NCellInit];
			double[] radiusModifier = new double[model.NCellInit];
			Vector3d[] directionInit = new Vector3d[model.NCellInit];
			Vector3d[] position0Init = new Vector3d[model.NCellInit];
			Vector3d[] position1Init = new Vector3d[model.NCellInit];
			
			// Create parameters for new cells
			for(int ii=0; ii<model.NCellInit; ii++) {
				nInit[ii] = 0.5*model.nCellMax[0] * (1.0 + rand.Double());
				radiusModifier[ii] = model.radiusCellStDev[typeInit[ii]]*random.rand.Gaussian();
				directionInit[ii] = new Vector3d((rand.Double()-0.5), 								(rand.Double()-0.5), 								0.0).normalise();
				position0Init[ii] = new Vector3d((rand.Double()-0.5)*model.Linit.x, 	(rand.Double()-0.5)*model.Linit.y,	Ball.Radius(nInit[ii]/2.0, typeInit[ii], model) + radiusModifier[ii]);  
				final double restLength =  RodSpring.RestLength(Ball.Radius(nInit[ii], typeInit[ii], model), nInit[ii], typeInit[ii], model);
				position1Init[ii] = position0Init[ii].plus(directionInit[ii].times(restLength));
			}
			
			// Create initial cells
			for(int iCell = 0; iCell < model.NCellInit; iCell++){
				boolean filament = model.filament && model.filType[typeInit[iCell]];
				@SuppressWarnings("unused")
				Cell cell = new Cell(typeInit[iCell], 				// Type of biomass
						nInit[iCell],
						radiusModifier[iCell],
						position0Init[iCell],
						position1Init[iCell],
						filament,										// With capability to form filaments?
						model);
			}
			model.Write(model.cellArray.size() + " initial cells created","iter");
	
			// Save and convert the file
			model.Save();
			ser2mat.Convert(model);	
		}

		while(model.growthIter<model.growthIterMax) {
			// Reset the random seed
			rand.Seed((model.randomSeed+1)*(model.growthIter+1)*(model.relaxationIter+1));			// + something because if growthIter == 0, randomSeed doesn't matter.

			// Grow cells
			model.Write("Growing cells", "iter");
			model.GrowthSimple();
			// Mark mother cell for division if ready
			ArrayList<Cell> dividingCellArray = new ArrayList<Cell>(0);
			for(Cell mother : model.cellArray) {
				if(mother.GetAmount() > model.nCellMax[mother.type])
					dividingCellArray.add(mother);
			}
			// Divide marked cells
			int NFil = 0; int NBranch = 0;													// Keep track of how many filament springs and how many new branches we make
			for(Cell mother : dividingCellArray) {
				Cell daughter = model.DivideCell(mother);
//				// Slightly displace to prevent deadlock (doesn't work)
//				for(Ball ball : daughter.ballArray) { 
//					ball.pos.x += 1e-7*(rand.Double()-0.5);
//					ball.pos.y += 1e-7*(rand.Double()-0.5);
//				}
//				for(Ball ball : mother.ballArray) { 
//					ball.pos.x += 1e-7*(rand.Double()-0.5);
//					ball.pos.y += 1e-7*(rand.Double()-0.5);
//				}
				if(mother.filament) {
					Cell neighbourDaughter = mother.GetNeighbour();
					if(mother.filSpringArray.size()>2 && rand.Double() < model.filRodBranchFrequency && neighbourDaughter != null) {
						model.CreateFilament(daughter, mother, neighbourDaughter);		// 3 arguments --> branched, 2 springs daughter to mother and 2 daughter to neighbour 
						NFil += 4; NBranch++;
					} else {															// If we insert the cell in the straight filament
						model.TransferFilament(mother, daughter);		 
						model.CreateFilament(mother, daughter);							// 2 arguments --> unbranched, 2 springs daughter to mother
						NFil += 2;
					}
				}
			}
			// Advance growth
			model.growthIter++;
			model.growthTime += model.growthTimeStep;
			if(dividingCellArray.size()>0) {
				model.Write(dividingCellArray.size() + " cells divided, total " + model.cellArray.size() + " cells","iter");
				model.Write(NFil + " filament springs formed, " + NBranch + " new branches", "iter");
			}
			// Reset springs where needed
			model.Write("Resetting springs","iter");
			for(Spring rod : model.rodSpringArray) 	rod.ResetRestLength();
			for(Spring fil : model.filSpringArray) 	fil.ResetRestLength();
			// Attach new cells
			if(model.attachmentRate > 0) {
				final double NNew = model.attachmentRate*(model.growthTimeStep/3600.0);
				model.attachCounter += NNew;
				model.Write("Attaching " + (int)model.attachCounter + " new cells", "iter");
				model.Attachment((int)model.attachCounter);
				model.attachCounter -= (int)model.attachCounter;	// Subtract how many cells we've added this turn
			}
			// Relaxation
			int relaxationIterInit = (int) (model.relaxationTimeStep/model.relaxationTimeStepdt);
			model.Write("Starting relaxation calculations","iter"); 
			int NAnchorBreak = 0, NAnchorForm = 0, NStickBreak = 0, NStickForm = 0, NFilBreak = 0;			
			for(int ir=0; ir<relaxationIterInit; ir++) {
				int[] relaxationOut = model.Relaxation();
				int nstp 	=  relaxationOut[0]; 
				NAnchorBreak+= relaxationOut[1];
				NAnchorForm	+= relaxationOut[2];
				NStickBreak += relaxationOut[3];
				NStickForm 	+= relaxationOut[4];
				NFilBreak 	+= relaxationOut[5];
				model.relaxationIter++;
				model.relaxationTime += model.relaxationTimeStepdt;
				model.Write("    Relaxation finished in " + nstp + " solver steps","iter");
				// And finally: save stuff
				model.Save();
				ser2mat.Convert(model);
			}
			model.Write("Anchor springs broken/formed: " + NAnchorBreak + "/" + NAnchorForm + ", net " + (NAnchorForm-NAnchorBreak) + ", total " + model.anchorSpringArray.size(), "iter");
			model.Write("Filament springs broken: "      + NFilBreak          														+ ", total " + model.filSpringArray.size(), "iter");
			model.Write("Stick springs broken/formed: "  + NStickBreak  + "/" + NStickForm  + ", net " + (NStickForm-NStickBreak) 	+ ", total " + model.stickSpringArray.size(), "iter");
		}
	}
}
