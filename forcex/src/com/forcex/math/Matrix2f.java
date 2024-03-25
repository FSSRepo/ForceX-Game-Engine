package com.forcex.math;

public class Matrix2f {
    public float[] data = new float[4];

    public Matrix2f(
            float m00, float m10,
            float m01, float m11) {
        this.data[0] = m00;
        this.data[1] = m01;
        this.data[2] = m10;
        this.data[3] = m11;
    }

    public Matrix2f() {
        this.data[0] = 1.0f;
        this.data[1] = 0;
        this.data[2] = 0;
        this.data[3] = 1.0f;
    }

    public Matrix2f addLocal(Matrix2f m) {
        for (int i = 0; i < 4; i++) {
            data[i] += m.data[i];
        }
        return this;
    }

    public Matrix2f subLocal(Matrix2f m) {
        for (byte i = 0; i < 4; i++) {
            data[i] -= m.data[i];
        }
        return this;
    }

    public Matrix2f multLocal(float f) {
        for (byte i = 0; i < 4; i++) {
            data[i] *= f;
        }
        return this;
    }

    public Vector2f mult(Vector2f v) {
        return new Vector2f(
                (this.data[0] * v.x) + (this.data[2] * v.y),
                (this.data[1] * v.x) + (this.data[3] * v.y));
    }

    public Matrix2f mult(Matrix2f m) {
        Matrix2f dest = new Matrix2f();
        dest.data[0] = data[0] * m.data[0] + data[1] * m.data[2];
        dest.data[1] = data[0] * m.data[1] + data[1] * m.data[3];
        dest.data[2] = data[2] * m.data[0] + data[3] * m.data[2];
        dest.data[3] = data[2] * m.data[1] + data[3] * m.data[3];
        return dest;
    }

    public Matrix2f setTransform(float angle, float x, float y) {
        float a = (angle * Maths.toRadians);
        float cs = Maths.cos(a);
        float sn = Maths.sin(a);
        data[0] = cs * x;
        data[1] = sn * x;
        data[2] = -sn * y;
        data[3] = cs * y;
        return this;
    }

    public void setScale(float x, float y) {
        this.data[0] = x;
        this.data[1] = 0;
        this.data[2] = 0;
        this.data[3] = y;
    }

    public Matrix2f setRotation(float angle) {
        float a = (angle * Maths.toRadians);
        float cs = Maths.cos(a);
        float sn = Maths.sin(a);
        data[0] = cs;
        data[1] = sn;
        data[2] = -sn;
        data[3] = cs;
        return this;
    }

    public Matrix2f inverse() {
        float det = (data[0] * data[3] - data[1] * data[2]);
        if (det == 0) {
            return this;
        }
        float inv = 1 / det;
        Matrix2f m = new Matrix2f();
        m.data[0] = (data[3] * inv);
        m.data[1] = -(data[1] * inv);
        m.data[2] = -(data[2] * inv);
        m.data[3] = (data[0] * inv);
        return m;
    }

    @Override
    public String toString() {
        String debug = "[";
        for (byte i = 0; i < 4; i += 2) {
            debug += String.format("%.3f | %.3f", data[i], data[i + 1]) + (i == 2 ? "" : "\n");
        }
        debug += "]";
        return debug;
    }
}
