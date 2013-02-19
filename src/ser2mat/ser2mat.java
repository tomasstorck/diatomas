package ser2mat;

import java.io.IOException;
import java.util.ArrayList;

import cell.*;
import jmatio.*;

public class ser2mat {
	public static void Convert(CModel model) {
		MLStructure mlModel = new MLStructure("model", new int[] {1,1});
		int N;
		double[] arrayIndex;
		// Set serializable information
		// Model miscellaneous settings
		mlModel.setField("name",                          new MLChar(null, new String[] {model.name}, model.name.length()));              	
		mlModel.setField("simulation",                    new MLDouble(null, new double[] {model.simulation}, 1));                        	// The simulation type: see Run
		mlModel.setField("randomSeed",                    new MLDouble(null, new double[] {model.randomSeed}, 1));                        	
		mlModel.setField("comsol",                        new MLDouble(null, new double[] {model.comsol?1:0}, 1));                        	
		mlModel.setField("NXType",                        new MLDouble(null, new double[] {model.NXType}, 1));                            	
		// --> Sticking
		mlModel.setField("sticking",                      new MLDouble(null, new double[] {model.sticking?1:0}, 1));                      	
		mlModel.setField("stickType",                     new MLDouble(null, boolean2double(model.stickType)));                           
		// --> Anchoring
		mlModel.setField("anchoring",                     new MLDouble(null, new double[] {model.anchoring?1:0}, 1));                     	
		// --> Filaments
		mlModel.setField("filament",                      new MLDouble(null, new double[] {model.filament?1:0}, 1));                      	
		mlModel.setField("filamentType",                  new MLDouble(null, boolean2double(model.filamentType), model.filamentType.length));
		mlModel.setField("sphereStraightFil",             new MLDouble(null, new double[] {model.sphereStraightFil?1:0}, 1));             	// Make streptococci-like structures if true, otherwise staphylococci
		mlModel.setField("gravity",                       new MLDouble(null, new double[] {model.gravity?1:0}, 1));                       	
		mlModel.setField("gravityZ",                      new MLDouble(null, new double[] {model.gravityZ?1:0}, 1));                      	
		// --> Substratum
		mlModel.setField("normalForce",                   new MLDouble(null, new double[] {model.normalForce?1:0}, 1));                   	// Use normal force to simulate cells colliding with substratum (at y=0)
		mlModel.setField("initialAtSubstratum",           new MLDouble(null, new double[] {model.initialAtSubstratum?1:0}, 1));           	// All initial balls are positioned at y(t=0) = ball.radius
		// Domain properties
		mlModel.setField("L",                             new MLDouble(null, new double[] {model.L.x, model.L.y, model.L.z}, 3));         	
		mlModel.setField("G",                             new MLDouble(null, new double[] {model.G}, 1));                                 	// [m/s2], acceleration due to gravity
		mlModel.setField("rhoWater",                      new MLDouble(null, new double[] {model.rhoWater}, 1));                          	// [kg/m3], density of bulk liquid (water)
		mlModel.setField("rhoX",                          new MLDouble(null, new double[] {model.rhoX}, 1));                              	// [kg/m3], diatoma density
		mlModel.setField("MWX",                           new MLDouble(null, new double[] {model.MWX}, 1));                               	// [kg/mol], composition CH1.8O0.5N0.2
		// Initial cell properties
		//
		double[] DtypeInit = new double[model.typeInit.length];		for(int ii=0; ii<model.typeInit.length; ii++)		DtypeInit[ii] = model.typeInit[ii];		mlModel.setField("typeInit",                      new MLDouble(null, DtypeInit, model.typeInit.length));                          	
		//
		mlModel.setField("nInit",                         new MLDouble(null, model.nInit, model.nInit.length));                           	
		// Spring constants and drag coefficient
		mlModel.setField("Kd",                            new MLDouble(null, new double[] {model.Kd}, 1));                                	// drag force coefficient
		mlModel.setField("Kc",                            new MLDouble(null, new double[] {model.Kc}, 1));                                	// cell-cell collision
		mlModel.setField("Kw",                            new MLDouble(null, new double[] {model.Kw}, 1));                                	// wall(substratum)-cell spring
		mlModel.setField("Kr",                            new MLDouble(null, new double[] {model.Kr}, 1));                                	// internal cell spring
		mlModel.setField("KfSphere",                      new MLDouble(null, new double[] {model.KfSphere}, 1));                          	// filament spring for sphere-sphere filial links
		mlModel.setField("KfRod0",                        new MLDouble(null, new double[] {model.KfRod0}, 1));                            	// filament spring for rod-rod filial links, short spring
		mlModel.setField("KfRod1",                        new MLDouble(null, new double[] {model.KfRod1}, 1));                            	// filament spring for rod-rod filial links, long sprong
		mlModel.setField("Kan",                           new MLDouble(null, new double[] {model.Kan}, 1));                               	// anchor
		mlModel.setField("Ks",                            new MLDouble(null, new double[] {model.Ks}, 1));                                	// sticking
		mlModel.setField("anchorStretchLim",              new MLDouble(null, new double[] {model.anchorStretchLim}, 1));                  	// Maximum tension for anchoring springs
		mlModel.setField("anchorFormLim",                 new MLDouble(null, new double[] {model.anchorFormLim}, 1));                     	// Multiplication factor for rest length to form anchors. Note that actual rest length is the distance between the two, which could be less
		mlModel.setField("stickStretchLim",               new MLDouble(null, new double[] {model.stickStretchLim}, 1));                   	// Maximum tension for sticking springs
		mlModel.setField("stickFormLim",                  new MLDouble(null, new double[] {model.stickFormLim}, 1));                      	// Multiplication factor for rest length to form sticking springs.
		mlModel.setField("filStretchLim",                 new MLDouble(null, new double[] {model.filStretchLim}, 1));                     	// Maximum tension for sticking springs
		mlModel.setField("filLengthSphere",               new MLDouble(null, new double[] {model.filLengthSphere}, 1));                   	// How many times R2 the sphere filament's rest length is
		mlModel.setField("filLengthRod",                  new MLDouble(null, model.filLengthRod, model.filLengthRod.length));             	// How many times R2 the rod filament's [0] short and [1] long spring rest length is
		// Model biomass and growth properties
		mlModel.setField("NdComp",                        new MLDouble(null, new double[] {model.NdComp}, 1));                            	// d for dynamic compound (e.g. total Ac)
		mlModel.setField("NcComp",                        new MLDouble(null, new double[] {model.NcComp}, 1));                            	// c for concentration (or virtual compound, e.g. Ac-)
		mlModel.setField("NAcidDiss",                     new MLDouble(null, new double[] {model.NAcidDiss}, 1));                         	// Number of acid dissociation reactions
		mlModel.setField("NInitCell",                     new MLDouble(null, new double[] {model.NInitCell}, 1));                         	// Initial number of cells
		mlModel.setField("radiusCellMax",                 new MLDouble(null, model.radiusCellMax, model.radiusCellMax.length));           	
		mlModel.setField("radiusCellMin",                 new MLDouble(null, model.radiusCellMin, model.radiusCellMin.length));           	
		mlModel.setField("lengthCellMax",                 new MLDouble(null, model.lengthCellMax, model.lengthCellMax.length));           	
		mlModel.setField("lengthCellMin",                 new MLDouble(null, model.lengthCellMin, model.lengthCellMin.length));           	
		mlModel.setField("nCellMax",                      new MLDouble(null, model.nCellMax, model.nCellMax.length));                     	
		mlModel.setField("nCellMin",                      new MLDouble(null, model.nCellMin, model.nCellMin.length));                     	
		mlModel.setField("muAvgSimple",                   new MLDouble(null, model.muAvgSimple, model.muAvgSimple.length));               	// [h-1] 0.33  == doubling every 20 minutes. Only used in GrowthSimple!
		mlModel.setField("muStDev",                       new MLDouble(null, model.muStDev, model.muStDev.length));                       	// Standard deviation. Only used in GrowthSimple()!
		mlModel.setField("attachmentRate",                new MLDouble(null, new double[] {model.attachmentRate}, 1));                    	// [h-1] Number of cells newly attached per hour
		mlModel.setField("syntrophyFactor",               new MLDouble(null, new double[] {model.syntrophyFactor}, 1));                   	// Accelerated growth if two cells of different types are stuck to each other
		mlModel.setField("growthSkipMax",                 new MLDouble(null, new double[] {model.growthSkipMax}, 1));                     	// The maximum number of growth iterations we are allowed to skip before we should do growth again
		mlModel.setField("growthSkip",                    new MLDouble(null, new double[] {model.growthSkip}, 1));                        	// How many growth iterations we have skipped
		// Progress
		mlModel.setField("growthTime",                    new MLDouble(null, new double[] {model.growthTime}, 1));                        	// [s] Current time for the growth
		mlModel.setField("growthTimeStep",                new MLDouble(null, new double[] {model.growthTimeStep}, 1));                    	// [s] Time step for growth
		mlModel.setField("growthIter",                    new MLDouble(null, new double[] {model.growthIter}, 1));                        	// [-] Counter time iterations for growth
		mlModel.setField("relaxationTime",                new MLDouble(null, new double[] {model.relaxationTime}, 1));                    	// [s] initial time for relaxation (for ODE solver)
		mlModel.setField("relaxationTimeStepdt",          new MLDouble(null, new double[] {model.relaxationTimeStepdt}, 1));              	// [s] output time step  for relaxation
		mlModel.setField("relaxationTimeStep",            new MLDouble(null, new double[] {model.relaxationTimeStep}, 1));                	// [s] time interval for relaxation (for ODE solver), 5*relaxationTimeStep by default
		mlModel.setField("relaxationIter",                new MLDouble(null, new double[] {model.relaxationIter}, 1));                    	// [-] counter time iterations for relaxation
		// Arrays

		// cellArray
		N = model.cellArray.size();
		MLStructure mlcellArray = new MLStructure(null, new int[] {model.cellArray.size() ,1});
		for(int ii=0; ii<N; ii++) {
			CCell obj = model.cellArray.get(ii);
			mlcellArray.setField("type",                      new MLDouble(null, new double[] {obj.type}, 1), ii);                            	
			mlcellArray.setField("filament",                  new MLDouble(null, new double[] {obj.filament?1:0}, 1), ii);                    	
			
			arrayIndex = new double[obj.ballArray.length];
			for(int jj=0; jj<obj.ballArray.length; jj++)	arrayIndex[jj] = obj.ballArray[jj].Index();
			mlcellArray.setField("ballArray",                 new MLDouble(null, arrayIndex, 1), ii);                                         	// Note that this ballArray has the same name as CModel's
			
			arrayIndex = new double[obj.rodSpringArray.size()];
			for(int jj=0; jj<obj.rodSpringArray.size(); jj++)	arrayIndex[jj] = obj.rodSpringArray.get(jj).Index();
			mlcellArray.setField("rodSpringArray",            new MLDouble(null, arrayIndex, 1), ii);                                         	
			
			arrayIndex = new double[obj.stickCellArray.size()];
			for(int jj=0; jj<obj.stickCellArray.size(); jj++)	arrayIndex[jj] = obj.stickCellArray.get(jj).Index();
			mlcellArray.setField("stickCellArray",            new MLDouble(null, arrayIndex, 1), ii);                                         	
			
			arrayIndex = new double[obj.stickSpringArray.size()];
			for(int jj=0; jj<obj.stickSpringArray.size(); jj++)	arrayIndex[jj] = obj.stickSpringArray.get(jj).Index();
			mlcellArray.setField("stickSpringArray",          new MLDouble(null, arrayIndex, 1), ii);                                         	
			
			arrayIndex = new double[obj.anchorSpringArray.size()];
			for(int jj=0; jj<obj.anchorSpringArray.size(); jj++)	arrayIndex[jj] = obj.anchorSpringArray.get(jj).Index();
			mlcellArray.setField("anchorSpringArray",         new MLDouble(null, arrayIndex, 1), ii);                                         	
			
			arrayIndex = new double[obj.filSpringArray.size()];
			for(int jj=0; jj<obj.filSpringArray.size(); jj++)	arrayIndex[jj] = obj.filSpringArray.get(jj).Index();
			mlcellArray.setField("filSpringArray",            new MLDouble(null, arrayIndex, 1), ii);                                         	
			// Set mother	
			if(obj.mother==null)
				mlcellArray.setField("mother",                    new MLDouble(null, new double[] {}, 0), ii);                                    
			else
				mlcellArray.setField("mother",                    new MLDouble(null, new double[] {obj.mother.Index()}, 1), ii);                  
			mlcellArray.setField("born",                      new MLDouble(null, new double[] {obj.born}, 1), ii);                            	// Growth iteration at which this cell was born
			mlcellArray.setField("q",                         new MLDouble(null, new double[] {obj.q}, 1), ii);                               	// [mol reactions (CmolX * s)-1]
		}
		mlModel.setField("cellArray", mlcellArray);

		// ballArray
		N = model.ballArray.size();
		MLStructure mlballArray = new MLStructure(null, new int[] {model.ballArray.size() ,1});
		for(int ii=0; ii<N; ii++) {
			CBall obj = model.ballArray.get(ii);
			mlballArray.setField("n",                         new MLDouble(null, new double[] {obj.n}, 1), ii);                               	// [mol] Chemical amount
			mlballArray.setField("radius",                    new MLDouble(null, new double[] {obj.radius}, 1), ii);                          	
			mlballArray.setField("pos",                       new MLDouble(null, new double[] {obj.pos.x, obj.pos.y, obj.pos.z}, 3), ii);     	
			mlballArray.setField("vel",                       new MLDouble(null, new double[] {obj.vel.x, obj.vel.y, obj.vel.z}, 3), ii);     	
			mlballArray.setField("force",                     new MLDouble(null, new double[] {obj.force.x, obj.force.y, obj.force.z}, 3), ii);	
			//posSave
			{int N2 = (int) obj.posSave.length;
			double[][] posSave = new double[N2][3];
			for(int jj=0; jj<N2; jj++) {
				posSave[jj][0] = obj.posSave[jj].x;
				posSave[jj][1] = obj.posSave[jj].y;
				posSave[jj][2] = obj.posSave[jj].z;
			}
			mlballArray.setField("posSave",                   new MLDouble(null, posSave));}                                                  
			//velSave
			{int N2 = (int) obj.velSave.length;
			double[][] velSave = new double[N2][3];
			for(int jj=0; jj<N2; jj++) {
				velSave[jj][0] = obj.velSave[jj].x;
				velSave[jj][1] = obj.velSave[jj].y;
				velSave[jj][2] = obj.velSave[jj].z;
			}
			mlballArray.setField("velSave",                   new MLDouble(null, velSave));}                                                  
			// Set cell	
			if(obj.cell==null)
				mlballArray.setField("cell",                      new MLDouble(null, new double[] {}, 0), ii);                                    
			else
				mlballArray.setField("cell",                      new MLDouble(null, new double[] {obj.cell.Index()}, 1), ii);                    
		}
		mlModel.setField("ballArray", mlballArray);

		// rodSpringArray
		N = model.rodSpringArray.size();
		MLStructure mlrodSpringArray = new MLStructure(null, new int[] {model.rodSpringArray.size() ,1});
		for(int ii=0; ii<N; ii++) {
			CSpring obj = model.rodSpringArray.get(ii);
			
			arrayIndex = new double[obj.ballArray.length];
			for(int jj=0; jj<obj.ballArray.length; jj++)	arrayIndex[jj] = obj.ballArray[jj].Index();
			mlrodSpringArray.setField("ballArray",            new MLDouble(null, arrayIndex, 1), ii);                                         	
			mlrodSpringArray.setField("anchorPoint",          new MLDouble(null, new double[] {obj.anchorPoint.x, obj.anchorPoint.y, obj.anchorPoint.z}, 3), ii);	
			mlrodSpringArray.setField("K",                    new MLDouble(null, new double[] {obj.K}, 1), ii);                               	
			mlrodSpringArray.setField("restLength",           new MLDouble(null, new double[] {obj.restLength}, 1), ii);                      	
			mlrodSpringArray.setField("type",                 new MLDouble(null, new double[] {obj.type}, 1), ii);                            	
			
			arrayIndex = new double[obj.siblingArray.size()];
			for(int jj=0; jj<obj.siblingArray.size(); jj++)	arrayIndex[jj] = obj.siblingArray.get(jj).Index();
			mlrodSpringArray.setField("siblingArray",         new MLDouble(null, arrayIndex, 1), ii);                                         	
		}
		mlModel.setField("rodSpringArray", mlrodSpringArray);

		// stickSpringArray
		N = model.stickSpringArray.size();
		MLStructure mlstickSpringArray = new MLStructure(null, new int[] {model.stickSpringArray.size() ,1});
		for(int ii=0; ii<N; ii++) {
			CSpring obj = model.stickSpringArray.get(ii);
			
			arrayIndex = new double[obj.ballArray.length];
			for(int jj=0; jj<obj.ballArray.length; jj++)	arrayIndex[jj] = obj.ballArray[jj].Index();
			mlstickSpringArray.setField("ballArray",          new MLDouble(null, arrayIndex, 1), ii);                                         	
			mlstickSpringArray.setField("anchorPoint",        new MLDouble(null, new double[] {obj.anchorPoint.x, obj.anchorPoint.y, obj.anchorPoint.z}, 3), ii);	
			mlstickSpringArray.setField("K",                  new MLDouble(null, new double[] {obj.K}, 1), ii);                               	
			mlstickSpringArray.setField("restLength",         new MLDouble(null, new double[] {obj.restLength}, 1), ii);                      	
			mlstickSpringArray.setField("type",               new MLDouble(null, new double[] {obj.type}, 1), ii);                            	
			
			arrayIndex = new double[obj.siblingArray.size()];
			for(int jj=0; jj<obj.siblingArray.size(); jj++)	arrayIndex[jj] = obj.siblingArray.get(jj).Index();
			mlstickSpringArray.setField("siblingArray",       new MLDouble(null, arrayIndex, 1), ii);                                         	
		}
		mlModel.setField("stickSpringArray", mlstickSpringArray);

		// filSpringArray
		N = model.filSpringArray.size();
		MLStructure mlfilSpringArray = new MLStructure(null, new int[] {model.filSpringArray.size() ,1});
		for(int ii=0; ii<N; ii++) {
			CSpring obj = model.filSpringArray.get(ii);
			
			arrayIndex = new double[obj.ballArray.length];
			for(int jj=0; jj<obj.ballArray.length; jj++)	arrayIndex[jj] = obj.ballArray[jj].Index();
			mlfilSpringArray.setField("ballArray",            new MLDouble(null, arrayIndex, 1), ii);                                         	
			mlfilSpringArray.setField("anchorPoint",          new MLDouble(null, new double[] {obj.anchorPoint.x, obj.anchorPoint.y, obj.anchorPoint.z}, 3), ii);	
			mlfilSpringArray.setField("K",                    new MLDouble(null, new double[] {obj.K}, 1), ii);                               	
			mlfilSpringArray.setField("restLength",           new MLDouble(null, new double[] {obj.restLength}, 1), ii);                      	
			mlfilSpringArray.setField("type",                 new MLDouble(null, new double[] {obj.type}, 1), ii);                            	
			
			arrayIndex = new double[obj.siblingArray.size()];
			for(int jj=0; jj<obj.siblingArray.size(); jj++)	arrayIndex[jj] = obj.siblingArray.get(jj).Index();
			mlfilSpringArray.setField("siblingArray",         new MLDouble(null, arrayIndex, 1), ii);                                         	
		}
		mlModel.setField("filSpringArray", mlfilSpringArray);

		// anchorSpringArray
		N = model.anchorSpringArray.size();
		MLStructure mlanchorSpringArray = new MLStructure(null, new int[] {model.anchorSpringArray.size() ,1});
		for(int ii=0; ii<N; ii++) {
			CSpring obj = model.anchorSpringArray.get(ii);
			
			arrayIndex = new double[obj.ballArray.length];
			for(int jj=0; jj<obj.ballArray.length; jj++)	arrayIndex[jj] = obj.ballArray[jj].Index();
			mlanchorSpringArray.setField("ballArray",         new MLDouble(null, arrayIndex, 1), ii);                                         	
			mlanchorSpringArray.setField("anchorPoint",       new MLDouble(null, new double[] {obj.anchorPoint.x, obj.anchorPoint.y, obj.anchorPoint.z}, 3), ii);	
			mlanchorSpringArray.setField("K",                 new MLDouble(null, new double[] {obj.K}, 1), ii);                               	
			mlanchorSpringArray.setField("restLength",        new MLDouble(null, new double[] {obj.restLength}, 1), ii);                      	
			mlanchorSpringArray.setField("type",              new MLDouble(null, new double[] {obj.type}, 1), ii);                            	
			
			arrayIndex = new double[obj.siblingArray.size()];
			for(int jj=0; jj<obj.siblingArray.size(); jj++)	arrayIndex[jj] = obj.siblingArray.get(jj).Index();
			mlanchorSpringArray.setField("siblingArray",      new MLDouble(null, arrayIndex, 1), ii);                                         	
		}
		mlModel.setField("anchorSpringArray", mlanchorSpringArray);
		// === SOLVER STUFF ===
		mlModel.setField("ODEbeta",                       new MLDouble(null, new double[] {model.ODEbeta}, 1));                           	
		mlModel.setField("ODEalpha",                      new MLDouble(null, new double[] {model.ODEalpha}, 1));                          	
		// === COMSOL STUFF ===
		// Biomass, assuming Cmol and composition CH1.8O0.5N0.2 (i.e. MW = 24.6 g/mol)
		//							type 0					type 1					type 2					type 3					type 4					type 5
		// 							m. hungatei				m. hungatei				s. fumaroxidans			s. fumaroxidans			s. fumaroxidans			s. fumaroxidans
		mlModel.setField("SMX",                           new MLDouble(null, model.SMX, model.SMX.length));                               	// [Cmol X/mol reacted] Biomass yields per flux reaction. All types from Scholten 2000, grown in coculture on propionate
		mlModel.setField("K",                             new MLDouble(null, model.K, model.K.length));                                   	// 
		mlModel.setField("qMax",                          new MLDouble(null, model.qMax, model.qMax.length));                             	// [mol (Cmol*s)-1] M.h. from Robinson 1984, assuming yield, growth on NaAc in coculture. S.f. from Scholten 2000;
		mlModel.setField("rateEquation",                  new MLChar(null, model.rateEquation));                                          	
		// 	 pH calculations
		//							HPro		CO2			HCO3-		HAc
		//							0,			1,			2,			3
		mlModel.setField("Ka",                            new MLDouble(null, model.Ka, model.Ka.length));                                 	// From Wikipedia 120811. CO2 and H2CO3 --> HCO3- + H+;
		mlModel.setField("pHEquation",                    new MLChar(null, model.pHEquation));                                            	// pH calculations
		// Diffusion
		// 							ProT, 		CO2T,				AcT,				H2, 				CH4
		//							0,    		1,   				2, 					3,   				4
		mlModel.setField("BCConc",                        new MLDouble(null, model.BCConc, model.BCConc.length));                         	
		mlModel.setField("D",                             new MLDouble(null, model.D, model.D.length));                                   	
		mlModel.setField("SMdiffusion",                   new MLDouble(null, model.SMdiffusion));                                         

		// Create a list and add mlModel
		ArrayList<MLArray> list = new ArrayList<MLArray>(1);
		list.add(mlModel);
		try {
			new MatFileWriter(model.name + "/output/" + String.format("g%04dr%04d", model.growthIter, model.relaxationIter) + ".mat",list);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	// Converts a boolean[] to a double[] with true == 1 and false == 0 (MATLAB style)
	public static double[] boolean2double(boolean[] input) {
		double[] output = new double[input.length]; 
			for(int ii=0; ii<input.length; ii++)
				output[ii] = input[ii] ? 1.0 : 0.0;
		return output;
	}
	// Converts a boolean[][] to a double[][] with true == 1 and false == 0 (MATLAB style)
	public static double[][] boolean2double(boolean[][] input) {
		double[][] output = new double[input.length][input[0].length]; 
			for(int ii=0; ii<input.length; ii++)
				for(int jj=0; jj<input[0].length; jj++)
					output[ii][jj] = input[ii][jj] ? 1.0 : 0.0;
		return output;
	}
}
