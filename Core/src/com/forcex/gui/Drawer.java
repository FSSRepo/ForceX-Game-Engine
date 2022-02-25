package com.forcex.gui;
import com.forcex.gfx3d.shader.*;
import com.forcex.math.*;
import com.forcex.utils.*;
import com.forcex.core.*;
import com.forcex.*;
import com.forcex.core.gpu.*;

public class Drawer {
	private int line_vbo;
	private int quad_vbo;
	private int quad_line_vbo;
	public int texture_temporaly;
	public SpriteShader shader;
	private Matrix2f transform;
	public GL gl;
	
	protected Drawer(SpriteShader shader){
		this.shader = shader;
		transform = new Matrix2f();
		gl = FX.gl;
		texture_temporaly = Texture.genTextureWhite();
		line_vbo = genBuffer(new float[]{
								 1,0,0,0,
								 - 1,0,0,0
							 },false,false);
		quad_vbo = genBuffer(new float[]{
				-1,1,0,0,
				-1,-1,0,1,
				1,1,1,0,
				1,-1,1,1},false,false);
		quad_line_vbo = genBuffer(new float[]{
									  -1,1,0,0,
									  1,1,0,0,
									  1,-1,0,0,
									  -1,-1,0,0,
									  -1,1,0,0
		},false,false);
	}
	
	public void setScale(float x,float y){
		transform.setScale(x,y);
	}
	
	public void setRotation(float angle){
		transform.setRotation(angle);
	}
	
	public void setTransform(float angle,float x,float y){
		transform.setTransform(angle,x,y);
	}
	
	public static int genBuffer(Object data,boolean indices,boolean dynamic){
		GL gl = FX.gl;
		int vbo = gl.glGenBuffer();
		int target = GL.GL_ARRAY_BUFFER;
		if(indices){
			target = GL.GL_ELEMENT_ARRAY_BUFFER;
		}
		gl.glBindBuffer(target,vbo);
		gl.glBufferData(target,
			indices ? 
			((short[])data).length*2 : 
			((float[])data).length*4,
			indices ? 
			BufferUtils.createShortBuffer((short[])data) : 
			BufferUtils.createFloatBuffer((float[])data),dynamic ? GL.GL_DYNAMIC_DRAW : GL.GL_STATIC_DRAW);
		gl.glBindBuffer(target,0);
		return vbo;
	}
	
	public static void updateBuffer(int vbo,float[] data){
		if(data == null)return;
		GL gl = FX.gl;
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER,vbo);
		gl.glBufferSubData(GL.GL_ARRAY_BUFFER,data.length*4,BufferUtils.createFloatBuffer(data));
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER,0);
	}
	
	public void setupBuffer(int vbo,Vector2f position){
		shader.setSpriteTransform(position,transform);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER,vbo);
		gl.glVertexAttribPointer(shader.attribute_vertex, 4, GL.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(shader.attribute_vertex);
	}
	
	public void freeRender(int vbo,Vector2f position,Color color,int texture){
		shader.setSpriteTransform(position,transform);
		shader.setSpriteColor(color);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER,vbo);
		gl.glVertexAttribPointer(shader.attribute_vertex, 4, GL.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(shader.attribute_vertex);
		Texture.bind(GL.GL_TEXTURE0,texture == -1 ? texture_temporaly : texture);
	}
	
	public void renderQuad(Vector2f position,Color color,int texture){
		shader.setSpriteTransform(position,transform);
		shader.setSpriteColor(color);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER,quad_vbo);
		gl.glVertexAttribPointer(shader.attribute_vertex, 4, GL.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(shader.attribute_vertex);
		Texture.bind(GL.GL_TEXTURE0,texture == -1 ? texture_temporaly : texture);
		gl.glDrawArrays(GL.GL_TRIANGLE_STRIP,0,4);
	}
	
	public void renderLineQuad(Vector2f position,Color color){
		shader.setSpriteTransform(position,transform);
		shader.setSpriteColor(color);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER,quad_line_vbo);
		gl.glVertexAttribPointer(shader.attribute_vertex, 4, GL.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(shader.attribute_vertex);
		Texture.bind(GL.GL_TEXTURE0,texture_temporaly);
		gl.glDrawArrays(GL.GL_LINE_LOOP,0,5);
	}
	
	public void renderLine(Vector2f position,Color color){
		shader.setSpriteTransform(position,transform);
		shader.setSpriteColor(color);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER,line_vbo);
		gl.glVertexAttribPointer(shader.attribute_vertex, 4, GL.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(shader.attribute_vertex);
		Texture.bind(GL.GL_TEXTURE0,texture_temporaly);
		gl.glDrawArrays(GL.GL_LINES,0,2);
	}
	
	public void scissorArea(float x,float y,float width,float height){
		gl.glEnable(GL.GL_SCISSOR_TEST);
		gl.glScissor(
			(int)((FX.gpu.getWidth() * ((x - width) + 1.0f)) / 2.0f),
			(int)((FX.gpu.getHeight() * ((y - height) + 1.0f)) / 2.0f),(int)(FX.gpu.getWidth() * width),(int)(FX.gpu.getHeight() * height));
	}
	
	public void finishScissor(){
		gl.glDisable(GL.GL_SCISSOR_TEST);
	}
	
	void start(){
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glDisable(GL.GL_DEPTH_TEST);
		shader.start();
	}
	
	void end(){
		shader.stop();
		gl.glDisable(GL.GL_BLEND);
		gl.glEnable(GL.GL_DEPTH_TEST);
	}
}
