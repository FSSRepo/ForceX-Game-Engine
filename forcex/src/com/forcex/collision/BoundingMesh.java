package com.forcex.collision;

import com.forcex.gfx3d.MeshPart;
import com.forcex.math.MathGeom;
import com.forcex.math.Matrix4f;
import com.forcex.math.Ray;
import com.forcex.math.Vector3f;

import java.util.ArrayList;

public class BoundingMesh {
    ArrayList<BoundingTriangle> triangles = new ArrayList<BoundingTriangle>();
    boolean collision = false;
    BoundingTriangle triangle_intersected;

    public boolean rayTest(Ray ray) {
        if (collision) {
            triangle_intersected = null;
            for (BoundingTriangle t : triangles) {
                if (Interceptor.intersectsRayTriangle(ray, t, false)) {
                    triangle_intersected = t;
                    return true;
                }
            }
        }
        return false;
    }

    public void compute(float[] vertices, ArrayList<MeshPart> parts, Matrix4f modelMatrix) {
        triangles.clear();
        collision = false;
        try {
            float[] vertex = MathGeom.transformVertices(vertices, modelMatrix);
            byte p = 0;
            for (MeshPart s : parts) {
                short[] index = s.index;
                short numFaces = (short) (index.length / 3);
                if ((numFaces % 3) != 0) { // fix faces unused
                    numFaces -= (index.length % 3);
                }
                for (short i = 0; i < numFaces; i++) {
                    BoundingTriangle t = new BoundingTriangle();
                    t.v1 = new Vector3f(vertex, index[(i * 3)] * 3);
                    t.v2 = new Vector3f(vertex, index[(i * 3) + 1] * 3);
                    t.v3 = new Vector3f(vertex, index[(i * 3) + 2] * 3);
                    t.part = p;
                    t.index_face = i;
                    triangles.add(t);
                }
                p++;
            }
            collision = true;
        } catch (Exception e) {
        }
    }
}
