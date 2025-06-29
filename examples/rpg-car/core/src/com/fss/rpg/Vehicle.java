package com.fss.rpg;

import static com.openpl.PL10.PL_ACTIVATION_STATE;
import static com.openpl.PL10.PL_ADD_CHILD_SHAPE;
import static com.openpl.PL10.PL_ADD_RIGID_BODY;
import static com.openpl.PL10.PL_ADD_VEHICLE;
import static com.openpl.PL10.PL_ADD_WHEEL;
import static com.openpl.PL10.PL_COLLISION_SHAPE;
import static com.openpl.PL10.PL_COMPOUND_SHAPE;
import static com.openpl.PL10.PL_COORDINATE_SYSTEM;
import static com.openpl.PL10.PL_DEBUG_INFO;
import static com.openpl.PL10.PL_DIS_DACTIVATION;
import static com.openpl.PL10.PL_FRICTION_SLIP;
import static com.openpl.PL10.PL_MASS;
import static com.openpl.PL10.PL_MAX_SUSPENSION_FORCE;
import static com.openpl.PL10.PL_MAX_SUSPENSION_TRAVEL;
import static com.openpl.PL10.PL_MOTION_STATE_TRANSFORM;
import static com.openpl.PL10.PL_RAYCAST_VEHICLE;
import static com.openpl.PL10.PL_RIGID_BODY;
import static com.openpl.PL10.PL_SUSPENSION_COMPRESSION;
import static com.openpl.PL10.PL_SUSPENSION_DAMPING;
import static com.openpl.PL10.PL_SUSPENSION_STIFFNESS;
import static com.openpl.PL10.PL_TRANSFORM;
import static com.openpl.PL10.PL_VEHICLE_CHASSIS;
import static com.openpl.PL10.PL_WHEEL_BRAKE;
import static com.openpl.PL10.PL_WHEEL_ENGINE_FORCE;
import static com.openpl.PL10.PL_WHEEL_ROLL_INFLUENCE;
import static com.openpl.PL10.PL_WHEEL_STEERING;
import static com.openpl.PL10.PL_WHEEL_TRANSFORM_INTERPOLATION;
import static com.openpl.PL10.plBindBody;
import static com.openpl.PL10.plBindShape;
import static com.openpl.PL10.plBindVehicle;
import static com.openpl.PL10.plCreate;
import static com.openpl.PL10.plDynamicWorldi;
import static com.openpl.PL10.plGenBody;
import static com.openpl.PL10.plGenShape;
import static com.openpl.PL10.plGenVehicle;
import static com.openpl.PL10.plGetRigidBodyfv;
import static com.openpl.PL10.plGetString;
import static com.openpl.PL10.plGetWheelfv;
import static com.openpl.PL10.plRigidBodyf;
import static com.openpl.PL10.plRigidBodyfv;
import static com.openpl.PL10.plRigidBodyi;
import static com.openpl.PL10.plShapefv;
import static com.openpl.PL10.plShapei;
import static com.openpl.PL10.plVehicle3f;
import static com.openpl.PL10.plVehiclef;
import static com.openpl.PL10.plVehiclefv;
import static com.openpl.PL10.plVehiclei;
import static com.openpl.PL10.plWheelf;

import com.forcex.app.Key;
import com.forcex.gfx3d.Camera;
import com.forcex.gfx3d.Mesh;
import com.forcex.gfx3d.ModelObject;
import com.forcex.gfx3d.ModelRenderer;
import com.forcex.gfx3d.Node;
import com.forcex.gfx3d.shapes.Box;
import com.forcex.gfx3d.shapes.Cylinder;
import com.forcex.math.Maths;
import com.forcex.math.Matrix3f;
import com.forcex.math.Matrix4f;
import com.forcex.math.Quaternion;
import com.forcex.math.Vector3f;
import com.forcex.utils.Color;

import java.util.ArrayList;

public class Vehicle {
    int chassis_body;
    int vehicle;
    float wheel_width = 0.2f, wheel_radius = 0.34f;

    ModelObject chassis;

    ArrayList<ModelObject> wheels = new ArrayList<>();
    Matrix4f wheel_flip = Matrix4f.fromRotation(Quaternion.fromEulerAngles(0, 180, 0));

    Matrix4f tmp = new Matrix4f();
    Matrix4f vehicle_physics_matrix = new Matrix4f();

    boolean onVehicle = false;
    Quaternion start_cam_rot = new Quaternion();
    Quaternion end_cam_rot;
    Quaternion tmp_rot = new Quaternion();
    float refresh_cam_time = 0.0f;
    float camera_blend = -1.0f;
    float CAM_REORIENT_TIME = 1.5f;
    private Vector3f follow_fixed = new Vector3f(0, 0, 0);
    float distance = 14f, rot_x = 180, rot_y = 30;

    public Vehicle(Node vehicle_nodes, Mesh wheel) {
        Matrix4f root = new Matrix4f();
        root.setLocation(30, 2.5f, 25);
        {
            float chassis_extent_x = 1f;
            float chassis_extent_z = 2f;

            System.out.println(plGetString(PL_DEBUG_INFO));

            chassis = new ModelObject(new Box(chassis_extent_x, 0.5f, chassis_extent_z));

            int chassis_shape = CollisionShape.createBox(new Vector3f(chassis_extent_x, 0.2f, chassis_extent_z));

            int compound_shape = plGenShape();
            plBindShape(compound_shape);
            plCreate(PL_COMPOUND_SHAPE);
            plShapefv(PL_TRANSFORM, tmp.data, 16);
            plShapei(PL_ADD_CHILD_SHAPE, chassis_shape);
            plBindShape(0);

            chassis_body = plGenBody();
            plBindBody(chassis_body);
            plRigidBodyi(PL_COLLISION_SHAPE, compound_shape);
            plRigidBodyf(PL_MASS, 400);
            plRigidBodyfv(PL_MOTION_STATE_TRANSFORM, root.data, 16);
            plCreate(PL_RIGID_BODY);
            plRigidBodyi(PL_ACTIVATION_STATE, PL_DIS_DACTIVATION);
            plBindBody(0);
            plDynamicWorldi(PL_ADD_RIGID_BODY, chassis_body);

            float stiffness = 80;
            float compression = 0.49f;
            float damping = 0.2f;

            // setting tuning
            plVehiclef(PL_SUSPENSION_STIFFNESS, stiffness);
            plVehiclef(PL_SUSPENSION_DAMPING, damping * 3f * Maths.sqrt(stiffness));
            plVehiclef(PL_SUSPENSION_COMPRESSION, compression * 0.1f * Maths.sqrt(stiffness));

            float max_suspension_travel = 10.0f;
            plVehiclef(PL_MAX_SUSPENSION_TRAVEL, max_suspension_travel);
            plVehiclef(PL_MAX_SUSPENSION_FORCE, 3456);
            plVehiclef(PL_FRICTION_SLIP, 20);

            vehicle = plGenVehicle();
            plVehiclei(PL_VEHICLE_CHASSIS, chassis_body);

            plCreate(PL_RAYCAST_VEHICLE);
            plBindVehicle(vehicle);
            plVehicle3f(PL_COORDINATE_SYSTEM, 0, 1, 2);

            plDynamicWorldi(PL_ADD_VEHICLE, vehicle);

            float rest_len = max_suspension_travel / 50.0f;

            Vector3f[] connections = new Vector3f[] {
//                    // front
                    vehicle_nodes.getNode("wheel_rf_dummy").position.swapYZ(),
                    vehicle_nodes.getNode("wheel_lf_dummy").position.swapYZ(),
//                    new Vector3f(chassis_extent_x - wheel_width * 0.5f, wheels_height + rest_len, chassis_extent_z - wheel_radius),
//                    new Vector3f(-chassis_extent_x + wheel_width * 0.5f, wheels_height + rest_len, chassis_extent_z - wheel_radius),
//
//                    // rear
                    vehicle_nodes.getNode("wheel_rb_dummy").position.swapYZ(),
                    vehicle_nodes.getNode("wheel_lb_dummy").position.swapYZ(),
//                    new Vector3f(-chassis_extent_x + wheel_width * 0.5f, wheels_height + rest_len, -chassis_extent_z + wheel_radius),
//                    new Vector3f(chassis_extent_x - wheel_width * 0.5f, wheels_height + rest_len, -chassis_extent_z + wheel_radius),
            };

            float[] params = new float[12];

            for (int i = 0; i < 4; i++) {
                // connection xyz
                params[0] = connections[i].x;
                params[1] = connections[i].y;
                params[2] = connections[i].z;

                // wheel direction xyz
                params[3] = 0;
                params[4] = -1;
                params[5] = 0;

                // wheel axis xyz
                params[6] = -1;
                params[7] = 0;
                params[8] = 0;

                params[9] = rest_len;
                params[10] = wheel_radius;
                params[11] = i  > 1 ? 1.0f : 0.0f;

                plVehiclefv(PL_ADD_WHEEL, params, 12);

                plWheelf(i, PL_WHEEL_ROLL_INFLUENCE, 1f);
                wheels.add(new ModelObject(wheel));
            }

            // TODO: support PL_RESET_SUSPENSION
            plBindVehicle(0);
        }
    }

    public void update(Camera camera, float delta, Node root) {
        // handle follow camera
        if (onVehicle) {
            end_cam_rot = vehicle_physics_matrix.getRotation(tmp_rot);
            Vector3f chassis_pos = vehicle_physics_matrix.getLocation(null);
            if (refresh_cam_time == 0.0f) {
                start_cam_rot.set(end_cam_rot);
            }

            Quaternion cur = start_cam_rot;

            if (refresh_cam_time > 0.4f) {
                if (camera_blend >= 0.0f) {
                    if (camera_blend > CAM_REORIENT_TIME) {
                        cur = end_cam_rot;
                        start_cam_rot.set(end_cam_rot);
                        refresh_cam_time = 0.0f;
                        camera_blend = -1.0f;
                    } else {
                        cur = start_cam_rot.slerp(end_cam_rot, camera_blend / CAM_REORIENT_TIME);
                        camera_blend += delta;
                    }
                } else if (!cur.equals(end_cam_rot)) {
                    camera_blend = 0.0f;
                }
            }

            refresh_cam_time += delta;

            Matrix3f cam_orientation = cur.toMatrix();
            Vector3f local_follow = cam_orientation.mult(follow_fixed.rotateOnSphereOrigin(distance, rot_x, rot_y, false));
            Vector3f follow = chassis_pos.add(local_follow);
            camera.setPosition(follow.x, follow.y, follow.z);
            camera.lookAt(chassis_pos);
        }

        plGetRigidBodyfv(chassis_body, PL_MOTION_STATE_TRANSFORM, vehicle_physics_matrix.data, 16);

        root.rotation_matrix = vehicle_physics_matrix.getUpperLeft().multLocal(Quaternion.fromEulerAngles(-90, 180, 0).toMatrix());
        vehicle_physics_matrix.getLocation(root.position);
        root.notifyUpdate();
    }

    public void render(ModelRenderer renderer) {
        renderer.render(chassis);
        plBindVehicle(vehicle);
        for (int i = 0; i < 4; i++) {
            plGetWheelfv(i, PL_WHEEL_TRANSFORM_INTERPOLATION, tmp.data, 16);
            if(i == 0 || i == 2) {
                wheels.get(i).setTransform(tmp.multLocal(wheel_flip));
            } else {
                wheels.get(i).setTransform(tmp);
            }
            renderer.render(wheels.get(i));
        }
        plBindVehicle(0);
    }

    public void control(int key, boolean pressed) {
        if (!onVehicle) {
            return;
        }
        plBindVehicle(vehicle);

        if (pressed) {
            if (key == Key.A_KEY) {
                plWheelf(0, PL_WHEEL_STEERING, 0.4f);
                plWheelf(1, PL_WHEEL_STEERING, 0.4f);
            } else if (key == Key.D_KEY) {
                plWheelf(0, PL_WHEEL_STEERING, -0.4f);
                plWheelf(1, PL_WHEEL_STEERING, -0.4f);
            } else if (key == Key.W_KEY) {
                plWheelf(2, PL_WHEEL_ENGINE_FORCE, 400);
                plWheelf(3, PL_WHEEL_ENGINE_FORCE, 400);
            } else if (key == Key.S_KEY) {
                plWheelf(2, PL_WHEEL_ENGINE_FORCE, -200);
                plWheelf(3, PL_WHEEL_ENGINE_FORCE, -200);
            }
        } else {
            if (key == Key.A_KEY || key == Key.D_KEY) {
                plWheelf(0, PL_WHEEL_STEERING, 0);
                plWheelf(1, PL_WHEEL_STEERING, 0);
            }
            if (key == Key.W_KEY || key == Key.S_KEY) {
                plWheelf(2, PL_WHEEL_ENGINE_FORCE, 0);
                plWheelf(3, PL_WHEEL_ENGINE_FORCE, 0);
                plWheelf(2, PL_WHEEL_BRAKE, 70);
                plWheelf(3, PL_WHEEL_BRAKE, 70);
            }
        }
        plBindVehicle(0);
    }
}
