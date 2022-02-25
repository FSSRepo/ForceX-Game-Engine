package com.forcex.gfx3d.particle;
import com.forcex.*;
import com.forcex.core.*;
import com.forcex.io.*;
import com.forcex.math.*;
import java.nio.*;
import java.util.*;
import com.forcex.gfx3d.*;
import com.forcex.utils.*;
import com.forcex.gfx3d.shader.*;
import com.forcex.core.gpu.*;

public class ParticleRenderer{
	ArrayList<Particle> particles = new ArrayList<>();
	int vbo,texture;
	SpriteShader shader;
	GL gl = FX.gl;
	Matrix4f mm = new Matrix4f();
	Matrix4f mv = new Matrix4f();
	float[] vertices = {
		-1,1,0,0,
		-1,-1,0,1,
		1,1,1,0,
		1,-1,1,1
	};
	
	public void addParticle(Particle particle){
		particles.add(particle);
	}
	
	public void create(){
		vbo = gl.glGenBuffer();
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER,vbo);
		gl.glBufferData(GL.GL_ARRAY_BUFFER,vertices.length * 4,BufferUtils.createFloatBuffer(vertices),GL.GL_STATIC_DRAW);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER,0);
		shader = new SpriteShader(false,true,true);
	}
	
	public void setTextureID(int id){
		texture = id;
	}
	
	public void update(){
		Iterator<Particle> itp = particles.iterator();
		while(itp.hasNext()){
			Particle pt = itp.next();
			boolean live = pt.update();
			if(!live){
				itp.remove();
			}
		}
	}
	public void render(Camera cam){
		begin();
		for(Particle p : particles){
			renderParticle(p,cam);
		}
		end();
	}
	private void renderParticle(Particle particle, Camera camera){
		shader.setMVPMatrix(updateMVP(particle.position,particle.scale,camera));
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER,vbo);
		gl.glVertexAttribPointer(shader.attribute_vertex,4,GL.GL_FLOAT,false,0,0);
		gl.glEnableVertexAttribArray(shader.attribute_vertex);
		shader.setSpriteColor(particle.color);
		shader.setSpriteTexOffsets(new Vector2f(0),new Vector2f(0));
		shader.setSpriteTexInfo(1,1f - particle.liveFactor);
		gl.glActiveTexture(GL.GL_TEXTURE0);
		gl.glBindTexture(GL.GL_TEXTURE_2D,texture);
		gl.glDrawArrays(GL.GL_TRIANGLE_STRIP,0,4);
	}
	
	private Matrix4f updateMVP(Vector3f position,float scale,Camera cam){
		Matrix4f vm = cam.getViewMatrix();
		mm.setLocation(position);
		mm.data[Matrix4f.M00] = vm.data[Matrix4f.M00];
		mm.data[Matrix4f.M01] = vm.data[Matrix4f.M10];
		mm.data[Matrix4f.M02] = vm.data[Matrix4f.M20];
		mm.data[Matrix4f.M10] = vm.data[Matrix4f.M01];
		mm.data[Matrix4f.M11] = vm.data[Matrix4f.M11];
		mm.data[Matrix4f.M12] = vm.data[Matrix4f.M21];
		mm.data[Matrix4f.M20] = vm.data[Matrix4f.M02];
		mm.data[Matrix4f.M21] = vm.data[Matrix4f.M12];
		mm.data[Matrix4f.M22] = vm.data[Matrix4f.M22];
		vm.mult(mv, mm);
		mv.multLocal(Matrix4f.scale(scale));
		return cam.getProjectionMatrix().mult(null,mv);
	}
	
	private void begin(){
		shader.start();
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA,GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glDepthMask(false);
	}
	
	private void end(){
		gl.glDepthMask(true);
		gl.glDisable(GL.GL_BLEND);
		shader.stop();
	}
	
	public void delete(){
		particles.clear();
		particles = null;
		FX.gl.glDeleteBuffer(vbo);
		FX.gl.glDeleteTexture(texture);
		shader.cleanUp();
		shader = null;
	}
}
