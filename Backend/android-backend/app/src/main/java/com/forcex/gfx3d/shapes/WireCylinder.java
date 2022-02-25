package com.forcex.gfx3d.shapes;
import com.forcex.gfx3d.*;
import com.forcex.math.*;
import com.forcex.core.*;

public class WireCylinder extends Mesh
{
	public static enum Axis{
		AXIS_X,AXIS_Y,AXIS_Z
	}
	int vertex_offset;
	int index_offset;
	short[] indices;
	byte segments = 15;
	
	public WireCylinder(float height,float radius,Axis axis,boolean isCapsule){
		super(true);
		float[] vertices = new float[(segments * 6) + (isCapsule ? (segments * 36) : (segments * 12))];
		indices = new short[segments*2 + (isCapsule ? (segments*12) : (segments*4))];
		byte axs = 1;
		switch(axis){
			case AXIS_X:
				axs = 0;
				break;
			case AXIS_Y:
				axs = 1;
				break;
			case AXIS_Z:
				axs = 2;
				break;
			default:
			axs = 1;
			break;
		}
		drawDiagonalLines(vertices,height,radius,axs);
		if(isCapsule){
			drawSphere(vertices,radius,height / -2f,axs);
			drawSphere(vertices,radius,height / 2f,axs);
		}else{
			drawCircle(vertices,height/-2f,radius,axs);
			drawCircle(vertices,height/2f,radius,axs);
		}
		setVertices(vertices);
		setPrimitiveType(GL.GL_LINES);
		addPart(new MeshPart(indices));
	}
	
	private void drawDiagonalLines(
		float[] vertexs,float height,float radius,byte axis){
		float half_height = height * 0.5f;
		float rate = (Maths.PI_2 / segments);
		float angle = 0;
		boolean isX = axis == 0,isZ = axis == 2;
		for(byte i = 0;i < segments;i++){
			float x = radius * Maths.cos(angle);
			float y = radius * Maths.sin(angle);
			vertexs[vertex_offset  ] = isX ? half_height : (isZ ? x : x);
			vertexs[vertex_offset+1] = isX ? x : (isZ ? y : half_height);
			vertexs[vertex_offset+2] = isX ? y : (isZ ? half_height : y);
			vertexs[vertex_offset+3] = isX ? -half_height : (isZ ? x : x);
			vertexs[vertex_offset+4] = isX ? x : (isZ ? y : -half_height);
			vertexs[vertex_offset+5] = isX ? y : (isZ ? -half_height : y);
			vertex_offset += 6;
			angle += rate;
		}
		for(byte i = 0;i < segments;i++){
			indices[index_offset] = (short)(index_offset);
			indices[index_offset+1] = (short)(index_offset+1);
			index_offset += 2;
		}
	}
	
	private void drawCircle(
		float[] vertexs,float h,float radius,byte axis){
		float rate = (Maths.PI_2 / segments);
		float angle = 0;
		boolean isX = axis == 0,isZ = axis == 2;
		for(byte i = 0;i < segments;i++){
			float x = radius * Maths.cos(angle);
			float y = radius * Maths.sin(angle);
			vertexs[vertex_offset  ] = isX ? h : (isZ ? x : x);
			vertexs[vertex_offset+1] = isX ? x : (isZ ? y : h);
			vertexs[vertex_offset+2] = isX ? y : (isZ ? h : y);
			x = radius * Maths.cos(angle + rate);
			y = radius * Maths.sin(angle + rate);
			vertexs[vertex_offset+3] = isX ? h : (isZ ? x : x);
			vertexs[vertex_offset+4] = isX ? x : (isZ ? y : h);
			vertexs[vertex_offset+5] = isX ? y : (isZ ? h : y);
			vertex_offset += 6;
			angle += rate;
		}
		for(byte i = 0;i < segments;i++){
			indices[index_offset] = (short)(index_offset);
			indices[index_offset+1] = (short)(index_offset+1);
			index_offset += 2;
		}
	}
	
	private void drawSphere(float[] vertexs,float radius,float h,byte axis){
		float rate = Maths.PI_2 / segments;
		float angle = 0;
		float tx = (axis == 0 ? h : 0);
		float ty = (axis == 1 ? h : 0);
		float tz = (axis == 2 ? h : 0);
		for(byte i = 0;i < segments; i ++){
			// xy
			float x = radius * Maths.cos(angle);
			float y = radius * Maths.sin(angle);
			float x2 = radius * Maths.cos(angle + rate);
			float y2 = radius * Maths.sin(angle + rate);
			vertexs[vertex_offset    ] = x + tx;
			vertexs[vertex_offset + 1] = y + ty;
			vertexs[vertex_offset + 2] = tz;
			
			vertexs[vertex_offset + 3] = x2 + tx;
			vertexs[vertex_offset + 4] = y2 + ty;
			vertexs[vertex_offset + 5] = tz;
			// xz
			vertex_offset += 6;
			vertexs[vertex_offset    ] = tx + x;
			vertexs[vertex_offset + 1] = ty;
			vertexs[vertex_offset + 2] = tz + y;
			
			vertexs[vertex_offset + 3] = tx + x2;
			vertexs[vertex_offset + 4] = ty;
			vertexs[vertex_offset + 5] = tz +y2;
			vertex_offset += 6;
			// zy
			vertexs[vertex_offset    ] = tx;
			vertexs[vertex_offset + 1] = ty + x;
			vertexs[vertex_offset + 2] = tz + y;
			
			vertexs[vertex_offset + 3] = tx;
			vertexs[vertex_offset + 4] = ty + x2;
			vertexs[vertex_offset + 5] = tz + y2;
			vertex_offset += 6;
			angle += rate;
		}
		for(int i = 0;i < segments;i++){
			// xy
			indices[index_offset    ] = (short)(index_offset);
			indices[index_offset + 1] = (short)(index_offset +1);
			// xz
			indices[index_offset + 2] = (short)(index_offset +2);
			indices[index_offset + 3] = (short)(index_offset +3);
			// yz
			indices[index_offset + 4] = (short)(index_offset+4);
			indices[index_offset + 5] = (short)(index_offset+5);
			index_offset += 6;
		}
	}
}
