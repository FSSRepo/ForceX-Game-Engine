package com.forcex.gfx3d.shapes;

import com.forcex.FX;
import com.forcex.core.GL;
import com.forcex.gfx3d.Mesh;
import com.forcex.gfx3d.MeshPart;
import com.forcex.math.Maths;

public class DynamicCircle extends Mesh {
    private final int segments;
    private final float radius;
    private final float[] vertices;
    private final MeshPart solid_circle;
    private final MeshPart cursor_line;

    public DynamicCircle(float radius, int segments) {
        super(true);
        this.radius = radius;
        this.segments = segments;
        solid_circle = new MeshPart(new short[segments * 3]);
        solid_circle.material.color.set(255, 255, 255, 120);
        setPrimitiveType(GL.GL_TRIANGLES);
        addPart(solid_circle);
        MeshPart circle_line = new MeshPart(new short[segments + 1]);
        circle_line.type = GL.GL_LINE_STRIP;
        for (short i = 0; i < segments; i++) {
            circle_line.index[i] = (short) (i + 1);
        }
        circle_line.index[segments] = 1;
        cursor_line = new MeshPart(new short[4]);
        cursor_line.index[2] = 0;
        cursor_line.index[3] = 1;
        cursor_line.type = GL.GL_LINES;
        addPart(circle_line);
        addPart(cursor_line);
        vertices = new float[(segments + 2) * 3];
        setAngle(0);
        useCustomPartType = true;
        lineSize = 1f;
    }

    @Override
    public void preRender() {
        FX.gl.glDisable(GL.GL_DEPTH_TEST);
    }

    @Override
    public void postRender() {
        FX.gl.glEnable(GL.GL_DEPTH_TEST);
    }

    public void setAxis(int axis) {
        switch (axis) {
            case 0:
                getPart(1).material.color.set(255, 0, 0);
                break;
            case 1:
                getPart(1).material.color.set(0, 255, 0);
                break;
            case 2:
                getPart(1).material.color.set(0, 0, 255);
                break;
        }
        getPart(2).material.color.set(getPart(1).material.color);
        short offset = 0;
        vertices[offset] = 0;
        vertices[offset + 1] = 0;
        vertices[offset + 2] = 0;
        offset += 3;
        float rate = Maths.PI_2 / segments;
        float current = 0.0f;
        for (int i = 0; i < (segments + 1); i++) {
            float c = radius * Maths.cos(current);
            float s = radius * Maths.sin(current);
            vertices[offset] = axis == 0 ? 0 : (axis == 1 ? s : c);
            vertices[offset + 1] = axis == 1 ? 0 : (axis == 0 ? c : s);
            vertices[offset + 2] = axis == 2 ? 0 : (axis == 1 ? c : s);
            offset += 3;
            current += rate;
        }
        current = 0;
        setVertices(vertices);
    }

    public void setAngle(float angle) {
        float delta = angle % 360f;
        int steps = ((int) ((Math.abs(delta) / 360.0f) * segments) - 1);
        int num = segments * 3;
        if (delta >= 0) {
            short offset = 0;
            for (int i = 0; i < num; i += 3) {
                if (offset <= steps) {
                    solid_circle.index[i] = 0;
                    solid_circle.index[i + 1] = (short) (offset + 1);
                    solid_circle.index[i + 2] = (short) (offset + 2);
                    offset++;
                } else {
                    solid_circle.index[i] = 0;
                    solid_circle.index[i + 1] = 0;
                    solid_circle.index[i + 2] = 0;
                }
            }
            cursor_line.index[0] = 0;
            cursor_line.index[1] = (short) (offset + 1);
        } else {
            short offset = 0;
            for (int i = 0; i < num; i += 3) {
                if (offset >= (segments - steps)) {
                    solid_circle.index[i] = 0;
                    solid_circle.index[i + 1] = (short) (offset + 1);
                    solid_circle.index[i + 2] = (short) (offset + 2);
                } else {
                    solid_circle.index[i] = 0;
                    solid_circle.index[i + 1] = 0;
                    solid_circle.index[i + 2] = 0;
                }
                offset++;
            }
            cursor_line.index[0] = 0;
            cursor_line.index[1] = (short) ((segments - steps) - 1);
        }
        if (solid_circle.buffer != null) {
            solid_circle.buffer.reset();
            cursor_line.buffer.reset();
        }
    }
}
