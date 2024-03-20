package com.forcex.rte;
import com.forcex.rte.utils.*;
import com.forcex.math.*;

public class RTMaterial {
	public RTColor color = new RTColor(1,1,1);
	public float roughness = 0.4f;
	public float emission = 0f;
	public float specular = 0.2f;
	public float transparent = 0.0f;
	
	public RTColor getTextureColor(Vector3f direction) {
		return color;
	}
}
