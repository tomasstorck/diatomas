package ibmTest;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.Before;

import ibm.Cell;
import ibm.Model;
import ibm.RodSpring;

public class CellTest {
	Model model;
	Cell sphere0, rod2, rod4;
	double r;

	@Before
	public void SetUp() {
		model = new Model();
		model.MWX[0] = model.MWX[2] = model.MWX[4] = 10;
		model.rhoX[0] = model.rhoX[2] = model.rhoX[4] = 100;
		r = 0.5e-6;
		// Sphere
		// Rod
		model.radiusCellMax[0] = model.radiusCellMax[2] = model.radiusCellMax[4] = r;
		model.lengthCellMax[2] = model.lengthCellMax[4] = 2*model.radiusCellMax[2];
		model.UpdateDependentParameters();
		sphere0 = 	new Cell(0, model.nCellMax[0],	0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false, model); 		// variable radius, fixed aspect  
		rod2 = 		new Cell(2, model.nCellMax[2], 	0.0, 0.0, 0.0, 0.0+model.lengthCellMax[2], 0.0, 0.0, false, model); 		// variable radius, fixed aspect
		rod4 = 		new Cell(4, model.nCellMax[4], 	0.0, 0.0, 0.0, 0.0+model.lengthCellMax[4], 0.0, 0.0, false, model); 		// fixed radius, variable length
	}

	@Test
	public void testBallArraySize() {
		assertTrue(sphere0.ballArray.length == 1 &&
				rod2.ballArray.length == 2 &&
				rod4.ballArray.length == 2);
	}
	
	@Test
	public void testCellAmountCellMax() {
		double nSphere = 4.0/3.0*Math.PI*Math.pow(r, 3) * model.rhoX[0]/model.MWX[0];
		double nRod = (4.0/3.0*Math.PI*Math.pow(r, 3)  +  Math.PI*Math.pow(r, 2)*model.lengthCellMax[2]) * model.rhoX[2]/model.MWX[2];
		assertTrue(model.nCellMax[0] == nSphere &&
				model.nCellMax[2] == nRod &&
				model.nCellMax[4] == nRod);
	}
	
	@Test	
	public void testSurfaceAreaSphere() {
		assertTrue(4.0*Math.PI*Math.pow(r, 2) < 1.01*sphere0.SurfaceArea() &&
				4.0*Math.PI*Math.pow(r, 2) > 0.99*sphere0.SurfaceArea());
	}
	
	@Test	
	public void testSurfaceAreaRod() {
		double Lrod2 =rod2.rodSpringArray.get(0).GetL().norm();
		double SArod2 = 4.0*Math.PI*Math.pow(r, 2) + 
				2*Math.PI*r*Lrod2;  
		assertTrue( SArod2 < 1.01*rod2.SurfaceArea() &&
				SArod2 > 0.99*rod2.SurfaceArea() &&
				rod2.SurfaceArea() < 1.01*rod4.SurfaceArea() &&
				rod2.SurfaceArea() > 0.99*rod4.SurfaceArea());
	}
	
	@Test
	public void testVolumeSphere() {
		double Vsphere = 4.0/3.0*Math.PI*Math.pow(r, 3.0);
		assertTrue( Vsphere < 1.01*sphere0.Volume() &&
				Vsphere > 0.99*sphere0.Volume() );
	}
	
	@Test
	public void testVolumeRod() {
		double Vrod = 4.0/3.0*Math.PI*Math.pow(r, 3.0) + Math.PI*Math.pow(r, 2)*model.lengthCellMax[2];
		assertTrue( Vrod < 1.01*rod2.Volume() &&
				Vrod > 0.99*rod2.Volume() &&
				rod2.Volume() < 1.01*rod4.Volume() &&
				rod2.Volume() > 0.99*rod4.Volume() );
	}
	
	@Test
	public void testNeighbour() {
		assertTrue( sphere0.GetNeighbour() == null &&
				rod2.GetNeighbour() == null &&
				rod4.GetNeighbour() == null ); 
		model.filament = true;
		sphere0.filament = true;
		rod2.filament = true;
		rod4.filament = true;
		Cell sphere0Daughter = model.DivideCell(sphere0);
		Cell rod2Daughter = model.DivideCell(rod2);
		Cell rod4Daughter = model.DivideCell(rod4);
		model.CreateFilament(sphere0, sphere0Daughter);
		model.CreateFilament(rod2, rod2Daughter);
		model.CreateFilament(rod4, rod4Daughter);
		assertTrue( sphere0.GetNeighbour() == sphere0Daughter &&
				rod2.GetNeighbour() == rod2Daughter &&
				rod4.GetNeighbour() == rod4Daughter );
	}
	
	@Test
	public void testReshapeSphere0() {
		sphere0.SetAmount(sphere0.GetAmount());
		sphere0.SetAmount(sphere0.GetAmount()/2.0);
		assertTrue(sphere0.ballArray[0].radius/r < 1.01*Math.pow(0.5,(1.0/3.0)) &&
				sphere0.ballArray[0].radius/r > 0.99*Math.pow(0.5,(1.0/3.0)));
	}
	
	@Test
	public void testReshapeRod() {
		RodSpring spring2 = rod2.rodSpringArray.get(0);
		RodSpring spring4 = rod4.rodSpringArray.get(0);
		// Get indicators
		double oldAspect2 = spring2.restLength / rod2.ballArray[0].radius;
		double oldL4 = 		spring4.restLength;
		// Half mass
		rod2.SetAmount(rod2.GetAmount()/2.0);
		rod4.SetAmount(rod4.GetAmount()/2.0);
		spring2.ResetRestLength();
		spring4.ResetRestLength();
		// Get new indicators
		double newAspect2 = spring2.restLength / rod2.ballArray[0].radius;
		double newL4 = 		spring4.restLength;
		// Compare
		assertTrue(newAspect2 < 1.01*oldAspect2 &&
				newAspect2 > 0.99*oldAspect2 &&
				rod2.ballArray[0].radius < r*0.8 && 		// Rough guess
				newL4 < 1.01* (0.5*oldL4 - 2.0/3.0*r) && 	// solve: 4/3*pi*r^3+pi*r^2*L == 0.5*(4/3*pi*r^3+pi*r^2*Lold)
				newL4 > 0.99* (0.5*oldL4 - 2.0/3.0*r) &&
				rod4.ballArray[0].radius < 1.01*r &&
				rod4.ballArray[0].radius > 0.99*r);
		
	}
	
}
