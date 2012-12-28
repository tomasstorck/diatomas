package cell;

import java.io.Serializable;
import java.util.ArrayList;

public class CSpring implements Serializable {
	private static final long serialVersionUID = 1L;
	//
	public CBall[] ballArray = new CBall[2];
	public double K;
	public double restLength;
	public int type;
	public ArrayList<CSpring> siblingArray = new ArrayList<CSpring>(4);
	
	///////////////////////////////////////////////////////////////////

	// Adds new spring to model's array, cell's array. Does NOT add to siblingArray
	public CSpring(CBall ball0, CBall ball1, int type, CSpring[] siblingArray){			// Note that siblingArray is by default not initialised
		CModel model = ball0.cell.model;
		ballArray[0] = ball0;
		ballArray[1] = ball1;
		this.type = type;
		for(CSpring sibling : siblingArray) 	this.siblingArray.add(sibling);
		ResetK();
		ResetRestLength();
		
		switch(type) {
		case 0:			// Rod spring
			model.rodSpringArray.add(this);
			ball0.cell.rodSpringArray.add(this);
			break;
		case 1:			// Sticking spring
			model.stickSpringArray.add(this);
			ball0.cell.stickSpringArray.add(this);
			ball1.cell.stickSpringArray.add(this);
			break;
		case 2:	case 3: // 2: Small filament spring, continue to doing whatever is in case 3: big filament spring
			model.filSpringArray.add(this);
			ball0.cell.filSpringArray.add(this);
			ball1.cell.filSpringArray.add(this);
			break;
		}
	}
	
	public CSpring(CBall ball0, CBall ball1, int type) {
		this(ball0, ball1, type, new CSpring[0]);
	}

	public void ResetRestLength() {
		switch(type) {
		case 0:				// Rod
			CModel model = this.ballArray[0].cell.model;
			// If type == 1 based on mass, type==2 based on max mass
			if(ballArray[0].cell.type<4) {
				restLength = ballArray[0].radius * model.cellLengthMax[ballArray[0].cell.type]/model.cellRadiusMax[ballArray[0].cell.type];							// About 2 balls in the same cell, so no need to make it complicated  
			} else {
				restLength = ballArray[0].cell.GetAmount()*model.MWX/(Math.PI*model.rhoX*ballArray[0].radius*ballArray[0].radius) - 4.0/3.0*ballArray[0].radius;
//				restLength = 2.0*ballArray[0].radius*model.aspect[ballArray[0].cell.type] * ballArray[0].cell.GetMass()/model.MCellMax[ballArray[0].cell.type];
			};
			break;
		case 1:				// Stick
			restLength = ballArray[1].pos.minus(ballArray[0].pos).norm();
			break;
		case 2:				// Small fil spring
			restLength = 1.1*(ballArray[0].radius + ballArray[1].radius);
			break;
		case 3:				// Big fil spring
			CCell cell0 = ballArray[0].cell;
			CCell cell1 = ballArray[1].cell;
			restLength = 1.6*siblingArray.get(0).restLength + cell0.rodSpringArray.get(0).restLength + cell1.rodSpringArray.get(0).restLength;
			break;
		}
	}
	
	public void ResetK() {
		CCell cell0 = ballArray[0].cell;
		CCell cell1 = ballArray[1].cell;
		CModel model = cell0.model;
		int NSpring = 0;
		switch(type) {
		case 0:
			K = model.Kr;			// Two identical dimension balls, same cell
			break;
		case 1:														// Two different balls, possible different cell types
			if(cell0.type<2 && cell1.type<2)		NSpring = 1;
			else if(cell0.type>1 && cell1.type>1) {
				if(cell0.type<6 && cell1.type<6) 	NSpring = 4;
				else model.Write("Unknown cell types in ResetK", "error");
			} else 									NSpring = 2;
			K = model.Ks/NSpring;
			break;
		case 2:	case 3:												// Two different balls, same cell type
			if(cell0.type<2)						NSpring = 1;
			else if(cell0.type<6)					NSpring = 2;
			else model.Write("Unknown cell types in ResetK", "error");
			K = model.Kf/NSpring;
			break;
		}
	}
	
	public int Break() {												// Also removes siblings
		CModel model = this.ballArray[0].cell.model;
		int count = 0;
		CCell cell0 = ballArray[0].cell;
		CCell cell1 = ballArray[1].cell;
		
		switch (type) {
		case 0: 														// Rod spring
			// Can't Break()
			break;
		case 1:															// Sticking spring
			cell0.stickCellArray.remove(cell1);
			cell1.stickCellArray.remove(cell0);	
			// Remove this and siblings from model and cells
			count += (model.stickSpringArray.remove(this))?1:0;			// Add one to counter if successfully removed
			cell0.stickSpringArray.remove(this);
			cell1.stickSpringArray.remove(this);
			for(CSpring sibling : siblingArray) {
				cell0.stickSpringArray.remove(sibling);
				cell1.stickSpringArray.remove(sibling);
				count += (model.stickSpringArray.remove(sibling))?1:0;
			}
			break;
		case 2:	case 3:													// Filament springs		
			// Remove this and siblings from model and cells
			count += (model.filSpringArray.remove(this))?1:0;			// Add one to counter if successfully removed
			cell0.filSpringArray.remove(this);
			cell1.filSpringArray.remove(this);
			for(CSpring sibling : siblingArray) {
				cell0.filSpringArray.remove(sibling);
				cell1.filSpringArray.remove(sibling);
				count += (model.filSpringArray.remove(sibling))?1:0;
			}
			break;
		}
		return count;
	}
	
	//////////////////////////////////////////////////////////////////////
	
	public void set(CBall ball0, CBall ball1) {
		ballArray[0] = ball0;
		ballArray[1] = ball1;
	}
	
	public int Index() {
		CModel model = this.ballArray[0].cell.model;
		ArrayList<CSpring> array = new ArrayList<CSpring>();
		switch(type) {
		case 0:
			break;
		case 1:
			array = model.stickSpringArray;
			break;
		case 2: case 3:
			array = model.filSpringArray;
			break;
		}
		
		for(int index=0; index<array.size(); index++) {
			if(array.get(index).equals(this))	return index;
		}
		return -1;
	}

} 