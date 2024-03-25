package com.forcex.collision;

import com.forcex.math.Maths;
import com.forcex.math.Ray;
import com.forcex.math.Vector3f;

public class BoundingBox {
    public Vector3f max = new Vector3f(Float.MIN_NORMAL);
    public Vector3f min = new Vector3f(Float.MAX_VALUE);
    public Vector3f center = new Vector3f();
    public Vector3f extent = new Vector3f();
    public float radius = 0f;

    public static void create(BoundingBox dest, float[] vertices) {
        for (int i = 0; i < vertices.length; i += 3) {
            dest.min.x = Maths.min(dest.min.x, vertices[i]);
            dest.min.y = Maths.min(dest.min.y, vertices[i + 1]);
            dest.min.z = Maths.min(dest.min.z, vertices[i + 2]);
            dest.max.x = Maths.max(dest.max.x, vertices[i]);
            dest.max.y = Maths.max(dest.max.y, vertices[i + 1]);
            dest.max.z = Maths.max(dest.max.z, vertices[i + 2]);
        }
    }

    public void calculateExtents() {
        center.set(max);
        center.addLocal(min);
        center.multLocal(0.5f);
        extent.set(max.x - center.x, max.y - center.y, max.z - center.z);
        extent.absoluteLocal();
        radius = extent.length() * 0.5f;
    }

    public BoundingSphere toSphere() {
        return new BoundingSphere(center, radius);
    }

    public boolean intersectRay(Vector3f position, Ray ray) {
        if (intersectsPoint(ray.origin)) {
            return true;
        }
        Vector3f min_t = min.add(position);
        Vector3f max_t = max.add(position);
        float t1, t2, dist_near = Float.NEGATIVE_INFINITY, dist_far = Float.POSITIVE_INFINITY, temp;
        boolean intersectFlag = true;
        for (int i = 0; i < 3; i++) {
            if (ray.direction.get(i) == 0) {
                if (ray.origin.get(i) < min_t.get(i) || ray.origin.get(i) > max_t.get(i))
                    intersectFlag = false;
            } else {
                t1 = (min_t.get(i) - ray.origin.get(i)) / ray.direction.get(i);
                t2 = (max_t.get(i) - ray.origin.get(i)) / ray.direction.get(i);
                if (t1 > t2) {
                    temp = t1;
                    t1 = t2;
                    t2 = temp;
                }
                if (t1 > dist_near)
                    dist_near = t1;
                if (t2 < dist_far)
                    dist_far = t2;
                if (dist_near > dist_far)
                    intersectFlag = false;
                if (dist_far < 0)
                    intersectFlag = false;
            }
        }
        return intersectFlag;
    }

    public boolean intersectsPoint(Vector3f point) {
		return point.x >= min.x && point.y >= min.y && point.z >= min.z &&
				point.x <= max.x && point.y <= max.y && point.z <= max.z;
    }
}
