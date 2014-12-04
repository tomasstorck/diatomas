package ibmTest;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.Before;

import ibm.Cell;
import ibm.Model;
import ibm.Vector3d;

public class RodSpringTest {
	Model model;
	Cell rod2, rod4;
	double r;

	@Before
	public void SetUp() {
		model = new Model();
		model.MWX = 10;
		model.rhoX = 100;
		model.Kr = 1e-11;
		r = 0.5e-6; 
		// Rod
		model.radiusCellMax[2] = model.radiusCellMax[4] = r;
		model.lengthCellMax[2] = model.lengthCellMax[4] = 2*model.radiusCellMax[2];
		model.UpdateAmountCellMax();
		double n = (4.0/3.0*Math.PI*Math.pow(r, 3)  +  Math.PI*Math.pow(r, 2)*model.lengthCellMax[2]) * model.rhoX/model.MWX;
		rod2 = new Cell(2, n, 0.0, r, 0.0, 0.0+model.lengthCellMax[2], r, 0.0, false, model); 		// variable radius, fixed aspect
		rod4 = new Cell(4, n, 0.0, r, 0.0, 0.0+model.lengthCellMax[2], r, 0.0, false, model); 		// fixed radius, variable length
	}
	
	@Test
	public void testRod2GetL() {
		Vector3d L = rod2.rodSpringArray.get(0).GetL();
		assertTrue(L.x == model.lengthCellMax[2] && L.y == 0.0 && L.z == 0.0);
	}

	public void testRod4GetL() {
		Vector3d L = rod4.rodSpringArray.get(0).GetL();
		assertTrue(L.x == model.lengthCellMax[2] && L.y == 0.0 && L.z == 0.0);
	}
	
	@Test
	public void testRodResetRestlength() {

		
	}
}
