package cell;

public class CFilSpring extends CSpring {
	private static final long serialVersionUID = 1L;
//	ArrayList<CFilSpring> siblingArray;
	public int type;
	
	///////////////////////////////////////////////////////////////////

	public CFilSpring(CBall ball0, CBall ball1, int filType) {
		ballArray = new CBall[2];
		ballArray[0] = ball0;
		ballArray[1] = ball1;
		type = filType; 
		ResetK();
		ResetRestLength();
		// Add to arrays
		final CModel model = ball0.cell.model;
		model.filSpringArray.add(this);
		ball0.cell.filSpringArray.add(this);
		ball1.cell.filSpringArray.add(this);
	}

	public static double RestLength(int type, double radius0, double radius1, double rodRestLength0, double rodRestLength1, CModel model) {
		switch(type) {
		case 3:				// Small fil spring
			return model.filLengthSphere*(radius0 + radius1);
		case 4:				// Small fil spring
			return model.filLengthRod[0]*(radius0 + radius1);
		case 5:				// Big fil spring
			return model.filLengthRod[1]*(radius0 + radius1) + rodRestLength0 + rodRestLength1;
		case 6:
			return  Math.sqrt(rodRestLength0*rodRestLength0 + Math.pow(0.5*(radius0+radius1) + rodRestLength1, 2.0));
		default:
			throw new IndexOutOfBoundsException("Spring type: " + type);
		}
	}
	
	public void ResetRestLength() {
		final CModel model = ballArray[0].cell.model;
		CBall ball0 = ballArray[0];
		CBall ball1 = ballArray[1];
		double rodRestLength0 = ball0.cell.rodSpringArray.get(0).restLength;
		double rodRestLength1 = ball1.cell.rodSpringArray.get(0).restLength;
		restLength = RestLength(type, ball0.radius, ball1.radius, rodRestLength0, rodRestLength1, model);
	}

	public void ResetK() {
		final CModel model = ballArray[0].cell.model;
		double springDiv;
		CCell cell0 = ballArray[0].cell;
		switch(type) {
		case 3:														// Two different balls, same cell type
			// Sphere filament
			if(cell0.type<2)						springDiv = 1.0;
			else if(cell0.type<6)					springDiv = 2.0;
			else throw new IndexOutOfBoundsException("Cell type: " + cell0.type);
			K = model.KfSphere/springDiv;
			break;
		case 4:														// Two different balls, same cell type
			// Rod filament, short spring
			if(cell0.type<2)						springDiv = 1.0;
			else if(cell0.type<6)					springDiv = 2.0;
			else throw new IndexOutOfBoundsException("Cell type: " + cell0.type);
			K = model.KfRod0/springDiv;
			break;
		case 5:	case 6:												// Two different balls, same cell type
			// Rod filament, long spring
			if(cell0.type<2)						springDiv = 1.0;
			else if(cell0.type<6)					springDiv = 2.0;
			else throw new IndexOutOfBoundsException("Cell type: " + cell0.type);
			K = model.KfRod0/springDiv;				// FIXME
			break;
		default:
			throw new IndexOutOfBoundsException("Spring type: " + type);
		}
	}

	public int Break() {
		final CModel model = ballArray[0].cell.model;
		int count = 0;
		CCell cell0 = ballArray[0].cell;
		CCell cell1 = ballArray[1].cell;
		// Remove this and siblings from model and cells
		count += (model.filSpringArray.remove(this))?1:0;			// Add one to counter if successfully removed
		cell0.filSpringArray.remove(this);
		cell1.filSpringArray.remove(this);
		for(CSpring sibling : siblingArray) {
			cell0.filSpringArray.remove(sibling);
			cell1.filSpringArray.remove(sibling);
			count += (model.filSpringArray.remove(sibling))?1:0;
		}
		return count;
	}

	public int Index() {
		final CModel model = ballArray[0].cell.model;
		return Index(model.filSpringArray);
	}
}