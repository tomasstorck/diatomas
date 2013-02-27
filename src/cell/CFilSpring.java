package cell;

public class CFilSpring extends CSpring {
	private static final long serialVersionUID = 1L;
//	ArrayList<CFilSpring> siblingArray;
	public int type;
	
	///////////////////////////////////////////////////////////////////

	public CFilSpring(CBall ball0, CBall ball1, int filType) {
		super(ball0, ball1);
		type = filType; 
		// Add to arrays
		final CModel model = ball0.cell.model;
		model.filSpringArray.add(this);
		ball0.cell.filSpringArray.add(this);
		ball1.cell.filSpringArray.add(this);
	}

	public void ResetRestLength() {
		final CModel model = ballArray[0].cell.model;
		switch(type) {
		case 3:				// Small fil spring
			restLength = model.filLengthSphere*(ballArray[0].radius + ballArray[1].radius);
			break;
		case 4:				// Small fil spring
			restLength = model.filLengthRod[0]*(ballArray[0].radius + ballArray[1].radius);
			break;
		case 5:				// Big fil spring
			restLength = model.filLengthRod[1]*(ballArray[0].radius + ballArray[1].radius) + ballArray[0].cell.rodSpringArray.get(0).restLength + ballArray[1].cell.rodSpringArray.get(0).restLength;
			break;
		default:
			throw new IndexOutOfBoundsException("Spring type: " + type);
		}
	}

	public void ResetK() {
		final CModel model = ballArray[0].cell.model;
		double springDiv;
		CCell cell0 = ballArray[0].cell;
		switch(type) {
		case 3:														// Two different balls, same cell type
			if(cell0.type<2)						springDiv = 1.0;
			else if(cell0.type<6)					springDiv = 2.0;
			else throw new IndexOutOfBoundsException("Cell type: " + cell0.type);
			K = model.KfSphere/springDiv;
			break;
		case 4:														// Two different balls, same cell type
			if(cell0.type<2)						springDiv = 1.0;
			else if(cell0.type<6)					springDiv = 2.0;
			else throw new IndexOutOfBoundsException("Cell type: " + cell0.type);
			K = model.KfRod0/springDiv;
			break;
		case 5:														// Two different balls, same cell type
			if(cell0.type<2)						springDiv = 1.0;
			else if(cell0.type<6)					springDiv = 2.0;
			else throw new IndexOutOfBoundsException("Cell type: " + cell0.type);
			K = model.KfRod0/springDiv;
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