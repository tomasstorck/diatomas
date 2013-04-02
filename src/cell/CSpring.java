package cell;

import java.io.Serializable;
import java.util.ArrayList;


public abstract class CSpring implements Serializable {
	private static final long serialVersionUID = 1L;
	//
	public CBall[] ballArray;
//	public Vector3d anchorPoint;
	public double K;
	public double restLength;
//	public int type;
	
	///////////////////////////////////////////////////////////////////
	
	public void ResetRestLength() {
//		CModel model = this.ballArray[0].cell.model;
//		switch(type) {
//		case 0:				// Rod
//			// If type == 1 based on mass, type==2 based on max mass
//			if(ballArray[0].cell.type<4) {
//				restLength = ballArray[0].radius * model.lengthCellMax[ballArray[0].cell.type]/model.radiusCellMax[ballArray[0].cell.type];							// About 2 balls in the same cell, so no need to make it complicated  
//			} else if (ballArray[0].cell.type<6) {
//				restLength = ballArray[0].cell.GetAmount()*model.MWX/(Math.PI*model.rhoX*ballArray[0].radius*ballArray[0].radius) - 4.0/3.0*ballArray[0].radius;
//			} else {
//				throw new IndexOutOfBoundsException("Cell type: " + type);
//			}
//			break;
//		case 1:				// Stick
//			restLength = Math.max(
//					ballArray[1].pos.minus(ballArray[0].pos).norm(),		// The rest length we desire
//					1.0*(ballArray[0].radius + ballArray[1].radius));		// But we want the spring not to cause cell overlap in relaxed state
//			break;
//		case 2:				// Anchoring spring
//			CBall ball = ballArray[0];
//			restLength = Math.max(ball.pos.y,ball.radius*1.01);				// WORKAROUND: Choose current position, but make sure it is not forcing the spring into the substratum
//			break;
//		case 3:				// Small fil spring
//			restLength = model.filLengthSphere*(ballArray[0].radius + ballArray[1].radius);
//			break;
//		case 4:				// Small fil spring
//			restLength = model.filLengthRod[0]*(ballArray[0].radius + ballArray[1].radius);
//			break;
//		case 5:				// Big fil spring
//			restLength = model.filLengthRod[1]*(ballArray[0].radius + ballArray[1].radius) + ballArray[0].cell.rodSpringArray.get(0).restLength + ballArray[1].cell.rodSpringArray.get(0).restLength;
//			break;
//		default:
//			throw new IndexOutOfBoundsException("Spring type: " + type);
//		}
	}
	
	public void ResetK() {
//		CCell cell0 = ballArray[0].cell;
//		CModel model = cell0.model;
//		double springDiv = 0.0;
//		switch(type) {
//		case 0:
//			K = model.Kr;			// Two identical dimension balls, same cell
//			break;
//		case 1:														// Two different balls, possible different cell types
//			CCell cell1 = ballArray[1].cell;
//			if(cell0.type<2 && cell1.type<2)		springDiv = 1.0;
//			else if(cell0.type>1 && cell1.type>1) {
//				if(cell0.type<6 && cell1.type<6) 	springDiv = 4.0;
//				else throw new IndexOutOfBoundsException("Cell types: " + cell0.type + " and " + cell1.type);
//			} else 									springDiv = 2.0;
//			K = model.Ks/springDiv;
//			break;
//		case 2:
//			if(cell0.type<2)						springDiv = 1.0;
//			else if(cell0.type<6)					springDiv = 2.0;
//			else throw new IndexOutOfBoundsException("Cell type: " + cell0.type);
//			K = model.Kan/springDiv;
//			break;
//		case 3:														// Two different balls, same cell type
//			if(cell0.type<2)						springDiv = 1.0;
//			else if(cell0.type<6)					springDiv = 2.0;
//			else throw new IndexOutOfBoundsException("Cell type: " + cell0.type);
//			K = model.KfSphere/springDiv;
//			break;
//		case 4:														// Two different balls, same cell type
//			if(cell0.type<2)						springDiv = 1.0;
//			else if(cell0.type<6)					springDiv = 2.0;
//			else throw new IndexOutOfBoundsException("Cell type: " + cell0.type);
//			K = model.KfRod0/springDiv;
//			break;
//		case 5:														// Two different balls, same cell type
//			if(cell0.type<2)						springDiv = 1.0;
//			else if(cell0.type<6)					springDiv = 2.0;
//			else throw new IndexOutOfBoundsException("Cell type: " + cell0.type);
//			K = model.KfRod0/springDiv;
//			break;
//		default:
//			throw new IndexOutOfBoundsException("Spring type: " + type);
//		}
	}
	
	public int Break() {												// Also removes siblings
//		CModel model = this.ballArray[0].cell.model;
//		int count = 0;
//		CCell cell0 = ballArray[0].cell;
//		CCell cell1 = null;
//		
//		switch (type) {
//		case 0: 														// Rod spring
//			// Can't Break()
//			throw new RuntimeException("Cannot break rod springs");
//		case 1:															// Sticking spring
//			cell1 = ballArray[1].cell;
//			cell0.stickCellArray.remove(cell1);
//			cell1.stickCellArray.remove(cell0);	
//			// Remove this and siblings from model and cells
//			count += (model.stickSpringArray.remove(this))?1:0;			// Add one to counter if successfully removed
//			cell0.stickSpringArray.remove(this);
//			cell1.stickSpringArray.remove(this);
//			for(CSpring sibling : siblingArray) {
//				cell0.stickSpringArray.remove(sibling);
//				cell1.stickSpringArray.remove(sibling);
//				count += (model.stickSpringArray.remove(sibling))?1:0;
//			}
//			break;
//		case 2:
//			// Remove this and siblings from model and cells			// Anchoring springs
//			count += (model.anchorSpringArray.remove(this))?1:0;		// Add one to counter if successfully removed
//			cell0.anchorSpringArray.remove(this);
//			for(CSpring sibling : siblingArray) {
//				cell0.anchorSpringArray.remove(sibling);
//				count += (model.anchorSpringArray.remove(sibling))?1:0;
//			}
//			break;
//		case 3:	case 4: case 5:													// Filament springs
//			cell1 = ballArray[1].cell;
//			// Remove this and siblings from model and cells
//			count += (model.filSpringArray.remove(this))?1:0;			// Add one to counter if successfully removed
//			cell0.filSpringArray.remove(this);
//			cell1.filSpringArray.remove(this);
//			for(CSpring sibling : siblingArray) {
//				cell0.filSpringArray.remove(sibling);
//				cell1.filSpringArray.remove(sibling);
//				count += (model.filSpringArray.remove(sibling))?1:0;
//			}
//			break;
//		default:
//			throw new IndexOutOfBoundsException("Spring type: " + type);
//		}
		return -1;
	}
	
	//////////////////////////////////////////////////////////////////////
	
//	public void set(CBall ball0, CBall ball1) {
//		ballArray[0] = ball0;
//		ballArray[1] = ball1;
//	}
	
	public int Index() {
		// Overwritten in subclass
		return -1;
	}
	
	public int Index(ArrayList<? extends CSpring> array) {
//		CModel model = this.ballArray[0].cell.model;
//		ArrayList<CSpring> array = new ArrayList<CSpring>();
//		switch(type) {
//		case 0:
//			break;
//		case 1:
//			array = model.stickSpringArray;
//			break;
//		case 2:
//			array = model.anchorSpringArray;
//			break;
//		case 3: case 4: case 5:
//			array = model.filSpringArray;
//			break;
//		}
		
		for(int index=0; index<array.size(); index++) {
			if(array.get(index).equals(this))	return index;
		}
		return -1;
	}

	public Vector3d GetL() {
//		switch(type) {
//		case 0: case 1: case 3: case 4: case 5:
//			return ballArray[1].pos.minus(ballArray[0].pos);
//		case 2:
//			return ballArray[0].pos.minus(anchorPoint);
//		default:
//			throw new IndexOutOfBoundsException("Spring type: " + type);						
//		}
		
		return ballArray[1].pos.minus(ballArray[0].pos);
	}
} 