package com.forcex.gui;
import com.forcex.math.*;
import com.forcex.utils.*;
import com.forcex.*;
import com.forcex.core.*;

public class RoundQuad{
	float[] vertex_data;
	short[] index_data;
	short offset = 0,offset2 = 0;
	int vbo,ibo,count;
	float porcent = 0;
	
	public RoundQuad(float pcnt){
		porcent = pcnt;
	}

	public RoundQuad bind(Drawer drawer,int samples,float aspect_ratio){
		float radius = porcent * 0.01f;
		vertex_data = new float[(12 + (samples - 1) * 4) * 7];
		index_data = new short[(10 + samples * 4) * 3];
		// medium quad
		addVertex(-1 + radius,1 - radius); // v1
		addVertex(1 - radius,1 - radius); // v2
		addVertex(-1 + radius,-1 + radius); // v3
		addVertex(1 - radius,-1 + radius); // v4
		addQuad(2,1,3,0);
		// top quad
		addVertex(-1 + radius,1); // v5
		addVertex(1 - radius,1); // v6
		addQuad(0,5,1,4);
		// right quad
		addVertex(1,1 - radius); // v7
		addVertex(1,-1 + radius); // v8
		addQuad(3,6,7,1);
		// bottom quad
		addVertex(1 - radius,-1); //v9
		addVertex(-1 + radius,-1); // v10
		addQuad(9,3,8,2);
		// left quad
		addVertex(-1,-1 + radius); // v11
		addVertex(-1,1 - radius); // v12
		addQuad(10,0,2,11);
		// top left corner
		float cir_mid = Maths.PI * 0.5f;
		Vector2f translate = new Vector2f(-1 + radius,1 - radius*aspect_ratio);
		for(float i = 1;i < samples;i++) {
			float sample = cir_mid + (cir_mid * (i / samples));
			addVertex(Maths.cos(sample) * radius + translate.x,Maths.sin(sample) * radius * aspect_ratio + translate.y);
		}
		int index_corner = 12;
		addTriangle(4,index_corner,0);
		for(byte i = 0;i < (samples - 2);i++) {
			addTriangle(index_corner,index_corner + 1,0);
			index_corner++;
		}
		addTriangle(11,index_corner,0);
		// top right corner
		translate.set(1 - radius,1 - radius * aspect_ratio);
		for(float i = 1;i < samples;i++) {
			float sample = cir_mid * (i / samples);
			addVertex(Maths.cos(sample) * radius + translate.x,Maths.sin(sample) *radius * aspect_ratio + translate.y);
		}
		addTriangle(6,index_corner + 1,1);
		index_corner++;
		for(byte i = 0;i < (samples - 2);i++) {
			addTriangle(index_corner,index_corner + 1,1);
			index_corner++;
		}
		addTriangle(5,index_corner,1);
		// bottom right corner
		translate.set(1 - radius,-1 + radius*aspect_ratio);
		for(float i = 1;i < samples;i++) {
			float sample = -cir_mid * (i / samples);
			addVertex(Maths.cos(sample) * radius + translate.x,Maths.sin(sample) *radius * aspect_ratio+ translate.y);
		}
		addTriangle(7,index_corner + 1,3);
		index_corner++;
		for(byte i = 0;i < (samples - 2);i++) {
			addTriangle(index_corner,index_corner + 1,3);
			index_corner++;
		}
		addTriangle(8,index_corner,3);
		// bottom left corner
		translate.set(-1 + radius,-1 + radius*aspect_ratio);
		for(float i = 1;i < samples;i++) {
			float sample = -cir_mid - cir_mid * (i / samples);
			addVertex(Maths.cos(sample) * radius + translate.x,Maths.sin(sample) * radius * aspect_ratio+ translate.y);
		}
		addTriangle(9,index_corner + 1,2);
		index_corner++;
		for(byte i = 0;i < (samples - 2);i++) {
			addTriangle(index_corner,index_corner + 1,2);
			index_corner++;
		}
		addTriangle(10,index_corner,2);
		vbo = drawer.createBuffer(vertex_data,false,false);
		ibo = drawer.createBuffer(index_data,true,false);
		count = index_data.length;
		vertex_data = null;
		index_data = null;
		translate = null;
		return this;
	}
	
	private void addVertex(float x,float y){
		vertex_data[offset] = x;
		vertex_data[offset+1] = y;
		vertex_data[offset+2] = 0;
		vertex_data[offset+3] = 0;
		vertex_data[offset+4] = 1;
		vertex_data[offset+5] = 1;
		vertex_data[offset+6] = 1;
		offset += 7;
	}

	private void addQuad(int a,int b,int c,int d){
		index_data[offset2] = (short)a;
		index_data[offset2+1] = (short)b;
		index_data[offset2+2] = (short)c;
		index_data[offset2+3] = (short)a;
		index_data[offset2+4] = (short)b;
		index_data[offset2+5] = (short)d;
		offset2 += 6;
	}

	private void addTriangle(int a,int b,int c){
		index_data[offset2] = (short)a;
		index_data[offset2+1] = (short)b;
		index_data[offset2+2] = (short)c;
		offset2 += 3;
	}
	
	public void render(Drawer drawer,Vector2f local,Color color) {
		drawer.freeRender(vbo,local,color,-1);
		FX.gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,ibo);
		FX.gl.glDrawElements(GL.GL_TRIANGLES,count);
	}
	
	public void delete(){
		FX.gl.glDeleteBuffer(vbo);
		FX.gl.glDeleteBuffer(ibo);
	}
}
