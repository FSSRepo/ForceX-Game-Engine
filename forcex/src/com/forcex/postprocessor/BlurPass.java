package com.forcex.postprocessor;

import com.forcex.FX;
import com.forcex.core.GL;
import com.forcex.gfx3d.shader.ShaderProgram;

public class BlurPass extends Pass {
    public static final byte VERTICAL = 0;
    public static final byte HORIZONTAL = 1;
    ShaderProgram shader;
    FrameBuffer fbo;

    public BlurPass(byte type, boolean x4, int width, int height) {
        fbo = new FrameBuffer(width, height);
        shader = new ShaderProgram();
        String prefix = "";
        float pixelSize = 0;
        switch (type) {
            case 0:
                pixelSize = 1f / (float) height;
                prefix += "#define blurVertical\n";
                break;
            case 1:
                pixelSize = 1f / (float) width;
                break;
        }
        if (x4) {
            prefix += "#define blurx4\n";
        }
        prefix += "#define pixelSize " + pixelSize + "\n";
        shader.createProgram(
                "shaders/blur.vs", "shaders/blur.fs", prefix);
        shader.attrib_position = shader.getAttribLocation("positions");
        render_in_framebuffer = true;
    }

    public void process(int texture) {
        if (render_in_framebuffer) {
            fbo.begin();
            render(texture);
            fbo.end();
        } else {
            render(texture);
        }
    }

    private void render(int texture) {
        shader.start();
        FX.gl.glActiveTexture(GL.GL_TEXTURE0);
        FX.gl.glBindTexture(GL.GL_TEXTURE_2D, texture);
        Utils.renderQuad(shader);
        shader.stop();
    }

    public int getTexture() {
        return fbo.getTexture();
    }

    public void delete() {
        shader.cleanUp();
        fbo.delete();
    }
}
