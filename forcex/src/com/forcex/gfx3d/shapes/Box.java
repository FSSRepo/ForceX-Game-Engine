package com.forcex.gfx3d.shapes;

import com.forcex.gfx3d.Mesh;
import com.forcex.gfx3d.MeshPart;

public class Box extends Mesh {
    public Box(float width, float height, float depth) {
        super(true);
        setExtent(width, height, depth);
    }

    public void setExtent(float width, float height, float depth) {
        setVertices(new float[]{
                //front
                -width, +height, +depth,
                +width, +height, +depth,
                +width, -height, +depth,
                -width, -height, +depth,
                //right
                +width, +height, +depth,
                +width, +height, -depth,
                +width, -height, -depth,
                +width, -height, +depth,
                //back
                +width, +height, -depth,
                -width, +height, -depth,
                -width, -height, -depth,
                +width, -height, -depth,
                //left
                -width, +height, -depth,
                -width, +height, +depth,
                -width, -height, +depth,
                -width, -height, -depth,
                //top
                -width, +height, -depth,
                +width, +height, -depth,
                +width, +height, +depth,
                -width, +height, +depth,
                //bottom
                -width, -height, +depth,
                +width, -height, +depth,
                +width, -height, -depth,
                -width, -height, -depth
        });
        setNormals(new float[]{
                0, 0, 1,
                0, 0, 1,
                0, 0, 1,
                0, 0, 1,

                1, 0, 0,
                1, 0, 0,
                1, 0, 0,
                1, 0, 0,

                0, 0, -1,
                0, 0, -1,
                0, 0, -1,
                0, 0, -1,

                -1, 0, 0,
                -1, 0, 0,
                -1, 0, 0,
                -1, 0, 0,

                0, 1, 0,
                0, 1, 0,
                0, 1, 0,
                0, 1, 0,

                0, -1, 0,
                0, -1, 0,
                0, -1, 0,
                0, -1, 0,
        });
        setTextureCoords(new float[]{
                //f
                0, 0,
                1, 0,
                1, 1,
                0, 1,
                //right
                0, 0,
                1, 0,
                1, 1,
                0, 1,
                //back
                0, 0,
                1, 0,
                1, 1,
                0, 1,
                //left
                0, 0,
                1, 0,
                1, 1,
                0, 1,
                //top
                0, 0,
                1, 0,
                1, 1,
                0, 1,
                //bottom
                0, 0,
                1, 0,
                1, 1,
                0, 1,
        });
        addPart(new MeshPart(new short[]{
                0, 2, 1, 0, 3, 2, //front
                4, 6, 5, 4, 7, 6, //right
                8, 10, 9, 8, 11, 10, //back

                12, 14, 13, 12, 15, 14, //left
                16, 18, 17, 16, 19, 18, //top
                20, 22, 21, 20, 23, 22 //bottom
        }));
    }
}
