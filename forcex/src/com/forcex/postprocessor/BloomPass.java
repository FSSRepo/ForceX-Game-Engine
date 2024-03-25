package com.forcex.postprocessor;

import com.forcex.FX;
import com.forcex.core.GL;
import com.forcex.gfx3d.shader.ShaderProgram;

public class BloomPass extends Pass {
    ShaderProgram shader;
    FrameBuffer fbo;
    BrightnessPass bright;
    BlurPass vBlur;
    BlurPass hBlur;

    public BloomPass(BrightnessPass bright, BlurPass horizontal_blur, BlurPass vertical_blur) {
        this.bright = bright;
        this.hBlur = horizontal_blur;
        this.vBlur = vertical_blur;

        shader = new ShaderProgram();
        shader.createProgram(
                Utils.vdefault,
                (FX.gpu.isOpenGLES() ? "precision mediump float;\n" : "") +
                        "varying vec2 texcoords;\n" +
                        "uniform sampler2D texture;\n" +
                        "uniform sampler2D hl_texture;\n" +
                        "void main(){\n" +
                        "	vec4 color1 = texture2D(texture,texcoords);\n" +
                        "	vec4 color2 = texture2D(hl_texture,texcoords);\n" +
                        "	gl_FragColor = color1 + color2;\n" +
                        "}");
        shader.attrib_position = shader.getAttribLocation("positions");
        shader.start();
        int tex2 = shader.getUniformLocation("hl_texture");
        shader.setInt(tex2, 1);
        shader.stop();
        fbo = new FrameBuffer(FX.gpu.getWidth(), FX.gpu.getHeight());
    }

    public void process(int colorTexture) {
        bright.process(colorTexture);
        hBlur.process(bright.getTexture());
        vBlur.process(hBlur.getTexture());
        if (renderfbo) {
            fbo.begin();
            render(colorTexture, vBlur.getTexture());
            fbo.end();
        } else {
            render(colorTexture, vBlur.getTexture());
        }
    }

    private void render(int texture, int highlighting_texture) {
        GL gl = FX.gl;
        shader.start();
        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, texture);
        gl.glActiveTexture(GL.GL_TEXTURE1);
        gl.glBindTexture(GL.GL_TEXTURE_2D, highlighting_texture);
        Utils.renderQuad(shader);
        shader.stop();
    }

    public int getTexture() {
        return fbo.getTexture();
    }

    public void delete() {
        bright.delete();
        hBlur.delete();
        vBlur.delete();
        shader.cleanUp();
        fbo.delete();
    }
}
