package ibm;

import java.util.ArrayList;

import random.rand;
import ser2mat.ser2mat;

public class RunAOM extends Run {
	
	public RunAOM(Model model) {
		this.model = model;
	}
	
	public void Initialise() {
		model.Write("Loading parameters for AOM/SR","");
		// Load default parameters
		int aom = model.anme = 0;
		int dss = model.dss = 1;
		model.Linit = new Vector3d(5e-6, 5e-6, 5e-6);
		model.L = new Vector3d(10e-6, 10e-6, 10e-6);
		model.radiusCellMax[aom] = 0.55e-6;					// [m]
		model.radiusCellMax[dss] = 0.44e-6;					// [m]
		model.muAvgSimple[aom] = 0.003/24.0;				// [h-1] muMax = 6.5 day-1 = 0.271 h-1, S. natans, (Lau 1984). Monod coefficient *should* be low (not in Lau) so justified high growth versus species 5. 
		model.muAvgSimple[dss] = 0.003/24.0;				// [h-1] muMax = 9.2 day-1 = 0.383 h-1, "floc former" (Lau 1984). Monod coefficient *should* be high (not in Lau)
		model.muStDev[aom] = 0.2*model.muAvgSimple[aom];
		model.muStDev[dss]  = 0.2*model.muAvgSimple[dss];	// Defined as one fifth
		model.syntrophyA = 5;
		model.syntrophyB = 0.2;
		model.syntrophyDist = 10e-6;
		model.NCellInit = 6;
		model.growthTimeStep = 2*7*24*3600.0;
		model.attachCellType = 1;
		model.attachmentRate = 0.0;
		model.filament = false;
		model.sticking = true;
		model.stickType[aom][aom] = model.stickType[aom][dss] = model.stickType[dss][aom] = model.stickType[dss][dss] = true;	// Anything sticks
		model.anchoring = false;
		model.normalForce = false;
		model.electrostatic = false;
	}
	
	public void Start() {
		model.UpdateDependentParameters();		// Update model parameters
		int aom = model.anme;
		int dss = model.dss;
		// Initialise model if we are starting a new simulation
		if(model.growthIter == 0 && model.relaxationIter == 0) {
			// Set initial cell parameters based on model
			rand.Seed(model.randomSeed);
			int[] typeInit = new int[model.NCellInit];
			double[] nInit = new double[model.NCellInit];
			double[] radiusModifier = new double[model.NCellInit];
			Vector3d[] directionInit = new Vector3d[model.NCellInit];
			Vector3d[] position0Init = new Vector3d[model.NCellInit];
			Vector3d[] position1Init = new Vector3d[model.NCellInit];
			
			// Create parameters for new cells
			for(int ii=0; ii<model.NCellInit; ii++){
				if(model.nCellMax[aom]>model.nCellMax[dss]) {
					final int div = (int) (model.nCellMax[aom] / model.nCellMax[dss]) + 1;	// e.g. 5 is 3x heavier --> div is 1/4, so there will be 3x more 4 cells than 5
					typeInit[ii] = ii%div==0 ? aom : dss;
				} else {
					final int div = (int) (model.nCellMax[dss] / model.nCellMax[aom]) + 1;
					typeInit[ii] = ii%div==0 ? dss : aom;
				}
			}
			for(int ii=0; ii<model.NCellInit; ii++) {
				nInit[ii] = 0.5*model.nCellMax[typeInit[ii]] * (1.0 + rand.Double());
				radiusModifier[ii] = 0.0;
				final double restLength =  RodSpring.RestLength(Ball.Radius(nInit[ii], typeInit[ii], model), nInit[ii], typeInit[ii], model);
				directionInit[ii] = new Vector3d((rand.Double()-0.5), 							(rand.Double()-0.5), 							(rand.Double()-0.5)).normalise();
				position0Init[ii] = new Vector3d(model.L.x/2 + (rand.Double()-0.5)*model.L.x, 	model.L.y/2 + (rand.Double()-0.5)*model.L.y, 	(rand.Double()-0.5)*model.L.z);
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

		// Start growth/relaxation loop
		while(model.growthIter<model.growthIterMax) {
			// Reset the random seed
			rand.Seed((model.randomSeed+1)*(model.growthIter+1)*(model.relaxationIter+1));	// + something because if growthIter == 0, randomSeed doesn't matter.

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
				if(mother.filament) {
					if(mother.type<2) {
						if(model.filSphereStraightFil)
							model.TransferFilament(mother, daughter);
						model.CreateFilament(mother, daughter);
						NFil += 1;
					} else if (mother.type<6) {
						Cell neighbourDaughter = mother.GetNeighbour();
						if(mother.filSpringArray.size()>2 && rand.Double() < model.filRodBranchFrequency && neighbourDaughter != null) {
							model.CreateFilament(daughter, mother, neighbourDaughter);		// 3 arguments --> branched, 2 springs daughter to mother and 2 daughter to neighbour 
							NFil += 4; NBranch++;
						} else {															// If we insert the cell in the straight filament
							model.TransferFilament(mother, daughter);		 
							model.CreateFilament(mother, daughter);							// 2 arguments --> unbranched, 2 springs daughter to mother
							NFil += 2;
						}
					} else
						throw new IndexOutOfBoundsException("Unknown mother cell type: " + mother.type);
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
				model.Attachment( (int) model.attachCounter );
				model.attachCounter -= (int) model.attachCounter;	// Subtract how many cells we've added this turn
			}
			
			// Relaxation
			int relaxationNIter = (int) (model.relaxationTimeStep/model.relaxationTimeStepdt);
			model.Write("Starting relaxation calculations","iter"); 
			int NAnchorBreak = 0, NAnchorForm = 0, NStickBreak = 0, NStickForm = 0, NFilBreak = 0;
			for(int ir=0; ir<relaxationNIter; ir++) {
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
