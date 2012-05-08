package cell;

public class Vector3d {
	double x;
	double y;
	double z;
	
	public Vector3d(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Vector3d(Vector3d v) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
	}
	
	public Vector3d() {}
	
	///////////////////////////////////////////////
	
	public double length() {
		return Math.sqrt(x*x+y*y+z*z);
	}
	
	public void normalise() {
		double L = this.length();
		x = x/L;
		y = y/L;
		z = z/L;
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
	
	public Vector3d divide(double number) {
		double newx = this.x / number;
		double newy = this.y / number;
		double newz = this.z / number;
		
		Vector3d divide = new Vector3d(newx, newy, newz);
		
		return divide;
	}

	///////////////////////
	// Vector operations //
	///////////////////////
	public double dot(Vector3d u) {
		return this.x*u.x + this.y*u.y + this.z*u.z;
	}
	
	
}
