// This function is NOT AT ALL like Vector.java. This is a vector as you would encounter in linear algebra, i.e. a collection of three dimensional locations [x, y, z]. 

package NR;

import java.io.Serializable;

public class Vector3d implements Serializable {
	private static final long serialVersionUID = 1L;
	//
	public double x;
	public double y;
	public double z;
	
	///////////////////////////////////////////////////////////////////
	
	public Vector3d(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Vector3d(double[] pos) {
		if(pos.length != 3)		throw new IndexOutOfBoundsException("pos.length must be equal to 3");
		this.x = pos[0];
		this.y = pos[1];
		this.z = pos[2];
	}
	
	public Vector3d(Vector3d v) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
	}
	
	public Vector3d() {}
	
	///////////////////////////////////////////////
	
	public double norm() {
		return Math.sqrt(x*x+y*y+z*z);
	}
	
	public Vector3d normalise() {
		double L = this.norm();
		return new Vector3d(x/L, y/L, z/L);
	}
	
	public boolean equals(Vector3d v) {
		if(x==v.x && y==v.y && z==v.z) {
			return true;
		}
		return false;
	}
	
	//////////////////////////////////
	// Arithmetic operations 		//
	//////////////////////////////////
	public Vector3d minus(Vector3d v) {
		double newx = this.x - v.x;
		double newy = this.y - v.y;
		double newz = this.z - v.z;
		
		Vector3d min = new Vector3d(newx, newy, newz);
		
		return min;
	}
	
	public Vector3d minus(double n) {
		double newx = this.x - n;
		double newy = this.y - n;
		double newz = this.z - n;
		
		Vector3d min = new Vector3d(newx, newy, newz);
		
		return min;
	}
	
	public Vector3d plus(Vector3d v) {
		double newx = this.x + v.x;
		double newy = this.y + v.y;
		double newz = this.z + v.z;
		
		Vector3d plus = new Vector3d(newx, newy, newz);
		
		return plus;
	}
	
	public Vector3d plus(double n) {
		double newx = this.x + n;
		double newy = this.y + n;
		double newz = this.z + n;
		
		Vector3d plus = new Vector3d(newx, newy, newz);
		
		return plus;
	}
	
	public Vector3d times(double number) {
		double newx = this.x * number;
		double newy = this.y * number;
		double newz = this.z * number;
		
		Vector3d times = new Vector3d(newx, newy, newz);
		
		return times;
	}
	
	public Vector3d times(Vector3d v) {
		double newx = this.x * v.x;
		double newy = this.y * v.y;
		double newz = this.z * v.z;
		
		Vector3d times = new Vector3d(newx, newy, newz);
		
		return times;
	}
	
	public Vector3d divide(double number) {
		double newx = this.x / number;
		double newy = this.y / number;
		double newz = this.z / number;
		
		Vector3d divide = new Vector3d(newx, newy, newz);
		
		return divide;
	}
	
	////////////////////////////
	// Vector multiplications //
	////////////////////////////
	public double dot(Vector3d u) {
		return this.x*u.x + this.y*u.y + this.z*u.z;
	}
	
	public Vector3d cross(Vector3d u) {
		double x = this.y*u.z - this.z*u.y;
		double y = this.z*u.x - this.x*u.z;
		double z = this.x*u.y - this.y*u.x;
		return new Vector3d(x,y,z);
	}

	////////////////////////////
	// Vector projections     //
	////////////////////////////
	public double comp(Vector3d u) {		// Scalar projection of u onto this, not the other way around
		return this.dot(u) / this.norm();
	}
	
	public Vector3d proj(Vector3d u) {
		return this.times(this.dot(u) / (this.norm()*this.norm()));		// i.e. (a dot b over length(a)^2 times a) or (comp of b onto a times a over length(a))
	}
	
}
