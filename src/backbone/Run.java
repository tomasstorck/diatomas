package backbone;

import java.util.ArrayList;

import NR.Common;

import comsol.Comsol;
import comsol.Server;

import random.rand;
import ser2mat.ser2mat;
import cell.CBall;
import cell.CCell;
import cell.CModel;
import cell.CSpring;
import cell.Vector3d;

public class Run {
	CModel model;
	 
	double restLength;

	public Run(CModel model) {
		this.model = model;
	}
	
	public void Initialise() throws Exception{				

		///////////////////////////////////////////////////////////////////////////////////////////////////////////
		//\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\//
		///////////////////////////////////////////////////////////////////////////////////////////////////////////

		// Set parameters. This overwrites both CModel and supplied arguments
		rand.Seed(model.randomSeed);			// Set seed
		switch(model.simulation) {
		case 0:
			model.Write("Loading parameters for E. coli","");
			/////////////
			// E. COLI //
			/////////////
			model.radiusCellMax[4] = 0.25e-6;
			model.lengthCellMax[4] = 2.5e-6;
			model.NInitCell = 1;
			model.colourByType = false;
			model.normalForce = true;
			model.growthSkipMax = 10;
			model.sticking = model.filament = false;
			model.Kd 	= 1e-13;				// drag force coefficient
			model.Kc 	= 1e-9;					// cell-cell collision
			model.Kw 	= 5e-10;				// wall(substratum)-cell spring
			model.Kr 	= 5e-11;				// internal cell spring
			model.Kf 	= 2e-11;				// filament spring
			model.Kan	= 1e-11;				// anchor
			model.Ks 	= 1e-11;				// sticking
			break;
		case 1: case 2:
			////////
			// AS //
			////////				
			model.radiusCellMax[0] = 0.25e-6 * 1.25;
			model.radiusCellMax[4] = 0.25e-6;
			model.lengthCellMax[4] = 2.5e-6;
			model.NInitCell = 6;
			model.muAvgSimple[0] = 0.33;
			model.muAvgSimple[4] = 0.20;
			model.growthSkipMax = 10;
			model.syntrophyFactor = 1.5;
			model.attachmentRate = 1.0;
			model.filament = model.sticking = true;
			model.sticking = true;
			model.stickRodRod = model.stickSphereSphere = false;
			model.Kd 	= 1e-13;				// drag force coefficient
			model.Kc 	= 1e-9;					// cell-cell collision
			model.Kw 	= 5e-10;				// wall(substratum)-cell spring
			model.Kr 	= 5e-11;				// internal cell spring
			model.Kf 	= 2e-11;				// filament spring
			model.Kan	= 1e-11;				// anchor
			model.Ks 	= 1e-11;				// sticking
			// Anchoring and normalForce are set later
			break;
		default:
			throw new IndexOutOfBoundsException("Model simulation: " + model.simulation);
		}

		model.muAvgSimple[0] = model.muAvgSimple[4] = model.muSpread = 0.0;
		model.attachmentRate = 6.0;
		model.growthSkipMax = 0;
	}
	
	public void Start() throws Exception {
		// Set initial cell parameters based on model
		model.UpdateAmountCellMax();
		switch(model.simulation) {
		case 0:
			model.typeInit = new int[model.NInitCell];
			model.nInit = new double[model.NInitCell];
			model.directionInit = new Vector3d[model.NInitCell];
			model.position0Init = new Vector3d[model.NInitCell];
			model.position1Init = new Vector3d[model.NInitCell];
			restLength = model.lengthCellMax[4]*0.75;
			for(int ii=0; ii<model.NInitCell; ii++) {
				model.typeInit[ii] = 4;
				model.nInit[ii] = 0.5*model.nCellMax[model.typeInit[ii]] * (1.0 + rand.Double());
				model.directionInit[ii] = new Vector3d((rand.Double()-0.5), 			0.0*rand.Double(), 																		(rand.Double()-0.5))			.normalise();
				model.position0Init[ii] = new Vector3d((rand.Double()-0.5)*model.L.x, CBall.Radius(model.nInit[ii]/2.0, model.typeInit[ii], model)+0.0*rand.Double(),			(rand.Double()-0.5)*model.L.z);
				model.position1Init[ii] = model.position0Init[ii].plus(model.directionInit[ii].times(restLength));
			}
			break;
		case 1: case 2:
			// Set type
			model.typeInit = new int[model.NInitCell];
			for(int ii=0; ii<model.NInitCell/2; ii++)						model.typeInit[ii] = 4;			// First half: rods
			for(int ii=model.NInitCell/2+1; ii<model.NInitCell; ii++)		model.typeInit[ii] = 0;			// Second half: spheres
			// Various
			restLength = model.lengthCellMax[4]*0.75;
			model.nInit = new double[model.NInitCell];
			model.directionInit = new Vector3d[model.NInitCell];
			model.position0Init = new Vector3d[model.NInitCell];
			model.position1Init = new Vector3d[model.NInitCell];

			if(model.simulation==1) {
				model.Write("Loading parameters for AS/biofilm","");
				// Biofilm-like
				for(int ii=0; ii<model.NInitCell; ii++) {
					model.nInit[ii] = 0.5*model.nCellMax[model.typeInit[ii]] * (1.0 + rand.Double());
					model.directionInit[ii] = new Vector3d((rand.Double()-0.5), 			(rand.Double()-0.5)+5.0,															(rand.Double()-0.5))			.normalise();
					model.position0Init[ii] = new Vector3d((rand.Double()-0.5)*model.L.x, CBall.Radius(model.nInit[ii]/2.0, model.typeInit[ii], model)+0.0*rand.Double(),		(rand.Double()-0.5)*model.L.z);
					model.position1Init[ii] = model.position0Init[ii].plus(model.directionInit[ii].times(restLength));
				}
				model.anchoring = true;
				model.normalForce = true;
			} else {
				model.Write("Loading parameters for AS/flock","");
				// Flock-like
				for(int ii=0; ii<model.NInitCell; ii++) {
					model.nInit[ii] = 0.5*model.nCellMax[model.typeInit[ii]] * (1.0 + rand.Double());
					model.directionInit[ii] = new Vector3d((rand.Double()-0.5), 			(rand.Double()-0.5), 																(rand.Double()-0.5))			.normalise();
					model.position0Init[ii] = new Vector3d((rand.Double()-0.5)*model.L.x, (rand.Double()-0.5)*model.L.y - (model.typeInit[ii]>1 ? 0.5*restLength:0),			(rand.Double()-0.5)*model.L.z);
					model.position1Init[ii] = model.position0Init[ii].plus(model.directionInit[ii].times(restLength));
				}
				model.anchoring = false;
				model.normalForce = false;
			}
			break;
		default:
			throw new IndexOutOfBoundsException("Model simulation: " + model.simulation);
		}
		
		///////////////////////////////////////////////////////////////////////////////////////////////////////////
		//\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\//
		///////////////////////////////////////////////////////////////////////////////////////////////////////////
		if(model.growthIter == 0 && model.relaxationIter == 0) {
			// Create initial cells
			int[] types = Common.Unique(model.typeInit);
			for(int iCell = 0; iCell < model.NInitCell; iCell++){
				boolean filament = false;
				if(model.filament) {
					if(model.typeInit[iCell]<2) 		filament = model.filSphere; 
					else if(model.typeInit[iCell]<6)	filament = model.filRod;
					else throw new IndexOutOfBoundsException("Cell type: " + model.typeInit); 
				}
				// Use desired colour
				double[] colour;
				if(model.colourByType) {
					int ci=0; for(int ii=0; ii<types.length; ii++)	if(model.typeInit[iCell]==types[ii])	ci = ii;		// Find index of this cell's type in types[]
					colour = model.colour[ci];
				}
				else					colour = model.colour[iCell];
				@SuppressWarnings("unused")
				CCell cell = new CCell(model.typeInit[iCell], 				// Type of biomass
						model.nInit[iCell],
						model.position0Init[iCell],
						model.position1Init[iCell],
						filament,									// With capability to form filaments?
						colour,
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
			ArrayList<CCell> overlapCellArray = new ArrayList<CCell>(0);
			boolean grow = false;
			// Find out if we want to grow
			if(model.growthSkip < model.growthSkipMax) {
				overlapCellArray = model.DetectCellCollision_Proper(1.0);
				if(overlapCellArray.isEmpty())		grow = true;									// Grow if there are no overlapping cells
			} else {
				if(model.growthSkip!=0)				model.Write("Maximum number of growth iters skipped", "warning");	// Warn if we don't always continue with overlap
				grow = true;																		// Grow if we have skipped the maximum number of iterations
			}
			if(grow) {
				model.growthSkip = 0;
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
				ArrayList<CCell> dividedCellArray;
				if(model.comsol) {
					dividedCellArray = model.GrowthFlux();
				} else {
					dividedCellArray = model.GrowthSimple();
				}
				// Advance growth
				model.growthIter++;
				model.growthTime += model.growthTimeStep;
				if(dividedCellArray.size()>0) {
					model.Write(dividedCellArray.size() + " cells divided, total " + model.cellArray.size() + " cells","iter");
					//					String cellNumber = "" + dividedCellArray.get(0).Index();
					//					for(int ii=1; ii<dividedCellArray.size(); ii++) 	cellNumber += ", " + dividedCellArray.get(ii).Index();
					//					model.Write("Cells grown: " + cellNumber,"iter");
				}
				// Reset springs where needed
				model.Write("Resetting springs","iter");
				for(CSpring rod : model.rodSpringArray) {
					rod.ResetRestLength();
				}
				for(CSpring fil : model.filSpringArray) 	{
					fil.ResetRestLength();
				}
				// Attach new cells
				final double NNewPerStep = model.attachmentRate*(model.growthTimeStep/3600.0);
				//			N guaranteed	+ 1 the integer of this iteration is not equal to the previous one (this will be wrong for growthIter==0)
				int NNew = (int)NNewPerStep + (int)(model.growthIter*NNewPerStep)==(int)((model.growthIter-1)*NNewPerStep) ? 0:1;
				model.Write("Attaching " + NNew + " new cells", "iter");
				model.Attachment(NNew);
			} else {
				model.growthSkip++;
				model.Write(overlapCellArray.size()/2 + " overlapping cell pairs detected, growth delayed","iter");
				String cellNumber = "" + overlapCellArray.get(0).Index() + " & " + overlapCellArray.get(1).Index();
				for(int ii=2; ii<overlapCellArray.size(); ii=ii+2) 	cellNumber += ", " + overlapCellArray.get(ii).Index() + " & " + overlapCellArray.get(ii+1).Index();
				model.Write("Cells overlapping: " + cellNumber,"iter");
			}

			// Relaxation
			model.Write("Starting relaxation calculations","iter");
			int nstp = model.Relaxation();
			model.relaxationIter++;
			model.relaxationTime += model.relaxationTimeStepdt;
			model.Write("Relaxation finished in " + nstp + " solver steps","iter");
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

			// And finally: save stuff
			model.Write("Saving model as serialised file", "iter");
			model.Save();
			ser2mat.Convert(model);
		}
	}
}