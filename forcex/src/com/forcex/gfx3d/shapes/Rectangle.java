package com.forcex.gfx3d.shapes;
import com.forcex.gfx3d.*;

public class Rectangle extends Mesh
{
	float[] positions = {
		-1,+1,0,
		-1,-1,0,
		+1,-1,0,
		+1,+1,0
	};
	float[] normals = {
		0,0,1,
		0,0,1,
		0,0,1,
		0,0,1
	};
	short[] indices = {
		0,1,3,
		3,1,2
	};
	float[] textureCoords = {
		0,0,
		0,1,
		1,1,
		1,0
	};
	public Rectangle(float width,float height,boolean mirror){
		super(true);
		float w = width * 0.5f;
		float h = height * 0.5f;
		positions  = new float[]{
			-w,+h,0,
			-w,-h,0,
			+w,-h,0,
			+w,+h,0
		};
		getModel(mirror);
	}
	
	public void getModel(boolean mirror){
		setVertices(positions);
		setTextureCoords(mirror ? new float[]{
							 0,1,
							 0,0,
							 1,0,
							 1,1
						 } : textureCoords);
		setNormals(normals);
		addPart(new MeshPart(indices));
	}
}
