package com.forcex.math;

public class Vector4f {

    public float x, y, z, w;

    public Vector4f() {
        x = 0;
        y = 0;
        z = 0;
    }

    public Vector4f(float ix, float iy, float iz, float iw) {
        x = ix;
        y = iy;
        z = iz;
        w = iw;
    }

    public Vector4f(Vector3f v, float iw) {
        x = v.x;
        y = v.y;
        z = v.z;
        w = iw;
    }

    public Vector4f(float[] array, int offset, float w) {
        x = array[offset];
        y = array[offset + 1];
        z = array[offset + 2];
        this.w = w;
    }

    public Vector4f(Vector4f v) {
        x = v.x;
        y = v.y;
        z = v.z;
        w = v.w;
    }

    public Vector3f xyz() {
        return new Vector3f(x, y, z);
    }

    public Vector4f div(float i) {
        x /= i;
        y /= i;
        z /= i;
        w /= i;
        return this;
    }

    public Vector3f negative() {
        return new Vector3f(-x, -y, -z);
    }

    public Vector4f set(float sx, float sy, float sz, float sw) {
        x = sx;
        y = sy;
        z = sz;
        w = sw;
        return this;
    }

    public Vector4f set(Vector3f v3, float sw) {
        x = v3.x;
        y = v3.y;
        z = v3.z;
        w = sw;
        return this;
    }

    public Vector4f set(Vector4f v) {
        x = v.x;
        y = v.y;
        z = v.z;
        w = v.w;
        return this;
    }

    public String toString() {
        return "[" + String.format("%.4f", x) + " | " + String.format("%.4f", y) + " | " + String.format("%.4f", z) + " | " + String.format("%.4f", w) + "]";
    }

    public float length() {
        return Maths.sqrt(x * x + y * y + z * z);
    }

    public float normalize() {
        float invLen = 1f / length();
        multLocal(invLen);
        return invLen;
    }

    public Vector4f mult(float f) {
        return new Vector4f(x * f, y * f, z * f, w * f);
    }

    public Vector4f multLocal(float f) {
        x *= f;
        y *= f;
        z *= f;
        w *= f;
        return this;
    }

    public Vector4f addLocal(Vector3f v) {
        x += v.x;
        y += v.y;
        z += v.z;
        return this;
    }

    public float distanceTo(Vector3f point) {
        return point.dot(x, y, z) + w;
    }

    public Vector3f cross(Vector3f v) {
        return new Vector3f(
                y * v.z - z * v.y,
                z * v.x - x * v.z,
                x * v.y - y * v.x
        );
    }

    public Vector4f crossLocal(Vector3f v) {
        Vector3f tmp = new Vector3f(x, y, z);
        x = (tmp.y * v.z - tmp.z * v.y);
        y = (tmp.z * v.x - tmp.x * v.z);
        z = (tmp.x * v.y - tmp.y * v.x);
        return this;
    }
}
