package ibm;

import java.util.ArrayList;

import random.rand;
import ser2mat.ser2mat;

public class RunAOM extends Run {		// simulation == 4 
	
	public RunAOM(Model model) {
		this.model = model;
	}
	
	public void Initialise() {
		model.Write("Loading parameters for AOM/SR","");
		// Load default parameters
		int anme = 0;
		int dss = 1;
		model.shapeX[anme] = 0;
		model.shapeX[dss] = 0;
		model.Linit = new Vector3d(2e-6, 2e-6, 2e-6);
		model.L = new Vector3d(10e-6, 10e-6, 10e-6);
		model.MWX[anme] = model.MWX[dss] = 24.6e-3;				// [kg mol-1]
		model.rhoX[anme] = model.rhoX[dss] = 1010; 				// [kg m-3]
		model.radiusCellMax[anme] = 0.55e-6/2.0;				// [m]
		model.radiusCellMax[dss] = 0.44e-6/2.0;					// [m]
//		model.muAvgSimple[anme] = 1.2*0.003/24.0;				// [h-1]. Works for model.NCellInit = 60  
		model.muAvgSimple[anme] = 0.003/24.0;					// [h-1]. Works for model.NCellInit = 13
		model.muAvgSimple[dss] = 0.003/24.0;					// [h-1] 
		model.muStDev[anme] = 0.2*model.muAvgSimple[anme];
		model.muStDev[dss]  = 0.2*model.muAvgSimple[dss];		// Defined as one fifth
		model.syntrophyType = new int[]{dss};
		model.syntrophyPartner = new int[]{anme};
		model.syntrophyA = 1.0; 								// Syntrophy can speed up growth to a factor syntrophyA. 1.0 means no speed-up.
		model.syntrophyB = 0.2; 								// 0.2 --> doesn't reach max. growth rate easily
		model.syntrophyDist = 1e-6;
//		model.NCellInit = 60;
		model.NCellInit = 18;
		model.growthTimeStep = 7*24*3600.0;
		model.attachCellType = 1;
		model.attachmentRate = 0.0;
		model.filament = false;
		model.sticking = true;
		model.stickType[anme][anme] = true;
		model.Ks[anme][anme] = 1e-12; 			// Scaled for cell size (default 1e-11)
		model.Ks[anme][dss] = model.Ks[dss][anme] = model.Ks[dss][dss] = 1e-12; 	// Remeber it is still disabled 
		model.stickFormLim = 0.1e-6;
		model.stickStretchLim = 0.5e-6;
		model.anchoring = false;
		model.normalForce = false;
		model.electrostatic = false;
		// With sulfur species (S8 (s)), include below
		int s8s = 2; 											// NXCType is high enough
		model.shapeX[s8s] = 0;
		model.MWX[s8s] =256e-3;									// [kg mol-1]
		model.rhoX[s8s] = 2070; 								// [kg m-3]
		model.radiusCellMax[s8s] = 1e-5;						// [m]. Arbitrary high value
		model.muAvgSimple[s8s] = 0.0;							// [h-1] 
		model.muStDev[s8s] = 0.0;
		model.stickType[anme][s8s] = model.stickType[s8s][anme] = model.stickType[dss][s8s] = model.stickType[s8s][dss] = true;
		model.Ks[anme][s8s] = model.Ks[s8s][anme] = model.Ks[dss][s8s] = model.Ks[s8s][dss] = 1e-12;
		model.yieldMatrix[s8s][anme] = 0.1;
	}
	
	public void Start() {
		model.UpdateDependentParameters();		// Update model parameters
		int anme = 0;
		int dss = 1;
		int s8s = 2; 							// No problem if unused
		// Initialise model if we are starting a new simulation
		if(model.growthIter == 0 && model.relaxationIter == 0) {
			model.Write("Generating inoculum configuration", "iter");
			// Set initial cell parameters based on model
			rand.Seed(model.randomSeed);
			int[] typeInit = new int[model.NCellInit];
			double[] nInit = new double[model.NCellInit];
			double[] radiusInit = new double[model.NCellInit];
			double[] radiusModifier = new double[model.NCellInit];
			Vector3d[] position0Init = new Vector3d[model.NCellInit];
			Vector3d[] position1Init = new Vector3d[model.NCellInit];
//			// Create parameters for new cells: using div
//			for(int ii=0; ii<model.NCellInit; ii++){
//				int div = 6; 							// 1 in $div cells is AOM
//				typeInit[ii] = ii%div==0 ? anme : dss;	
//			}
			// Create parameters for new ANME and DSS cells: using absolute count (for NCellInit == 13)
			for(int ii=0; ii<model.NCellInit; ii++){
				int Ndss = 12;
				int Nanme = model.NCellInit-Ndss;
				typeInit[ii] = ii<Nanme ? anme : dss;	
			}
			for(int ii=0; ii<model.NCellInit; ii++) {
				nInit[ii] = 0.5*model.nCellMax[typeInit[ii]] * (1.0 + rand.Double());
				radiusInit[ii] = Ball.Radius(nInit[ii], typeInit[ii], model);		// TODO: Is nInit correct or should we divide by 2 for rod cells?
				radiusModifier[ii] = 0.0;
//				double rAggregate = 1.0e-6; 	// works well for model.NCellInit == 60
//				double rAggregate = 0.9e-6; 	// works well for model.NCellInit == 18 with organised aggregate
				double rAggregate = 0.7e-6; 	// works well for model.NCellInit == 18 with random inoculum
//				// Inoculum positioning: organised aggregate
//				int NOverlap = 0;
//				while(true) {
//					if(NOverlap > 10000) {
//						throw new RuntimeException("Could not find a non-overlappting initial cell configuration"); 
//					}
//					if(typeInit[ii]==anme) {
//						double d = rand.Double()*(rAggregate - 0.2e-6 - model.radiusCellMax[anme]);
//						position0Init[ii] = new Vector3d((rand.Double()-0.5),(rand.Double()-0.5),(rand.Double()-0.5)).normalise().times(d);
//					}
//					else if(typeInit[ii]==dss) {
//						double d = rAggregate;
//						position0Init[ii] = new Vector3d((rand.Double()-0.5),(rand.Double()-0.5),(rand.Double()-0.5)).normalise().times(d);
//					}
//					boolean overlap = false;
//					for(int jj=0; jj<ii; jj++) {
//						if( (position0Init[ii].minus(position0Init[jj])).norm() - radiusInit[ii] - radiusInit[jj] < 0.0) { 		// Cells probably overlap (always if spheres)
//							overlap = true;
//							break;
//						}
//					}
//					if(overlap) { 
//						NOverlap += 1; 		// Add 1 to number of failed attempts
//						continue;			// Continue finding a better position
//					} else {
//						break; 				// No overlap, next cell
//					}
//					
//				}
				
				// Inoculum positioning: random
				int NOverlap = 0;
				boolean overlap = true;
				while(overlap) {
					if(NOverlap>10000) {
						throw new RuntimeException("Could not find a non-overlappting initial cell configuration");
					}
					double d = rand.Double() * rAggregate;
					position0Init[ii] = new Vector3d((rand.Double()-0.5),(rand.Double()-0.5),(rand.Double()-0.5)).normalise().times(d);
					overlap = false;
					for(int jj=0; jj<ii; jj++) {
						if( (position0Init[ii].minus(position0Init[jj])).norm() - radiusInit[ii] - radiusInit[jj] < 0.0) { 		// Cells probably overlap
							overlap = true;
							NOverlap += 1;
							break;
						}
					}
				}
			}
			position1Init = position0Init;
			
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
	
			if(!model.DetectCollisionCellArray(1.0).isEmpty()) {
				model.Write("Initial cells overlap", "warning");
			}
			
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
			double[] increaseAmountArray = model.GrowthSyntrophy();
			
			// Produce S8 (s)
			if(model.MWX[s8s] != model.MWX[anme] || model.rhoX[s8s] != model.rhoX[anme]) { 	// Assume MWX or rhoX are set if S8 (s) is enabled 
				model.Write("Distributing produced S8 (s)", "iter");
				int NCell = model.cellArray.size();
				// Based on increase in growth for ANME
				for(int iCell = 0; iCell<NCell; iCell++) {
					Cell cell = model.cellArray.get(iCell);
					if(cell.type!=anme) 	continue; 										// Only ANME cells produce S8 (s)
					double s8sAmount = increaseAmountArray[iCell] * model.yieldMatrix[s8s][anme];
					double radius = cell.ballArray[0].radius;
					Vector3d dir = new Vector3d(2.0*(rand.Double()-0.5), 2.0*(rand.Double()-0.5), 2.0*(rand.Double()-0.5)).normalise();
					Vector3d disp = dir.times(radius);
					new Cell(s8s, s8sAmount, cell.ballArray[0].pos.plus(disp), new Vector3d(), false, model);
				}
			}
			
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
				int shapeMother = model.shapeX[mother.type];
				if(mother.filament) {
					if(shapeMother==0) {
						if(model.filSphereStraightFil)
							model.TransferFilament(mother, daughter);
						model.CreateFilament(mother, daughter);
						NFil += 1;
					} else if (shapeMother==1 || shapeMother==2) {
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
