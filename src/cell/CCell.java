package cell;

import java.util.ArrayList;

public class CCell {
	int type;
	boolean filament;
	Vector colour;
	CBall[] ballArray = 	new CBall[2];
	CSpring springArray;
	CCell mother;
	int cellArrayIndex;
	CModel pModel;
	
	////////////////////////////////////////////////////////////////////////////////////////////
	
	public CCell(int type, double base0x, double base0y, double base0z, double base1x, double base1y, double base1z, boolean filament, CModel model) {
		this.type = type;
		this.filament = filament;
		colour = new Vector(rand.Double(),rand.Double(),rand.Double());
		cellArrayIndex = model.cellArray.size();			// +1 because it's the next, -1 because index in Java is 0-based
		pModel = model;
		
		if(type==0) { // Leaves ballArray and springArray, and mother
			ballArray[0] = new CBall(base0x, base0y, base0z, model.MCellInit,   0, this);
		} else {
			ballArray[0] = new CBall(base0x, base0y, base0z, model.MCellInit/2, 0, this);
			ballArray[1] = new CBall(base1x, base1y, base1z, model.MCellInit/2, 0, this);
			springArray = new CSpring(ballArray[0],ballArray[1]);
		}
		model.cellArray.add(this);	
	}
	
	public CCell(int type, double base0x, double base0y, double base0z, boolean filament, CModel model) {
		this.type = type;
		this.filament = filament;
		colour = new Vector(rand.Double(),rand.Double(),rand.Double());
		cellArrayIndex = model.cellArray.size();			// +1 because it's the next, -1 because index in Java is 0-based
		pModel = model;
		
		if(type==0) { // Leaves ballArray and springArray, and mother
			ballArray[0] = new CBall(base0x, base0y, base0z, model.MCellInit,   0, this);
		} else {
			ballArray[0] = new CBall(base0x, base0y, base0z, model.MCellInit/2, 0, this);
			
			Vector direction = new Vector(rand.Double(),rand.Double(),rand.Double());
			direction.normalise();	// Normalise direction
			
			double Rpos;
			if(type==1) {
				Rpos = ballArray[0].Radius() * model.aspect*2;							// Find new pos
			} else {
				Rpos = ballArray[0].Radius() * model.aspect*2 * ballArray[0].mass/model.MCellMax;
			}
			double base1x = base0x + direction.x * Rpos;
			double base1y = base0y + direction.y * Rpos;
			double base1z = base0z + direction.z * Rpos;
			
			ballArray[1] = new CBall(base1x, base1y, base1z, model.MCellInit/2, 0, this);
			springArray = new CSpring(ballArray[0],ballArray[1]);
		}
		model.cellArray.add(this);
	}
	
	public CCell(int type, Vector base, boolean filament, CModel model) {
		new CCell(type, base.x, base.y, base.z, filament, model);
	}
	
	public void Anchor() {
		int NBall = (type==0) ? 1 : 2;
		CAnchorSpring[] anchorArray = new CAnchorSpring[NBall];
		for(int iBall = 0; iBall < NBall; iBall++) {
			anchorArray[iBall] = new CAnchorSpring(ballArray[iBall]);
		}
		
		// Define siblings, just hardcode, saves time (for me)
		if(NBall > 1) {
			anchorArray[0].siblingArray = new CAnchorSpring[2];
			anchorArray[1].siblingArray = new CAnchorSpring[2];
			anchorArray[0].siblingArray[0] = anchorArray[1];
			anchorArray[1].siblingArray[0] = anchorArray[0];
		}
		
		// All done, add them
		for(int iSpring=0; iSpring < NBall; iSpring++) {
			pModel.anchorSpringArray.add(anchorArray[iSpring]);
		}
	}
	
	public void Stick(CCell pCell) {
		// Determine how many sticking springs we need
		int NSpring;
		CCell pA, pB;
		if(type == 0 && pCell.type == 0) 		{NSpring = 1; pA = this; 	pB = pCell;}	// Doesn't matter which one goes first
		else if(type > 0 && pCell.type == 0) 	{NSpring = 2; pA = pCell; 	pB = this;}	// Sphere goes first (see indexing next paragraph)
		else if(type == 0) 						{NSpring = 2; pA = this; 	pB = pCell;}	// Sphere goes first
		else 									{NSpring = 4; pA = this; 	pB = pCell;} 	// Doesn't matter
		
		CStickSpring[] stickArray = new CStickSpring[NSpring];
		for(int iSpring = 0; iSpring < NSpring; iSpring++) {				// Create all springs, with input balls
			stickArray[iSpring] = new CStickSpring(pA.ballArray[iSpring/2],	// 0, 0, 1, 1, ...
					pB.ballArray[iSpring%2]); 								// 0, 1, 0, 1, ...
		}
		
		// Define siblings
		for(int iSpring = 0; iSpring < NSpring; iSpring++) {				// For each spring and siblingspring
			CStickSpring pSpring1 = stickArray[iSpring];
			pSpring1.siblingArray = new ArrayList<CStickSpring>();			// Initialise the siblingArray for the spring

			for(int iSpring2 = 0; iSpring2 < NSpring; iSpring2++) {			
				if(iSpring != iSpring2) {									// For all its siblings
					pSpring1.siblingArray.add(stickArray[iSpring2]);
				}
			}
		}
		
		//All done, add them
		for(int iSpring = 0; iSpring < stickArray.length; iSpring++) {
			pModel.stickSpringArray.add(stickArray[iSpring]);
		}
	}
	
	public void Stick(int otherCellIndex) {
		CCell pCell = pModel.cellArray.get(otherCellIndex);
		Stick(pCell);
	}
	
	public double GetMass() {
		int NBall = (type==0) ? 1 : 2;
		double mass = 0; 
		for(int iBall=0; iBall<NBall; iBall++) {
			mass += ballArray[iBall].mass;
		}
		return mass;
	}
	
	public void SetMass(double newMass) {
		if(type==0) {
			ballArray[0].mass = newMass;
		} else {
			ballArray[0].mass = newMass/2;
			ballArray[1].mass = newMass/2;
		}
	}
}
