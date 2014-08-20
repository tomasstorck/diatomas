package ericson;

import cell.Vector3d;

public class ReturnObject {		// Used for collision detection multiple return
	public Vector3d dP;
	public double dist;
	public double sc;
	public double tc;
	public Vector3d c1;
	public Vector3d c2;
	
	public ReturnObject(Vector3d dP, double dist, double sc) {
		this.dP = dP;
		this.dist = dist; 
		this.sc = sc;
	}
	
	public ReturnObject(Vector3d dP, double dist, double sc, double tc, Vector3d c1, Vector3d c2) {
		this.dP = dP;
		this.dist = dist; 
		this.sc = sc;
		this.tc = tc;
		this.c1 = c1;
		this.c2 = c2;
	}
}