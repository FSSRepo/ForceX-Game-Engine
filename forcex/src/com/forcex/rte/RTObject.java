package com.forcex.rte;
import java.util.ArrayList;

import com.forcex.math.Ray;
import com.forcex.math.Vector3f;

public abstract class RTObject {
	protected boolean arePrimitives = true;
	protected ArrayList<RTMaterial> materials = new ArrayList<RTMaterial>();
	protected Vector3f position;
	
	public RTObject(Vector3f position){
		this.position = position;
	}
	
	public RTMaterial getMaterial(int index){
		return materials.get(index);
	}
	
	public abstract Vector3f intersect(Ray ray);
	public abstract Vector3f getNormal(Vector3f point);
}
