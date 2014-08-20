package cell;

// Import Apache Commons ODE stuff
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;

public class RelaxationODE implements FirstOrderDifferentialEquations {
	CModel model;
	
	public RelaxationODE(CModel model) {
		this.model = model;
	}
	
	public int getDimension() {
		return model.ballArray.size()*6;
	}
	
	public void computeDerivatives(double t, double[] y, double[] yDot) {	
		// Read data from y
		int ii=0; 				// TODO: Completely redundant? Check via StepperBase
		for(CBall ball : model.ballArray) {
			ball.pos.x = 	y[ii++];
			ball.pos.y = 	y[ii++];
			ball.pos.z = 	y[ii++];
			ball.vel.x = 	y[ii++];
			ball.vel.y = 	y[ii++];
			ball.vel.z = 	y[ii++];
			ball.force.x = 0;	// Clear forces for first use
			ball.force.y = 0;
			ball.force.z = 0;
		}
		// Collision formodel.Ces
		for(int iCell=0; iCell<model.cellArray.size(); iCell++) {
			CCell cell0 = model.cellArray.get(iCell);
			CBall c0b0 = cell0.ballArray[0];
			// Base collision on the cell type
			if(cell0.type<2) {														// cell0 is a ball
				// Check for all remaining cells
				for(int jCell=iCell+1; jCell<model.cellArray.size(); jCell++) {
					CCell cell1 = model.cellArray.get(jCell);
					CBall c1b0 = cell1.ballArray[0];
					double R2 = c0b0.radius + c1b0.radius;
					Vector3d dirn = c0b0.pos.minus(c1b0.pos);
					if(cell1.type<2) {												// The other cell1 is a ball too
						double dist = dirn.norm();
						// do a simple collision detection if close enough
						double d = R2*1.01-dist;									// d is the magnitude of the overlap vector, as defined in the IbM paper
						if(d>0.0) {
							// We have a collision
							Vector3d Fs = dirn.normalise().times(model.Kc*d);
							// Add formodel.Ces
							c0b0.force = c0b0.force.plus(Fs);
							c1b0.force = c1b0.force.minus(Fs);
						}
					} else if(cell1.type<6) {										// cell0 is a ball, cell1 is a rod
						double H2 = 1.5*(model.lengthCellMax[cell1.type] + R2);			// H2 is maximum allowed distance with still change to collide: R0 + R1 + 2*R1*aspect. 1.5 is to make it more robust (stretching)
						if(dirn.x<H2 && dirn.z<H2 && dirn.y<H2) {
							// do a sphere-rod collision detection
							CBall c1b1 = cell1.ballArray[1];
							EricsonObject C = model.DetectLinesegPoint(c1b0.pos, c1b1.pos, c0b0.pos); 			// TODO migrate DetectLinesegPoint to separate class/package
							Vector3d dP = C.dP;
							double dist = C.dist;									// Make distance more accurate
							double sc = C.sc;
							// Collision detection
							double d = R2*1.01-dist;								// d is the magnitude of the overlap vector, as defined in the IbM paper
							if(d>0.0) {
								double f = model.Kc/dist*d;
								Vector3d Fs = dP.times(f);
								// Add these elastic formodel.Ces to the cells
								// both balls in rod
								c1b0.force = c1b0.force.plus(Fs.times(1.0-sc)); 
								c1b1.force = c1b1.force.plus(Fs.times(sc));
								// ball in sphere
								c0b0.force = c0b0.force.minus(Fs);
							}	
						}
					} else {
						throw new RuntimeException("Unknown cell type");
					}
				}
			} else if (cell0.type<6) {												// cell0.type > 1
				CBall c0b1 = cell0.ballArray[1];
				for(int jCell = iCell+1; jCell<model.cellArray.size(); jCell++) {
					CCell cell1 = model.cellArray.get(jCell);
					CBall c1b0 = cell1.ballArray[0];
					double R2 = c0b0.radius + c1b0.radius;
					Vector3d dirn = c0b0.pos.minus(c1b0.pos);
					if(cell1.type<2) {												// cell0 is a rod, the cell1 is a ball
						double H2 = 1.5*(model.lengthCellMax[cell0.type] + R2);			// H2 is maximum allowed distance with still change to collide: R0 + R1 + 2*R1*aspect
						if(dirn.x<H2 && dirn.z<H2 && dirn.y<H2) {
							// do a rod-sphere collision detection
							EricsonObject C = model.DetectLinesegPoint(c0b0.pos, c0b1.pos, c1b0.pos); 
							Vector3d dP = C.dP;
							double dist = C.dist;
							double sc = C.sc;
							// Collision detection
							double d = R2*1.01-dist;								// d is the magnitude of the overlap vector, as defined in the IbM paper
							if(d>0.0) {
								double f = model.Kc/dist*d;
								Vector3d Fs = dP.times(f);
								// Add these elastic formodel.Ces to the cells
								double sc1 = 1-sc;
								// both balls in rod
								c0b0.force = c0b0.force.plus(Fs.times(sc1));
								c0b1.force = c0b1.force.plus(Fs.times(sc));
								// ball in sphere
								c1b0.force = c1b0.force.minus(Fs);
							}	
						}
					} else if (cell1.type<6){										// type>1 --> the other cell is a rod too. This is where it gets tricky
						Vector3d c0b0pos = new Vector3d(c0b0.pos);
						Vector3d c0b1pos = new Vector3d(c0b1.pos);
						Vector3d c1b0pos = new Vector3d(c1b0.pos);
						CBall c1b1 = cell1.ballArray[1];
						Vector3d c1b1pos = new Vector3d(c1b1.pos);
						double H2 = 1.5*( model.lengthCellMax[cell0.type] + model.lengthCellMax[cell1.type] + R2 );		// aspect0*2*R0 + aspect1*2*R1 + R0 + R1
						if(dirn.x<H2 && dirn.z<H2 && dirn.y<H2) {
							// calculate the distance between the segments
							EricsonObject C = model.DetectLinesegLineseg(c0b0pos, c0b1pos, c1b0pos, c1b1pos);
							Vector3d dP = C.dP;					// dP is vector from closest point 2 --> 1
							double dist = C.dist;
							double sc = C.sc;
							double tc = C.tc;
							double d = R2*1.01-dist;								// d is the magnitude of the overlap vector, as defined in the IbM paper
							if(d>0.0) {
								double f = model.Kc/dist*d;
								Vector3d Fs = dP.times(f);
								// Add these elastic formodel.Ces to the cells
								double sc1 = 1-sc;
								double tc1 = 1-tc;
								// both balls in 1st rod
								c0b0.force = c0b0.force.plus(Fs.times(sc1));
								c0b1.force = c0b1.force.plus(Fs.times(sc));
								// both balls in 1st rod
								c1b0.force = c1b0.force.minus(Fs.times(tc1));
								c1b1.force = c1b1.force.minus(Fs.times(tc));
							}
						}
					} else {
						throw new RuntimeException("Unknown cell type");
					}
				}
			} else {
				throw new RuntimeException("Unknown cell type");
			}
		}
		// Calculate gravity+bouyancy, normal formodel.Ces and drag
		for(CBall ball : model.ballArray) {
			// Contact formodel.Ces
			double yPos = ball.pos.y;
			double r = ball.radius;
			if(model.normalForce) {
				if(yPos<r){
					ball.force.y += model.Kw*(r-yPos);
				}
			}
			// Gravity and buoyancy
			if(model.gravity) {
				if(model.gravityZ) {
					ball.force.z += model.G * (model.rhoX-model.rhoWater) * ball.n*model.MWX/model.rhoX;
				} else if(yPos>r*1.1) {			// Only if not already at the floor plus a tiny bit 
					ball.force.y += model.G * (model.rhoX-model.rhoWater) * ball.n*model.MWX/model.rhoX;  //let the ball fall. Note that G is negative 
				}
			}
			// Electrostatic attraction
			if(model.electrostatic) {
				double d = (yPos-r);
				double dlim = model.dlimFactor*(1.0/model.kappa); 
				d = Math.max(d, dlim);			// Limit d to dlim. If it's smaller, we will get horrible solver stiffness 
				ball.force.y += model.kappa*model.Ces*Math.exp(-model.kappa*d) - model.Cvdw/(d*d);
			}
			
			// Velocity damping
			ball.force = ball.force.minus(ball.vel.times(model.Kd));			// TODO Should be v^2
		}
		
		// Elastic formodel.Ces between springs within cells
		for(CRodSpring rod : model.rodSpringArray) {
			CBall ball0 = rod.ballArray[0];
			CBall ball1 = rod.ballArray[1];
			// find difference vector and distance dn between balls (euclidian distance) 
			Vector3d diff = rod.GetL();
			double dn = diff.norm();
			// Get force
			double f = rod.K/dn * (dn - rod.restLength);
			// Hooke's law
			Vector3d Fs = diff.times(f);
			// apply formodel.Ces on balls
			ball0.force = ball0.force.plus(Fs);
			ball1.force = ball1.force.minus(Fs);
		}
		
		// Apply formodel.Ces due to anchor springs
		for(CAnchorSpring anchor : model.anchorSpringArray) {
			Vector3d diff = anchor.GetL();
			double dn = diff.norm();
			// Get force
			double f = anchor.K/dn * (dn - anchor.restLength);
			// Hooke's law
			Vector3d Fs = diff.times(f);
			// apply formodel.Ces on balls
			anchor.ballArray[0].force = anchor.ballArray[0].force.plus(Fs);

		}
		
		// Apply formodel.Ces on sticking springs
		for(CStickSpring stick : model.stickSpringArray) {
			CBall ball0 = stick.ballArray[0];
			CBall ball1 = stick.ballArray[1];
			// find difference vector and distance dn between balls (euclidian distance) 
			Vector3d diff = stick.GetL();
			double dn = diff.norm();
			// Get force
			double f = stick.K/dn * (dn - stick.restLength);
			// Hooke's law
			Vector3d Fs = diff.times(f);
			// apply formodel.Ces on balls
			ball0.force = ball0.force.plus(Fs);
			ball1.force = ball1.force.minus(Fs);
		}
		
		// Filament spring elastic force (CSpring in filSpringArray)
		for(CSpring fil : model.filSpringArray) {
			CBall ball0 = fil.ballArray[0];
			CBall ball1 = fil.ballArray[1];
			{// find difference vector and distance dn between balls (euclidian distance) 
			Vector3d diff = fil.GetL();
			double dn = diff.norm();
			// Get force
			double f = fil.K/dn * (dn - fil.restLength);
			// Hooke's law
			Vector3d Fs = diff.times(f);
			// apply formodel.Ces on balls
			ball0.force = ball0.force.plus(Fs);
			ball1.force = ball1.force.minus(Fs);
			}
		}
		
		// Return results
		int jj=0;
		for(CBall ball : model.ballArray) {
			double m = ball.n*model.MWX;	
			yDot[jj++] = ball.vel.x;						// dpos/dt = v;
			yDot[jj++] = ball.vel.y;
			yDot[jj++] = ball.vel.z;
			yDot[jj++] = ball.force.x/m;					// dvel/dt = a = f/M
			yDot[jj++] = ball.force.y/m;
			yDot[jj++] = ball.force.z/m;
		}
	}
}
