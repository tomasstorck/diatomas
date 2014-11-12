package ibmTest;

import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.Before;

import ibm.Cell;
import ibm.Ball;
import ibm.Model;
import ibm.AnchorSpring;


public class AnchorSpringTest {
	Model model;
	Cell cell0, cell1;
	double r;
	
	@Before
	public void SetUp() {
		model = new Model();
		model.anchoring = true;
		model.MWX = 10;
		model.rhoX = 100;
		// Sphere
		r = 0.5e-6; 
		double n = 4.0/3.0*Math.PI*Math.pow(r, 3) * model.rhoX/model.MWX;
		cell0 = new Cell(0, n, 1.0, r, 1.0, 0, 0, 0, false, model);
		// Rod
		model.radiusCellMax[2] = r;
		model.lengthCellMax[2] = 2*model.radiusCellMax[2];
		n = (4.0/3.0*Math.PI*Math.pow(r, 3)  +  Math.PI*Math.pow(r, 2)*model.lengthCellMax[2]) * model.rhoX/model.MWX;
		cell1 = new Cell(2, n, 1.0, r, 1.0, 1.0+model.lengthCellMax[2], r, 1.0, false, model);
	}
	
	@Test
	public void testSpringAnchorAnchor() {							
		cell0.Anchor();
		cell1.Anchor();
		assertTrue(model.anchorSpringArray.size() == 3 && 
				cell0.anchorSpringArray.get(0).anchorPoint.x == 1.0 &&
				cell0.anchorSpringArray.get(0).anchorPoint.y == 0.0 &&
				cell0.anchorSpringArray.get(0).anchorPoint.z == 1.0 &&
				cell1.anchorSpringArray.get(0).anchorPoint.x == 1.0 &&
				cell1.anchorSpringArray.get(0).anchorPoint.y == 0.0 &&
				cell1.anchorSpringArray.get(0).anchorPoint.z == 1.0 &&
				cell1.anchorSpringArray.get(1).anchorPoint.x == 1.0+model.lengthCellMax[2] &&
				cell1.anchorSpringArray.get(1).anchorPoint.y == 0.0 &&
				cell1.anchorSpringArray.get(1).anchorPoint.z == 1.0);
	}
	
	@Test
	public void testSpringAnchorBreak() {							
		cell0.Anchor();
		cell1.Anchor();
		// Break links by displacing cells
		for(Ball ball : model.ballArray) {
			ball.pos.y += 1.0; 
		}
		model.FormBreak();
		assertTrue(model.anchorSpringArray.size() == 0);
	}
	@Test
	// Form links by putting cells on plane
	public void testSpringAnchorForm() {							
		for(Ball ball : model.ballArray) {
			ball.pos.y = r; 
		}
		model.FormBreak();
		assertTrue(model.anchorSpringArray.size() == 3);
	}
	@Test
	public void testSpringAnchorGetL() {							
		cell0.Anchor();
		cell1.Anchor();
		assertTrue(model.anchorSpringArray.get(0).GetL().norm() == r &&
				model.anchorSpringArray.get(1).GetL().norm() == r && 
				model.anchorSpringArray.get(2).GetL().norm() == r);	
	}
	@Test
	// Try to find out if the rest length reset is done well by anchoring, displacing, resetting
	public void testSpringAnchorResetRestlength() {							
		cell0.Anchor();
		cell1.Anchor();
		// Displace
		for(Ball ball : model.ballArray) {
			ball.pos.y = 1.0;
			for(AnchorSpring spring : ball.cell.anchorSpringArray) {
				spring.ResetRestLength();
			}
		}
		assertTrue(model.anchorSpringArray.get(0).restLength < 1.1 &&
				model.anchorSpringArray.get(0).restLength > 0.9 &&
				model.anchorSpringArray.get(1).restLength < 1.1 &&
				model.anchorSpringArray.get(1).restLength > 0.9 &&
				model.anchorSpringArray.get(2).restLength < 1.1 &&
				model.anchorSpringArray.get(2).restLength > 0.9);	
	}
	@Test
	// See if spring constants are scaled equally for spheres and rods 
	public void testSpringAnchorKScaling() {							
		cell0.Anchor();
		cell1.Anchor();
		// Check for sphere
		double sphereKNorm = cell0.anchorSpringArray.get(0).K * cell0.anchorSpringArray.size();
		double rodKNorm = cell1.anchorSpringArray.get(0).K * cell1.anchorSpringArray.size();
		assertTrue(sphereKNorm == rodKNorm );	
	}
}
