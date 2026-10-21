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

    private final CameraCollision camera_collision = new CameraCollision();
    // mitad del largo del chasis mas margen: impactos mas cerca que esto pertenecen al propio auto
    private static final float CAMERA_SELF_RADIUS = 2.8f;

    ModelObject chassis;

    ArrayList<ModelObject> wheels = new ArrayList<>();
    Matrix4f wheel_flip = Matrix4f.fromRotation(Quaternion.fromEulerAngles(0, 180, 0));

    Matrix4f tmp = new Matrix4f();
    Matrix4f vehicle_physics_matrix = new Matrix4f();

    boolean onVehicle = false;
    private Vector3f follow_fixed = new Vector3f(0, 0, 0);
    // rot_x es el giro de la camara respecto al rumbo del auto (180 = justo detras)
    float distance = 14f, rot_x = 180, rot_y = 30;

    // camara de seguimiento: solo sigue el rumbo (yaw) del auto, no su cabeceo ni balanceo
    private static final float CAMERA_HEADING_SMOOTH = 3.5f;
    private static final float CAMERA_RECENTER_DELAY = 1.5f;
    private static final float CAMERA_RECENTER_SMOOTH = 1.8f;
    private static final float CAMERA_RECENTER_MIN_SPEED = 2f;
    private static final float CAMERA_DEFAULT_PITCH = 30f;
    private static final float CAMERA_MIN_PITCH = 0f, CAMERA_MAX_PITCH = 85f;
    private float camera_heading = 0.0f;
    private boolean camera_ready = false;
    private float camera_idle = 0.0f;
    private final Vector3f last_chassis_pos = new Vector3f();
    private final Vector3f forward_tmp = new Vector3f();

    public Vehicle(Vector3f spawn, Node vehicle_nodes, Mesh wheel) {
        Matrix4f root = new Matrix4f();
        root.setLocation(spawn.x, spawn.y, spawn.z);
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
            plVehiclef(PL_FRICTION_SLIP, 10);

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

                plWheelf(i, PL_WHEEL_ROLL_INFLUENCE, 0.1f);
                wheels.add(new ModelObject(wheel));
            }

            // TODO: support PL_RESET_SUSPENSION
            plBindVehicle(0);
        }
    }

    private static final Matrix3f MODEL_TO_PHYSICS = Quaternion.fromEulerAngles(-90, 180, 0).toMatrix();
    private Matrix3f door_base_rotation;

    // abre la puerta del conductor girandola sobre su bisagra (eje vertical del modelo); 0 = cerrada
    public void setDriverDoorAngle(Node nodes, float degrees) {
        Node door = nodes.getNode("door_lf_dummy");
        if(door_base_rotation == null) {
            door_base_rotation = new Matrix3f().set(door.rotation_matrix);
        }
        door.rotation_matrix = new Matrix3f().set(door_base_rotation).multLocal(Quaternion.fromEulerAngles(0, 0, degrees).toMatrix());
        door.notifyUpdate();
    }

    // asiento del conductor (entre la puerta y el centro del auto) y direccion hacia donde avanza el auto
    public Vector3f getDriverSeat(Node nodes, Vector3f forward) {
        Vector3f door = MODEL_TO_PHYSICS.mult(nodes.getNode("door_lf_dummy").position);
        Vector3f seat = vehicle_physics_matrix.mult(new Vector3f(door.x * 0.5f, door.y, door.z));

        // el frente es hacia donde estan las ruedas delanteras respecto a las traseras
        Vector3f front = MODEL_TO_PHYSICS.mult(nodes.getNode("wheel_rf_dummy").position);
        Vector3f rear = MODEL_TO_PHYSICS.mult(nodes.getNode("wheel_rb_dummy").position);
        Vector3f front_world = vehicle_physics_matrix.mult(front);
        Vector3f rear_world = vehicle_physics_matrix.mult(rear);
        forward.set(front_world.x - rear_world.x, 0.0f, front_world.z - rear_world.z);
        forward.normalize();

        seat.y = getChassisHeight() - 0.2f;
        return seat;
    }

    public float getChassisHeight() {
        return vehicle_physics_matrix.getLocation(null).y;
    }

    // posicion junto a la puerta del conductor (izquierda delantera) a la altura y, con facing apuntando hacia el auto
    public Vector3f getDriverDoorPosition(Node nodes, float y, Vector3f facing) {
        // misma orientacion que usa el render del modelo (ver update): rotacion del frame raiz antes de la matriz fisica
        Vector3f door = MODEL_TO_PHYSICS.mult(nodes.getNode("door_lf_dummy").position);
        float side = door.x < 0.0f ? -1.0f : 1.0f;
        Vector3f outside = vehicle_physics_matrix.mult(new Vector3f(door.x + side * 0.9f, door.y, door.z));
        Vector3f inside = vehicle_physics_matrix.mult(door);
        facing.set(inside.x - outside.x, 0.0f, inside.z - outside.z);
        facing.normalize();
        outside.y = y;
        return outside;
    }

    /** Llamar cuando el jugador mueve la camara a mano: pausa el recolocado automatico. */
    public void onCameraInput() {
        camera_idle = 0.0f;
    }

    // lleva un angulo en grados al rango [-180, 180)
    private static float wrapDegrees(float degrees) {
        return ((degrees % 360.0f) + 540.0f) % 360.0f - 180.0f;
    }

    public void update(Camera camera, float delta, Node root) {
        // handle follow camera
        if (onVehicle) {
            Vector3f chassis_pos = vehicle_physics_matrix.getLocation(null);
            float speed = camera_ready && delta > 0.0f ? chassis_pos.distance(last_chassis_pos) / delta : 0.0f;
            last_chassis_pos.set(chassis_pos);

            // rumbo actual del auto: hacia donde apunta su eje z local, proyectado al plano horizontal
            Vector3f forward = vehicle_physics_matrix.getUpperLeft().mult(forward_tmp.set(0, 0, 1));
            float heading = (float) Math.toDegrees(Math.atan2(forward.x, forward.z));
            if (!camera_ready) {
                // al subir, la camara empieza ya detras del auto
                camera_heading = heading;
                camera_ready = true;
                camera_idle = CAMERA_RECENTER_DELAY;
            }

            // el rumbo de la camara persigue al del auto, mas lento si esta casi detenido
            float heading_diff = wrapDegrees(heading - camera_heading);
            float heading_rate = CAMERA_HEADING_SMOOTH * Math.max(0.15f, Math.min(1.0f, speed / 3.0f));
            camera_heading += heading_diff * Math.min(1.0f, heading_rate * delta);

            // sin tocar la camara y en marcha, vuelve a ponerse detras
            camera_idle += delta;
            rot_y = Math.max(CAMERA_MIN_PITCH, Math.min(CAMERA_MAX_PITCH, rot_y));
            if (camera_idle > CAMERA_RECENTER_DELAY && speed > CAMERA_RECENTER_MIN_SPEED) {
                float blend = Math.min(1.0f, CAMERA_RECENTER_SMOOTH * delta);
                rot_x += wrapDegrees(180.0f - rot_x) * blend;
                rot_y += (CAMERA_DEFAULT_PITCH - rot_y) * blend;
            }

            Vector3f local_follow = follow_fixed.rotateOnSphereOrigin(distance, camera_heading + rot_x, rot_y, false);
            // los impactos dentro del radio del auto son el propio chasis y no cuentan
            Vector3f follow = camera_collision.resolve(
                    chassis_pos.add(new Vector3f(0, 1.0f, 0)), chassis_pos.add(local_follow), CAMERA_SELF_RADIUS, delta);
            camera.setPosition(follow.x, follow.y, follow.z);
            camera.lookAt(chassis_pos);
        } else {
            camera_ready = false;
        }

        plGetRigidBodyfv(chassis_body, PL_MOTION_STATE_TRANSFORM, vehicle_physics_matrix.data, 16);

        root.rotation_matrix = vehicle_physics_matrix.getUpperLeft().multLocal(Quaternion.fromEulerAngles(-90, 180, 0).toMatrix());
        root.position = vehicle_physics_matrix.getLocation(null);
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
