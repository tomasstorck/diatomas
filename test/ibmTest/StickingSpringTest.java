package ibmTest;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.Before;

import ibm.Cell;
import ibm.Ball;
import ibm.Model;
import ibm.StickSpring;

public class StickingSpringTest {
	Model model;
	Cell sphere0, sphere1, rod0, rod1;
	double r;
	
	@Before
	public void SetUp() {
		model = new Model();
		model.sticking = true;
		model.MWX = 10;
		model.rhoX = 100;
		model.Ks[0][0] = 1e-11;
		for(int ii=0; ii<model.stickType.length; ii++) {
			for(int jj=0; jj<model.stickType[0].length; jj++) {
				model.stickType[ii][jj] = true;
			}
		}
		// Sphere
		r = 0.5e-6; 
		double n = 4.0/3.0*Math.PI*Math.pow(r, 3) * model.rhoX/model.MWX;
		sphere0 = new Cell(0, n, 1.0, r, 1.0, 0, 0, 0, false, model);
		sphere1 = new Cell(0, n, 2.0, r, 1.0, 0, 0, 0, false, model);
		// Rod
		model.radiusCellMax[2] = r;
		model.lengthCellMax[2] = 2*model.radiusCellMax[2];
		n = (4.0/3.0*Math.PI*Math.pow(r, 3)  +  Math.PI*Math.pow(r, 2)*model.lengthCellMax[2]) * model.rhoX/model.MWX;
		rod0 = new Cell(2, n, -1.0, r, 1.0, -1.0+model.lengthCellMax[2], r, 1.0, false, model);
		rod1 = new Cell(2, n, -2.0, r, 1.0, -2.0+model.lengthCellMax[2], r, 1.0, false, model);
	}

	@Test
	public void testStickSame() {
		sphere0.Stick(sphere1);
		rod0.Stick(rod1);
		assertTrue(model.stickSpringArray.size() == 1+4);
	}
	
	@Test
	public void testStickDifferent() {
		sphere0.Stick(rod1);
		rod0.Stick(sphere1);
		assertTrue(model.stickSpringArray.size() == 2+2);
	}
	
	@Test
	public void testStickUnstick() {
		sphere0.Stick(sphere1); 		// 1 spring
		rod0.Stick(rod1); 				// 4 
		sphere0.Stick(rod1); 			// 2
		rod0.Stick(sphere1); 			// 2
		int NBreak = 0;
		// Break one spring for each total connection to also test the sibling springs, counting from the end as to not break indexing
		NBreak += model.stickSpringArray.get(7).Break();
		NBreak += model.stickSpringArray.get(5).Break();
		NBreak += model.stickSpringArray.get(1).Break();
		NBreak += model.stickSpringArray.get(0).Break();
		assertTrue(model.stickSpringArray.size() == 0 && NBreak == 9);
	}
	
	@Test
	public void testStickForm() {
		for(Ball ball : model.ballArray) {
			ball.pos.x = random.rand.Double()*1e-9;
			ball.pos.y = random.rand.Double()*1e-9;
			ball.pos.z = random.rand.Double()*1e-9;
		}
		// Total springs:
		// 1 sphere0-sphere1
		// 2 sphere0-rod0
		// 2 sphere0-rod1
		// 2 sphere1-rod0
		// 2 sphere1-rod1
		// 4 rod0-rod1
		// Total: 13
		model.FormBreak();
		assertTrue(model.stickSpringArray.size() == 13);
	}
	
	@Test
	public void testStickBreak() {
		// Put close together
		for(Ball ball : model.ballArray) {
			ball.pos.x = random.rand.Double()*1e-9;
			ball.pos.y = random.rand.Double()*1e-9;
			ball.pos.z = random.rand.Double()*1e-9;
		}
		// Make links with rl=distance (~ 1 nm)
		for(int ic=0; ic<model.cellArray.size(); ic++) {
			for(int jc=ic+1; jc<model.cellArray.size(); jc++) {
				model.cellArray.get(ic).Stick( model.cellArray.get(jc) );
			}
		}
		// Displace (~ 1 m)
		for(Ball ball : model.ballArray) {
			ball.pos.x = random.rand.Double();
			ball.pos.y = random.rand.Double();
			ball.pos.z = random.rand.Double();
		}
		// And break
		model.FormBreak();
		assertTrue(model.stickSpringArray.size() == 0);
	}
	
	@Test
	public void testStickGetL() {
		sphere0.Stick(sphere1);
		sphere0.Stick(rod0);
		rod0.Stick(rod1);
		for(StickSpring spring : model.stickSpringArray) {
			if(spring.ballArray[1].pos.minus( spring.ballArray[0].pos ).norm() != spring.GetL().norm()) {
				assertTrue(false);
			}
		}
		assertTrue(true);
	}
	
	@Test
	public void testStickResetRestlength() {
		// Displace (~ 1 m)
		for(Ball ball : model.ballArray) {
			ball.pos.x = random.rand.Double();
			ball.pos.y = random.rand.Double();
			ball.pos.z = random.rand.Double();
		}
		for(StickSpring spring : model.stickSpringArray) {
			spring.ResetRestLength();
			if(spring.ballArray[1].pos.minus( spring.ballArray[1].pos ).norm() != spring.restLength) {
				assertTrue(false);
			}
		}
		assertTrue(true);
	}
	
	@Test
	public void testStickKScaling() {
		// Sphere-Sphere
		sphere0.Stick(sphere1);
		double SphereSphereKNorm = sphere1.stickSpringArray.get(0).K*sphere1.stickSpringArray.size();
		sphere0.stickSpringArray.get(0).Break();
		// Sphere-Rod
		sphere0.Stick(rod0);
		double SphereRodKNorm = rod0.stickSpringArray.get(0).K*rod0.stickSpringArray.size();
		sphere0.stickSpringArray.get(0).Break();
		// Rod-Rod
		rod0.Stick(rod1);
		double RodRodKNorm = rod1.stickSpringArray.get(0).K*rod1.stickSpringArray.size();
		rod0.stickSpringArray.get(0).Break();
		// Test
		assertTrue(SphereSphereKNorm == SphereRodKNorm && SphereSphereKNorm == RodRodKNorm);
	}
}
