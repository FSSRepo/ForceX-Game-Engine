package com.forcex.math;

public class Quaternion {

    static Matrix4f temp_matrix;
    public float x, y, z, w;

    public Quaternion(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Quaternion() {
        w = 1.0f;
        x = y = z = 0;
    }

    public static Quaternion fromAxisAngle(Vector3f axis, float angle) {
        return new Quaternion().setAxisAngle(axis, angle);
    }

    public static Quaternion fromEulerAngles(float x, float y, float z) {
        return new Quaternion().setEulerAngles(x, y, z);
    }

    public static Quaternion fromRotationMatrix(Vector3f right, Vector3f up, Vector3f direction) {
        return new Quaternion().setRotationMatrix(
                right.x, up.x, direction.x,
                right.y, up.y, direction.y,
                right.z, up.z, direction.z);
    }

    public static Quaternion fromRotationMatrix(Matrix3f m) {
        float[] mat = m.data;
        return new Quaternion().setRotationMatrix(
                mat[Matrix3f.M00], mat[Matrix3f.M01], mat[Matrix3f.M02],
                mat[Matrix3f.M10], mat[Matrix3f.M11], mat[Matrix3f.M12],
                mat[Matrix3f.M20], mat[Matrix3f.M21], mat[Matrix3f.M22]);
    }

    public Quaternion addLocal(Quaternion o) {
        w += o.w;
        x += o.x;
        y += o.y;
        z += o.z;
        return this;
    }

    public Quaternion subLocal(Quaternion o) {
        w -= o.w;
        x -= o.x;
        y -= o.y;
        z -= o.z;
        return this;
    }

    public Quaternion multLocal(Quaternion o) {
        final float
                tx = w * o.x + x * o.w + y * o.z - z * o.y,
                ty = w * o.y + y * o.w + z * o.x - x * o.z,
                tz = w * o.z + z * o.w + x * o.y - y * o.x,
                tw = w * o.w - x * o.x - y * o.y - z * o.z;
        x = tx;
        y = ty;
        z = tz;
        w = tw;
        return this;
    }

    public Quaternion mult(final Quaternion other) {
        final float newX = this.w * other.x + this.x * other.w + this.y * other.z - this.z * other.y;
        final float newY = this.w * other.y + this.y * other.w + this.z * other.x - this.x * other.z;
        final float newZ = this.w * other.z + this.z * other.w + this.x * other.y - this.y * other.x;
        final float newW = this.w * other.w - this.x * other.x - this.y * other.y - this.z * other.z;
        return new Quaternion(newX, newY, newZ, newW);
    }

    @Override
    public boolean equals(Object obj) {
        Quaternion q = (Quaternion) obj;
        return w == q.w && x == q.x && y == q.y && z == q.z;
    }

    public Quaternion mult(float s) {
        w *= s;
        x *= s;
        y *= s;
        z *= s;
        return this;
    }

    public Quaternion lookAt(Vector3f source, Vector3f target, boolean upZ) {
        // dir = nor(dest - src)
        Vector3f d = target.sub(source).normalize();
        return lookAt(d, upZ);
    }

    public Quaternion lookAt(Vector3f direction, boolean upZ) {
        if(temp_matrix == null) {
            temp_matrix = new Matrix4f();
        }
        temp_matrix.lookAt(direction, upZ);
        temp_matrix.getRotation(this);
        return this;
    }

    public Quaternion setAxisAngle(Vector3f axis, float angle) {
        return setAxisAngleRad(axis, angle * Maths.toRadians);
    }

    public Quaternion setAxisAngleRad(Vector3f axis, float angle) {
        float half_angle = angle * .5f;
        float s = Maths.sin(half_angle);
        set(axis.x * s, axis.y * s, axis.z * s, Maths.cos(half_angle));
        return this;
    }

    public Quaternion setEulerAngles(float x, float y, float z) {
        final float ar = (z * Maths.toRadians) * 0.5f;
        final float sr = Maths.sin(ar);
        final float cr = Maths.cos(ar);
        final float ap = (x * Maths.toRadians) * 0.5f;
        final float sp = Maths.sin(ap);
        final float cp = Maths.cos(ap);
        final float ay = (y * Maths.toRadians) * 0.5f;
        final float sy = Maths.sin(ay);
        final float cy = Maths.cos(ay);
        final float cy_sp = cy * sp;
        final float sy_cp = sy * cp;
        final float cy_cp = cy * cp;
        final float sy_sp = sy * sp;
        this.x = (cy_sp * cr) + (sy_cp * sr); // cos(yaw/2) * sin(pitch/2) * cos(roll/2) + sin(yaw/2) * cos(pitch/2) * sin(roll/2)
        this.y = (sy_cp * cr) - (cy_sp * sr); // sin(yaw/2) * cos(pitch/2) * cos(roll/2) - cos(yaw/2) * sin(pitch/2) * sin(roll/2)
        this.z = (cy_cp * sr) - (sy_sp * cr); // cos(yaw/2) * cos(pitch/2) * sin(roll/2) - sin(yaw/2) * sin(pitch/2) * cos(roll/2)
        this.w = (cy_cp * cr) + (sy_sp * sr); // cos(yaw/2) * cos(pitch/2) * cos(roll/2) + sin(yaw/2) * sin(pitch/2) * sin(roll/2)
        normalize();
        return this;
    }

    public Vector3f getAngles() {
        final float t = y * x + z * w;
        final float pole = t > 0.499f ? 1 : (t < -0.499f ? -1 : 0);
        float ax = (pole == 0 ? Maths.asin(Maths.clamp(2f * (w * x - z * y), -1f, 1f)) : pole * Maths.PI * 0.5f);
        float ay = (pole == 0 ? Maths.atan2(2f * (y * w + x * z), 1f - 2f * (y * y + x * x)) : 0f);
        float az = (pole == 0 ? Maths.atan2(2f * (w * z + y * x), 1f - 2f * (x * x + z * z)) : pole * 2f * Maths.atan2(y, w));
        return new Vector3f(ax, ay, az).multLocal(Maths.toDegrees);
    }

    public Quaternion lerp(Quaternion other, float t) {
        return addLocal(other.subLocal(this).mult(t));
    }

    public Quaternion slerp(Quaternion end, float percent) {
        float cosTheta = Maths.abs(dot(end)) / Maths.sqrt(lengthSquared() * end.lengthSquared());
        if (cosTheta >= 1.0f) {
            return this;
        }
        float theta = Maths.acos(cosTheta);
        float sinTheta = Maths.sin(theta);
        if (Maths.abs(sinTheta) < 0.0000001f) {
            return this;
        }
        float rs = 1.0f / sinTheta;
        float f1 = Maths.sin((1.0f - percent) * theta);
        float f2 = Maths.sin(percent * theta);
        if (dot(end) < 0.0f) {
            return new Quaternion(
                    (x * f1 - end.x * f2) * rs,
                    (y * f1 - end.y * f2) * rs,
                    (z * f1 - end.z * f2) * rs,
                    (w * f1 - end.w * f2) * rs
            );
        } else {
            return new Quaternion(
                    (x * f1 + end.x * f2) * rs,
                    (y * f1 + end.y * f2) * rs,
                    (z * f1 + end.z * f2) * rs,
                    (w * f1 + end.w * f2) * rs
            );
        }
    }

    public Quaternion conjugate() {
        return new Quaternion(-x, -y, -z, w);
    }

    public float lengthSquared() {
        return (w * w) + (x * x) + (y * y) + (z * z);
    }

    public float length() {
        return Maths.sqrt(lengthSquared());
    }

    public float normalize() {
        float l = 1.0f / length();
        x *= l;
        y *= l;
        z *= l;
        w *= l;
        return l;
    }

    public float dot(Quaternion o) {
        return (x * o.x) + (y * o.y) + (z * o.z) + (w * o.w);
    }

    public float angle(Quaternion other) {
        return Maths.acos(Maths.abs(dot(other)) / Maths.sqrt(lengthSquared() * other.lengthSquared()));
    }

    public Quaternion nlerp(Quaternion other, float t) {
        Quaternion res = lerp(other, t);
        res.normalize();
        return res;
    }

    public Quaternion set(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    public void set(Quaternion o) {
        this.x = o.x;
        this.y = o.y;
        this.z = o.z;
        this.w = o.w;
    }

    public Quaternion setRotationMatrix(
            float rightx, float upx, float atx,
            float righty, float upy, float aty,
            float rightz, float upz, float atz) {
        float tr = rightx + upy + atz;
        float s;
        if (tr > 0.0f) {
            s = Maths.sqrt(1.0f + tr) * 2.0f;
            w = s / 4.0f;
            x = (upz - aty) / s;
            y = (atx - rightz) / s;
            z = (righty - upx) / s;
        } else if (rightx > upy && rightx > atz) {
            s = Maths.sqrt(1.0f + rightx - upy - atz) * 2.0f;
            w = (upz - aty) / s;
            x = s / 4.0f;
            y = (upx + righty) / s;
            z = (atx + rightz) / s;
        } else if (upy > atz) {
            s = Maths.sqrt(1.0f + upy - rightx - atz) * 2.0f;
            w = (atx - rightz) / s;
            x = (upx + righty) / s;
            y = s / 4.0f;
            z = (aty + upz) / s;
        } else {
            s = Maths.sqrt(1.0f + atz - rightx - upy) * 2.0f;
            w = (righty - upx) / s;
            x = (atx + rightz) / s;
            y = (aty + upz) / s;
            z = s / 4.0f;
        }
        return this;
    }

    public Matrix3f getMatrix(Matrix3f m) {
        final float xs = x * 2f, ys = y * 2f, zs = z * 2f;
        final float wx = w * xs, wy = w * ys, wz = w * zs;
        final float xx = x * xs, xy = x * ys, xz = x * zs;
        final float yy = y * ys, yz = y * zs, zz = z * zs;
        m.data[Matrix3f.M00] = (1.0f - (yy + zz));
        m.data[Matrix3f.M01] = (xy - wz);
        m.data[Matrix3f.M02] = (xz + wy);

        m.data[Matrix3f.M10] = (xy + wz);
        m.data[Matrix3f.M11] = (1.0f - (xx + zz));
        m.data[Matrix3f.M12] = (yz - wx);

        m.data[Matrix3f.M20] = (xz - wy);
        m.data[Matrix3f.M21] = (yz + wx);
        m.data[Matrix3f.M22] = (1.0f - (xx + yy));
        return m;
    }

    public Matrix3f toMatrix() {
        float xx = x * x;
        float yy = y * y;
        float zz = z * z;
        float xy = x * y;
        float zw = z * w;
        float xz = x * z;
        float yw = y * w;
        float yz = y * z;
        float xw = x * w;

        return new Matrix3f(
                1f - 2f * (yy + zz), 2f * (xy + zw), 2f * (xz - yw),
                2f * (xy - zw), 1f - 2f * (xx + zz), 2f * (yz + xw),
                2f * (xz + yw), 2f * (yz - xw), 1f - 2f * (xx + yy)
        );
    }

    public Vector4f toAxisAngle(Vector3f fallback) {
        float angle = 2.0f * Maths.acos(w);
        angle *= Maths.toDegrees;
        float sqrtInvW2 = Maths.sqrt(1.0f - w * w);
        Vector3f axis = new Vector3f(x / sqrtInvW2, y / sqrtInvW2, z / sqrtInvW2);
        if (sqrtInvW2 < 0.0000001f) {
            return new Vector4f(fallback, 0.0f);
        }
        return new Vector4f(axis, angle);
    }

    public String toString() {
        return "[" + x + " , " + y + " , " + z + " , " + w + "]";
    }

    public Vector3f getRight() {
        Vector3f store = new Vector3f();
        float norm = lengthSquared();
        if (norm != 1.0f) {
            norm = 1.0f / Maths.sqrt(norm);
        }
        float xy = x * y * norm;
        float xz = x * z * norm;
        float yy = y * y * norm;
        float yw = y * w * norm;
        float zz = z * z * norm;
        float zw = z * w * norm;
        store.x = 1 - 2 * (yy + zz);
        store.y = 2 * (xy + zw);
        store.z = 2 * (xz - yw);
        return store;
    }

    public Vector3f getUp() {
        Vector3f store = new Vector3f();
        float norm = lengthSquared();
        if (norm != 1.0f) {
            norm = 1.0f / Maths.sqrt(norm);
        }
        float xx = x * x * norm;
        float xy = x * y * norm;
        float xw = x * w * norm;
        float yz = y * z * norm;
        float zz = z * z * norm;
        float zw = z * w * norm;
        store.x = 2 * (xy - zw);
        store.y = 1 - 2 * (xx + zz);
        store.z = 2 * (yz + xw);
        return store;
    }

    public Vector3f getDirection() {
        Vector3f store = new Vector3f();
        float norm = lengthSquared();
        if (norm != 1.0f) {
            norm = 1.0f / Maths.sqrt(norm);
        }
        float xx = x * x * norm;
        float xz = x * z * norm;
        float xw = x * w * norm;
        float yy = y * y * norm;
        float yz = y * z * norm;
        float yw = y * w * norm;
        store.x = 2 * (xz + yw);
        store.y = 2 * (yz - xw);
        store.z = 1 - 2 * (xx + yy);
        return store;
    }

    @Override
    protected Quaternion clone() {
        return new Quaternion(x, y, z, w);
    }
}
