package backbone;

import cell.CModel;

public abstract class Run {
	CModel model;
	public int port = 2036;
	public boolean bit64 = false;
	
	public void Initialise() throws RuntimeException {}
	
	public void Start() throws Exception {}
}
