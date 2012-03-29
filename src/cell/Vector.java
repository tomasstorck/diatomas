package cell;

public class Vector {
	double x;
	double y;
	double z;
	
	public Vector(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public double length() {
		return Math.sqrt(x*x+y*y+z*z);
	}
	
	public void normalise() {
		double L = this.length();
		x = x/L;
		y = y/L;
		z = z/L;
	}
	
	public boolean equals(Vector v) {
		if(x==v.x && y==v.y && z==v.z) {
			return true;
		}
		return false;
	}
	
	//////////////////////////////////
	// Arithmetic operations 		//
	//////////////////////////////////
	public Vector minus(Vector v) {
		double newx = this.x - v.x;
		double newy = this.y - v.y;
		double newz = this.z - v.z;
		
		Vector min = new Vector(newx, newy, newz);
		
		return min;
	}
	
	public Vector minus(double n) {
		double newx = this.x - n;
		double newy = this.y - n;
		double newz = this.z - n;
		
		Vector min = new Vector(newx, newy, newz);
		
		return min;
	}
	
	public Vector plus(Vector v) {
		double newx = this.x + v.x;
		double newy = this.y + v.y;
		double newz = this.z + v.z;
		
		Vector plus = new Vector(newx, newy, newz);
		
		return plus;
	}
	
	public Vector plus(double n) {
		double newx = this.x + n;
		double newy = this.y + n;
		double newz = this.z + n;
		
		Vector plus = new Vector(newx, newy, newz);
		
		return plus;
	}
	
	public Vector times(double number) {
		double newx = this.x * number;
		double newy = this.y * number;
		double newz = this.z * number;
		
		Vector times = new Vector(newx, newy, newz);
		
		return times;
	}
	
	public Vector divide(double number) {
		double newx = this.x / number;
		double newy = this.y / number;
		double newz = this.z / number;
		
		Vector divide = new Vector(newx, newy, newz);
		
		return divide;
	}
	
	
}
