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
	
	public void Initialise() throws Exception{				

		///////////////////////////////////////////////////////////////////////////////////////////////////////////
		//\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\//
		///////////////////////////////////////////////////////////////////////////////////////////////////////////

		rand.Seed(model.randomSeed);			// Set seed
		switch(model.simulation) {
		case 0:
			model.Write("Loading parameters for E. coli","");
			/////////////
			// E. COLI //
			/////////////
			model.radiusCellMax[4] = 0.375e-6;	// m. From Pierucci, 1978
			model.lengthCellMax[4] = 5.0e-6;	// m. From Pierucci, 1978. Theirs is initial cell length, so including 1*D
			model.NInitCell = 1;
			model.normalForce = true;
			model.sticking = model.filament = false;
			model.Kd 	= 2e-13;				// drag force coefficient doubled for ~doubled mass
			model.Kr 	= 5e-11;				// internal cell spring
			model.Kan	= 1e-11;				// anchor
			model.KfRod0 = 2e-11;
			model.KfRod1 = 2e-11;
			model.Ks = 2e-12;
			model.filLengthRod = new double[]{0.5, 1.7};
			model.muAvgSimple[4] = 1.23;		// h-1, i.e. doubling every 33 minutes. Koch & Wang, 1982
			model.muStDev[4] = 0.277;			// h-1. Képès, 1986
			model.growthTimeStep = 180.0;		// s, i.e. 3 minutes
//			model.relaxationIterSuccessiveMax = 10;
			break;
		case 1: case 2:
			model.Write("Loading parameters for AS","");
			////////
			// AS //
			////////			
//			model.L = new Vector3d(20e-6, 0.0, 20e-6);
			model.L = new Vector3d(10e-6, 0.0, 10e-6);
			model.radiusCellMax[4] = 0.45e-6;	// Pseudomonas sp. 138, Tanaka 1985
			model.lengthCellMax[4] = 1.1e-6;	// Pseudomonas sp. 138 (max "measured" length 2 micron), Tanaka 1985
			model.radiusCellMax[5] = 0.5e-6;	// Sphaerotilus sp. F6, Tanaka 1985
			model.lengthCellMax[5] = 3e-6;		// Sphaerotilus sp. F6 (max "measured" length 4 micron), Tanaka 1985
			model.NInitCell = 6;
			model.muAvgSimple[4] = 0.13;		// Based on S. natans, weighed for QO2max in Tanaka 1985
			model.muAvgSimple[5] = 0.38;		// Sphaerotilus natans, average of different strains, Pellegrin 1999 
//			model.relaxationIterSuccessiveMax = 10;
//			model.syntrophyFactor = 1.5;		// Let's not touch substrate transfer just yet
			model.attachmentRate = 1.0;
			model.attachCellType = 4;
			model.attachNotTo = new int[]{4};
			model.filament = true;
			model.filType[5] = true;
			model.filLengthRod = new double[]{0.5, 1.7};
			model.filStretchLim = 1.0;
			model.filBranchFrequency = 0.0;
			model.sticking = true;
//			model.stickType[4][5] = model.stickType[5][4] = model.stickType[4][4] = model.stickType[5][5] = true;
			model.stickType[4][5] = model.stickType[5][4] = true;
			model.Kd 	= 1e-13;				// drag force coefficient
			model.Kc 	= 1e-9;					// cell-cell collision
			model.Kw 	= 5e-10;				// wall(substratum)-cell spring
			model.Kr 	= 5e-11;				// internal cell spring
			model.KfSphere 	= 2e-11;			// filament spring
			model.KfRod0 = 2e-11;
			model.KfRod1 = 2e-11;
			model.Kan	= 1e-11;				// anchor
			model.Ks 	= 1e-11;				// sticking
			if(model.simulation==1) {
				model.anchoring = true;
				model.normalForce = true;
			} else {
				model.anchoring = false;
				model.normalForce = false;
			}
			break;
		case 3:
			model.Write("Loading parameters for Cyanobacteria","");
			///////////////////
			// CYANOBACTERIA //
			///////////////////
			model.radiusCellMax[4] = 0.375e-6;	// m. From Pierucci, 1978
			model.lengthCellMax[4] = 5.0e-6;	// m. From Pierucci, 1978. Theirs is initial cell length, so including 1*D
			model.NInitCell = 10;
			model.normalForce = true;
			model.sticking = false;
			model.filament = true;
			model.filStretchLim = 1.0;
			model.L = new Vector3d(30e-6, model.radiusCellMax[4], 30e-6);
			model.Kd 	= 2e-13;				// drag force coefficient doubled for ~doubled mass
			model.Kr 	= 5e-11;				// internal cell spring
			model.Kan	= 1e-11;				// anchor
			model.KfRod0 = 2e-11;
			model.KfRod1 = 2e-11;
			model.filLengthRod = new double[]{0.5, 1.7};
			model.muAvgSimple[4] = 1.23;		// h-1, i.e. doubling every 33 minutes. Koch & Wang, 1982
			model.muStDev[4] = 0.277;			// h-1. Képès, 1986
			model.growthTimeStep = 180.0;		// s, i.e. 3 minutes
//			model.relaxationIterSuccessiveMax = 10;
			model.randomSeed = 4;
			break;
		default:
			throw new IndexOutOfBoundsException("Model simulation: " + model.simulation);
		}
	}
	
	public void Start() throws Exception {
		// Set initial cell parameters based on model
		rand.Seed(model.randomSeed);
		model.UpdateAmountCellMax();
		switch(model.simulation) {
		case 0: case 3: case 4:
			model.typeInit = new int[model.NInitCell];
			model.nInit = new double[model.NInitCell];
			model.directionInit = new Vector3d[model.NInitCell];
			model.position0Init = new Vector3d[model.NInitCell];
			model.position1Init = new Vector3d[model.NInitCell];
			for(int ii=0; ii<model.NInitCell; ii++) {
				model.typeInit[ii] = 4;
				model.nInit[ii] = 0.5*model.nCellMax[model.typeInit[ii]] * (1.0 + rand.Double());
				model.directionInit[ii] = new Vector3d((rand.Double()-0.5), 				0.0*rand.Double(), 																		(rand.Double()-0.5))			.normalise();
				model.position0Init[ii] = new Vector3d((rand.Double()-0.5)*model.L.x, 		CBall.Radius(model.nInit[ii]/2.0, model.typeInit[ii], model)+0.0*rand.Double(),			(rand.Double()-0.5)*model.L.z);						// *0.0*rand.Double() to maintain reproducibility between floc and biofilm  
				final double restLength =  CRodSpring.RestLength(CBall.Radius(model.nInit[ii], model.typeInit[ii], model), model.nInit[ii], model.typeInit[ii], model);
				model.position1Init[ii] = model.position0Init[ii].plus(model.directionInit[ii].times(restLength));
			}
			break;
		case 1: case 2:
			// Set type
			model.typeInit = new int[model.NInitCell];
			for(int ii=0; ii<model.NInitCell/2; ii++)						model.typeInit[ii] = 4;			// First half: 
			for(int ii=model.NInitCell/2; ii<model.NInitCell; ii++)			model.typeInit[ii] = 5;			// Second half: 
			// Various
			model.nInit = new double[model.NInitCell];
			model.directionInit = new Vector3d[model.NInitCell];
			model.position0Init = new Vector3d[model.NInitCell];
			model.position1Init = new Vector3d[model.NInitCell];

			if(model.simulation==1) {
				model.Write("Defining cell parameters for AS/biofilm","");
				// Biofilm-like
				for(int ii=0; ii<model.NInitCell; ii++) {
					model.nInit[ii] = 0.5*model.nCellMax[model.typeInit[ii]] * (1.0 + rand.Double());
					model.directionInit[ii] = new Vector3d((rand.Double()-0.5), 			(rand.Double()-0.5)+5.0,																(rand.Double()-0.5))			.normalise();
					model.position0Init[ii] = new Vector3d((rand.Double()-0.5)*model.L.x, 	CBall.Radius(model.nInit[ii]/2.0, model.typeInit[ii], model)+0.0*rand.Double(),			(rand.Double()-0.5)*model.L.z);
					final double restLength =  CRodSpring.RestLength(CBall.Radius(model.nInit[ii], model.typeInit[ii], model), model.nInit[ii], model.typeInit[ii], model);
					model.position1Init[ii] = model.position0Init[ii].plus(model.directionInit[ii].times(restLength));
				}
			} else {
				model.Write("Defining cell parameters for AS/flock","");
				// Flock-like
				for(int ii=0; ii<model.NInitCell; ii++) {
					model.nInit[ii] = 0.5*model.nCellMax[model.typeInit[ii]] * (1.0 + rand.Double());
					model.directionInit[ii] = new Vector3d((rand.Double()-0.5), 			(rand.Double()-0.5), 																	(rand.Double()-0.5))			.normalise();
					final double restLength =  CRodSpring.RestLength(CBall.Radius(model.nInit[ii], model.typeInit[ii], model), model.nInit[ii], model.typeInit[ii], model);
					model.position0Init[ii] = new Vector3d((rand.Double()-0.5)*model.L.x, 	(rand.Double()-0.5)*model.L.y,															(rand.Double()-0.5)*model.L.z);
					model.position1Init[ii] = model.position0Init[ii].plus(model.directionInit[ii].times(restLength));
				}
			}
			break;
		default:
			throw new IndexOutOfBoundsException("Model simulation: " + model.simulation);
		}
		
		///////////////////////////////////////////////////////////////////////////////////////////////////////////
		//\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\//
		///////////////////////////////////////////////////////////////////////////////////////////////////////////
		if(model.growthIter == 0 && model.relaxationIter == 0) {			// First time we run this simulation, didn't load it
			// Create initial cells
			for(int iCell = 0; iCell < model.NInitCell; iCell++){
				boolean filament = model.filament && model.filType[model.typeInit[iCell]];
				@SuppressWarnings("unused")
				CCell cell = new CCell(model.typeInit[iCell], 				// Type of biomass
						model.nInit[iCell],
						model.position0Init[iCell],
						model.position1Init[iCell],
						filament,											// With capability to form filaments?
						model);
			}
			model.Write(model.cellArray.size() + " initial cells created","iter");
	
			// Save and convert the file
			model.Save();
			ser2mat.Convert(model);	
	
			// Start server and connect if we're using COMSOL
			if(model.comsol) {
				model.Write("Starting server and connecting model to localhost:" + Assistant.port, "iter");
				//				Server.Stop(false);
				Server.Start(Assistant.port);
				Server.Connect(Assistant.port);
			}
		}

		while(true) {
			// Reset the random seed
			rand.Seed((model.randomSeed+1)*(model.growthIter+1)*(model.relaxationIter+1));			// + something because if growthIter == 0, randomSeed doesn't matter.

			// Grow cells
			// Compute concentration fields with COMSOL
			if(model.comsol) {
				// Do COMSOL things
				model.Write("Calculating cell steady state concentrations (COMSOL)","iter");
				// Make the model
				Comsol comsol = new Comsol(model);
				model.Write("\tInitialising geometry", "iter");
				comsol.Initialise();
				model.Write("\tCreating cells", "iter");
				// Create cells in the COMSOL model
				for(CCell cell : model.cellArray) {
					if(cell.type<2) 		comsol.CreateSphere(cell);
					else if(cell.type<6)	comsol.CreateRod(cell);
					else 					throw new IndexOutOfBoundsException("Cell type: " + cell.type);
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
			for(CCell mother : dividingCellArray) {
				CCell daughter = model.DivideCell(mother);
				if(mother.filament) {
					if(rand.Double() < model.filBranchFrequency) {		// If we make a new branch
						model.CreateFilament(mother, daughter, true);
					} else {
						model.TransferFilament(mother, daughter);		// If we insert the cell in the straight filament 
						model.CreateFilament(mother, daughter, false);
					}
				}
			}
			// Advance growth
			model.growthIter++;
			model.growthTime += model.growthTimeStep;
			if(dividingCellArray.size()>0) {
				model.Write(dividingCellArray.size() + " cells divided, total " + model.cellArray.size() + " cells","iter");
//				String cellNumber = "" + dividedCellArray.get(0).Index();
//				for(int ii=1; ii<dividedCellArray.size(); ii++) 	cellNumber += ", " + dividedCellArray.get(ii).Index();
//				model.Write("Cells grown: " + cellNumber,"iter");
			}
			// Reset springs where needed
			model.Write("Resetting springs","iter");
			for(CSpring rod : model.rodSpringArray) 	rod.ResetRestLength();
			for(CSpring fil : model.filSpringArray) 	fil.ResetRestLength();
			// Attach new cells
			final double NNewPerStep = model.attachmentRate*(model.growthTimeStep/3600.0);
			//			N guaranteed	+ 1 the integer of this iteration is not equal to the previous one (this will be wrong for growthIter==0)
			int NNew = (int)NNewPerStep + (int)(model.growthIter*NNewPerStep)==(int)((model.growthIter-1)*NNewPerStep) ? 0:1;
			model.Write("Attaching " + NNew + " new cells", "iter");
			model.Attachment(NNew);

			// Relaxation
			boolean keepMoving = true;
			int relaxationIterInit=model.relaxationIter;
			int nstp=0;
			while(keepMoving) {
				model.Write("Starting relaxation calculations","iter");
				int iter = model.relaxationIter-relaxationIterInit;
				nstp = model.Relaxation();
				model.relaxationIter++;
				model.relaxationTime += model.relaxationTimeStepdt;
				model.Write("Relaxation finished in " + nstp + " solver steps","iter");
				keepMoving = false;
				for(CBall ball : model.ballArray) {
					final double thresholdForce = 1e-20;
					final double thresholdVel = 1e-7;
					if( ball.force.x + ball.force.y + ball.force.z > thresholdForce   ||   ball.vel.x + ball.vel.y + ball.vel.z > thresholdVel ) {
						keepMoving = true;
					}
				}
				// Stop relaxing if we have relaxted too much already
				if(iter==model.relaxationIterSuccessiveMax) {
					if(iter>0)
						model.Write("Maximum successive relaxation steps done, continuing", "warning");
					keepMoving = false;
				}
				// And finally: save stuff
				model.Write("Saving model as serialised file", "iter");
				model.Save();
				ser2mat.Convert(model);
			}
			model.Write("Anchor springs broken/formed: " + Assistant.NAnchorBreak + "/" + Assistant.NAnchorForm + ", net " + (Assistant.NAnchorForm-Assistant.NAnchorBreak) + ", total " + model.anchorSpringArray.size(), "iter");
			model.Write("Filament springs broken: " + Assistant.NFilBreak + ", total " + model.filSpringArray.size(), "iter");
			model.Write("Stick springs broken/formed: " + Assistant.NStickBreak + "/" + Assistant.NStickForm + ", net " + (Assistant.NStickForm-Assistant.NStickBreak) + ", total " + model.stickSpringArray.size(), "iter");
			// Lower beta in ODE solver if too many steps
			if(model.ODEbeta>0.0 && nstp>(int)4e4*model.relaxationTimeStep) {
				if(model.ODEbeta>1e-3) 	model.ODEbeta *= 0.75;
				else 					model.ODEbeta = 0.0;
				model.ODEalpha = 1.0/8.0-model.ODEbeta*0.2;		// alpha is per default a function of beta
				model.Write("Lowered ODE beta to " + model.ODEbeta +  " for next relaxation iteration","warning");
			}
		}
	}
}
