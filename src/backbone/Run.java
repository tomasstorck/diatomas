package backbone;

import java.util.ArrayList;

import comsol.Comsol;
import comsol.Server;
import random.rand;
import ser2mat.ser2mat;
import cell.CBall;
import cell.CCell;
import cell.CModel;
import cell.CRodSpring;
import cell.CSpring;
import cell.Vector3d;

public class Run {
	CModel model;
	
	public Run(CModel model) {
		this.model = model;
	}
	
	public void Initialise() throws RuntimeException{				
		rand.Seed(model.randomSeed);			// Set seed
		switch(model.simulation) {
		case 0:
			model.Write("Loading parameters for E. coli","");
			/////////////
			// E. COLI //
			/////////////
			model.radiusCellMax[4] = 0.375e-6;	// m. From Pierucci, 1978
			model.lengthCellMax[4] = 5.0e-6;	// m. From Pierucci, 1978. Theirs is initial cell length, so including 1*D
			model.radiusCellStDev[4] = model.radiusCellMax[4]*0.05;	// [m] Standard deviation from radiusCellMax. Note that this will work only with fixed radius rods! Spheres and var. radius rods must have a "free" radius
			model.NCellInit = 1;
			model.normalForce = true;
			model.KfRod = new double[]{5e-13, 5e-13};
			model.filStretchLim = 1e-6;
			model.filType[4] = true;
			model.anchorStretchLim = 1e-6;		// Bit longer than initial to work with DLVO forces 
			model.sticking = model.filament = false;
			model.muAvgSimple[4] = 1.23;		// h-1, i.e. doubling every 33 minutes. Koch & Wang, 1982
			model.muStDev[4] = 0.277;			// h-1. Képès, 1986
			model.growthTimeStep = 240.0;		// s, i.e. 4 minutes
			break;
		case 2:
			model.Write("Loading parameters for AS","");
			////////
			// AS //
			////////			
			model.L = new Vector3d(7e-6, 7e-6, 7e-6);
			model.radiusCellMax[0] = 0.52e-6;
			model.radiusCellMax[4] = 0.5e-6;	// [m] (Lau 1984)
			model.radiusCellMax[5] = 0.35e-6;	// [m] (Lau 1984)
			model.lengthCellMax[4] = 4e-6;		// [m] (Lau 1984), compensated for model length = actual length - 2*r
			model.lengthCellMax[5] = 1.1e-6;	// [m] (Lau 1984), compensated
			model.muAvgSimple[0] = 0.383;		// [h-1]
			model.muAvgSimple[4] = 0.271;		// [h-1] muMax = 6.5 day-1 = 0.271 h-1, S. natans, (Lau 1984). Monod coefficient *should* be low (not in Lau) so justified high growth versus species 5. 
			model.muAvgSimple[5] = 0.383;		// [h-1] muMax = 9.2 day-1 = 0.383 h-1, "floc former" (Lau 1984). Monod coefficient *should* be high (not in Lau)
			model.muStDev[0] = 0.2*model.muAvgSimple[0];
			model.muStDev[4] = 0.2*model.muAvgSimple[4];		// Defined as one fifth 
			model.muStDev[5] = 0.2*model.muAvgSimple[5];		//
			model.NCellInit = 18;
			model.growthTimeStep = 300.0;
			model.attachCellType = 5;
			model.attachNotTo = new int[]{};
			model.filament = true;
			model.filType[4] = true;			// Only filament former forms filaments
			model.sticking = true;
			model.stickType[4][5] = model.stickType[5][4] = model.stickType[4][4] = model.stickType[5][5] = true;	// Anything sticks
			model.stickType[4][0] = model.stickType[0][4] = model.stickType[4][4] = model.stickType[0][0] = true;	// Anything sticks
			model.anchoring = false;
			model.normalForce = false;
			break;
		default:
			throw new IndexOutOfBoundsException("Model simulation: " + model.simulation);
		}
	}
	
	public void Start() throws Exception {
		// Update model parameters
		model.UpdateAmountCellMax();
		
		// Start server and connect if we're using COMSOL
		if(model.comsol) {
			model.Write("Starting server and connecting model to localhost:" + CModel.port, "iter");
//			Server.Start(CModel.port);
			Server.Connect(CModel.port);
		}
		
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
			
			switch(model.simulation) {
			case 0:
				for(int ii=0; ii<model.NCellInit; ii++) {
					typeInit[ii] = 4;
					nInit[ii] = 0.5*model.nCellMax[typeInit[ii]] * (1.0 + rand.Double());
					radiusModifier[ii] = model.radiusCellStDev[typeInit[ii]]*random.rand.Gaussian();
					directionInit[ii] = new Vector3d((rand.Double()-0.5), 				0.0*rand.Double(), 																				(rand.Double()-0.5))			.normalise();
					position0Init[ii] = new Vector3d((rand.Double()-0.5)*model.L.x, 	CBall.Radius(nInit[ii]/2.0, typeInit[ii], model) + radiusModifier[ii] + 0.0*rand.Double(),		(rand.Double()-0.5)*model.L.z);						// *0.0*rand.Double() to maintain reproducibility between floc and biofilm  
					final double restLength =  CRodSpring.RestLength(CBall.Radius(nInit[ii], typeInit[ii], model), nInit[ii], typeInit[ii], model);
					position1Init[ii] = position0Init[ii].plus(directionInit[ii].times(restLength));
				}
				break;
			case 2:
				// Set type
				final int filF = 4;
				final int flocF = 5;
				for(int ii=0; ii<model.NCellInit; ii++)			 {
					if(model.nCellMax[flocF]>model.nCellMax[filF]) {
						final int div = (int) (model.nCellMax[flocF] / model.nCellMax[filF]) + 1;	// e.g. 5 is 3x heavier --> div is 1/4, so there will be 3x more 4 cells than 5
						typeInit[ii] = ii%div==0 ? flocF : filF;
					} else {
						final int div = (int) (model.nCellMax[filF] / model.nCellMax[flocF]) + 1;
						typeInit[ii] = ii%div==0 ? filF : flocF;
					}
				}
				for(int ii=0; ii<model.NCellInit; ii++) {
					nInit[ii] = 0.5*model.nCellMax[typeInit[ii]] * (1.0 + rand.Double());
					radiusModifier[ii] = 0.0; 
					directionInit[ii] = new Vector3d((rand.Double()-0.5), (rand.Double()-0.5), (rand.Double()-0.5)).normalise();
					final double restLength =  CRodSpring.RestLength(CBall.Radius(nInit[ii], typeInit[ii], model), nInit[ii], typeInit[ii], model);
					position0Init[ii] = new Vector3d((rand.Double()-0.5)*model.L.x, (rand.Double()-0.5)*model.L.y,															(rand.Double()-0.5)*model.L.z);
					position1Init[ii] = position0Init[ii].plus(directionInit[ii].times(restLength));
				}
				break;
			default:
				throw new IndexOutOfBoundsException("Model simulation: " + model.simulation);
			}
			// Displace cells because of multiple colonies
			for(int iCell = 0; iCell<model.NCellInit; iCell++) {
				int iCol = iCell/(model.NCellInit/model.NColoniesInit);
				final Vector3d[] dirColonies = new Vector3d[]{			 	// Displace colonies along the X vector, so we can easily see them in the renders
						new Vector3d(0.0, 0.0, 0.0),
						new Vector3d(-18e-6, 0.0, -18e-6),
						new Vector3d( 18e-6, 0.0, -18e-6),
						new Vector3d( 18e-6, 0.0,  18e-6),
						new Vector3d(-18e-6, 0.0,  18e-6)};			
				position0Init[iCell] = position0Init[iCell].plus(dirColonies[iCol]);
				position1Init[iCell] = position1Init[iCell].plus(dirColonies[iCol]);
			}
			// Create initial cells
			for(int iCell = 0; iCell < model.NCellInit; iCell++){
				boolean filament = model.filament && model.filType[typeInit[iCell]];
				@SuppressWarnings("unused")
				CCell cell = new CCell(typeInit[iCell], 				// Type of biomass
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

		while(model.growthIter<model.growthIterMax && model.relaxationIter<model.relaxationIterMax) {
			// Reset the random seed
			rand.Seed((model.randomSeed+1)*(model.growthIter+1)*(model.relaxationIter+1));			// + something because if growthIter == 0, randomSeed doesn't matter.

			// Grow cells
			// Compute concentration fields with COMSOL
			if(model.comsol) {
				// Do COMSOL things
				model.Write("Calculating cell steady state concentrations (COMSOL)","iter");
				// Make the model
				Comsol comsol = new Comsol(model);
				model.Write("\tInitialising geometry, physics, mesh, study and solver", "iter");
				comsol.Initialise();
				model.Write("\tCreating cells", "iter");
				// Create cells in the COMSOL model
				for(CCell cell : model.cellArray) {
					if(cell.type<2) 		comsol.CreateSphere(cell);
					else if(cell.type<6)	comsol.CreateRod(cell);
					else 					throw new IndexOutOfBoundsException("Cell type: " + cell.type);
				}
				// Compile array with oxidating, reducing MO
				ArrayList<CCell> oxCellArray = new ArrayList<CCell>();
				ArrayList<CCell> redCellArray = new ArrayList<CCell>();
				final int oxType = 4;
				final int redType = 0;
				for(CCell cell : model.cellArray) {
					if(cell.type==oxType)					// FIXME Correct cell type?
						oxCellArray.add(cell);
					else if(cell.type==redType)
						redCellArray.add(cell);
					else
						throw new IndexOutOfBoundsException("Cell type: " + cell.type);
					// Create average over this domain
					comsol.CreateAverageOp(cell);
				}
//				model.Write("\tCreate geometry repair methods", "iter");
//				comsol.CreateRepair(model.cellArray);
				model.Write("\tCreating boundary box and building geometry", "iter");
				comsol.CreateBCBox();					// Create a large box where we also set the "bulk" conditions
				comsol.BuildGeometry();
				model.Write("\tAdding acid dissociation reactions, setting cell electric potentials and currents", "iter");
				comsol.CreateAcidDissociation();
				String iet = "diet";
				for(CCell cell : model.cellArray) {
					String type = cell.type==oxType?"ox":"red";
					comsol.CreateCurrentDiscontinuity(cell, type);
					comsol.CreateBiomassReaction(cell, type);
					if(cell.type==redType)
						comsol.CreateElectricPotential(cell);
				}
				comsol.CreateRatioDiet(oxCellArray, redCellArray);
				model.Write("\tSaving model", "iter");
				comsol.Save();							// Save .mph file
				// Calculate and extract the results
				model.Write("\tRunning model", "iter");
				comsol.Run();							// Run model to calculate concentrations
				model.Write("\tCalculating cell surface concentrations", "iter");
				for(CCell cell : model.cellArray) {
					String type = cell.type==oxType?"ox":"red";
					cell.Rx = comsol.GetRx(cell, type, iet);
				}
				// Clean up after ourselves 
				model.Write("\tCleaning model from server", "iter");
				comsol.RemoveModel();
			}
			// Grow cells either with COMSOL or simple 
			model.Write("Growing cells", "iter");
			if(model.comsol) {
				model.GrowthFlux();
			} else {
				model.GrowthSimple();
			}
			// Mark mother cell for division if ready
			ArrayList<CCell> dividingCellArray = new ArrayList<CCell>(0);
			for(CCell mother : model.cellArray) {
				if(mother.GetAmount() > model.nCellMax[mother.type])
					dividingCellArray.add(mother);
			}
			// Divide marked cells
			int NFil = 0; int NBranch = 0;													// Keep track of how many filament springs and how many new branches we make
			for(CCell mother : dividingCellArray) {
				CCell daughter = model.DivideCell(mother);
				if(mother.filament) {
					if(mother.type<2) {
						if(model.filSphereStraightFil)
							model.TransferFilament(mother, daughter);
						model.CreateFilament(mother, daughter);
						NFil += 1;
					} else if (mother.type<6) {
						CCell neighbourDaughter = mother.GetNeighbour();
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
//				String cellNumber = "" + dividedCellArray.get(0).Index();
//				for(int ii=1; ii<dividedCellArray.size(); ii++) 	cellNumber += ", " + dividedCellArray.get(ii).Index();
//				model.Write("Cells grown: " + cellNumber,"iter");
			}
			// Reset springs where needed
			model.Write("Resetting springs","iter");
			for(CSpring rod : model.rodSpringArray) 	rod.ResetRestLength();
			for(CSpring fil : model.filSpringArray) 	fil.ResetRestLength();
			// Attach new cells
			final double NNew = model.attachmentRate*(model.growthTimeStep/3600.0);
			//			N guaranteed	+ 1 the integer of this iteration is not equal to the previous one (this will be wrong for growthIter==0)
			model.attachCounter += NNew;
			model.Write("Attaching " + (int)model.attachCounter + " new cells", "iter");
			model.Attachment((int)model.attachCounter);
			model.attachCounter -= (int)model.attachCounter;	// Subtract how many cells we've added this turn

			// Relaxation
			int relaxationIterInit = (int) (model.relaxationTimeStep/model.relaxationTimeStepdt);
			model.Write("Starting relaxation calculations","iter"); 
			int NAnchorBreak= 0;
			int NAnchorForm	= 0;
			int NStickBreak = 0;
			int NStickForm 	= 0;
			int NFilBreak 	= 0;
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
				// Throw warning if cells are overlapping (will crash COMSOL)
				if(model.comsol) {
					ArrayList<CCell> overlapCellArray = model.DetectCellCollision_Proper(1.01);
					if(!overlapCellArray.isEmpty()) {
						String overlapCellArrayString = ""; 
						for(CCell c : overlapCellArray) {
							overlapCellArrayString += c.Index() + " ";
						}
						model.Write("    Overlapping cells detected: " + overlapCellArrayString, "warning");
					}
				}
				// And finally: save stuff
				model.Write("    Saving model as serialised file", "iter");
				model.Save();
				ser2mat.Convert(model);
				// Lower beta in ODE solver if too many steps
				if(model.ODEbeta>0.0 && nstp>(int)4e4*model.relaxationTimeStep) {
					if(model.ODEbeta>1e-3) 	model.ODEbeta *= 0.75;
					else 					model.ODEbeta = 0.0;
					model.ODEalpha = 1.0/8.0-model.ODEbeta*0.2;		// alpha is per default a function of beta
					model.Write("    Lowered ODE beta to " + model.ODEbeta +  " for next relaxation iteration","warning");
				}	
			}
			model.Write("Anchor springs broken/formed: " + NAnchorBreak + "/" + NAnchorForm + ", net " + (NAnchorForm-NAnchorBreak) + ", total " + model.anchorSpringArray.size(), "iter");
			model.Write("Filament springs broken: "      + NFilBreak          														+ ", total " + model.filSpringArray.size(), "iter");
			model.Write("Stick springs broken/formed: "  + NStickBreak  + "/" + NStickForm  + ", net " + (NStickForm-NStickBreak) 	+ ", total " + model.stickSpringArray.size(), "iter");
		}
	}
}
