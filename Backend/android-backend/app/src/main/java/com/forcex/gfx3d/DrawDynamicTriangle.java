package com.forcex.gfx3d;
import com.forcex.math.*;
import com.forcex.utils.*;
import com.forcex.gfx3d.shader.*;
import java.util.*;
import com.forcex.core.*;
import com.forcex.*;
import java.nio.*;
import com.forcex.core.gpu.*;

public class DrawDynamicTriangle {
	private Matrix4f transform;
	private int vbo,ibo,texture;
	private ArrayList<DynamicTriangle> triangles = new ArrayList<>();
	GL gl = FX.gl;
	FloatBuffer buffer;
	
	public DrawDynamicTriangle(){
		transform = new Matrix4f();
		buffer = BufferUtils.createFloatBuffer(15);
		vbo = gl.glGenBuffer();
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER,vbo);
		gl.glBufferData(GL.GL_ARRAY_BUFFER,15 * 4,buffer,GL.GL_DYNAMIC_DRAW);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER,0);
		ibo = gl.glGenBuffer();
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,ibo);
		gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER,3 * 2,BufferUtils.createShortBuffer(new short[]{0,1,2}),GL.GL_STATIC_DRAW);
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,0);
		texture = Texture.genTextureWhite();
	}
	
	public void setPosition(float x,float y,float z){
		transform.setLocation(x,y,z);
	}
	
	public void resetMatrix(){
		transform.setIdentity();
	}
	
	public DynamicTriangle addTriangle(int id,Vector3f a,Vector3f b,Vector3f c,Color color){
		DynamicTriangle t = new DynamicTriangle();
		t.color = color;
		t.data = new float[15];
		a.get(t.data,0); 
		b.get(t.data,5); 
		c.get(t.data,10);
		t.id = id;
		triangles.add(t);
		return t;
	}
	
	
	public DynamicTriangle get(int id){
		for(DynamicTriangle t : triangles){
			if(t.id == id){
				return t;
			}
		}
		return null;
	}
	
	
	public void remove(int id){
		for(DynamicTriangle t : triangles){
			if(t.id == id){
				t.remove = true;
				break;
			}
		}
	}
	
	public void render(DefaultShader shader){
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA,GL.GL_ONE_MINUS_SRC_ALPHA);
		shader.setModelMatrix(transform);
		Iterator<DynamicTriangle> it = triangles.iterator();
		while(it.hasNext()){
			DynamicTriangle t = it.next();
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER,vbo);
			buffer.position(0);
			buffer.put(t.data);
			buffer.position(0);
			gl.glBufferSubData(GL.GL_ARRAY_BUFFER,60,buffer);
			gl.glVertexAttribPointer(shader.attrib_position,3,GL.GL_FLOAT,false,20,0);
			gl.glEnableVertexAttribArray(shader.attrib_position);
			gl.glVertexAttribPointer(shader.attrib_texcoord,2,GL.GL_FLOAT,false,20,12);
			gl.glEnableVertexAttribArray(shader.attrib_texcoord);
			shader.setMaterial(0.8f,0.8f,0.8f,0.8f,t.color);
			gl.glActiveTexture(GL.GL_TEXTURE0);
			gl.glBindTexture(GL.GL_TEXTURE_2D, texture);
			gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,ibo);
			gl.glDrawElements(GL.GL_TRIANGLES,3);
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER,0);
			if(t.remove){
				it.remove();
			}
		}
		gl.glDisable(GL.GL_BLEND);
	}
}
