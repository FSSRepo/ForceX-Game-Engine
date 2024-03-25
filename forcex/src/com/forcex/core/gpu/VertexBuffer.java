package com.forcex.core.gpu;

import com.forcex.FX;
import com.forcex.core.GL;

import java.nio.FloatBuffer;

public class VertexBuffer {
    private boolean need_update = true;
    private final boolean isStatic;
    private int buffer = -1;
    public VertexData vertex_data;
    public VertexInfo vertex_info;
    private final GL gl = FX.gl;

    public VertexBuffer(
            VertexData data,
            VertexInfo vertex_info,
            boolean isStatic) {
        this.isStatic = isStatic;
        this.vertex_data = data;
        this.vertex_info = vertex_info;
    }

    public void reset() {
        need_update = true;
    }

    public VertexBuffer clone() {
        VertexBuffer buf = new VertexBuffer(null, vertex_info.clone(), isStatic);
        buf.buffer = buffer;
        buf.need_update = false;
        return buf;
    }

    public void update() {
        if (need_update) {
            FloatBuffer data = vertex_data.convert(vertex_info);
            if (buffer == -1) { // init
                buffer = gl.glGenBuffer();
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, buffer);
                gl.glBufferData(GL.GL_ARRAY_BUFFER, vertex_info.dataSize, data, !isStatic ? GL.GL_DYNAMIC_DRAW : GL.GL_STATIC_DRAW);
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
                need_update = false;
                return;
            }
            if (!isStatic) {
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, buffer);
                gl.glBufferSubData(GL.GL_ARRAY_BUFFER, vertex_info.dataSize, data);
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
            } else {
                gl.glDeleteBuffer(buffer);
                buffer = FX.gl.glGenBuffer();
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, buffer);
                gl.glBufferData(GL.GL_ARRAY_BUFFER, vertex_info.dataSize, data, GL.GL_STATIC_DRAW);
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
            }
            need_update = false;
        }
    }

    public void bind() {
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, buffer);
    }

    public void EnableVertexAttrib(int attrib_idx, int dimen, int offset) {
        gl.glVertexAttribPointer(attrib_idx, dimen, GL.GL_FLOAT, false, vertex_info.stride, offset);
        gl.glEnableVertexAttribArray(attrib_idx);
    }

    public int getBuffer() {
        return buffer;
    }

    public void delete() {
        if (buffer != -1) {
            FX.gl.glDeleteBuffer(buffer);
        }
        vertex_data.delete();
        vertex_data = null;
        vertex_info = null;
    }
}
