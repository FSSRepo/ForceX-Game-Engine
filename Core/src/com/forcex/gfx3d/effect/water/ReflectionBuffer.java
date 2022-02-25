package com.forcex.gfx3d.effect.water;
import com.forcex.core.*;
import com.forcex.*;

public class ReflectionBuffer
{
	private int size = 128;
	private int texture;
	private int fbo,rbo;
	private GL gl = FX.gl;
	
	public ReflectionBuffer(){
		texture = createTexture();
		createFrameBuffer();
	}
	
	public void begin(){
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo);
        gl.glViewport(size, size);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
	}
	
	public void end(){
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
        gl.glViewport(FX.gpu.getWidth(), FX.gpu.getHeight());
	}
	
	public void delete(){
		gl.glDeleteRenderBuffer(rbo);
        gl.glDeleteFrameBuffer(fbo);
		gl.glDeleteTexture(texture);
	}
	
	private void createFrameBuffer(){
		rbo = gl.glGenRenderbuffer();
        gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, rbo);
        gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, GL.GL_DEPTH_COMPONENT16, size, size);
        fbo = gl.glGenFramebuffer();
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo);
        gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_RENDERBUFFER, rbo);
        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, GL.GL_TEXTURE_2D, texture);
		if(gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER) != GL.GL_FRAMEBUFFER_COMPLETE){
			FX.device.showInfo(
				"ForceX: \n"+
				"FrameBuffer:\n"+
				"Estate: CRASHED\n"+
				"Error can't create the water framebuffer.",true);

			FX.device.stopRender();
		}
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
	}
	
	private int createTexture(){
		int tex = gl.glGenTexture();
        gl.glBindTexture(GL.GL_TEXTURE_2D, tex);
        gl.glEmptyTexture(GL.GL_TEXTURE_2D, size, size, GL.GL_RGB, GL.GL_RGB, GL.GL_UNSIGNED_BYTE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
		return tex;
	}
	
	public int getReflectionTexture(){
		return texture;
	}
}
