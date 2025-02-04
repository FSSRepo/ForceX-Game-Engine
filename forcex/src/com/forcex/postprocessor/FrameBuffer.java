package com.forcex.postprocessor;

import com.forcex.FX;
import com.forcex.core.GL;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FrameBuffer {
    private int fbo = -1, rbo, texture, width, height;
    private final GL gl = FX.gl;

    public FrameBuffer(int width, int height) {
        create(width, height);
    }

    public static byte[] readPixelFrame(int x, int y, int width, int height) {
        ByteBuffer out = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
        out.position(0);
        FX.gl.glReadPixel(x, y, width, height, GL.GL_RGBA, out);
        byte[] buffer = new byte[width * height * 4];
        for (int cy = height - 1; cy >= 0; cy--) {
            for (int cx = 0; cx < width; cx++) {
                int offset = (cx + (cy * width)) * 4;
                buffer[offset] = out.get();
                buffer[offset + 1] = out.get();
                buffer[offset + 2] = out.get();
                buffer[offset + 3] = out.get();
            }
        }
        out.clear();
        return buffer;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int createTexture() {
        texture = gl.glGenTexture();
        gl.glBindTexture(GL.GL_TEXTURE_2D, texture);
        gl.glEmptyTexture(GL.GL_TEXTURE_2D, width, height, GL.GL_RGBA, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        if (fbo != -1) {
            gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo);
            gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, GL.GL_TEXTURE_2D, texture);
            gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
        }
        return texture;
    }

    public void create(int width, int height) {
        this.width = width;
        this.height = height;
        createTexture();
        rbo = gl.glGenRenderbuffer();
        gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, rbo);
        gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, GL.GL_DEPTH_COMPONENT16, width, height);
        fbo = gl.glGenFramebuffer();
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo);
        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, GL.GL_TEXTURE_2D, texture);
        gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_RENDERBUFFER, rbo);
        if (gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER) != GL.GL_FRAMEBUFFER_COMPLETE) {
            FX.device.showInfo(
                    "ForceX: \n" +
                            "FrameBuffer:\n" +
                            "Estate: CRASHED\n" +
                            "Error can't create the framebuffer.", true);

            FX.device.stopRender();
        }
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
    }

    public void begin() {
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo);
        gl.glViewport(width, height);
    }

    public void clear() {
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
    }

    public void end() {
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
        gl.glViewport(FX.gpu.getWidth(), FX.gpu.getHeight());
    }

    public int getTexture() {
        return texture;
    }

    public void release() {
        gl.glDeleteRenderBuffer(rbo);
        gl.glDeleteFrameBuffer(fbo);
    }

    public void delete() {
        gl.glDeleteTexture(texture);
        gl.glDeleteRenderBuffer(rbo);
        gl.glDeleteFrameBuffer(fbo);
    }
}
