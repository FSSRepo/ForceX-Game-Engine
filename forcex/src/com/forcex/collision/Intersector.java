package com.forcex.collision;
import com.forcex.math.*;

public class Intersector {
	public static boolean intersectsRayTriangle(Ray ray,BoundingTriangle triangle,boolean both_sides){
		float dot = ray.direction.dot(triangle.normal());
		if(!both_sides && dot > -Maths.epsilon){
			return false;
		}else if(both_sides && dot < Maths.epsilon && dot > -Maths.epsilon){
			return false;
		}
		Vector3f edge1 = triangle.v2.sub(triangle.v1);
		Vector3f edge2 = triangle.v3.sub(triangle.v1);
		Vector3f s1 = edge2.cross(ray.direction);
		float invDet = 1.0f / s1.dot(edge1);
		Vector3f dist = ray.origin.sub(triangle.v1);
		Vector3f s2 = dist.cross(edge1);
		float b1 = dist.dot(s1) * invDet;
		if(b1 < 0.0f || b1 > 1.0f)
			return false;
		float b2 = ray.direction.dot(s2) * invDet;
		if(b2 < 0.0f || (b1 + b2) > 1.0f)
			return false;
		float tray = edge2.dot(s2);
		return tray > 0.0f;
	}
}
