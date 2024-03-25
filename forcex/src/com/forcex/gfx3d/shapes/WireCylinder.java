package com.forcex.gfx3d.shapes;

import com.forcex.core.GL;
import com.forcex.gfx3d.Mesh;
import com.forcex.gfx3d.MeshPart;
import com.forcex.math.Maths;

public class WireCylinder extends Mesh {
    public enum Axis {
        AXIS_X, AXIS_Y, AXIS_Z
    }

    private int vertex_offset;
	private int index_offset;
	private final short[] indices;
	private final byte segments = 15;

	Axis axis;

    public WireCylinder(float height, float radius, Axis axis, boolean isCapsule) {
        super(true);
		this.axis = axis;
        float[] vertices = new float[(segments * 6) + (isCapsule ? (segments * 36) : (segments * 12))];
        indices = new short[segments * 2 + (isCapsule ? (segments * 12) : (segments * 4))];
        drawDiagonalLines(vertices, height, radius);
        if (isCapsule) {
            drawSphere(vertices, radius, height / -2f);
            drawSphere(vertices, radius, height / 2f);
        } else {
            drawCircle(vertices, height / -2f, radius);
            drawCircle(vertices, height / 2f, radius);
        }
        setVertices(vertices);
        setPrimitiveType(GL.GL_LINES);
        addPart(new MeshPart(indices));
    }

    private void drawDiagonalLines(
            float[] vertices, float height, float radius) {
        float half_height = height * 0.5f;
        float rate = (Maths.PI_2 / segments);
        float angle = 0;
        boolean isX = axis == Axis.AXIS_X, isZ = axis == Axis.AXIS_Z;
        for (byte i = 0; i < segments; i++) {
            float x = radius * Maths.cos(angle);
            float y = radius * Maths.sin(angle);
            vertices[vertex_offset] = isX ? half_height : (x);
            vertices[vertex_offset + 1] = isX ? x : (isZ ? y : half_height);
            vertices[vertex_offset + 2] = isX ? y : (isZ ? half_height : y);
            vertices[vertex_offset + 3] = isX ? -half_height : (x);
            vertices[vertex_offset + 4] = isX ? x : (isZ ? y : -half_height);
            vertices[vertex_offset + 5] = isX ? y : (isZ ? -half_height : y);
            vertex_offset += 6;
            angle += rate;
        }
        for (byte i = 0; i < segments; i++) {
            indices[index_offset] = (short) (index_offset);
            indices[index_offset + 1] = (short) (index_offset + 1);
            index_offset += 2;
        }
    }

    private void drawCircle(
            float[] vertices, float height, float radius) {
        float rate = (Maths.PI_2 / segments);
        float angle = 0;
        boolean isX = axis == Axis.AXIS_X, isZ = axis == Axis.AXIS_Z;
        for (byte i = 0; i < segments; i++) {
            float x = radius * Maths.cos(angle);
            float y = radius * Maths.sin(angle);
            vertices[vertex_offset] = isX ? height : (x);
            vertices[vertex_offset + 1] = isX ? x : (isZ ? y : height);
            vertices[vertex_offset + 2] = isX ? y : (isZ ? height : y);
            x = radius * Maths.cos(angle + rate);
            y = radius * Maths.sin(angle + rate);
            vertices[vertex_offset + 3] = isX ? height : (x);
            vertices[vertex_offset + 4] = isX ? x : (isZ ? y : height);
            vertices[vertex_offset + 5] = isX ? y : (isZ ? height : y);
            vertex_offset += 6;
            angle += rate;
        }
        for (byte i = 0; i < segments; i++) {
            indices[index_offset] = (short) (index_offset);
            indices[index_offset + 1] = (short) (index_offset + 1);
            index_offset += 2;
        }
    }

    private void drawSphere(float[] vertices, float radius, float height) {
        float rate = Maths.PI_2 / segments;
        float angle = 0;
        float tx = (axis == Axis.AXIS_X ? height : 0);
        float ty = (axis == Axis.AXIS_Y ? height : 0);
        float tz = (axis == Axis.AXIS_Z ? height : 0);
        for (byte i = 0; i < segments; i++) {
            // xy
            float x = radius * Maths.cos(angle);
            float y = radius * Maths.sin(angle);
            float x2 = radius * Maths.cos(angle + rate);
            float y2 = radius * Maths.sin(angle + rate);

            vertices[vertex_offset] = x + tx;
            vertices[vertex_offset + 1] = y + ty;
            vertices[vertex_offset + 2] = tz;

            vertices[vertex_offset + 3] = x2 + tx;
            vertices[vertex_offset + 4] = y2 + ty;
            vertices[vertex_offset + 5] = tz;
            // xz
            vertex_offset += 6;
            vertices[vertex_offset] = tx + x;
            vertices[vertex_offset + 1] = ty;
            vertices[vertex_offset + 2] = tz + y;

            vertices[vertex_offset + 3] = tx + x2;
            vertices[vertex_offset + 4] = ty;
            vertices[vertex_offset + 5] = tz + y2;
            vertex_offset += 6;
            // zy
            vertices[vertex_offset] = tx;
            vertices[vertex_offset + 1] = ty + x;
            vertices[vertex_offset + 2] = tz + y;

            vertices[vertex_offset + 3] = tx;
            vertices[vertex_offset + 4] = ty + x2;
            vertices[vertex_offset + 5] = tz + y2;
            vertex_offset += 6;
            angle += rate;
        }
        for (int i = 0; i < segments; i++) {
            // xy
            indices[index_offset] = (short) (index_offset);
            indices[index_offset + 1] = (short) (index_offset + 1);
            // xz
            indices[index_offset + 2] = (short) (index_offset + 2);
            indices[index_offset + 3] = (short) (index_offset + 3);
            // yz
            indices[index_offset + 4] = (short) (index_offset + 4);
            indices[index_offset + 5] = (short) (index_offset + 5);
            index_offset += 6;
        }
    }
}
