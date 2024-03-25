package com.forcex.gfx3d.shapes;

import com.forcex.FX;
import com.forcex.core.GL;
import com.forcex.core.gpu.Texture;
import com.forcex.gfx3d.shader.DefaultShader;
import com.forcex.math.Matrix4f;
import com.forcex.utils.BufferUtils;
import com.forcex.utils.Color;

import java.nio.ShortBuffer;

public class DrawDynamicTriangle {
    private final Matrix4f transform;
    private int vbo = -1;
	private int ibo = -1;
	private final int texture;
	private int indexSize;
    private Color color;
    private final GL gl = FX.gl;
    ShortBuffer buffer;
    boolean use = false;

    public DrawDynamicTriangle() {
        transform = new Matrix4f();
        texture = Texture.genTextureWhite();
    }

    public void setPosition(float x, float y, float z) {
        transform.setLocation(x, y, z);
    }

    public void reset() {
        transform.setIdentity();
        gl.glDeleteBuffer(vbo);
        gl.glDeleteBuffer(ibo);
        vbo = -1;
        ibo = -1;
        use = false;
    }

    public void setup(float[] vertices, Color color, float scale) {
        this.color = color;
        int vertexCount = vertices.length / 3;
        float[] dst_vertices = new float[vertexCount * 5];
        for (int i = 0; i < vertexCount; i++) {
            dst_vertices[i * 5] = vertices[i * 3] * scale;
            dst_vertices[i * 5 + 1] = vertices[i * 3 + 1] * scale;
            dst_vertices[i * 5 + 2] = vertices[i * 3 + 2] * scale;
            dst_vertices[i * 5 + 3] = 0.0f;
            dst_vertices[i * 5 + 4] = 0.0f;
        }
        vbo = gl.glGenBuffer();
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, dst_vertices.length * 4, BufferUtils.createFloatBuffer(dst_vertices), GL.GL_STATIC_DRAW);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    }

    public void prepareBufferToFill(int estimatedIndexSize) {
        indexSize = estimatedIndexSize;
        buffer = BufferUtils.createShortBuffer(estimatedIndexSize);
        use = false;
    }

    public void setupIndexBuffer() {
        if (ibo != -1) {
            gl.glDeleteBuffer(ibo);
        }
        buffer.position(0);
        ibo = gl.glGenBuffer();
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ibo);
        gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, indexSize * 2, buffer, GL.GL_STATIC_DRAW);
        buffer = null;
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
        use = true;
    }

    public void fill(short[] index, int offset) {
        buffer.put(index[offset]);
        buffer.put(index[offset + 1]);
        buffer.put(index[offset + 2]);
    }

    public void render(DefaultShader shader) {
        if (!use) {
            return;
        }
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        shader.setModelMatrix(transform);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
        gl.glVertexAttribPointer(shader.attrib_position, 3, GL.GL_FLOAT, false, 20, 0);
        gl.glEnableVertexAttribArray(shader.attrib_position);
        gl.glVertexAttribPointer(shader.attrib_texcoord, 2, GL.GL_FLOAT, false, 20, 12);
        gl.glEnableVertexAttribArray(shader.attrib_texcoord);
        shader.setMaterial(0.8f, 0.8f, 0.8f, 0.8f, color);
        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, texture);
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ibo);
        gl.glDrawElements(GL.GL_TRIANGLES, indexSize);
        gl.glDisable(GL.GL_BLEND);
    }
}
