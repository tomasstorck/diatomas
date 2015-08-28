package ibmTest;

import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.Before;

import ibm.Cell;
import ibm.Model;

import java.util.ArrayList;

public class ModelTest {
	Model model;
	Cell sphere0;
	Cell sphere1;
	Cell rod0;
	Cell rod1;
	double rSphere; 
	double nSphere; 
	double rRod; 
	double nRod;
	
	double tol = 1e-9;
	
	@Before
	public void SetUp() {
		model = new Model();
		model.MWX[0] = model.MWX[2] = 10;
		model.rhoX[0] = model.rhoX[2] = 100;
		rSphere = 0.5e-6; 
		nSphere = 4.0/3.0*Math.PI*Math.pow(rSphere, 3) * model.rhoX[0]/model.MWX[0]; 
		model.radiusCellMax[2] = 0.5e-6;
		model.lengthCellMax[2] = 2*model.radiusCellMax[2];
		rRod = 0.5e-6; 
		nRod = (4.0/3.0*Math.PI*Math.pow(rRod, 3)  +  Math.PI*Math.pow(rRod, 2)*model.lengthCellMax[2]) * model.rhoX[2]/model.MWX[2];
		sphere0 = new Cell(0, nSphere, -1.0, 0, 0, 0, 0, 0, false, model);
		sphere1 = new Cell(0, nSphere, -2.0, 0, 0, 0, 0, 0, false, model);
		rod0 = 	  new Cell(2, nRod,     1.0, 0, 0, 1+model.lengthCellMax[2], 0, 0, false, model);
		rod1 = 	  new Cell(2, nRod,     2.0, 0, 0, 2+model.lengthCellMax[2], 0, 0, false, model);
	}
	
	@Test
	public void ModelTestInitialisation() {				
		assertTrue(model.simulation == 0);
	}
	
	@Test
	public void ModelTestCellIndex() {							
		assertTrue(sphere0.Index() == 0 && sphere1.Index() == 1 &&
				rod0.Index() == 2 && rod1.Index() == 3);
	}
	
	/* Radius and mass */
	@Test
	public void ModelTestSphereRadius() {
		assertTrue(sphere0.ballArray[0].radius < rSphere+tol && 
				sphere0.ballArray[0].radius > rSphere-tol);
	}

	@Test
	public void ModelTestRodRadius() {
		assertTrue(rod0.ballArray[0].radius == rod0.ballArray[1].radius  &&  
				rod0.ballArray[0].radius < rRod+tol && 
				rod0.ballArray[0].radius > rRod-tol);
	}
	
	/* Collision detection */
	@Test
	public void ModelTestNoOverlapByDefault() {
		ArrayList<Cell> overlap = model.DetectCollisionCellArray(1.0);
		assertTrue(overlap.isEmpty());
		
	}
	@Test
	public void ModelTestNoOverlapSphereSphere() {								
		// Overlap in x direction, at origin
		double dx = 0.01e-6;
		sphere0.ballArray[0].pos.x = -rSphere-dx;
		sphere1.ballArray[0].pos.x = rSphere+dx;
		ArrayList<Cell> overlap = model.DetectCollisionCellArray(1.0);
		int iSphere0 = overlap.indexOf(sphere0);
		int iSphere1 = overlap.indexOf(sphere1);
		assertTrue(iSphere1-iSphere0 != 1);  	// sphere1 should be after sphere1 in overlap  
	}
	
	@Test
	public void ModelTestOverlapSphereSphere() {							
		// Overlap in x direction, at origin
		double dx = 0.01e-6;
		sphere0.ballArray[0].pos.x = -rSphere+dx;
		sphere1.ballArray[0].pos.x = rSphere-dx;
		ArrayList<Cell> overlap = model.DetectCollisionCellArray(1.0);
		int iSphere0 = overlap.indexOf(sphere0);
		int iSphere1 = overlap.indexOf(sphere1);
		assertTrue(iSphere1-iSphere0 == 1);  	// sphere1 should be after sphere0 in overlap
	}

	@Test
	public void ModelTestNoOverlapRodRod() {								
		// NO overlap in x direction, at origin
		double dx = 0.01e-6;
		rod0.ballArray[0].pos.x = -rRod-dx;
		rod0.ballArray[1].pos.x = -rRod-model.lengthCellMax[2]-dx;
		rod1.ballArray[0].pos.x = rRod+dx;
		rod1.ballArray[1].pos.x = rRod+model.lengthCellMax[2]+dx;
		ArrayList<Cell> overlap = model.DetectCollisionCellArray(1.0);
		int iRod0 = overlap.indexOf(rod0);
		int iRod1 = overlap.indexOf(rod1);
		assertTrue(iRod1-iRod0 != 1); 
	}

	@Test
	public void ModelTestOverlapRodRod() {
		// Overlap in x direction, at origin
		double dx = 0.01e-6;
		rod0.ballArray[0].pos.x = -rRod+dx;
		rod0.ballArray[1].pos.x = -rRod-model.lengthCellMax[2]+dx;
		rod1.ballArray[0].pos.x = rRod-dx;
		rod1.ballArray[1].pos.x = rRod+model.lengthCellMax[2]-dx;
		ArrayList<Cell> overlap = model.DetectCollisionCellArray(1.0);
		int iRod0 = overlap.indexOf(rod0);
		int iRod1 = overlap.indexOf(rod1);
		assertTrue(iRod1-iRod0 == 1); 
	}
}
