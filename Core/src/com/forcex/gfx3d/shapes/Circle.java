package com.forcex.gfx3d.shapes;
import com.forcex.gfx3d.*;
import com.forcex.math.*;
import com.forcex.core.*;

public class Circle extends Mesh
{
	public Circle(float radius,int segments){
		super(true);
		float[] vertices = new float[(segments + 1) * 3];
		short[] indices = new short[segments * 2];
		short offset = 0;
		for(int i = 0;i < segments;i++){
			float angleS = (i / (float)segments) * Maths.PI_2;
			float cs0 = radius * Maths.cos(angleS);
			float sn0 = radius * Maths.sin(angleS);
			vertices[offset] = cs0;
			vertices[offset+1] = sn0;
			vertices[offset+2] = 0;
			offset += 3;
		}
		float a = Maths.PI_2;
		float cs = radius * Maths.cos(a);
		float sn = radius * Maths.sin(a);
		vertices[offset] = cs;
		vertices[offset+1] = sn;
		vertices[offset+2] = 0;

		for(int i = 0;i < segments;i++){
			indices[i*2] = (short)i;
			indices[i*2+1] = (short)(i + 1);
		}
		setVertices(vertices);
		setPrimitiveType(GL.GL_LINES);
		addPart(new MeshPart(indices));
	}
}
