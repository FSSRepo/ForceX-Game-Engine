package com.forcex.gfx3d;

import com.forcex.FX;
import com.forcex.app.EventType;
import com.forcex.math.Matrix4f;
import com.forcex.math.Quaternion;
import com.forcex.math.Ray;
import com.forcex.math.Vector3f;
import com.forcex.math.Vector4f;

public class Camera {
    private Matrix4f viewMatrix = new Matrix4f();
    private Matrix4f ProjViewMatrix = new Matrix4f();
    private Matrix4f projectMatrix = new Matrix4f();

    public Vector3f position = new Vector3f(0, 0, 4);
    public Vector3f direction = new Vector3f(0, 0, -1);
    public Vector3f up = new Vector3f(0, 1, 0);

    float fov = 60, aspectRatio, near = 0.1f, far = 1000f;
    boolean update = true;
    Vector4f plane_ext;
    boolean use_up_z;

    // orbit camera, stateless with inertia
    public Vector3f orbit_point = new Vector3f();
    private float orbitInertiaX = 0, orbitInertiaY = 0;
    private boolean orbitDragging = false;
    private float orbitDamping = 0.9f;
    private static final float ORBIT_INERTIA_THRESHOLD = 0.001f;

    public enum ProjectionType {
        ORTHOGRAPHIC,
        PERSPECTIVE
    }

    private ProjectionType type;

    public Camera(float aspectRatio) {
        this.aspectRatio = aspectRatio;
        type = ProjectionType.PERSPECTIVE;
    }

    public Camera() {
        this((float) FX.gpu.getWidth() / FX.gpu.getHeight());
    }

    public void setProjectionType(ProjectionType type) {
        this.type = type;
        if (type == ProjectionType.ORTHOGRAPHIC) {
            plane_ext = new Vector4f(-1, 1, -1, 1);
        }
        update = true;
    }

    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

    public Matrix4f getProjViewMatrix() {
        return ProjViewMatrix;
    }

    public Matrix4f getProjectionMatrix() {
        return projectMatrix;
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
    }

    public void setOrthoExtent(float left, float right, float bottom, float top) {
        plane_ext.set(left, right, bottom, top);
        update = true;
    }

    public void setNearFar(float near, float far) {
        this.near = near;
        this.far = far;
        update = true;
    }

    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
        update = true;
    }

    public void setFieldOfView(float fov) {
        this.fov = fov;
        update = true;
    }

    public void move(float x, float y, float z) {
        Vector3f right = up.cross(direction).normalize();
        position.addLocal(right.mult(x));
        position.addLocal(direction.mult(y));
        position.addLocal(up.mult(z));
    }

    public void rotate(float x, float y) {
        direction.rotY(y);
        up.rotY(y);
        // Robust pitch axis: world-up cross direction, avoids gimbal lock.
        Vector3f worldUp = use_up_z ? new Vector3f(0, 0, 1) : new Vector3f(0, 1, 0);
        Vector3f side = worldUp.cross(direction);
        float sideLen = side.length();
        if (sideLen < 1e-3f) {
            side.set(1, 0, 0);
        } else {
            side.multLocal(1f / sideLen);
        }
        direction.multLocal(Matrix4f.setRotation(new Matrix4f(), x, side));
        up.set(direction).crossLocal(side);
        direction.normalize();
        up.normalize();
    }

    public void zoom(float amount) {
        position.addLocal(direction.mult(amount));
    }

    public void update() {
        if (update) {
            if (type == ProjectionType.PERSPECTIVE) {
                projectMatrix.setPerspective(fov, aspectRatio, near, far);
            } else {
                projectMatrix.setOrthogonal(plane_ext.x, plane_ext.y, plane_ext.z, plane_ext.w, near, far);
            }
            update = false;
        }

        if (!orbitDragging && (Math.abs(orbitInertiaX) > ORBIT_INERTIA_THRESHOLD || Math.abs(orbitInertiaY) > ORBIT_INERTIA_THRESHOLD)) {
            orbit(orbitInertiaX, orbitInertiaY);
            orbitInertiaX *= orbitDamping;
            orbitInertiaY *= orbitDamping;
            if (Math.abs(orbitInertiaX) < ORBIT_INERTIA_THRESHOLD) {
                orbitInertiaX = 0;
            }
            if (Math.abs(orbitInertiaY) < ORBIT_INERTIA_THRESHOLD) {
                orbitInertiaY = 0;
            }
        }
        projectMatrix.mult(ProjViewMatrix, viewMatrix.setlookAt(position, direction, up));
    }

    public void lookAt(float x, float y, float z) {
        lookAt(direction.set(x, y, z));
    }

    public void lookAt(Vector3f point) {
        direction.set(point).subLocal(position).normalize();
        Vector3f right = Vector3f.getUpFromDirection(direction, use_up_z).cross(direction).normalize();
        up.set(direction).crossLocal(right).normalize();
    }

    public Vector3f right() {
        return up.cross(direction).normalize();
    }

    public void set(Vector3f position, Quaternion rotation) {
        this.position.set(position);
        direction.set(rotation.getDirection());
        up.set(rotation.getUp());
    }

    public void setOrientation(Quaternion rot) {
        direction.set(rot.getDirection());
        up.set(rot.getUp());
    }

    public Ray getPickRay(float x, float y) {
        Ray ray = new Ray();
        Matrix4f InversePV = ProjViewMatrix.invert();
        ray.origin = unproject(InversePV, new Vector3f(x, y, 0));
        Vector3f rayDirection = unproject(InversePV, new Vector3f(x, y, 1));
        ray.direction = rayDirection.subLocal(ray.origin).normalize();
        return ray;
    }

    private Vector3f unproject(Matrix4f InversePV, Vector3f v) {
        v.z = 2 * v.z - 1;
        return v.project(InversePV);
    }

    public void setInputType(byte type) {
        if (type == EventType.TOUCH_PRESSED) {
            orbitDragging = true;
            orbitInertiaX = 0;
            orbitInertiaY = 0;
        } else if (type == EventType.TOUCH_DROPPED) {
            orbitDragging = false;
        }
    }

    public void orbit(float x, float y) {
        orbit(orbit_point, x, y);
    }

    public void orbit(Vector3f point, float x, float y) {
        Vector3f offset = position.sub(point);
        Vector3f worldUp = use_up_z ? new Vector3f(0, 0, 1) : new Vector3f(0, 1, 0);

        // Yaw: rotate offset around world up axis.
        if (Math.abs(y) > 1e-5f) {
            Quaternion yaw = Quaternion.fromAxisAngle(worldUp, y);
            offset.multLocal(yaw.toMatrix());
        }

        // Pitch: rotate offset around local side axis (perpendicular to offset and world up).
        // This avoids gimbal lock because each rotation is an incremental quaternion.
        if (Math.abs(x) > 1e-5f) {
            Vector3f side = worldUp.cross(offset);
            float sideLen = side.length();
            if (sideLen < 1e-3f) {
                side.set(1, 0, 0);
            } else {
                side.multLocal(1f / sideLen);
            }
            Quaternion pitch = Quaternion.fromAxisAngle(side, x);
            offset.multLocal(pitch.toMatrix());
        }

        position.set(point).addLocal(offset);
        if (Float.isNaN(position.x) || Float.isNaN(position.y) || Float.isNaN(position.z)) {
            position.set(point).addLocal(0, use_up_z ? 0 : 4, use_up_z ? 4 : 0);
        }
        lookAt(point);
        if (orbitDragging) {
            orbitInertiaX = x;
            orbitInertiaY = y;
        }
    }

    public void setOrbitDamping(float damping) {
        orbitDamping = Math.max(0, Math.min(damping, 0.99f));
    }

    public float getOrbitDamping() {
        return orbitDamping;
    }

    public void setUseZUp(boolean z) {
        this.use_up_z = z;
    }

    public void delete() {
        direction = null;
        up = null;
        position = null;
        projectMatrix = null;
        viewMatrix = null;
        ProjViewMatrix = null;
    }

    public void setDirection(int dir) {
        switch (dir) {
            case DIRECTION_RIGHT:
                direction.set(1, 0, 0);
                break;
            case DIRECTION_LEFT:
                direction.set(-1, 0, 0);
                break;
            case DIRECTION_FRONT:
                if (!use_up_z) {
                    direction.set(0, 0, 1);
                } else {
                    direction.set(0, 1, 0);
                }
                break;
            case DIRECTION_BACK:
                if (!use_up_z) {
                    direction.set(0, 0, -1);
                } else {
                    direction.set(0, -1, 0);
                }
                break;
        }
        if (!use_up_z) {
            if (dir == DIRECTION_TOP) {
                direction.set(0, 1, 0);
                up.set(0, 0, -1);
            } else if (dir == DIRECTION_BOTTOM) {
                direction.set(0, -1, 0);
                up.set(0, 0, 1);
            } else {
                up.set(0, 1, 0);
            }
        } else {
            if (dir == DIRECTION_TOP) {
                direction.set(0, 0, 1);
                up.set(0, -1, 0);
            } else if (dir == DIRECTION_BOTTOM) {
                direction.set(0, 0, -1);
                up.set(0, 1, 0);
            } else {
                up.set(0, 0, 1);
            }
        }
    }

    public static final byte DIRECTION_RIGHT = 0;
    public static final byte DIRECTION_LEFT = 1;
    public static final byte DIRECTION_TOP = 2;
    public static final byte DIRECTION_BOTTOM = 3;
    public static final byte DIRECTION_FRONT = 4;
    public static final byte DIRECTION_BACK = 5;
}
