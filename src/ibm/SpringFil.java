package ibm;

import java.util.ArrayList;

public class SpringFil extends Spring {
	private static final long serialVersionUID = 1L;
//	ArrayList<CFilSpring> siblingArray;
	public int type;
	public ArrayList<SpringFil> siblingArray = new ArrayList<SpringFil>();;
	
	///////////////////////////////////////////////////////////////////

	public SpringFil(Ball ball0, Ball ball1, int filType) {
		ballArray = new Ball[2];
		ballArray[0] = ball0;
		ballArray[1] = ball1;
		type = filType;
		ResetK();
		ResetRestLength();
		// Add to arrays
		final Model model = ball0.cell.model;
		model.filSpringArray.add(this);
		ball0.cell.filSpringArray.add(this);
		ball1.cell.filSpringArray.add(this);
	}

	public static double RestLength(int type, double radius0, double radius1, double rl0, double rl1, Model model) {
		final double avgFilLengthRod = 0.5*(model.filLengthRod[0]+model.filLengthRod[1]);
		switch(type) {
		case 3:				// Small fil spring
			return model.filLengthSphere*(radius0 + radius1);
		case 4:				// Small fil spring
			return model.filLengthRod[0]*(radius0 + radius1);
		case 5:				// Big fil spring
			return model.filLengthRod[1]*(radius0 + radius1) + rl0 + rl1;
		case 6:
			return avgFilLengthRod*(radius0 + radius1);
		case 7:
			double rls = avgFilLengthRod*(radius0 + radius1);		// Rest length short spring
			return avgFilLengthRod*Math.sqrt((rl0+0.5*rls)*(rl0+0.5*rls) + (rl1+Math.sqrt(0.75)*rls)*(rl1+Math.sqrt(0.75)*rls));	
		default:
			throw new IndexOutOfBoundsException("Spring type: " + type);
		}
	}
	
	public void ResetRestLength() {
		final Model model = ballArray[0].cell.model;
		Ball ball0 = ballArray[0];
		Ball ball1 = ballArray[1];
		double rodRestLength0;
		double rodRestLength1;
		switch(type) {
		case 3:
			rodRestLength0 = 0;
			rodRestLength1 = 0;
			break;
		case 4: case 5: case 6: case 7:
			rodRestLength0 = ball0.cell.rodSpringArray.get(0).restLength;
			rodRestLength1 = ball1.cell.rodSpringArray.get(0).restLength;
			break;
		default:
			throw new IndexOutOfBoundsException("Spring type: " + type);
		}
		restLength = RestLength(type, ball0.radius, ball1.radius, rodRestLength0, rodRestLength1, model);
	}

	public void ResetK() {
		final Model model = ballArray[0].cell.model;
		double springDiv;
		Cell cell0 = ballArray[0].cell;
		switch(type) {
		case 3:														// Two different balls, same cell type
			// Sphere filament
			if(cell0.type<2)						springDiv = 1.0;
			else if(cell0.type<6)					springDiv = 2.0;
			else throw new IndexOutOfBoundsException("Cell type: " + cell0.type);
			K = model.KfSphere/springDiv;
			break;
		case 4:	case 6:												// Two different balls, same cell type
			// Rod filament, short spring
			if(cell0.type<2)						springDiv = 1.0;
			else if(cell0.type<6)					springDiv = 2.0;
			else throw new IndexOutOfBoundsException("Cell type: " + cell0.type);
			K = model.KfRod[0]/springDiv;
			break;
		case 5:	case 7:												// Two different balls, same cell type
			// Rod filament, long spring
			if(cell0.type<2)						springDiv = 1.0;
			else if(cell0.type<6)					springDiv = 2.0;
			else throw new IndexOutOfBoundsException("Cell type: " + cell0.type);
			K = model.KfRod[1]/springDiv;				// FIXME
			break;
		default:
			throw new IndexOutOfBoundsException("Spring type: " + type);
		}
	}

	public int Break() {
		final Model model = ballArray[0].cell.model;
		int count = 0;
		Cell cell0 = ballArray[0].cell;
		Cell cell1 = ballArray[1].cell;
		// Remove this and siblings from model and cells
		count += (model.filSpringArray.remove(this))?1:0;			// Add one to counter if successfully removed
		cell0.filSpringArray.remove(this);
		cell1.filSpringArray.remove(this);
		for(Spring sibling : siblingArray) {
			cell0.filSpringArray.remove(sibling);
			cell1.filSpringArray.remove(sibling);
			count += (model.filSpringArray.remove(sibling))?1:0;
		}
		return count;
	}
	
	public Vector3d GetL() {
		return ballArray[1].pos.minus(ballArray[0].pos);
	}

	public int Index() {
		final Model model = ballArray[0].cell.model;
		return Index(model.filSpringArray);
	}
}