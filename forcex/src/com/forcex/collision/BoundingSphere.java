package com.forcex.collision;

import com.forcex.math.Ray;
import com.forcex.math.Vector3f;

public class BoundingSphere {
    public float radius = 5f;
    public Vector3f center = new Vector3f();

    public BoundingSphere() {
    }

    public BoundingSphere(Vector3f center, float radius) {
        this.center = center;
        this.radius = radius;
    }

    public boolean intersectsRay(Ray ray) {
        float len = ray.direction.dot(center.sub(ray.origin));
        if (len < 0.0f) {
            // behind this sphere
            return false;
        }
        float dist = center.distance2(ray.origin.add(ray.direction.mult(len)));
        return dist < (radius * radius);
    }
}
