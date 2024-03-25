package com.forcex.gfx3d.shapes;

import com.forcex.core.GL;
import com.forcex.gfx3d.Mesh;
import com.forcex.gfx3d.MeshPart;

public class GridRectangle extends Mesh {
    public GridRectangle(float length, int grid_size) {
        super(true);
        float[] vertices = new float[grid_size * grid_size * 3];
        short[] indices = new short[(grid_size - 1) * grid_size * 4];
        int j, i, vertex = 0, index = 0;
        for (i = 0; i < grid_size; i++) {
            float x = 0, y = 0;
            for (j = 0; j < grid_size - 1; j++) {
                x = (float) (i - (grid_size >> 1)) * length;
                y = (float) (j - (grid_size >> 1)) * length;
                vertices[vertex * 3] = x;
                vertices[vertex * 3 + 1] = 0;
                vertices[vertex * 3 + 2] = y;
                vertex++;
                indices[index++] = (short) (grid_size * i + j);
                indices[index++] = (short) (grid_size * i + j + 1);
                indices[index++] = (short) (i + grid_size * j);
                indices[index++] = (short) (i + grid_size * (j + 1));
            }
            j = grid_size - 1;
            x = (float) (i - (grid_size >> 1)) * length;
            y = (float) (j - (grid_size >> 1)) * length;
            vertices[vertex * 3] = x;
            vertices[vertex * 3 + 1] = 0;
            vertices[vertex * 3 + 2] = y;
            vertex++;
        }
        setVertices(vertices);
        addPart(new MeshPart(indices));
        lineSize = 0.1f;
        setPrimitiveType(GL.GL_LINES);
    }
}
