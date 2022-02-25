package com.forcex.gfx3d.shapes;
import com.forcex.math.*;
import com.forcex.gfx3d.*;
import com.forcex.core.*;
 
public class WireSphere extends Mesh
{
	public WireSphere(float radius,int samples){
		super(true);
		float[] vertices = new float[samples * 18];
		float rate = Maths.PI_2 / samples;
		float angle = 0;
		for(int i = 0;i < (samples * 18);){
			// xy
			vertices[i] = radius * Maths.cos(angle);
			vertices[i + 1] = radius * Maths.sin(angle);
			vertices[i + 2] = 0;
			vertices[i + 3] = radius * Maths.cos(angle + rate);
			vertices[i + 4] = radius * Maths.sin(angle + rate);
			vertices[i + 5] = 0;
			// xz
			i += 6;
			vertices[i ] = radius * Maths.cos(angle);
			vertices[i + 1] = 0;
			vertices[i + 2] = radius * Maths.sin(angle);
			vertices[i + 3] = radius * Maths.cos(angle + rate);
			vertices[i + 4] = 0;
			vertices[i + 5] = radius * Maths.sin(angle + rate);
			i += 6;
			// zy
			vertices[i] = 0;
			vertices[i + 1] = radius * Maths.cos(angle);
			vertices[i + 2] = radius * Maths.sin(angle);
			vertices[i + 3] = 0;
			vertices[i + 4] = radius * Maths.cos(angle + rate);
			vertices[i + 5] = radius * Maths.sin(angle + rate);
			i += 6;
			angle += rate;
		}
		short[] indices = new short[samples * 6];
		for(int i = 0;i < (samples * 6); i += 6){
			// xy
			indices[i] = (short)(i);
			indices[i+1] = (short)(i+1);
			// xz
			indices[i+2] = (short)(i+2);
			indices[i+3] = (short)(i+3);
			// yz
			indices[i+4] = (short)(i+4);
			indices[i+5] = (short)(i+5);
		}
		setVertices(vertices);
		setPrimitiveType(GL.GL_LINES);
		addPart(new MeshPart(indices));
	}
}
