package com.forcex.gfx3d.effect;
import com.forcex.math.*;
import com.forcex.gfx3d.*;
import java.util.*;
import com.forcex.gfx3d.shader.*;
import com.forcex.*;
import com.forcex.core.*;
import com.forcex.utils.*;

public class LensFlare{
	Flare[] flares;
	float spacing = 0.15f;
	ShaderProgram shader;
	int brightness_val,transform_val,vbo;
	Vector4f tmp = new Vector4f();
	boolean start = true;
	float[] vertices = {
		-1,1,-1,-1,
		1,1,1,-1
	};
	GL gl = FX.gl;
	
	public LensFlare(float spc,Flare... flares){
		this.flares = flares;
		spacing = spc;
	}
	
	public void render(Vector3f sunpos, Camera cam){
		if(start){
			shader = new ShaderProgram("shaders/lens.vs","shaders/lens.fs");
			shader.attrib_position = shader.getAttribLocation("vertexs");
			transform_val = shader.getUniformLocation("transform");
			brightness_val = shader.getUniformLocation("brightness");
			vbo = gl.glGenBuffer();
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
			gl.glBufferData(GL.GL_ARRAY_BUFFER, vertices.length * 4, BufferUtils.createFloatBuffer(vertices), GL.GL_STATIC_DRAW);
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
			start = false;
		}
		Vector2f suncoords = toScreenSpace(sunpos,cam);
		if(suncoords == null){
			return;
		}
		float dirx = 0.5f - suncoords.x;
		float diry = 0.5f - suncoords.y;
		float brightness = 1.0f - (Maths.sqrt(dirx * dirx + diry * diry) / 0.7f);
		if(brightness > 0){
			update(suncoords,dirx,diry);
			render(brightness);
		}
	}
	
	void update(Vector2f sc,float sx,float sy){
		for(byte i = 0;i < flares.length;i++){
			Vector2f dir = new Vector2f(sx,sy);
			dir.multLocal(i * spacing);
			dir.addLocal(sc);
			flares[i].position.set(dir);
		}
	}
	
	void render(float bright){
		float aspectRatio = ((float)FX.gpu.getWidth() / FX.gpu.getHeight());
		shader.start();
		gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glDisable(GL.GL_DEPTH_TEST);
		shader.setFloat(brightness_val,bright);
		for(Flare flare : flares){
			Vector2f pos = flare.position;
			float xscl = flare.scale;
			float yscl = xscl * aspectRatio;
			shader.setVector4(transform_val, tmp.set(pos.x,pos.y,xscl,yscl));
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
			gl.glVertexAttribPointer(shader.attrib_position, 2, GL.GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(shader.attrib_position);
			gl.glActiveTexture(GL.GL_TEXTURE0);
			gl.glBindTexture(GL.GL_TEXTURE_2D, flare.texture);
			gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
		}
		shader.stop();
		gl.glDisable(GL.GL_BLEND);
		gl.glEnable(GL.GL_DEPTH_TEST);
	}
	
	Vector2f toScreenSpace(Vector3f pos,Camera cam){
		final float[] l_mat = cam.getProjViewMatrix().data;
		float w = (pos.x * l_mat[Matrix4f.M30] + pos.y * l_mat[Matrix4f.M31] + pos.z * l_mat[Matrix4f.M32] + l_mat[Matrix4f.M33]); 
		if(w <= 0){
			return null;
		}
		Vector3f scoords = pos.project(cam.getProjViewMatrix());
		float x = (scoords.x + 1) / 2;
		float y = 1.0f - ((scoords.y + 1) / 2);
		return new Vector2f(x,y);
	}
}
