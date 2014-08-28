package ibm;

import java.io.Serializable;
import java.util.ArrayList;


public abstract class Spring implements Serializable {
	private static final long serialVersionUID = 1L;
	//
	public Ball[] ballArray;
//	public Vector3d anchorPoint;
	public double K;
	public double restLength;
//	public int type;
	
	///////////////////////////////////////////////////////////////////
	
	public void ResetRestLength() {
		throw new NoSuchMethodError("Not implemented in subclass");
	}
	
	public void ResetK() {
		throw new NoSuchMethodError("Not implemented in subclass");
	}
	
	public int Break() {
		throw new NoSuchMethodError("Not implemented in subclass");
	}
	
	//////////////////////////////////////////////////////////////////////
	
	
	public int Index() {
		throw new NoSuchMethodError("Not implemented in subclass");
	}
	
	public int Index(ArrayList<? extends Spring> array) {
		for(int index=0; index<array.size(); index++) {
			if(array.get(index).equals(this))	return index;
		}
		return -1;
	}

	public Vector3d GetL() {
		return ballArray[1].pos.minus(ballArray[0].pos);
	}
} 