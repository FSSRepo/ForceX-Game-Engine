package com.forcex.collision;
import com.forcex.math.*;

public class BoundingTriangle
{
	public Vector3f v1,v2,v3;
	public byte part;
	public short index_face;

	public Vector3f normal(){
		return MathGeom.normalTriangle(v1,v2,v3);
	}

	public Vector3f center(){
		return new Vector3f(
			v1.x + v2.x + v3.x,
			v1.y + v2.y + v3.y,
			v1.z + v2.z + v3.z).multLocal(1f / 3f);
	}
}
