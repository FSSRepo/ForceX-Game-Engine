package com.forcex.gfx3d.shapes;

import com.forcex.gfx3d.Mesh;
import com.forcex.gfx3d.MeshPart;

public class Rectangle extends Mesh {
    private final float[] positions;

    private final float[] normals = {
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
            0, 0, 1
    };
    private final short[] indices = {
            0, 1, 3,
            3, 1, 2
    };
    private final float[] texture_coords = {
            0, 0,
            0, 1,
            1, 1,
            1, 0
    };

    public Rectangle(float width, float height, boolean mirror) {
        super(true);
        float w = width * 0.5f;
        float h = height * 0.5f;
        positions = new float[]{
                -w, +h, 0,
                -w, -h, 0,
                +w, -h, 0,
                +w, +h, 0
        };
        getModel(mirror);
    }

    public void getModel(boolean mirror) {
        setVertices(positions);
        setTextureCoords(mirror ? new float[]{
                0, 1,
                0, 0,
                1, 0,
                1, 1
        } : texture_coords);
        setNormals(normals);
        addPart(new MeshPart(indices));
    }
}
