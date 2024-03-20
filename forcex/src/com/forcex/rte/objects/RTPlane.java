package com.forcex.rte.objects;
import com.forcex.math.*;
import com.forcex.utils.*;
import com.forcex.rte.*;
import com.forcex.rte.utils.*;

public class RTPlane extends RTObject
{
    private boolean checkerPattern;

    public RTPlane(float height, boolean checkerPattern) {
        super(new Vector3f(0,0,height));
        this.checkerPattern = checkerPattern;
		materials.add(new PlaneMaterial());
    }

    @Override
    public Vector3f intersect(Ray ray) {
        float t = -(ray.origin.z - position.z) / ray.direction.z;
        if (t > 0 && Float.isFinite(t)) {
            return ray.origin.add(ray.direction.mult(t));
        }
        return null;
    }

    @Override
    public Vector3f getNormal(Vector3f point) {
        return new Vector3f(0, 0, 1);
    }

    private class PlaneMaterial extends RTMaterial {
		@Override
		public RTColor getTextureColor(Vector3f point) {
			if (checkerPattern) {
				if ((int)point.x % 2 == 0 ^ (int)point.y % 2 != 0) {
					return new RTColor(0.9f,0.9f,0.9f);
				} else {
					return new RTColor(0,0,0);
				}
			} else {
				return color;
			}
		}
	}
}
