package com.forcex.utils;

import com.forcex.math.Matrix4f;
import com.forcex.math.Vector3f;
import com.forcex.math.Vector4f;

public class Frustum {
    Vector4f[] planes = new Vector4f[6];
    Vector3f pt1 = new Vector3f();
    Vector3f pt2 = new Vector3f();

    public Frustum() {
        for (byte i = 5; i >= 0; i--) {
            planes[i] = new Vector4f();
        }
    }

    public void update(Matrix4f clipMatrix) {
        float[] clip = clipMatrix.data;
        planes[0].set(clip[3] - clip[0], clip[7] - clip[4], clip[11] - clip[8], clip[15] - clip[12]);
        planes[0].normalize();

        planes[1].set(clip[3] + clip[0], clip[7] + clip[4], clip[11] + clip[8], clip[15] + clip[12]);
        planes[1].normalize();

        planes[2].set(clip[3] + clip[1], clip[7] + clip[5], clip[11] + clip[9], clip[15] + clip[13]);
        planes[2].normalize();

        planes[3].set(clip[3] - clip[1], clip[7] - clip[5], clip[11] - clip[9], clip[15] - clip[13]);
        planes[3].normalize();

        planes[4].set(clip[3] - clip[2], clip[7] - clip[6], clip[11] - clip[10], clip[15] - clip[14]);
        planes[4].normalize();

        planes[5].set(clip[3] + clip[2], clip[7] + clip[6], clip[11] + clip[10], clip[15] + clip[14]);
        planes[5].normalize();
    }

    public boolean isBoxInside(Vector3f min, Vector3f max) {
        for (byte i = 5; i >= 0; i--) {
            Vector4f p = planes[i];
            boolean ux = p.x > 0, uy = p.y > 0, uz = p.z > 0;
            pt1.set(ux ? max.x : min.x, uy ? max.y : min.y, uz ? max.z : min.z);
            pt2.set(ux ? min.x : max.x, uy ? min.y : max.y, uz ? min.z : max.z);
            if (p.distanceTo(pt1) < 0 && p.distanceTo(pt2) < 0) {
                return false;
            }
        }
        return true;
    }

    public boolean isSphereInside(Vector3f center, float radius) {
        for (byte i = 5; i >= 0; i--) {
            if (planes[i].distanceTo(center) < -radius) {
                return false;
            }
        }
        return true;
    }

    public boolean isPointInside(Vector3f point) {
        for (byte i = 5; i >= 0; i--) {
            if (planes[i].distanceTo(point) < 0) {
                return false;
            }
        }
        return true;
    }
}
