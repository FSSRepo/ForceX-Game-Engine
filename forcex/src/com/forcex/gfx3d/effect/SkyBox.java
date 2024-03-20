package com.forcex.gfx3d.effect;
import com.forcex.gfx3d.shader.*;
import java.nio.*;
import com.forcex.utils.*;
import com.forcex.*;
import com.forcex.core.*;
import com.forcex.gfx3d.*;
import com.forcex.math.*;

public class SkyBox{
	ShaderProgram shader;
	int ibo,vbo;
	int stride = 24;
	int color_ofs = 12,lenght;
	int vertex_idx,color_idx,mvp_idx;
	GL gl = FX.gl;
	Matrix4f rotx,temp;
	
	public SkyBox(float size){
		float w = size;
		float h = size;
		float d = size;
		rotx = Matrix4f.fromRotation(Quaternion.fromEulerAngles(-90,0,0));
		temp = new Matrix4f();
		shader = new ShaderProgram();
		shader.createProgram(
			"attribute vec3 vertexs;\n"+
			"attribute vec3 colors;\n"+
			"uniform mat4 mvp;\n"+
			"varying vec3 vcolors;\n"+
			"void main(){"+
			"	vcolors = colors;"+
			"	gl_Position =  mvp * vec4(vertexs,1.0);"+
			"}",
			"precision mediump float;"+
			"varying vec3 vcolors;"+
			"void main(){"+
			"	gl_FragColor = vec4(vcolors,1.0);"+
			"}");
		vertex_idx = shader.getAttribLocation("vertexs");
		color_idx = shader.getAttribLocation("colors");
		mvp_idx = shader.getUniformLocation("mvp");
		
		float[] vertices ={
			//front
			-w,+h,+d,
			+w,+h,+d,
			+w,-h,+d,
			-w,-h,+d,
			//right
			+w,+h,+d,
			+w,+h,-d,
			+w,-h,-d,
			+w,-h,+d,
			//back
			+w,+h,-d,
			-w,+h,-d,
			-w,-h,-d,
			+w,-h,-d,
			//left
			-w,+h,-d,
			-w,+h,+d,
			-w,-h,+d,
			-w,-h,-d,
			//top
			-w,+h,-d,
			+w,+h,-d,
			+w,+h,+d,
			-w,+h,+d,
			//bottom
			-w,-h,+d,
			+w,-h,+d,
			+w,-h,-d,
			-w,-h,-d
		};
		
		short[] colors = {
			//top
			0x37,0x65,0xee,
			0x37,0x65,0xee,
			0x37,0x65,0xee,
			0x37,0x65,0xee,
			//right
			0x37,0x65,0xee,
			0xa5,0xa5,0xa5,
			0xa5,0xa5,0xa5,
			0x37,0x65,0xee,
			//bottom
			0xa5,0xa5,0xa5,
			0xa5,0xa5,0xa5,
			0xa5,0xa5,0xa5,
			0xa5,0xa5,0xa5,
			//left
			0xa5,0xa5,0xa5,
			0x37,0x65,0xee,
			0x37,0x65,0xee,
			0xa5,0xa5,0xa5,
			//front
			0xa5,0xa5,0xa5,
			0xa5,0xa5,0xa5,
			0x37,0x65,0xee,
			0x37,0x65,0xee,
			//back
			0x37,0x65,0xee,
			0x37,0x65,0xee,
			0xa5,0xa5,0xa5,
			0xa5,0xa5,0xa5,
		};
		
		short[] indices = {
			0,2,1,0,3,2, //front
			4,6,5,4,7,6, //right
			8,10,9,8,11,10, //back
			12,14,13,12,15,14, //left
			16,18,17,16,19,18, //top
			20,22,21,20,23,22 //bottom
		};
		
		lenght = indices.length;
		int vertexcount = (vertices.length / 3);
		int datasize = (vertexcount * 24);
		FloatBuffer fb = BufferUtils.createByteBuffer(datasize).asFloatBuffer();
		for(int i = 0;i < vertexcount;i++){
			fb.put(vertices[i*3]);
			fb.put(vertices[i*3 + 1]);
			fb.put(vertices[i*3 + 2]);
			fb.put(colors[i*3] * 0.003921f);
			fb.put(colors[i*3+1] * 0.003921f);
			fb.put(colors[i*3+2] * 0.003921f);
		}
		fb.position(0);
		vbo = gl.glGenBuffer();
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER,vbo);
		gl.glBufferData(GL.GL_ARRAY_BUFFER,datasize,fb, GL.GL_STATIC_DRAW);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER,0);
		ibo = gl.glGenBuffer();
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,ibo);
		gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER,indices.length*2,BufferUtils.createShortBuffer(indices), GL.GL_STATIC_DRAW);
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,0);
		vertices = null;
		colors = null;
		indices = null;
	}
	public void render(Camera cam){
		shader.start();
		shader.setMatrix4f(mvp_idx,cam.getProjViewMatrix().mult(temp,rotx));
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER,vbo);
		gl.glVertexAttribPointer(vertex_idx,3,GL.GL_FLOAT,false,stride,0);
		gl.glEnableVertexAttribArray(vertex_idx);
		gl.glVertexAttribPointer(color_idx,3,GL.GL_FLOAT,false,stride,color_ofs);
		gl.glEnableVertexAttribArray(color_idx);
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ibo);
        gl.glDrawElements(GL.GL_TRIANGLES, lenght);
		shader.stop();
	}
}
