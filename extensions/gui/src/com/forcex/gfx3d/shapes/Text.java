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
        // Unidades de mundo: scale equivale a la altura de línea.
        float k = scale / font.getLineHeight();
        float cursorX = -getTextWidth(text) * 0.5f * k;
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
                Font.Glyph g = font.getGlyph(c);
                float x0 = cursorX + g.xoff * k;
                float x1 = x0 + g.w * k;
                float y0 = -g.yoff * k;       // y crece hacia arriba en el mundo
                float y1 = y0 - g.h * k;
                float[][] corners = {
                    {x0, y0, g.u0, g.v0},
                    {x0, y1, g.u0, g.v1},
                    {x1, y0, g.u1, g.v0},
                    {x1, y1, g.u1, g.v1}
                };
                for (float[] v : corners) {
                    vertices[offset * 3] = v[0];
                    vertices[offset * 3 + 1] = v[1];
                    vertices[offset * 3 + 2] = 0;
                    uvs[offset * 2] = v[2];
                    uvs[offset * 2 + 1] = v[3];
                    offset++;
                }
            }
            cursorX += font.getAdvance(c) * k;
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
            text_width += font.getAdvance(text.charAt(i));
        }
        return text_width;
    }
}
