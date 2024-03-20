package com.forcex.rte.objects;
import com.forcex.math.*;
import com.forcex.rte.*;

public class RTSphere extends RTObject
{
	float radius;
	
	public RTSphere(Vector3f position,float radius){
		super(position);
		this.radius = radius;
		materials.add(new RTMaterial());
	}
	
	@Override
	public Vector3f intersect(Ray ray) {
		float t = position.sub(ray.origin).dot(ray.direction);
        Vector3f p = ray.origin.add(ray.direction.mult(t));
        float y = position.sub(p).length();
        if (y < radius) {
            float x = (float) Math.sqrt(radius * radius - y * y);
            float t1 = t - x;
            if (t1 > 0) return ray.origin.add(ray.direction.mult(t1));
            else return null;
        } else {
            return null;
        }
	}
	
	@Override
    public Vector3f getNormal(Vector3f point) {
        return point.sub(position).normalize();
    }
}
