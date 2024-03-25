package com.forcex.gfx3d.effect;
import com.forcex.gfx3d.*;
import com.forcex.core.*;
import com.forcex.*;
import com.forcex.math.*;

public class ReflectionCubeMap {
    private final Camera camera;
    private int cube_map, fbo, rbo;
    private final GL gl = FX.gl;
    public int size = 128;

    public ReflectionCubeMap() {
		camera = new Camera(1);
    }
	
	public void create(){
		cube_map = gl.glGenTexture();
        gl.glBindTexture(GL.GL_TEXTURE_CUBE_MAP, cube_map);
        for (int i = 0; i < 6; i++) {
            gl.glEmptyTexture(GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, size, size, GL.GL_RGB, GL.GL_RGB, GL.GL_UNSIGNED_BYTE);
        }
        gl.glTexParameteri(GL.GL_TEXTURE_CUBE_MAP, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_CUBE_MAP, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        createFrameBuffer();
	}
	
	public void setCenter(float x,float y,float z) {
        camera.position.set(x,y,z);
    }
	
    public void setCenter(Vector3f center) {
        camera.position.set(center);
    }
	
    public void begin() {
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo);
        gl.glViewport(size, size);
    }

    public void end() {
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
        gl.glViewport(FX.gpu.getWidth(), FX.gpu.getHeight());
    }

    public Camera getCamera(int facing) {
		camera.setDirection(facing);
		camera.update();
        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X + facing, cube_map);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        return camera;
    }

    private void createFrameBuffer() {
        rbo = gl.glGenRenderbuffer();
        gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, rbo);
        gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, GL.GL_DEPTH_COMPONENT16, size, size);
        fbo = gl.glGenFramebuffer();
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo);
        gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_RENDERBUFFER, rbo);
		if(gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER) != GL.GL_FRAMEBUFFER_COMPLETE){
			FX.device.showInfo(
				"ForceX: \n"+
				"FrameBuffer:\n"+
				"Estate: CRASHED\n"+
				"Error can't create the framebuffer",true);

			FX.device.stopRender();
		}
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
    }

    public void delete() {
        gl.glDeleteRenderBuffer(rbo);
        gl.glDeleteFrameBuffer(fbo);
    }

    public int getTextureCubeMap() {
        return cube_map;
    }
}
