package com.forcex.collision;
import com.forcex.math.*;

public class BoundingBox
{
	public Vector3f max = new Vector3f(Float.MIN_NORMAL);
	public Vector3f min = new Vector3f(Float.MAX_VALUE);
	public Vector3f center = new Vector3f();
	public Vector3f extent = new Vector3f();
	public float radius = 0f;

	public static void create(BoundingBox dest,float[] vertices){
		for(int i = 0;i < vertices.length;i += 3){
			dest.min.x = Maths.min(dest.min.x,vertices[i]);
			dest.min.y = Maths.min(dest.min.y,vertices[i+1]);
			dest.min.z = Maths.min(dest.min.z,vertices[i+2]);
			dest.max.x = Maths.max(dest.max.x,vertices[i]);
			dest.max.y = Maths.max(dest.max.y,vertices[i+1]);
			dest.max.z = Maths.max(dest.max.z,vertices[i+2]);
		}
	}
	
	public void calculateExtents(){
		center.set(max);
		center.addLocal(min);
		center.multLocal(0.5f);
		extent.set(max.x - center.x,max.y - center.y,max.z - center.z);
		extent.absoluteLocal();
		radius = extent.length() * 0.5f;
	}
	
	public BoundingSphere toSphere(){
		return new BoundingSphere(center,radius);
	}

	
	public boolean intersectRay (Vector3f position, Ray ray) {
		if(intersectsPoint(ray.origin)){
			return true;
		}
		final float divX = 1f / ray.direction.x;
		final float divY = 1f / ray.direction.y;
		final float divZ = 1f / ray.direction.z;
		Vector3f cnt = position.add(center);
		float minx = ((cnt.x - extent.x * .5f) - ray.origin.x) * divX;
		float maxx = ((cnt.x + extent.x * .5f) - ray.origin.x) * divX;
		if (minx > maxx) {
			final float t = minx;
			minx = maxx;
			maxx = t;
		}
		float miny = ((cnt.y - extent.y * .5f) - ray.origin.y) * divY;
		float maxy = ((cnt.y + extent.y * .5f) - ray.origin.y) * divY;
		if (miny > maxy) {
			final float t = miny;
			miny = maxy;
			maxy = t;
		}
		float minz = ((cnt.z - extent.z * .5f) - ray.origin.z) * divZ;
		float maxz = ((cnt.z + extent.z * .5f) - ray.origin.z) * divZ;
		if (minz > maxz) {
			final float t = minz;
			minz = maxz;
			maxz = t;
		}
		float min = Math.max(Math.max(minx, miny), minz);
		float max = Math.min(Math.min(maxx, maxy), maxz);
		boolean collision = (max >= 0 && max >= min);
		float distance = -1;
		if(!collision){
			final float len = ray.direction.dot(new Vector3f(cnt.x-ray.origin.x, cnt.y-ray.origin.y, cnt.z-ray.origin.z));
			if (len < 0f)
				collision = false;
			float dist2 = position.distance2(ray.origin.x + ray.direction.x * len, ray.origin.y + ray.direction.y * len, ray.origin.z + ray.direction.z * len);
			if (distance >= 0f && dist2 > distance) 
				collision = false;
			if (dist2 <= radius * radius) {
				collision = true;
				distance = dist2;
			}
		}
		return collision;
	}
	
	public boolean intersectsPoint(Vector3f point) {
		if(point.x >= min.x && point.y >= min.y && point.z >= min.z && 
		   point.x <= max.x && point.y <= max.y && point.z <= max.z)
			return true;
		else
			return false;
	}
}
