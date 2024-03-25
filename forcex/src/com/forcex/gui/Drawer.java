package com.forcex.gui;

import com.forcex.FX;
import com.forcex.core.GL;
import com.forcex.core.gpu.Texture;
import com.forcex.gfx3d.shader.SpriteShader;
import com.forcex.math.Matrix2f;
import com.forcex.math.Vector2f;
import com.forcex.utils.BufferUtils;
import com.forcex.utils.Color;

public class Drawer {
    public int white_background_texture;
    public SpriteShader shader;
    private GL gl = FX.gl;
    int stride = 7 * 4; // 7 floats: 2 pos, 2 uv, 3 colors
    int colors_offset = 4 * 4;
    private final int line_vbo;
    private final int quad_vbo;
    private final int quad_line_vbo;
    private final Matrix2f transform;

    protected Drawer(SpriteShader shader) {
        this.shader = shader;
        transform = new Matrix2f();
        white_background_texture = Texture.genTextureWhite();
        line_vbo = createBuffer(new float[]{
                1, 0, 0, 0, 1, 1, 1,
                -1, 0, 0, 0, 1, 1, 1
        }, false, false);
        quad_vbo = createBuffer(new float[]{
                -1, 1, 0, 0, 1, 1, 1,
                -1, -1, 0, 1, 1, 1, 1,
                1, 1, 1, 0, 1, 1, 1,
                1, -1, 1, 1, 1, 1, 1
        }, false, false);
        quad_line_vbo = createBuffer(new float[]{
                -1, 1, 0, 0, 1, 1, 1,
                1, 1, 0, 0, 1, 1, 1,
                1, -1, 0, 0, 1, 1, 1,
                -1, -1, 0, 0, 1, 1, 1,
                -1, 1, 0, 0, 1, 1, 1
        }, false, false);
    }

    public static int createBuffer(Object data, boolean indices, boolean dynamic) {
        if (!indices) {
            if (((float[]) data).length % 7 != 0) {
                throw new RuntimeException("Array missing data");
            }
        }
        GL gl = FX.gl;
        int vbo = gl.glGenBuffer();
        int target = GL.GL_ARRAY_BUFFER;
        if (indices) {
            target = GL.GL_ELEMENT_ARRAY_BUFFER;
        }
        gl.glBindBuffer(target, vbo);
        gl.glBufferData(target,
                indices ?
                        ((short[]) data).length * 2 :
                        ((float[]) data).length * 4,
                indices ?
                        BufferUtils.createShortBuffer((short[]) data) :
                        BufferUtils.createFloatBuffer((float[]) data), dynamic ? GL.GL_DYNAMIC_DRAW : GL.GL_STATIC_DRAW);
        gl.glBindBuffer(target, 0);
        return vbo;
    }

    public static void updateBuffer(int vbo, float[] data) {
        if (data == null) return;
        GL gl = FX.gl;
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
        gl.glBufferSubData(GL.GL_ARRAY_BUFFER, data.length * 4, BufferUtils.createFloatBuffer(data));
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    }

    public void setScale(float x, float y) {
        transform.setScale(x, y);
    }

    public void setRotation(float angle) {
        transform.setRotation(angle);
    }

    public void setTransform(float angle, float x, float y) {
        transform.setTransform(angle, x, y);
    }

    public void setupBuffer(int vbo, Vector2f position) {
        shader.setSpriteTransform(position, transform);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
        gl.glVertexAttribPointer(shader.attribute_vertex, 4, GL.GL_FLOAT, false, stride, 0);
        gl.glEnableVertexAttribArray(shader.attribute_vertex);
        gl.glVertexAttribPointer(shader.attribute_colors, 3, GL.GL_FLOAT, false, stride, colors_offset);
        gl.glEnableVertexAttribArray(shader.attribute_colors);
    }

    public void freeRender(int vbo, Vector2f position, Color color, int texture) {
        shader.setSpriteTransform(position, transform);
        shader.setSpriteColor(color);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
        gl.glVertexAttribPointer(shader.attribute_vertex, 4, GL.GL_FLOAT, false, stride, 0);
        gl.glEnableVertexAttribArray(shader.attribute_vertex);
        gl.glVertexAttribPointer(shader.attribute_colors, 3, GL.GL_FLOAT, false, stride, colors_offset);
        gl.glEnableVertexAttribArray(shader.attribute_colors);
        Texture.bind(GL.GL_TEXTURE0, texture == -1 ? white_background_texture : texture);
    }

    public void renderQuad(Vector2f position, Color color, int texture) {
        shader.setSpriteTransform(position, transform);
        shader.setSpriteColor(color);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, quad_vbo);
        gl.glVertexAttribPointer(shader.attribute_vertex, 4, GL.GL_FLOAT, false, stride, 0);
        gl.glEnableVertexAttribArray(shader.attribute_vertex);
        gl.glVertexAttribPointer(shader.attribute_colors, 3, GL.GL_FLOAT, false, stride, colors_offset);
        gl.glEnableVertexAttribArray(shader.attribute_colors);
        Texture.bind(GL.GL_TEXTURE0, texture == -1 ? white_background_texture : texture);
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
    }

    public void renderLineQuad(Vector2f position, Color color) {
        shader.setSpriteTransform(position, transform);
        shader.setSpriteColor(color);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, quad_line_vbo);
        gl.glVertexAttribPointer(shader.attribute_vertex, 4, GL.GL_FLOAT, false, stride, 0);
        gl.glEnableVertexAttribArray(shader.attribute_vertex);
        gl.glVertexAttribPointer(shader.attribute_colors, 3, GL.GL_FLOAT, false, stride, colors_offset);
        gl.glEnableVertexAttribArray(shader.attribute_colors);
        Texture.bind(GL.GL_TEXTURE0, white_background_texture);
        gl.glDrawArrays(GL.GL_LINE_LOOP, 0, 5);
    }

    public void renderLine(Vector2f position, Color color) {
        shader.setSpriteTransform(position, transform);
        shader.setSpriteColor(color);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, line_vbo);
        gl.glVertexAttribPointer(shader.attribute_vertex, 4, GL.GL_FLOAT, false, stride, 0);
        gl.glEnableVertexAttribArray(shader.attribute_vertex);
        gl.glVertexAttribPointer(shader.attribute_colors, 3, GL.GL_FLOAT, false, stride, colors_offset);
        gl.glEnableVertexAttribArray(shader.attribute_colors);
        Texture.bind(GL.GL_TEXTURE0, white_background_texture);
        gl.glDrawArrays(GL.GL_LINES, 0, 2);
    }

    public void scissorArea(float x, float y, float width, float height) {
        gl.glEnable(GL.GL_SCISSOR_TEST);
        gl.glScissor(
                (int) ((FX.gpu.getWidth() * ((x - width) + 1)) * 0.5f),
                (int) ((FX.gpu.getHeight() * ((y - height) + 1)) * 0.5f), (int) (FX.gpu.getWidth() * width), (int) (FX.gpu.getHeight() * height));
    }

    public void finishScissor() {
        gl.glDisable(GL.GL_SCISSOR_TEST);
    }

    void start() {
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glDisable(GL.GL_DEPTH_TEST);
        shader.start();
    }

    void end() {
        shader.stop();
        gl.glDisable(GL.GL_BLEND);
        gl.glEnable(GL.GL_DEPTH_TEST);
    }
}
