package com.forcex.gfx3d.effect;

import com.forcex.FX;
import com.forcex.core.GL;
import com.forcex.gfx3d.Camera;
import com.forcex.gfx3d.shader.DefaultShader;

public class ReflectionTexture {
    Camera cam;
    int fbo = -1,rbo,texture;
    GL gl = FX.gl;
    public int size = 128;
    
    public ReflectionTexture() {
		cam = new Camera(1.0f);
    }
	
	public void create(){
		if(fbo == -1){
			texture = gl.glGenTexture();
        	gl.glBindTexture(GL.GL_TEXTURE_2D, texture);
       		gl.glEmptyTexture(GL.GL_TEXTURE_2D, size, size, GL.GL_RGB, GL.GL_RGB, GL.GL_UNSIGNED_BYTE);
        	gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
			createFrameBuffer();
		}
	}
	
    public Camera getCamera() {
        return cam;
    }

    public Camera getCameraToRender() {
        cam.update();
        return cam;
    }

    public void begin() {
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo);
        gl.glViewport(size, size);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
    }

    public void end() {
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
        gl.glViewport(FX.gpu.getWidth(), FX.gpu.getHeight());
    }

    public void delete() {
        gl.glDeleteRenderBuffer(rbo);
        gl.glDeleteFrameBuffer(fbo);
		gl.glDeleteTexture(texture);
        cam.delete();
    }

    private void createFrameBuffer() {
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
				"Error can't create the texture framebuffer.",true);

			FX.device.stopRender();
		}
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
    }

    public int getTexture() {
        return texture;
    }
}
