package com.forcex.math;

public class Matrix3f {
    public static final int M00 = 0;
    public static final int M01 = 3;
    public static final int M02 = 6;
    public static final int M10 = 1;
    public static final int M11 = 4;
    public static final int M12 = 7;
    public static final int M20 = 2;
    public static final int M21 = 5;
    public static final int M22 = 8;
    public float[] data = new float[9];

    public Matrix3f(
            float m00, float m10, float m20,
            float m01, float m11, float m21,
            float m02, float m12, float m22) {
        data[M00] = m00;
        data[M10] = m10;
        data[M20] = m20;
        data[M01] = m01;
        data[M11] = m11;
        data[M21] = m21;
        data[M02] = m02;
        data[M12] = m12;
        data[M22] = m22;
    }

    public Matrix3f(Vector3f right, Vector3f up, Vector3f at) {
        this(right.x, up.x, at.x,
                right.y, up.y, at.y,
                right.z, up.z, at.z);
    }

    public Matrix3f() {
        identity();
    }

    public void identity() {
        for (byte i = 0; i < 9; i++) {
            data[i] = 0.0f;
        }
        data[M00] = 1.0f;
        data[M11] = 1.0f;
        data[M22] = 1.0f;
    }

    public float getRowColumn(int r, int c) {
        return data[(c * 3) + (r)];
    }

    public float get(int l) {
        return data[l];
    }

    public Matrix3f addLocal(Matrix3f other) {
        for (byte i = 0; i < 9; i++) {
            data[i] += other.data[i];
        }
        return this;
    }

    public Matrix3f subLocal(Matrix3f other) {
        for (byte i = 0; i < 9; i++) {
            data[i] -= other.data[i];
        }
        return this;
    }

    public Matrix3f multLocal(float scalar) {
        for (byte i = 0; i < 9; i++) {
            data[i] *= scalar;
        }
        return this;
    }

    public Vector3f column(int i) {
        return new Vector3f(data[i * 3], data[1 + i * 3], data[2 + i * 3]);

    }

    public Vector3f row(int i) {
        return new Vector3f(data[i], data[3 + i], data[6 + i]);
    }

    public Matrix3f multLocal(Matrix3f other) {
        Vector3f r0 = row(0);
        Vector3f r1 = row(1);
        Vector3f r2 = row(2);

        Vector3f c0 = other.column(0);
        Vector3f c1 = other.column(1);
        Vector3f c2 = other.column(2);

        data[0] = r0.dot(c0);
        data[1] = r1.dot(c0);
        data[2] = r2.dot(c0);
        data[3] = r0.dot(c1);
        data[4] = r1.dot(c1);
        data[5] = r2.dot(c1);
        data[6] = r0.dot(c2);
        data[7] = r1.dot(c2);
        data[8] = r2.dot(c2);
        return this;
    }

    public Vector3f mult(Vector3f vec) {
        Vector3f o = new Vector3f();
        o.x = data[M00] * vec.x + data[M01] * vec.y + data[M02] * vec.z;
        o.y = data[M10] * vec.x + data[M11] * vec.y + data[M12] * vec.z;
        o.z = data[M20] * vec.x + data[M21] * vec.y + data[M22] * vec.z;
        return o;
    }

    public Matrix3f mult(float scalar) {
        Matrix3f m = new Matrix3f();
        for (byte i = 0; i < 9; i++) {
            m.data[i] = data[i] * scalar;
        }
        return m;
    }

    public Matrix3f set(Matrix3f m) {
        for (byte i = 0; i < 9; i++) {
            data[i] = m.data[i];
        }
        return this;
    }

    public Matrix3f transpose() {
        float v01 = data[M10];
        float v02 = data[M20];
        float v10 = data[M01];
        float v12 = data[M21];
        float v20 = data[M02];
        float v21 = data[M12];
        data[M01] = v01;
        data[M02] = v02;
        data[M10] = v10;
        data[M12] = v12;
        data[M20] = v20;
        data[M21] = v21;
        return this;
    }

    public Vector3f getRight(Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        store.set(data[M00], data[M01], data[M02]);
        return store;
    }

    public Vector3f getUp(Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        store.set(data[M10], data[M11], data[M12]);
        return store;
    }

    public Vector3f getAt(Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        store.set(data[M20], data[M21], data[M22]);
        return store;
    }

    public Matrix3f setScale(float x, float y, float z) {
        multLocal(new Matrix3f(
                x, 0, 0,
                0, y, 0,
                0, 0, z));
        return this;
    }

    public Matrix3f invert() {
        float det =
                data[M00] * data[M11] * data[M22] +
                        data[M01] * data[M12] * data[M20] +
                        data[M02] * data[M10] * data[M21] -
                        data[M00] * data[M12] * data[M21] -
                        data[M01] * data[M10] * data[M22] -
                        data[M02] * data[M11] * data[M20];

        if (det == 0) return this;

        float inv_det = 1.0f / det;
        float[] tmp = new float[9];
        tmp[M00] = data[M11] * data[M22] - data[M21] * data[M12];
        tmp[M10] = data[M20] * data[M12] - data[M10] * data[M22];
        tmp[M20] = data[M10] * data[M21] - data[M20] * data[M11];
        tmp[M01] = data[M21] * data[M02] - data[M01] * data[M22];
        tmp[M11] = data[M00] * data[M22] - data[M20] * data[M02];
        tmp[M21] = data[M20] * data[M01] - data[M00] * data[M21];
        tmp[M02] = data[M01] * data[M12] - data[M11] * data[M02];
        tmp[M12] = data[M10] * data[M02] - data[M00] * data[M12];
        tmp[M22] = data[M00] * data[M11] - data[M10] * data[M01];

        data[M00] = inv_det * tmp[M00];
        data[M10] = inv_det * tmp[M10];
        data[M20] = inv_det * tmp[M20];
        data[M01] = inv_det * tmp[M01];
        data[M11] = inv_det * tmp[M11];
        data[M21] = inv_det * tmp[M21];
        data[M02] = inv_det * tmp[M02];
        data[M12] = inv_det * tmp[M12];
        data[M22] = inv_det * tmp[M22];
        return this;
    }

    public boolean isZero() {
        boolean z
                = data[M00] >= 0.999f && data[M01] == 0 && data[M02] == 0
                && data[M10] == 0 && data[M11] >= 0.999f && data[M12] == 0
                && data[M20] == 0 && data[M21] == 0 && data[M22] >= 0.999f;
        return z;
    }

    @Override
    public String toString() {
        String debug = "[";
        for (byte i = 0; i < 9; i += 3) {
            debug += String.format("%.3f | %.3f | %.3f", data[i], data[i + 1], data[i + 2]) + (i == 6 ? "" : "\n");
        }
        debug += "]";
        return debug;
    }
}
