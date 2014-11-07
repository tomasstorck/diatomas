package ibmTest;

import static org.junit.Assert.*;

import org.junit.Test;

import ibm.Cell;
import ibm.Model;

import java.util.ArrayList;

public class ModelTest {

	@Test
	public void testCModelInitialisation() {				
		Model model = new Model();
		assertTrue(model.simulation == 0);
	}
	
	@Test
	public void testCModelCellIndex() {							
		Model model = new Model();
		Cell cell0 = new Cell(0, 0, 0, 0, 0, 0, 0, 0, false, model);
		Cell cell1 = new Cell(0, 0, 0, 0, 0, 0, 0, 0, false, model);
		assertTrue(cell0.Index() == 0 && cell1.Index() == 1);
	}
	
	/* Radius and mass */
	
	@Test
	public void testCModelSphereRadius() {
		Model model = new Model();
		model.MWX = 10;
		model.rhoX = 100;
		double r = 0.5e-6; 
		double n = 4.0/3.0*Math.PI*Math.pow(r, 3) * model.rhoX/model.MWX; 
		Cell cell0 = new Cell(0, n, 0, 0, 0, 0, 0, 0, false, model);
		assertTrue(cell0.ballArray[0].radius < r+1e-9  &&  cell0.ballArray[0].radius > r-1e-9);
	}

	@Test
	public void testCModelRodRadius() {
		Model model = new Model();
		model.MWX = 10;
		model.rhoX = 100;
		model.radiusCellMax[2] = 0.5e-6;
		model.lengthCellMax[2] = 2*model.radiusCellMax[2];
		double r = 0.5e-6; 
		double n = (4.0/3.0*Math.PI*Math.pow(r, 3)  +  Math.PI*Math.pow(r, 2)*model.lengthCellMax[2]) * model.rhoX/model.MWX;
		Cell cell0 = new Cell(2, n, 0, 0, 0, 0, 0, 0, false, model);
		assertTrue(cell0.ballArray[0].radius == cell0.ballArray[1].radius  &&  cell0.ballArray[0].radius < r+1e-9  &&  cell0.ballArray[0].radius > r-1e-9);
	}
	
	/* Collision detection */

	@Test
	public void testCModelNoOverlapSphereSphere() {								
		Model model = new Model();
		Cell cell0 = new Cell(0, 0, -0.5001e-6, 0e-6, 0e-6, 0, 0, 0, false, model);
		cell0.ballArray[0].radius = 0.5e-6;
		Cell cell1 = new Cell(0, 0,  0.5001e-6, 0e-6, 0e-6, 0, 0, 0, false, model);
		cell1.ballArray[0].radius = 0.5e-6;
		ArrayList<Cell> overlap = model.DetectCollisionCellArray(1.0);
		assertTrue(overlap.isEmpty());
	}
	
	@Test
	public void testCModelOverlapSphereSphere() {							
		Model model = new Model();
		Cell cell0 = new Cell(0, 0, -0.499e-6, 0e-6, 0e-6, 0, 0, 0, false, model);
		cell0.ballArray[0].radius = 0.5e-6;
		Cell cell1 = new Cell(0, 0,  0.499e-6, 0e-6, 0e-6, 0, 0, 0, false, model);
		cell1.ballArray[0].radius = 0.5e-6;
		ArrayList<Cell> overlap = model.DetectCollisionCellArray(1.0);
		assertTrue(overlap.size() == 2);
	}

	@Test
	public void testCModelNoOverlapRodRod() {								
		Model model = new Model();
		Cell cell0 = new Cell(0, 0, -0.5001e-6, 0e-6, 0e-6, -2.5001e-6, 0e-6, 0e-6, false, model);
		cell0.ballArray[0].radius = 0.5e-6;
		Cell cell1 = new Cell(0, 0,  0.5001e-6, 0e-6, 0e-6,  2.5001e-6, 0e-6, 0e-6, false, model);
		cell1.ballArray[0].radius = 0.5e-6;
		ArrayList<Cell> overlap = model.DetectCollisionCellArray(1.0);
		assertTrue(overlap.isEmpty());
	}

	@Test
	public void testCModelOverlapRodRod() {							
		Model model = new Model();
		Cell cell0 = new Cell(2, 0, -0.499e-6, 0e-6, 0e-6, -2.499e-6, 0e-6, 0e-6, false, model);
		cell0.ballArray[0].radius = 0.5e-6;
		Cell cell1 = new Cell(2, 0,  0.499e-6, 0e-6, 0e-6,  2.499e-6, 0e-6, 0e-6, false, model);
		cell1.ballArray[0].radius = 0.5e-6;
		ArrayList<Cell> overlap = model.DetectCollisionCellArray(1.0);
		assertTrue(overlap.size() == 2);
	}
}
