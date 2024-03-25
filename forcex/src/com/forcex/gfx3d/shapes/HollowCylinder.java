package com.forcex.gfx3d.shapes;

import com.forcex.gfx3d.Mesh;
import com.forcex.gfx3d.MeshPart;
import com.forcex.math.Maths;

public class HollowCylinder extends Mesh {
    private int vertex_offset;
    private int index_offset;
    private final short[] indices;

    public HollowCylinder(float radiusOuter, float radiusInner, float height, int segments) {
        super(true);
        int numVertices = segments * 4;
        float[] vertices = new float[numVertices * 3];
        float[] normals = new float[numVertices * 3];
        float[] uvs = new float[numVertices * 2];
        indices = new short[numVertices * 6];
        addHorizontalSurface(
                vertices, normals, uvs,
                true, radiusOuter, radiusInner,
                height / 2.0f, segments);
        addHorizontalSurface(
                vertices, normals, uvs,
                false, radiusOuter, radiusInner,
                height / -2.0f, segments);
        addVerticalSurface(true, segments);
        addVerticalSurface(false, segments);
        setVertices(vertices);
        setNormals(normals);
        setTextureCoords(uvs);
        addPart(new MeshPart(indices));
    }

    private void addHorizontalSurface(float[] vertices, float[] normals, float[] texcoords, boolean isTopSide, float radiusOuter, float radiusInner, float zOffset, int segments) {
        int indexOffset = vertex_offset;
        float step = (float) ((360.0 / segments) * Maths.toRadians);
        // verts
        for (int i = 0; i < segments; i++) {
            float angle = (float) i * step;
            // outer
            float x1 = (float) Math.sin(angle) * radiusOuter;
            float y1 = (float) Math.cos(angle) * radiusOuter;
            float z1 = zOffset;
            vertices[vertex_offset * 3] = x1;
            vertices[vertex_offset * 3 + 1] = y1;
            vertices[vertex_offset * 3 + 2] = z1;
            normals[vertex_offset * 3] = 0;
            normals[vertex_offset * 3 + 1] = 0;
            normals[vertex_offset * 3 + 2] = isTopSide ? -1 : +1;
            texcoords[vertex_offset * 2] = x1;
            texcoords[vertex_offset * 2 + 1] = y1;
            vertex_offset++;

            // inner
            float x2 = (float) Math.sin(angle) * radiusInner;
            float y2 = (float) Math.cos(angle) * radiusInner;
            float z2 = zOffset;

            vertices[vertex_offset * 3] = x2;
            vertices[vertex_offset * 3 + 1] = y2;
            vertices[vertex_offset * 3 + 2] = z2;
            normals[vertex_offset * 3] = 0;
            normals[vertex_offset * 3 + 1] = 0;
            normals[vertex_offset * 3 + 2] = isTopSide ? -1 : +1;
            texcoords[vertex_offset * 2] = x2;
            texcoords[vertex_offset * 2 + 1] = y2;
            vertex_offset++;
        }

        // indicies

        for (int i = 2; i <= segments; i++) {
            int a = indexOffset + i * 2 - 3 - 1;
            int b = indexOffset + i * 2 - 2 - 1;
            int c = indexOffset + i * 2 - 1 - 1;
            int d = indexOffset + i * 2 - 1;
            addQuad(a, b, c, d, isTopSide);
        }

        int a = indexOffset + segments * 2 - 1 - 1; // ... connect last segment
        int b = indexOffset + segments * 2 - 1;
        int c = indexOffset;
        int d = indexOffset + 1;
        addQuad(a, b, c, d, isTopSide);
    }

    private void addVerticalSurface(boolean isOuter, int segments) {
        int off = (vertex_offset / 2);
        for (int i = 0; i < segments - 1; i++) {
            int ul = i * 2;
            int bl = ul + off;
            int ur = i * 2 + 2;
            int br = ur + off;
            if (!isOuter) {
                ul++;
                bl++;
                ur++;
                br++;
            }
            addQuad(ul, bl, ur, br, isOuter);
        }

        int ul = (segments - 1) * 2;
        int bl = ul + off;
        int ur = 0;
        int br = ur + off;

        if (!isOuter) {
            ul++;
            bl++;
            ur++;
            br++;
        }

        addQuad(ul, bl, ur, br, isOuter);
    }

    private void addQuad(int ul, int bl, int ur, int br, boolean flipped) {
        if (!flipped) {
            addFace(ul, bl, ur);
            addFace(bl, br, ur);
        } else {
            addFace(ur, br, ul);
            addFace(br, bl, ul);
        }
    }

    public void addFace(int a, int b, int c) {
        indices[index_offset * 3] = (short) a;
        indices[index_offset * 3 + 1] = (short) b;
        indices[index_offset * 3 + 2] = (short) c;
        index_offset++;
    }
}
