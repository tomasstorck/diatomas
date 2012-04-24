package cell;

import java.util.ArrayList;

import NR.*;

public class Run {

	public Run(CModel model, boolean enablePlot){
		// Initialise random seed
		rand.Seed(model.randomSeed);

		// Create initial cells
		for(int iCell = 0; iCell < model.NInitCell; iCell++){
			new CCell(rand.Int(model.NType+1), 	// 0, 1 or 2 by default (specified numer is exclusive)
					rand.Double()*model.L.x, 					// Anywhere between 0 and Lx
					1e-4, 										// Standard height
					rand.Double()*model.L.z,					// Anywhere between 0 and Lz
					true,										// With filament
					model);										// And a pointer to the model
		}

		model.Write(model.cellArray.size() + " initial cells created","iter");

		for(int iteration=0; iteration<100; iteration++){					// Softcode this TODO
			// Reset the random seed
			rand.Seed(model.randomSeed*(2+model.growthIter));				// + something because if growthIter == 0, randomSeed doesn't matter. 

			// Movement
			model.Write("Starting movement calculations","iter");
			int nvar = 6*CModel.NBall;
			int ntimes = (int) (model.movementTimeEnd/model.movementTimeStep);
			double atol = 1.0e-6, rtol = atol;
			double h1 = 0.00001, hmin = 0;
			double t1 = model.movementTime; 
			double t2 = t1 + model.movementTime + model.movementTimeEnd;
			NRvector<Double> ystart = new NRvector<Double>(nvar,0.0);

			{int ii=0;											// Determine initial value vector
			for(CBall pBall : model.BallArray()) { 
				ystart.set(ii++, pBall.pos.x);
				ystart.set(ii++, pBall.pos.y);
				ystart.set(ii++, pBall.pos.z);
				ystart.set(ii++, pBall.vel.x);
				ystart.set(ii++, pBall.vel.x);
				ystart.set(ii++, pBall.vel.x);
			}}
			Output<StepperDopr853> out = new Output<StepperDopr853>(ntimes);
			feval dydt = new feval(model);
			Odeint<StepperDopr853> ode = new Odeint<StepperDopr853>(ystart, t1, t2, atol, rtol, h1, hmin, out, dydt);
			int nstp = ode.integrate();
			for(int iTime=0; iTime<out.count; iTime++) {
				int iVar = 0;
				for(CBall pBall : model.BallArray()) {
					pBall.pos.x = out.ysave.get(iVar++,iTime);
					pBall.pos.y = out.ysave.get(iVar++,iTime);
					pBall.pos.z = out.ysave.get(iVar++,iTime);
					pBall.vel.x = out.ysave.get(iVar++,iTime);
					pBall.vel.y = out.ysave.get(iVar++,iTime);
					pBall.vel.z = out.ysave.get(iVar++,iTime);
				}
				// save POV TODO
			}
			// Advance movement
			model.movementIter++;
			model.movementTime += model.movementTimeStep;
			model.Write("Movement finished in " + nstp + " solver steps","iter");

			// Break anchor springs
			// {} to make sure objects are destroyed when we're done (aka scope)
			{ArrayList<CAnchorSpring> breakArray = model.DetectAnchorBreak();	// This one will return a nice and unique ArrayList, though perhaps already anchored cells 
			model.anchorSpringArray.removeAll(breakArray);
			model.Write(breakArray.size() + " anchor springs broken","iter");
			// Build anchor springs
			model.Write("Detecting cell-floor collisions","iter");
			ArrayList<CCell> collisionArray = model.DetectFloorCollision();
			int NnewAnchor = model.BuildAnchor(collisionArray);
			model.Write(NnewAnchor + " anchor springs built","iter");}

			// Break stick springs
//			{ArrayList<CStickSpring> breakArray = model.DetectStickBreak();
//			model.stickSpringArray.removeAll(breakArray);
//			model.Write(breakArray.size() + " sticking springs broken","iter");
			// Build stick springs
			model.Write("Detecting cell-cell collisions","iter");
			ArrayList<CCell> collisionArray = model.DetectCellCollision_Simple();	 // Note that this one returns already stuck and duplicate cells
			model.Write("Building new sticking springs","iter");
			int NnewStick = model.BuildStick(collisionArray);
			model.Write(NnewStick + " cell pairs sticked","iter");};			// Divided by two, as array is based on origin and other cell (see for loop)

			// Plot
			if(enablePlot) {
				model.Write("Writing POV files","iter");
				model.POV_Write();
				model.POV_Plot(); 
			}
			// Grow cells
			{int newCell = model.GrowCell();
			model.Write(newCell + " new cells grown, total " + model.cellArray.size() + " cells","iter");}
			// Advance growth
			model.growthIter++;
			model.growthTime += model.growthTimeStep;
	}
}