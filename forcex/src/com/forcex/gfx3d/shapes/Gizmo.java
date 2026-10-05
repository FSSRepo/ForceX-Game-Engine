package com.forcex.gfx3d.shapes;

import com.forcex.core.GL;
import com.forcex.gfx3d.Mesh;
import com.forcex.gfx3d.MeshPart;
import com.forcex.gfx3d.ModelObject;
import com.forcex.math.Maths;
import com.forcex.math.Ray;
import com.forcex.math.Vector3f;

public class Gizmo extends ModelObject {
    public static final int TRANSLATION = 0;
    public static final int ROTATION = 1;

    private static final int CIRCLE_SEGMENTS = 32;

    private ModelObject target;
    private int updateType = TRANSLATION;
    private float axisLength = 1.5f;
    private float hitThreshold = 0.15f;

    private int selectedAxis = -1;
    private Vector3f lastPoint = new Vector3f();
    private boolean dragging = false;

    private MeshPart transX, transY, transZ;
    private MeshPart rotX, rotY, rotZ;

    public Gizmo() {
        super(createMesh());
        updateVisibility();
    }

    private static Mesh createMesh() {
        Mesh mesh = new Mesh(true);
        mesh.lineSize = 3f;
        mesh.setPrimitiveType(GL.GL_LINES);

        float s = 1.0f;
        float r = 1.0f;
        int vCount = 6 + CIRCLE_SEGMENTS * 3;
        float[] vertices = new float[vCount * 3];
        int idx = 0;

        // Translation axes
        // X axis
        vertices[idx++] = 0; vertices[idx++] = 0; vertices[idx++] = 0;
        vertices[idx++] = s; vertices[idx++] = 0; vertices[idx++] = 0;
        // Y axis
        vertices[idx++] = 0; vertices[idx++] = 0; vertices[idx++] = 0;
        vertices[idx++] = 0; vertices[idx++] = s; vertices[idx++] = 0;
        // Z axis
        vertices[idx++] = 0; vertices[idx++] = 0; vertices[idx++] = 0;
        vertices[idx++] = 0; vertices[idx++] = 0; vertices[idx++] = s;

        // Rotation circles
        // X circle (plane YZ)
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            float angle = (Maths.PI_2 / CIRCLE_SEGMENTS) * i;
            vertices[idx++] = 0;
            vertices[idx++] = r * Maths.cos(angle);
            vertices[idx++] = r * Maths.sin(angle);
        }
        // Y circle (plane XZ)
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            float angle = (Maths.PI_2 / CIRCLE_SEGMENTS) * i;
            vertices[idx++] = r * Maths.cos(angle);
            vertices[idx++] = 0;
            vertices[idx++] = r * Maths.sin(angle);
        }
        // Z circle (plane XY)
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            float angle = (Maths.PI_2 / CIRCLE_SEGMENTS) * i;
            vertices[idx++] = r * Maths.cos(angle);
            vertices[idx++] = r * Maths.sin(angle);
            vertices[idx++] = 0;
        }

        mesh.setVertices(vertices);

        // Translation parts
        MeshPart xp = new MeshPart(new short[]{0, 1});
        xp.material.color.set(255, 0, 0);
        MeshPart yp = new MeshPart(new short[]{2, 3});
        yp.material.color.set(0, 255, 0);
        MeshPart zp = new MeshPart(new short[]{4, 5});
        zp.material.color.set(0, 0, 255);
        mesh.addPart(xp);
        mesh.addPart(yp);
        mesh.addPart(zp);

        // Rotation parts
        short[] rx = new short[CIRCLE_SEGMENTS * 2];
        int off = 6;
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            rx[i * 2] = (short)(off + i);
            rx[i * 2 + 1] = (short)(off + ((i + 1) % CIRCLE_SEGMENTS));
        }
        MeshPart rxp = new MeshPart(rx);
        rxp.material.color.set(255, 0, 0);
        mesh.addPart(rxp);

        off += CIRCLE_SEGMENTS;
        short[] ry = new short[CIRCLE_SEGMENTS * 2];
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            ry[i * 2] = (short)(off + i);
            ry[i * 2 + 1] = (short)(off + ((i + 1) % CIRCLE_SEGMENTS));
        }
        MeshPart ryp = new MeshPart(ry);
        ryp.material.color.set(0, 255, 0);
        mesh.addPart(ryp);

        off += CIRCLE_SEGMENTS;
        short[] rz = new short[CIRCLE_SEGMENTS * 2];
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            rz[i * 2] = (short)(off + i);
            rz[i * 2 + 1] = (short)(off + ((i + 1) % CIRCLE_SEGMENTS));
        }
        MeshPart rzp = new MeshPart(rz);
        rzp.material.color.set(0, 0, 255);
        mesh.addPart(rzp);

        return mesh;
    }

    public void link(ModelObject target) {
        this.target = target;
        syncPosition();
    }

    public void unlink() {
        this.target = null;
    }

    public ModelObject getTarget() {
        return target;
    }

    public void setUpdateType(int type) {
        this.updateType = type;
        updateVisibility();
    }

    public int getUpdateType() {
        return updateType;
    }

    private void updateVisibility() {
        if (transX == null) {
            transX = getMesh().getPart(0);
            transY = getMesh().getPart(1);
            transZ = getMesh().getPart(2);
            rotX = getMesh().getPart(3);
            rotY = getMesh().getPart(4);
            rotZ = getMesh().getPart(5);
        }
        boolean t = updateType == TRANSLATION;
        transX.visible = t;
        transY.visible = t;
        transZ.visible = t;
        rotX.visible = !t;
        rotY.visible = !t;
        rotZ.visible = !t;
    }

    public void syncPosition() {
        if (target != null) {
            setPosition(target.getPosition());
        }
    }

    public int onTouch(Ray ray) {
        selectedAxis = -1;
        if (!isVisible() || target == null) {
            return -1;
        }
        Vector3f pos = getPosition();
        float closest = hitThreshold * hitThreshold;

        if (updateType == TRANSLATION) {
            float d = distRaySegmentSq(ray, pos, new Vector3f(pos.x + axisLength, pos.y, pos.z));
            if (d < closest) { closest = d; selectedAxis = 0; }
            d = distRaySegmentSq(ray, pos, new Vector3f(pos.x, pos.y + axisLength, pos.z));
            if (d < closest) { closest = d; selectedAxis = 1; }
            d = distRaySegmentSq(ray, pos, new Vector3f(pos.x, pos.y, pos.z + axisLength));
            if (d < closest) { closest = d; selectedAxis = 2; }
        } else {
            float d = distRayCircle(ray, pos, new Vector3f(1, 0, 0));
            if (d < closest) { closest = d; selectedAxis = 0; }
            d = distRayCircle(ray, pos, new Vector3f(0, 1, 0));
            if (d < closest) { closest = d; selectedAxis = 1; }
            d = distRayCircle(ray, pos, new Vector3f(0, 0, 1));
            if (d < closest) { closest = d; selectedAxis = 2; }
        }

        if (selectedAxis != -1) {
            lastPoint.set(getRayPlaneIntersection(ray, selectedAxis, pos));
            dragging = true;
        }
        return selectedAxis;
    }

    public void onDrag(Ray ray) {
        if (target == null || selectedAxis == -1 || !dragging) {
            return;
        }
        Vector3f pos = getPosition();
        Vector3f current = getRayPlaneIntersection(ray, selectedAxis, pos);

        if (updateType == TRANSLATION) {
            Vector3f delta = current.sub(lastPoint);
            Vector3f tPos = target.getPosition();
            if (selectedAxis == 0) tPos.x += delta.x;
            else if (selectedAxis == 1) tPos.y += delta.y;
            else if (selectedAxis == 2) tPos.z += delta.z;
            target.setPosition(tPos);
        } else {
            Vector3f toLast = lastPoint.sub(pos);
            Vector3f toCurrent = current.sub(pos);
            if (selectedAxis == 0) {
                toLast.x = 0;
                toCurrent.x = 0;
            } else if (selectedAxis == 1) {
                toLast.y = 0;
                toCurrent.y = 0;
            } else {
                toLast.z = 0;
                toCurrent.z = 0;
            }
            float angle = Maths.atan2(toCurrent.cross(toLast).length(), toCurrent.dot(toLast));
            if (toCurrent.cross(toLast).dot(getAxisNormal(selectedAxis)) < 0) {
                angle = -angle;
            }
            target.applyRotationAxis(getAxisNormal(selectedAxis), angle * Maths.toDegrees);
        }

        lastPoint.set(current);
        syncPosition();
    }

    public void onRelease() {
        selectedAxis = -1;
        dragging = false;
    }

    public int getSelectedAxis() {
        return selectedAxis;
    }

    public boolean isDragging() {
        return dragging;
    }

    private Vector3f getAxisNormal(int axis) {
        if (axis == 0) return new Vector3f(1, 0, 0);
        if (axis == 1) return new Vector3f(0, 1, 0);
        return new Vector3f(0, 0, 1);
    }

    private Vector3f getRayPlaneIntersection(Ray ray, int axis, Vector3f planePos) {
        Vector3f normal = getAxisNormal(axis);
        float denom = ray.direction.dot(normal);
        if (Math.abs(denom) > 0.0001f) {
            float t = planePos.sub(ray.origin).dot(normal) / denom;
            if (t >= 0) {
                return ray.origin.add(ray.direction.mult(t));
            }
        }
        return new Vector3f(planePos);
    }

    private float distRaySegmentSq(Ray ray, Vector3f segStart, Vector3f segEnd) {
        Vector3f ab = segEnd.sub(segStart);
        Vector3f ao = ray.origin.sub(segStart);
        float abLenSq = ab.dot(ab);
        float t = abLenSq > 0.0001f ? ao.dot(ab) / abLenSq : 0f;
        t = Math.max(0f, Math.min(1f, t));
        Vector3f closest = segStart.add(ab.mult(t));
        Vector3f diff = closest.sub(ray.origin);
        Vector3f cross = ray.direction.cross(diff);
        float dirLen = ray.direction.length();
        float dist = cross.length() / (dirLen > 0.0001f ? dirLen : 1f);
        return dist * dist;
    }

    private float distRayCircle(Ray ray, Vector3f center, Vector3f normal) {
        float denom = ray.direction.dot(normal);
        if (Math.abs(denom) < 0.0001f) return Float.MAX_VALUE;
        float t = center.sub(ray.origin).dot(normal) / denom;
        if (t < 0) return Float.MAX_VALUE;
        Vector3f point = ray.origin.add(ray.direction.mult(t));
        float distFromCenter = point.distance(center);
        return Math.abs(distFromCenter - axisLength);
    }
}
