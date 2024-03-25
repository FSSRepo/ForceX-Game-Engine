package com.forcex.gfx3d.shapes;

import com.forcex.FX;
import com.forcex.core.GL;
import com.forcex.gfx3d.Mesh;
import com.forcex.gfx3d.MeshPart;
import com.forcex.gui.Font;

public class Text extends Mesh {
    Font font;

    public Text(String text, Font font, float scale) {
        super(false);
        this.font = font;
        setText(text, scale);
    }

    private void setText(String text, float scale) {
        int text_length = getDrawTextLength(text);
        float[] vertices = new float[text_length * 12];
        float[] uvs = new float[text_length * 8];
        short[] indices = new short[text_length * 6];
        float cursorX = -getTextWidth(text) * 0.5f;
        int offset = 0, index_offset = 0, vertex_offset = 0;

        for (char c : text.toCharArray()) {
            if (c != ' ') {
                indices[index_offset] = (short) (vertex_offset);
                indices[index_offset + 1] = (short) (vertex_offset + 1);
                indices[index_offset + 2] = (short) (vertex_offset + 3);
                indices[index_offset + 3] = (short) (vertex_offset);
                indices[index_offset + 4] = (short) (vertex_offset + 2);
                indices[index_offset + 5] = (short) (vertex_offset + 3);
                vertex_offset += 4;
                index_offset += 6;
                byte row = (byte) ((c - font.startChar) / font.rowPitch);
                byte col = (byte) ((c - font.startChar) - (font.rowPitch * row));
                float u_start = col * font.columnFactor;
                float v_start = row * font.rowFactor;
                float u_end = u_start + font.columnFactor;
                float v_end = v_start + font.rowFactor;
                vertices[offset * 3] = cursorX;
                vertices[offset * 3 + 1] = 1;
                vertices[offset * 3 + 2] = 0;

                uvs[offset * 2] = u_start;
                uvs[offset * 2 + 1] = v_start;
                offset++;

                vertices[offset * 3] = cursorX;
                vertices[offset * 3 + 1] = -1;
                vertices[offset * 3 + 2] = 0;
                uvs[offset * 2] = u_start;
                uvs[offset * 2 + 1] = v_end;
                offset++;

                vertices[offset * 3] = cursorX + 1;
                vertices[offset * 3 + 1] = 1;
                vertices[offset * 3 + 2] = 0;
                uvs[offset * 2] = u_end;
                uvs[offset * 2 + 1] = v_start;
                offset++;

                vertices[offset * 3] = cursorX + 1;
                vertices[offset * 3 + 1] = -1;
                vertices[offset * 3 + 2] = 0;
                uvs[offset * 2] = u_end;
                uvs[offset * 2 + 1] = v_end;
                offset++;
            }
            cursorX += font.charWidths[(byte) c & 0xff];
        }
        for (offset = 0; offset < vertices.length; offset++) {
            vertices[offset] *= scale;
        }
        setVertices(vertices);
        setTextureCoords(uvs);
        MeshPart p = new MeshPart(indices);
        p.material.diffuseTexture = font.font_texture;
        addPart(p);
        setPrimitiveType(GL.GL_TRIANGLES);
    }

    @Override
    public void preRender() {
        FX.gl.glDisable(GL.GL_DEPTH_TEST);
    }

    @Override
    public void postRender() {
        FX.gl.glEnable(GL.GL_DEPTH_TEST);
    }

    @Override
    public void delete() {
        getPart(0).material.diffuseTexture = -1;
        font = null;
        super.delete();
    }

    private int getDrawTextLength(String process) {
        int i = 0;
        for (char c : process.toCharArray()) {
            if (c != ' ') {
                i++;
            }
        }
        return i;
    }

    private float getTextWidth(String text) {
        float text_width = 0.0f;
        for (short i = 0; i < text.length(); i++) {
            text_width += font.charWidths[(byte) text.charAt(i) & 0xff];
        }
        return text_width;
    }
}
