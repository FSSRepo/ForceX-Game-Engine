package com.fss.rpg;

import static com.openpl.PL10.PL_BOX_SHAPE;
import static com.openpl.PL10.PL_BVH_TRIANGLE_MESH_SHAPE;
import static com.openpl.PL10.PL_CAPSULE_SHAPE;
import static com.openpl.PL10.PL_EXTENT;
import static com.openpl.PL10.PL_HEIGHT;
import static com.openpl.PL10.PL_INDEX_BUFFER;
import static com.openpl.PL10.PL_PLANE_CONSTANT;
import static com.openpl.PL10.PL_PLANE_NORMAL;
import static com.openpl.PL10.PL_RADIUS;
import static com.openpl.PL10.PL_STATIC_PLANE_SHAPE;
import static com.openpl.PL10.PL_VERTEX_BUFFER;
import static com.openpl.PL10.plBindShape;
import static com.openpl.PL10.plBufferData;
import static com.openpl.PL10.plCreate;
import static com.openpl.PL10.plGenShape;
import static com.openpl.PL10.plShape3f;
import static com.openpl.PL10.plShapef;

import com.forcex.math.Vector3f;
import com.forcex.utils.BufferUtils;

import java.nio.Buffer;

public class CollisionShape {
    public static int createStaticPlane(Vector3f normal, float constant) {
        int shape = plGenShape();
        plBindShape(shape);
        plShapef(PL_PLANE_CONSTANT, constant);
        plShape3f(PL_PLANE_NORMAL, normal.x, normal.y, normal.z);
        plCreate(PL_STATIC_PLANE_SHAPE);
        plBindShape(0);
        return shape;
    }

    public static int createBox(Vector3f extents) {
        int shape = plGenShape();
        plBindShape(shape);
        plShape3f(PL_EXTENT, extents.x, extents.y, extents.z);
        plCreate(PL_BOX_SHAPE);
        plBindShape(0);
        return shape;
    }

    public static int createCapsule(float radius, float height) {
        int shape = plGenShape();
        plBindShape(shape);
        plShapef(PL_HEIGHT, height);
        plShapef(PL_RADIUS, radius);
        plCreate(PL_CAPSULE_SHAPE);
        plBindShape(0);
        return shape;
    }

    public static int createMeshShape(float[] vertices, short[] indices) {
        int shape = plGenShape();
        plBindShape(shape);
        plBufferData(PL_VERTEX_BUFFER, vertices.length, BufferUtils.createFloatBuffer(vertices));
        plBufferData(PL_INDEX_BUFFER, indices.length, BufferUtils.createShortBuffer(indices));
        plCreate(PL_BVH_TRIANGLE_MESH_SHAPE);
        plBindShape(0);
        return shape;
    }
}
