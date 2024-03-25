package com.forcex.gfx3d.shapes;

import com.forcex.gfx3d.Mesh;
import com.forcex.gfx3d.MeshPart;
import com.forcex.math.Maths;

public class Sphere extends Mesh {
    public Sphere(float radius, int columns, int rows) {
        super(true);
        int numVertices = (columns + 1) * (rows + 1);
        float[] vertices = new float[numVertices * 3];
        float[] normals = new float[numVertices * 3];
        float[] texture_coords = new float[numVertices * 2];
        short[] indices = new short[2 * columns * (rows - 1) * 3];

        int i, j;
        int vertIndex = 0, index = 0;
        final float normLen = 1.0f / radius;

        for (j = 0; j <= rows; ++j) {
            float horAngle = (Maths.PI * j / (float) rows);
            float z = radius * Maths.cos(horAngle);
            float ringRadius = radius * Maths.sin(horAngle);

            for (i = 0; i <= columns; ++i) {
                float verAngle = (Maths.PI_2 * i / (float) columns);
                float x = ringRadius * Maths.cos(verAngle);
                float y = ringRadius * Maths.sin(verAngle);

                normals[vertIndex] = x * normLen;
                vertices[vertIndex++] = x;
                normals[vertIndex] = y * normLen;
                vertices[vertIndex++] = y;
                normals[vertIndex] = z * normLen;
                vertices[vertIndex++] = z;

                if (i > 0 && j > 0) {
                    short a = (short) ((columns + 1) * j + i);
                    short b = (short) ((columns + 1) * j + i - 1);
                    short c = (short) ((columns + 1) * (j - 1) + i - 1);
                    short d = (short) ((columns + 1) * (j - 1) + i);

                    if (j == columns) {
                        indices[index++] = a;
                        indices[index++] = c;
                        indices[index++] = d;
                    } else if (j == 1) {
                        indices[index++] = a;
                        indices[index++] = b;
                        indices[index++] = c;
                    } else {
                        indices[index++] = a;
                        indices[index++] = b;
                        indices[index++] = c;
                        indices[index++] = a;
                        indices[index++] = c;
                        indices[index++] = d;
                    }
                }
            }
        }
        int uV = 0;
        for (j = 0; j <= rows; ++j) {
            for (i = columns; i >= 0; --i) {
                texture_coords[uV++] = (float) i / columns;
                texture_coords[uV++] = (float) j / rows;
            }
        }
        setVertices(vertices);
        setTextureCoords(texture_coords);
        setNormals(normals);
        addPart(new MeshPart(indices));
    }
}
