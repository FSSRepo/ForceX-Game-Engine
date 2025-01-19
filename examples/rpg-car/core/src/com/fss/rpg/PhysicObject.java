package com.fss.rpg;

import com.forcex.gfx3d.Mesh;
import com.forcex.gfx3d.ModelObject;
import com.forcex.math.Matrix4f;

import static com.openpl.PL10.*;

public class PhysicObject extends ModelObject {
    int body;
    float mass;

    public PhysicObject(int collision_shape, float mass, Matrix4f transform, Mesh mesh) {
        super(mesh);
        this.mass = mass;
        body = plGenBody();
        plBindBody(body);
        plRigidBodyi(PL_COLLISION_SHAPE, collision_shape);
        if(mass > 0) {
            plRigidBodyf(PL_MASS, mass);
        }
        plRigidBodyfv(PL_MOTION_STATE_TRANSFORM, transform.data, 16);
        plCreate(PL_RIGID_BODY);
        plBindBody(0);

        plDynamicWorldi(PL_ADD_RIGID_BODY, body);
    }

    public void updatePhysics() {
        plGetRigidBodyfv(body, PL_MOTION_STATE_TRANSFORM, getTransform().data, 16);
    }
}
