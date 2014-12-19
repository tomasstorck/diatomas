package ibm;

// Import Apache Commons ODE stuff
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;

public class RelaxationODE implements FirstOrderDifferentialEquations {
	Model model;
	public int NStep, NAnchorBreak, NAnchorForm, NStickBreak, NStickForm, NFilBreak;
	
	public RelaxationODE(Model model) {
		this.model = model;
		this.NStep = 0;
		this.NAnchorBreak = 0;
		this.NAnchorForm = 0;
		this.NStickBreak = 0;
		this.NStickForm = 0;
		this.NFilBreak = 0;
	}
	
	public int getDimension() {
		return model.ballArray.size()*6;
	}
	
	public void computeDerivatives(double t, double[] y, double[] yDot) {	
		// Read data from y
		for(int ii=0; ii<model.ballArray.size(); ii++) {
			Ball ball = model.ballArray.get(ii);
			ball.pos.x = 	y[6*ii];
			ball.pos.y = 	y[6*ii+1];
			ball.pos.z = 	y[6*ii+2];
			ball.vel.x = 	y[6*ii+3];
			ball.vel.y = 	y[6*ii+4];
			ball.vel.z = 	y[6*ii+5];
			ball.force.x = 0;	// Clear forces for first use
			ball.force.y = 0;
			ball.force.z = 0;
		}
		// Collision force
		final double radiusModifier = 1.01; 												// Multiplication factor for ball radii, maintaining a certain distance between balls
		for(int iCell=0; iCell<model.cellArray.size(); iCell++) {
			Cell cell0 = model.cellArray.get(iCell);
			Ball c0b0 = cell0.ballArray[0];
			for(int jCell=iCell+1; jCell<model.cellArray.size(); jCell++) { 		// Factorial elimination to optimise loop
				Cell cell1 = model.cellArray.get(jCell);
				Ball c1b0 = cell1.ballArray[0];
				// Do a very simple, cheap collision detection
				Vector3d dirn = c0b0.pos.minus(c1b0.pos);
				double dist = dirn.norm();
				final double maxCollDist = 								// The maximum distance between two overlapping cells can never be more than sum of:  
						(Common.maxArray(model.lengthCellMax) 			// 1) length of the longest cell
						+ Common.maxArray(model.radiusCellMax) * 2.0) 	// 2) twice the radius of the largest ball
						* 2.0; 											// 3) whatever stretching can be observed due to links (e.g. factor 2, up for discussion)
				// More accurate collision detection if overlap is possible
				if(dist<maxCollDist) { 												// Balls are close enough that they could collide --> further investigate
					double R2 = c0b0.radius + c1b0.radius; 							// We assume radius ball 0 and 1 are equal for all cells
					// Ball-ball collision
					if( cell0.type<2 && cell1.type<2 ) {
						double d = R2*radiusModifier - dist;
						if(d>0.0) {
							// We have a collision
							Vector3d Fs = dirn.normalise().times(model.Kc*d);
							// Add force
							c0b0.force = c0b0.force.plus(Fs);
							c1b0.force = c1b0.force.minus(Fs);
						}
					// Ball-rod (or rod-ball) collision
					} else if( cell0.type<2 || cell1.type<2 ) {
						// Find out which cell is rod, which is ball, and assign
						Ball ballb0, rodb0, rodb1;
						if(cell0.type < 2) {
							ballb0 = c0b0;
							rodb0 = c1b0;
							rodb1 = cell1.ballArray[1];
						} else {
							ballb0 = c1b0;
							rodb0 = c0b0;
							rodb1 = cell0.ballArray[1];
						}
						ericson.ReturnObject C = ericson.DetectCollision.LinesegPoint(rodb0.pos, rodb1.pos, ballb0.pos);
						Vector3d dP = C.dP;
						dist = C.dist;											// Make distance more accurate
						double sc = C.sc;
						double d = R2*radiusModifier - dist;					// d is the magnitude of the overlap vector, as defined in the IbM paper
						if(d>0.0) {
							double f = model.Kc/dist*d;
							Vector3d Fs = dP.times(f);
							// Add these elastic force to the cells
							// ball in sphere
							ballb0.force = ballb0.force.minus(Fs);
							// both balls in rod
							rodb0.force = rodb0.force.plus(Fs.times(1.0-sc)); 
							rodb1.force = rodb1.force.plus(Fs.times(sc));
						}	
					// Rod-rod
					} else if( cell0.type<6 && cell1.type<6 ) {
						Ball c0b1 = cell0.ballArray[1];
						Ball c1b1 = cell1.ballArray[1];
						// calculate the distance between the segments
						ericson.ReturnObject C = ericson.DetectCollision.LinesegLineseg(c0b0.pos, c0b1.pos, c1b0.pos, c1b1.pos);
						Vector3d dP = C.dP;										// dP is vector from closest point 2 --> 1
						dist = C.dist; 											// Make distance more accurate
						double sc = C.sc;
						double tc = C.tc;
						double d = R2*radiusModifier - dist;					// d is the magnitude of the overlap vector, as defined in the IbM paper
						if(d>0.0) {
							double f = model.Kc/dist*d;
							Vector3d Fs = dP.times(f);
							// Add these elastic force to the cells
							double sc1 = 1-sc;
							double tc1 = 1-tc;
							// both balls in 1st rod
							c0b0.force = c0b0.force.plus(Fs.times(sc1));
							c0b1.force = c0b1.force.plus(Fs.times(sc));
							// both balls in 2nd rod
							c1b0.force = c1b0.force.minus(Fs.times(tc1));
							c1b1.force = c1b1.force.minus(Fs.times(tc));
						}
					// Invalid cells
					} else {
						throw new RuntimeException("Unknown cell type");
					}
				}
			}
		}
		// Calculate gravity+bouyancy, normal force and drag
		for(Ball ball : model.ballArray) {
			// Contact force
			double zPos = ball.pos.z;
			double r = ball.radius;
			if(model.normalForce) {
				if(zPos<r){
					ball.force.z += model.Kw*(r-zPos);
				}
			}
			// Gravity and buoyancy
			if(model.gravity) {
				if(model.gravityZ) {
					ball.force.z += model.G * (model.rhoX-model.rhoWater) * ball.n*model.MWX/model.rhoX;
				} else if(zPos>r*1.1) {			// Only if not already at the floor plus a tiny bit 
					ball.force.z += model.G * (model.rhoX-model.rhoWater) * ball.n*model.MWX/model.rhoX;  //let the ball fall. Note that G is negative 
				}
			}
			// Electrostatic attraction
			if(model.electrostatic) {
				double d = (zPos-r);
				double dlim = model.dlimFactor*(1.0/model.kappa); 
				d = Math.max(d, dlim);			// Limit d to dlim. If it's smaller, we will get horrible solver stiffness 
				ball.force.z += model.kappa*model.Ces*Math.exp(-model.kappa*d) - model.Cvdw/(d*d);
			}
			
			// Velocity damping
			ball.force = ball.force.minus(ball.vel.times(model.Kd));			// TODO Should be v^2
		}
		
		// Elastic force between springs within cells
		for(RodSpring rod : model.rodSpringArray) {
			Ball ball0 = rod.ballArray[0];
			Ball ball1 = rod.ballArray[1];
			// find difference vector and distance dn between balls (euclidian distance) 
			Vector3d diff = rod.GetL();
			double dn = diff.norm();
			// Get force
			double f = rod.K/dn * (dn - rod.restLength);
			// Hooke's law
			Vector3d Fs = diff.times(f);
			// apply force on balls
			ball0.force = ball0.force.plus(Fs);
			ball1.force = ball1.force.minus(Fs);
		}
		
		// Apply force due to anchor springs
		for(AnchorSpring anchor : model.anchorSpringArray) {
			Vector3d diff = anchor.GetL();
			double dn = diff.norm();
			// Get force
			double f = anchor.K/dn * (dn - anchor.restLength);
			// Hooke's law
			Vector3d Fs = diff.times(f);
			// apply force on balls
			anchor.ballArray[0].force = anchor.ballArray[0].force.plus(Fs);

		}
		
		// Apply force on sticking springs
		for(StickSpring stick : model.stickSpringArray) {
			Ball ball0 = stick.ballArray[0];
			Ball ball1 = stick.ballArray[1];
			// find difference vector and distance dn between balls (euclidian distance) 
			Vector3d diff = stick.GetL();
			double dn = diff.norm();
			// Get force
			double f = stick.K/dn * (dn - stick.restLength);
			// Hooke's law
			Vector3d Fs = diff.times(f);
			// apply force on balls
			ball0.force = ball0.force.plus(Fs);
			ball1.force = ball1.force.minus(Fs);
		}
		
		// Filament spring elastic force (CSpring in filSpringArray)
		for(Spring fil : model.filSpringArray) {
			Ball ball0 = fil.ballArray[0];
			Ball ball1 = fil.ballArray[1];
			{// find difference vector and distance dn between balls (euclidian distance) 
			Vector3d diff = fil.GetL();
			double dn = diff.norm();
			// Get force
			double f = fil.K/dn * (dn - fil.restLength);
			// Hooke's law
			Vector3d Fs = diff.times(f);
			// apply force on balls
			ball0.force = ball0.force.plus(Fs);
			ball1.force = ball1.force.minus(Fs);
			}
		}
		// Return results
		for(int ii=0; ii<model.ballArray.size(); ii++) {
			Ball ball = model.ballArray.get(ii);
			double m = ball.n*model.MWX;	
			yDot[6*ii  ] = ball.vel.x;						// dpos/dt = v;
			yDot[6*ii+1] = ball.vel.y;
			yDot[6*ii+2] = ball.vel.z;
			yDot[6*ii+3] = ball.force.x/m;					// dvel/dt = a = f/M
			yDot[6*ii+4] = ball.force.y/m;
			yDot[6*ii+5] = ball.force.z/m;
		}
	}
}
