package com.forcex.rte.objects;
import com.forcex.math.Ray;
import com.forcex.math.Vector3f;
import com.forcex.rte.RTMaterial;
import com.forcex.rte.RTObject;

public class RTBox extends RTObject {

	@Override
	public Vector3f intersect(Ray ray) {
		float t1,t2,tnear = Float.NEGATIVE_INFINITY,tfar = Float.POSITIVE_INFINITY,temp;
		boolean intersectFlag = true;
		for(int i = 0;i < 3; i++){
            if(ray.direction.get(i) == 0){
                if(ray.origin.get(i) < min.get(i) || ray.origin.get(i) > max.get(i))
                    intersectFlag = false;
            }
            else{
                t1 = (min.get(i) - ray.origin.get(i)) / ray.direction.get(i);
                t2 = (max.get(i) - ray.origin.get(i)) / ray.direction.get(i);
                if(t1 > t2){
                    temp = t1;
                    t1 = t2;
                    t2 = temp;
                }
                if(t1 > tnear)
                    tnear = t1;
                if(t2 < tfar)
                    tfar = t2;
                if(tnear > tfar)
                    intersectFlag = false;
                if(tfar < 0)
                    intersectFlag = false;
            }
        }
		if(intersectFlag) {
			return ray.origin.add(ray.direction.mult(tnear));
		}
		return null;
	}
	
	Vector3f min;
	Vector3f max;
	
	public RTBox(Vector3f position,Vector3f extent){
		super(position);
		min = position.sub(extent);
		max = position.add(extent);
		materials.add(new RTMaterial());
	}

	@Override
	public Vector3f getNormal(Vector3f point) {
		Vector3f direction = point.sub(position);
        float biggestValue = Float.NaN;
        for (int i = 0; i < 3; i++) {
            if (Float.isNaN(biggestValue) || biggestValue < Math.abs(direction.get(i))) {
                biggestValue = Math.abs(direction.get(i));
            }
        }
        if (biggestValue == 0) {
            return new Vector3f(0, 0, 0);
        } else {
            for (int i = 0; i < 3; i++) {
                if (Math.abs(direction.get(i)) == biggestValue) {
                    float[] normal = new float[] {0,0,0};
                    normal[i] = direction.get(i) > 0 ? 1 : -1;
                    return new Vector3f(normal[0], normal[1], normal[2]);
                }
            }
        }
        return new Vector3f(0, 0, 0);
	}
}
