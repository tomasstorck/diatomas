package ericson;

import ibm.Vector3d;

public class DetectCollision {
	public static ericson.ReturnObject LinesegLineseg(Vector3d p1, Vector3d q1, Vector3d p2, Vector3d q2) {		// This is line segment - line segment collision detection. 
		// Rewritten 120912 because of strange results with the original function
		// Computes closest points C1 and C2 of S1(s) = P1+s*(Q1-P1) and S2(t) = P2+t*(Q2-P2)
		Vector3d d1 = q1.minus(p1);		// Direction of S1
		Vector3d d2 = q2.minus(p2);		// Direction of S2
		Vector3d r = p1.minus(p2);
		double a = d1.dot(d1);			// Squared length of S1, >0
		double e = d2.dot(d2);			// Squared length of S2, >0
		double f = d2.dot(r);
		double c = d1.dot(r);
		double b = d1.dot(d2);
		double denom = a*e-b*b;			// Always >0
		
		// If segments are not parallel, compute closest point on L1 to L2 and clamp to segment S1, otherwise pick arbitrary s (=0)
		double s;
		if(denom!=0.0) {
			s = Common.Clamp((b*f-c*e) /  denom, 0.0, 1.0);
		} else	s = 0.0;
		// Compute point on L2 closest to S1(s) using t = ((P1+D1*s) - P2).dot(D2) / D2.dot(D2) = (b*s + f) / e
		double t = (b*s + f) / e;
		
		// If t is in [0,1] (i.e. on S2) we're done. Else Clamp(t), recompute s for the new value of t using s = ((P2+D2*t) - P1).dot(D1) / D1.dot(D1) = (t*b - c) / a and clamp s to [0,1]
		if(t<0.0) {
			t = 0.0;
			s = Common.Clamp(-c/a, 0.0, 1.0);
		} else if (t>1.0) {
			t = 1.0;
			s = Common.Clamp((b-c)/a, 0.0, 1.0);
		}
		
		Vector3d c1 = p1.plus(d1.times(s));	// Collision point on S1
		Vector3d c2 = p2.plus(d2.times(t));	// Collision point on S2
		
		Vector3d dP = c1.minus(c2);  	// = S1(sc) - S2(tc)
		
		double dist2 = (c1.minus(c2)).dot(c1.minus(c2));
		
		return new ericson.ReturnObject(dP, Math.sqrt(dist2), s, t, c1, c2);
	}
	
	public static ericson.ReturnObject LinesegLine(Vector3d p1, Vector3d q1, Vector3d p2, Vector3d q2) {
		// Based on DetectLineSegLineSeg from Ericson
		// Computes closest points C1 and C2 of L1(s) = P1+s*(Q1-P1) and S2(t) = P2+t*(Q2-P2). s is unlimited, t is limited to [0, 1]
		Vector3d d1 = q1.minus(p1);		// Direction of S1
		Vector3d d2 = q2.minus(p2);		// Direction of S2
		Vector3d r = p1.minus(p2);
		double a = d1.dot(d1);			// Squared length of S1, >0
		double e = d2.dot(d2);			// Squared length of S2, >0
		double f = d2.dot(r);
		double c = d1.dot(r);
		double b = d1.dot(d2);
		double denom = a*e-b*b;			// Always >0
		
		// If segments are not parallel, compute closest point on L1 to L2 and clamp to segment S1, otherwise pick arbitrary s (=0)
		double s;
		if(denom!=0.0) {
			s = Common.Clamp((b*f-c*e) /  denom, 0.0, 1.0);
		} else	s = 0.0;
		// Compute point on L2 closest to S1(s) using t = ((P1+D1*s) - P2).dot(D2) / D2.dot(D2) = (b*s + f) / e
		double t = (b*s + f) / e;
		// t is unlimited, so no need to clamp --> we're done
		
		Vector3d c1 = p1.plus(d1.times(s));
		Vector3d c2 = p2.plus(d2.times(t));
		
		Vector3d dP = c1.minus(c2);  	// = S1(sc) - S2(tc)
		
		double dist2 = (c1.minus(c2)).dot(c1.minus(c2));
		
		return new ericson.ReturnObject(dP, Math.sqrt(dist2), s, t, c1, c2);
	}
	
	public static ericson.ReturnObject LinesegPoint(Vector3d p1, Vector3d q1, Vector3d p2) {
		Vector3d ab = q1.minus(p1);  	// line
		Vector3d w = p2.minus(p1);		//point-line
		//Project c onto ab, computing parameterized position d(t) = a + t*(b-a)
		double rpos = w.dot(ab)/ab.dot(ab);
		//if outside segment, clamp t and therefore d to the closest endpoint
		rpos = Common.Clamp(rpos, 0.0, 1.0);
		//compute projected position from the clamped t
		Vector3d d = p1.plus(ab.times(rpos));
		//calculate the vector p2 --> d
		Vector3d dP = d.minus(p2);
		ericson.ReturnObject R = new ericson.ReturnObject(dP, dP.norm(), rpos);	// Defined at the end of the model class. OPTIMISE: we don't need dP.norm() sometimes and could leave it out
		return R;
	}
	
	public static ericson.ReturnObject LinePoint(Vector3d p1, Vector3d q1, Vector3d p2) {
		Vector3d ab = q1.minus(p1);  	// vector from p1 to q1 (i.e. the line segment)
		Vector3d w = p2.minus(p1);		// vector from p1 to p2
		//Project c onto ab, computing parameterized position d(t) = a + t*(b-a). rpos can be >1.0, i.e. d can be longer than the line segment
		double rpos = w.dot(ab)/ab.dot(ab);
		// Do not clamp rpos to the range of the line segment, so our line segment becomes a line when projected onto it
		// Compute projected position of p1 --> p2 onto p1 --> q1
		Vector3d d = p1.plus(ab.times(rpos));
		//calculate the vector p2 --> d
		Vector3d dP = d.minus(p2);
		ericson.ReturnObject R = new ericson.ReturnObject(dP, dP.norm(), rpos);	// Defined at the end of the model class
		return R;
	}

}
