package com.forcex.gfx3d.shapes;
import com.forcex.gfx3d.*;
import com.forcex.core.*;
import com.forcex.math.*;

public class Axis extends Mesh
{
	public Axis(float scale){
		super(true);
		setVertices(new float[]{
						-scale,0,0,
						scale,0,0,
						0,-scale,0,
						0,scale,0,
						0,0,-scale,
						0,0,scale,
		});
		MeshPart xa = new MeshPart(new short[]{0,1});
		xa.material.color.set(255,0,0);
		MeshPart ya = new MeshPart(new short[]{2,3});
		ya.material.color.set(0,255,0);
		MeshPart za = new MeshPart(new short[]{4,5});
		za.material.color.set(0,0,255);
		addPart(xa);
		addPart(ya);
		addPart(za);
		lineSize = 2f;
		setPrimitiveType(GL.GL_LINES);
	}
	
	public Axis(){
		super(true);
		setVertices(new float[]{
						1,0,0,
						0,1,0,
						0,0,1,
						0,0,0,
					});
		MeshPart xa = new MeshPart(new short[]{3,0});
		xa.material.color.set(255,0,0);
		MeshPart ya = new MeshPart(new short[]{3,1});
		ya.material.color.set(0,255,0);
		MeshPart za = new MeshPart(new short[]{3,2});
		za.material.color.set(0,0,255);
		addPart(xa);
		addPart(ya);
		addPart(za);
		lineSize = 2f;
		setPrimitiveType(GL.GL_LINES);
	}
	
	
	
	public void update(Vector3f x,Vector3f y,Vector3f z) {
		setVertices(new float[]{
						x.x,x.y,x.z,
						y.x,y.y,y.z,
						z.x,z.y,z.z,
						0,0,0,
					});
	}
	
	public void setAxis(int axis){
		for(byte i = 0;i < 3;i++){
			if(axis != -1)
				getPart(i).visible = i == axis;
			else
				getPart(i).visible = true;
		}
	}
}
