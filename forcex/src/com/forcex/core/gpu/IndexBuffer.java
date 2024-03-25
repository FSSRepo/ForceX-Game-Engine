package com.forcex.core.gpu;

import com.forcex.FX;
import com.forcex.core.GL;
import com.forcex.utils.BufferUtils;

import java.nio.ShortBuffer;

public class IndexBuffer {
    private int buffer = -1;
    private boolean need_update = true;
    private final GL gl = FX.gl;

    public IndexBuffer clone() {
        IndexBuffer buf = new IndexBuffer();
        buf.buffer = buffer;
        buf.need_update = false;
        return buf;
    }

    public void reset() {
        need_update = true;
        delete();
    }

    public void update(short[] indices) {
        if (need_update) {
            ShortBuffer sb = BufferUtils.createShortBuffer(indices);
            buffer = FX.gl.glGenBuffer();
            gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, buffer);
            gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, indices.length * 2, sb, GL.GL_STATIC_DRAW);
            gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
            need_update = false;
        }
    }

    public void bind(int primitiveType, int count) {
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, buffer);
        gl.glDrawElements(primitiveType, count);
    }

    public void delete() {
        gl.glDeleteBuffer(buffer);
        buffer = -1;
    }
}
