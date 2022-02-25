package com.forcex.anim;
import com.forcex.math.*;
import com.forcex.utils.*;

public class KeyFrame
{
	public Quaternion rotation;
	public Vector3f position;
	public float time;
	
	public Matrix4f toMatrix(){
		Matrix4f m = new Matrix4f(rotation.toMatrix());
		m.multLocal(Matrix4f.translation(position));
		return m;
	}
}
