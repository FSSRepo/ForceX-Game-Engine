package com.forcex.math;
import com.forcex.utils.*;

public class Matrix4f {
    public float[] data = new float[16];
    public static final int M00 = 0;

    public static final int M01 = 4;

    public static final int M02 = 8;

    public static final int M03 = 12;

    public static final int M10 = 1;

    public static final int M11 = 5;

    public static final int M12 = 9;

    public static final int M13 = 13;

    public static final int M20 = 2;

    public static final int M21 = 6;

    public static final int M22 = 10;

    public static final int M23 = 14;

    public static final int M30 = 3;

    public static final int M31 = 7;

    public static final int M32 = 11;

    public static final int M33 = 15;

    public void setIdentity() {
		for(byte i = 0;i < 16;i++){
			data[i] = 0.0f;
		}
		data[M00] = 1.0f;
		data[M11] = 1.0f;
		data[M22] = 1.0f;
		data[M33] = 1.0f;
    }

    public Matrix4f() {
        setIdentity();
    }
	
    public Matrix4f(float[] data) {
        this.data = data;
    }
	
    public Matrix4f(Matrix3f other) {
        setUpperLeft(other);
    }
	
    public Matrix4f(
            float m00, float m10, float m20, float m30,
            float m01, float m11, float m21, float m31,
            float m02, float m12, float m22, float m32,
            float m03, float m13, float m23, float m33) {
        data[M00] = m00; data[M10] = m10; data[M20] = m20; data[M30] = m30;
		data[M01] = m01; data[M11] = m11; data[M21] = m21; data[M31] = m31;
        data[M02] = m02; data[M12] = m12; data[M22] = m22; data[M32] = m32;
        data[M03] = m03; data[M13] = m13; data[M23] = m23; data[M33] = m33;
    }
	
	/* add,sub,mult operations */
    public Matrix4f addLocal(Matrix4f other) {
        for(byte i = 0;i < 16;i++){
			data[i] += other.data[i];
		}
        return this;
    }
	
    public Matrix4f subLocal(Matrix4f other) {
        for(byte i = 0;i < 16;i++){
			data[i] -= other.data[i];
		}
        return this;
    }
	
	public Matrix4f multLocal(Matrix4f other) {
		float[] nd = new float[16];
        mult_(nd,data,other.data);
		data = null;
		data = nd;
		return this;
    }
	
	public Matrix4f multLocal(float scalar) {
        for(byte i = 0;i < 16;i++){
			data[i] *= scalar;
		}
        return this;
    }
	
	public Matrix4f add(Matrix4f other) {
        Matrix4f m = new Matrix4f();
        for(byte i = 0;i < 16;i++){
			m.data[i] = data[i] + other.data[i];
		}
        return m;
    }
	
    public Matrix4f sub(Matrix4f other) {
		Matrix4f m = new Matrix4f();
        for(byte i = 0;i < 16;i++){
			m.data[i] = data[i] - other.data[i];
		}
        return m;
    }
	
    public Matrix4f mult(Matrix4f dest, Matrix4f other) {
		if(dest == null){
			dest = new Matrix4f();
		}
        mult_(dest.data,data,other.data);
		return dest;
    }
	
	public Matrix4f mult(float scalar) {
		Matrix4f m = new Matrix4f();
        for(byte i = 0;i < 16;i++){
			m.data[i] = data[i] * scalar;
		}
        return m;
    }
	
	public void get(float[] array,int offset){
		for(byte i = 0; i < 16;i++){
			array[offset + i] = data[i];
		}
	}
	
	public Matrix4f clone(){
		float[] data_cpy = new float[16];
		for(byte i = 0;i < 16;i++){
			data_cpy[i] = data[i];
		}
		return new Matrix4f(data_cpy);
	}
	
	/* multiply vector */
	public Vector3f mult(Vector3f vec) {
		Vector3f o = new Vector3f();
		o.x = data[M00] * vec.x + data[M01] * vec.y + data[M02] * vec.z + data[M03];
		o.y = data[M10] * vec.x + data[M11] * vec.y + data[M12] * vec.z + data[M13];
		o.z = data[M20] * vec.x + data[M21] * vec.y + data[M22] * vec.z + data[M23];
        return o;
    }
	
    public Vector4f mult(Vector4f vec) {
         return new Vector4f(
                data[M00] * vec.x + data[M11] * vec.y + data[M02] * vec.z + data[M03] * vec.w,
                data[M10] * vec.x + data[M11] * vec.y + data[M12] * vec.z + data[M13] * vec.w,
                data[M20] * vec.x + data[M21] * vec.y + data[M22] * vec.z + data[M23] * vec.w,
                data[M30] * vec.x + data[M31] * vec.y + data[M32] * vec.z + data[M33] * vec.w
        );
    }
	/* 	set tranform (quaterion,vector) */
	public void setTransform(Quaternion rotation, Vector3f location) {
        transform_(this,rotation.x,rotation.y,rotation.z,rotation.w,location.x,location.y,location.z);
    }
	
	public void setRotation(Quaternion rotation) {
        transform_(this,rotation.x,rotation.y,rotation.z,rotation.w,data[M03],data[M13],data[M23]);
    }
	
	/* settes normal */
	public Matrix4f setLocation(float x,float y,float z) {
        data[M03] = x;
        data[M13] = y;
        data[M23] = z;
		return this;
    }
	
	public Matrix4f setLocation(Vector3f location) {
		data[M03] = location.x;
        data[M13] = location.y;
        data[M23] = location.z;
        return this;
    }
	
	public Matrix4f setScale(Vector3f scale) {
        multLocal(Matrix4f.scale(scale));
        return this;
    }
	
	public Matrix4f setScale(float scale) {
        data[M00] = scale;
		data[M11] = scale;
		data[M22] = scale;
        return this;
    }
	
	public Matrix4f setScale(float sx,float sy,float sz) {
        data[M00] = sx;
		data[M11] = sy;
		data[M22] = sz;
        return this;
    }
	
	public Matrix4f set(Matrix4f m){
		for(byte i = 0;i < 16;i++){
			data[i] = m.data[i];
		}
		return this;
	}
	
	public Matrix4f set(float[] m){
		for(byte i = 0;i < 16;i++){
			data[i] = m[i];
		}
		m = null;
		return this;
	}
	
	public Matrix4f setTransform(Vector3f rotation, Vector3f location, Vector3f scale) {
        multLocal(Matrix4f.fromRotationEuler(rotation.x,rotation.y,rotation.z));
        multLocal(Matrix4f.scale(scale));
        return setLocation(location);
    }
	
	public Matrix4f setTransform(Quaternion rotation, Vector3f location, Vector3f scale) {
        transform_(this,rotation.x,rotation.y,rotation.z,rotation.w,location.x,location.y,location.z);
        return this;
    }
	
	/*	set static tranform */
	public static Matrix4f fromTransform(Quaternion rotation, Vector3f location) {
		Matrix4f m = new Matrix4f();
        transform_(m,rotation.x,rotation.y,rotation.z,rotation.w,location.x,location.y,location.z);
		return m;
    }
	
	public static Matrix4f fromRotation(Quaternion rotation) {
		Matrix4f m = new Matrix4f();
        transform_(m,rotation.x,rotation.y,rotation.z,rotation.w,0,0,0);
		return m;
    }
	
    public static Matrix4f scale(Vector3f s) {
        return new Matrix4f(
                s.x, 0.0f, 0.0f, 0.0f,
                0.0f, s.y, 0.0f, 0.0f,
                0.0f, 0.0f, s.z, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f
        );
    }
	
	public static Matrix4f scale(float allAxis) {
        return new Matrix4f(
			allAxis, 0.0f, 0.0f, 0.0f,
			0.0f, allAxis, 0.0f, 0.0f,
			0.0f, 0.0f, allAxis, 0.0f,
			0.0f, 0.0f, 0.0f, 1.0f
        );
    }
	
    public static Matrix4f translation(Vector3f pos) {
        return translation(pos.x, pos.y, pos.z);
    }
	
    public static Matrix4f translation(float x, float y, float z) {
        return new Matrix4f(
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                x, y, z, 1.0f
        );
    }
	
	public void setUpperLeft(Matrix3f matrix) {
        data[M00] = matrix.data[Matrix3f.M00];
        data[M01] = matrix.data[Matrix3f.M01];
        data[M02] = matrix.data[Matrix3f.M02];
        data[3] = 0.0f;
        data[M10] = matrix.data[Matrix3f.M10];
        data[M11] = matrix.data[Matrix3f.M11];
        data[M12] = matrix.data[Matrix3f.M12];
        data[7] = 0.0f;
        data[M20] = matrix.data[Matrix3f.M20];
        data[M21] = matrix.data[Matrix3f.M21];
        data[M22] = matrix.data[Matrix3f.M22];
        data[11] = 0.0f;
        data[12] = 0.0f;
        data[13] = 0.0f;
        data[14] = 0.0f;
        data[15] = 1.0f;
    }
	
	public Quaternion getRotation(Quaternion dest) {
		if(dest == null){
			Matrix3f m3 = getUpperLeft();
			dest = Quaternion.fromRotationMatrix(m3);
			m3 = null;
		}else{
			Matrix3f m3 = getUpperLeft();
			dest.set(Quaternion.fromRotationMatrix(m3));
			m3 = null;
		}
        return dest;
    }
	
	public Vector3f getLocation(Vector3f store) {
        if(store == null){
			store = new Vector3f();
		}
		store.set(data[M03], data[M13], data[M23]);
        return store;
    }
	
	public static Matrix4f setRotation(Matrix4f mm,float angle,Vector3f vec)
	{
		float a = angle * Maths.toRadians;
		float s = Maths.sin(a);
		float c = Maths.cos(a);
		float[] matrix = mm.data;
		matrix[M00] = vec.x * vec.x * (1 - c) + c;
		matrix[M10] = vec.x * vec.y * (1 - c) + vec.z * s;
		matrix[M20] = vec.x * vec.z * (1 - c) - vec.y * s;
		matrix[M30] = 0;
		
		matrix[M01] = vec.y * vec.x * (1 - c) - vec.z * s;
		matrix[M11] = vec.y * vec.y * (1 - c) + c;
		matrix[M21] = vec.y * vec.z * (1 - c) + vec.x * s;
		matrix[M31] = 0;
		
		matrix[M02] = vec.z * vec.x * (1 - c) + vec.y * s;
		matrix[M12] = vec.z * vec.y * (1 - c) - vec.x * s;
		matrix[M22] = vec.z * vec.z * (1 - c) + c;
		matrix[M32] = 0;
		
		matrix[M03] = 0;
		matrix[M13] = 0;
		matrix[M23] = 0;
		matrix[M33] = 1;
		return mm;
	}
	
	public void lookAt(Vector3f direction,boolean upZ){
		Vector3f up = Vector3f.getUpFromDirection(direction,upZ);
		Vector3f right = up.cross(direction).normalize();
		right.cross(direction,up).negativeLocal().normalize();
		data[M00] = right.x; 	 data[M10] = right.y; 	  data[M20] = right.z;
		data[M01] = up.x;	 	 data[M11] = up.y;		  data[M21] = up.z;
		data[M02] = direction.x; data[M12] = direction.y; data[M22] = direction.z;
		up = null;
		right = null;
	}
	
	public Matrix3f getUpperLeft(Matrix3f m3) {
		m3.data[Matrix3f.M00] = data[M00];
		m3.data[Matrix3f.M10] = data[M10];
		m3.data[Matrix3f.M20] = data[M20];
		m3.data[Matrix3f.M01] = data[M01];
		m3.data[Matrix3f.M11] = data[M11];
		m3.data[Matrix3f.M21] = data[M21];
		m3.data[Matrix3f.M02] = data[M02];
		m3.data[Matrix3f.M12] = data[M12];
		m3.data[Matrix3f.M22] = data[M22];
		return m3;
    }
	
	public Matrix3f getUpperLeft() {
        return new Matrix3f(
			data[M00], data[M10], data[M20],
			data[M01], data[M11], data[M21],
			data[M02], data[M12], data[M22]
        );
    }
	/* specials */
	Vector3f xaxis_look = new Vector3f();
	
	public Matrix4f setlookAt(Vector3f position, Vector3f direction, Vector3f up) {
        direction.cross(up, xaxis_look).normalize();
        data[M00] = xaxis_look.x;
		data[M01] = xaxis_look.y;
		data[M02] = xaxis_look.z;
		
		data[M10] = up.x;
		data[M11] = up.y;
		data[M12] = up.z;
		
		data[M20] = -direction.x;
		data[M21] = -direction.y;
		data[M22] = -direction.z;
		
		data[M03] = -xaxis_look.dot(position);
		data[M13] = -up.dot(position);
		data[M23] = direction.dot(position);
		
		data[M30] = 0;
		data[M31] = 0;
		data[M32] = 0;
		data[M33] = 1;
		return this;
    }
	
	public static Matrix4f lookAt(Vector3f position, Vector3f direction, Vector3f up) {
        Vector3f zaxis = new Vector3f(direction);
        zaxis.normalize();
        Vector3f xaxis = zaxis.cross(up);
        xaxis.normalize();
        Vector3f yaxis = xaxis.cross(zaxis);
		
        return new Matrix4f(
			xaxis.x, yaxis.x, -zaxis.x, 0,
			xaxis.y, yaxis.y, -zaxis.y, 0,
			xaxis.z, yaxis.z, -zaxis.z, 0,
			-xaxis.dot(position), -yaxis.dot(position), zaxis.dot(position), 1);
    }
	
	public static Matrix4f fromRotationEuler(float x, float y, float z) {
		float rx = (x * Maths.toRadians);
		float ry = (y * Maths.toRadians);
		float rz = (z * Maths.toRadians);
        final float sx = Maths.sin(rx);
		final float cx = Maths.cos(rx);
		final float sy = Maths.sin(ry);
		final float cy = Maths.cos(ry);
		final float sz = Maths.sin(rz);
		final float cz = Maths.cos(rz);
        return new Matrix4f(
			cy * cz, sz, -sy * cz,0,
			-cy * sz * cx + sy * sx, cz * cx, sy * sz * cx + cy * sx,0,
			cy * sz * sx + sy * cx, -cz * cx, -sy * sz * sx + cy * cx,0,
			0,0,0,1
        );
    }
	
	/* projection type */
    public static Matrix4f frustum(float l, float r, float b, float t, float n, float f) {
        return new Matrix4f(
                2 * n / (r - l), 0, 0, 0,
                0, 2 * n / (t - b), 0, 0,
                (r + l) / (r - l), (t + b) / (t - b), (-(f + n)) / (f - n), -1,
                0, 0, (-2 * f * n) / (f - n), 0
        );
    }
	
	public Matrix4f setPerspective(float fov, float aspect, float near, float far) {
        float fc = 1.0f / Maths.tan((fov * Maths.toRadians) * 0.5f);
        data[M00] = fc / aspect;
		data[M11] = fc;
		data[M22] = (far + near) / (near - far);
		data[M23] = (2 * far * near) / (near - far);
		data[M32] = -1;
		data[M33] = 0;
		return this;
    }
	
    public static Matrix4f perspective(float fovy, float aspect, float n, float f) {
        float fc = 1.0f / Maths.tan((fovy * Maths.toRadians) * 0.5f);
        return new Matrix4f(
                fc / aspect, 0, 0, 0,
                0, fc, 0, 0,
                0, 0, (f + n) / (n - f), -1,
                0, 0, (2 * f * n) / (n - f), 0
        );
    }
	
	public void setOrthogonal(float left, float right, float bottom, float top, float near, float far){
		data[M00] = 2.0f / (right - left);
		data[M11] = 2.0f / (top - bottom);
		data[M22] = -2.0f / (far - near);
		setLocation(-((right + left) / (right - left)), -((top + bottom) / (top - bottom)), -((far + near) / (far - near)));
		data[M33] = 1;
	}
	
    public static Matrix4f orthogonal(float left, float right, float bottom, float top, float near, float far) {
        return new Matrix4f(
                2.0f / (right - left), 0, 0, 0,
                0, 2.0f / (top - bottom), 0, 0,
                0, 0, -2.0f / (far - near), 0,
                -((right + left) / (right - left)), -((top + bottom) / (top - bottom)), -((far + near) / (far - near)), 1
        );
    }
	
	/*	invert, transpose */
    public Matrix4f invert() {
		Matrix4f m = new Matrix4f();
        invert_(m,this);
		return m;
    }
	
	public Matrix4f invertLocal() {
	   	invert_(this,this);
		return this;
    }
	
	public Matrix4f transpose() {
		Matrix4f m = new Matrix4f();
        transpose_(data,m.data);
		return m;
    }
	
    public Matrix4f transposeLocal() {
		float[] n = new float[16];
        transpose_(data,n);
		data = n;
		return this;
    }
	
	@Override
	public String toString() {
		String debug = "[";
		for(byte i = 0;i < 16;i += 4){
			debug += String.format("%.4f", data[i])+" | "+String.format("%.4f", data[i+1])+" | "+String.format("%.4f", data[i+2])+" | "+String.format("%.4f", data[i+3]) + (i == 12 ? "":"\n");
		}
        debug += "]";
        return debug;
    }
	
	private static void mult_(float[] dest,float[] a,float[] b) {
		byte i = 0,j = 0,k = 0;
		for(;i < 4;i++){
			for(j = 0;j < 4;j++){
				dest[j * 4 + i] = 0.0f;
				for(k = 0;k < 4;k++){
					dest[j*4 + i] += a[k * 4 + i] * b[j * 4 + k];
				}
			}
		}
	}
	
	private static void transform_(Matrix4f dest,float qx,float qy,float qz,float qw,float x,float y,float z) {
        final float xs = qx * 2f, ys = qy * 2f, zs = qz * 2f;
        final float wx = qw * xs, wy = qw * ys, wz = qw * zs;
        final float xx = qx * xs, xy = qx * ys, xz = qx * zs;
        final float yy = qy * ys, yz = qy * zs, zz = qz * zs;
        dest.data[M00] = (1.0f - (yy + zz));
        dest.data[M01] = (xy - wz);
        dest.data[M02] = (xz + wy);
        dest.data[M03] = x;

        dest.data[M10] = (xy + wz);
        dest.data[M11] = (1.0f - (xx + zz));
        dest.data[M12] = (yz - wx);
        dest.data[M13] = y;

        dest.data[M20] = (xz - wy);
        dest.data[M21] = (yz + wx);
        dest.data[M22] = (1.0f - (xx + yy));
        dest.data[M23] = z;

        dest.data[M30] = 0.f;
        dest.data[M31] = 0.f;
        dest.data[M32] = 0.f;
        dest.data[M33] = 1.0f;
    }
	
	private void invert_(Matrix4f dest,Matrix4f matrix) {
		float[] m = matrix.data;
        dest.data[0] = m[5] * m[10] * m[15] - m[5] * m[11] * m[14] - m[9] * m[6] * m[15]
			+ m[9] * m[7] * m[14] + m[13] * m[6] * m[11] - m[13] * m[7] * m[10];
        dest.data[4] = -m[4] * m[10] * m[15] + m[4] * m[11] * m[14] + m[8] * m[6] * m[15]
			- m[8] * m[7] * m[14] - m[12] * m[6] * m[11] + m[12] * m[7] * m[10];
        dest.data[8] = m[4] * m[9] * m[15] - m[4] * m[11] * m[13] - m[8] * m[5] * m[15]
			+ m[8] * m[7] * m[13] + m[12] * m[5] * m[11] - m[12] * m[7] * m[9];
        dest.data[12] = -m[4] * m[9] * m[14] + m[4] * m[10] * m[13] + m[8] * m[5] * m[14]
			- m[8] * m[6] * m[13] - m[12] * m[5] * m[10] + m[12] * m[6] * m[9];
        dest.data[1] = -m[1] * m[10] * m[15] + m[1] * m[11] * m[14] + m[9] * m[2] * m[15]
			- m[9] * m[3] * m[14] - m[13] * m[2] * m[11] + m[13] * m[3] * m[10];
        dest.data[5] = m[0] * m[10] * m[15] - m[0] * m[11] * m[14] - m[8] * m[2] * m[15]
			+ m[8] * m[3] * m[14] + m[12] * m[2] * m[11] - m[12] * m[3] * m[10];
        dest.data[9] = -m[0] * m[9] * m[15] + m[0] * m[11] * m[13] + m[8] * m[1] * m[15]
			- m[8] * m[3] * m[13] - m[12] * m[1] * m[11] + m[12] * m[3] * m[9];
        dest.data[13] = m[0] * m[9] * m[14] - m[0] * m[10] * m[13] - m[8] * m[1] * m[14]
			+ m[8] * m[2] * m[13] + m[12] * m[1] * m[10] - m[12] * m[2] * m[9];
        dest.data[2] = m[1] * m[6] * m[15] - m[1] * m[7] * m[14] - m[5] * m[2] * m[15]
			+ m[5] * m[3] * m[14] + m[13] * m[2] * m[7] - m[13] * m[3] * m[6];
        dest.data[6] = -m[0] * m[6] * m[15] + m[0] * m[7] * m[14] + m[4] * m[2] * m[15]
			- m[4] * m[3] * m[14] - m[12] * m[2] * m[7] + m[12] * m[3] * m[6];
        dest.data[10] = m[0] * m[5] * m[15] - m[0] * m[7] * m[13] - m[4] * m[1] * m[15]
			+ m[4] * m[3] * m[13] + m[12] * m[1] * m[7] - m[12] * m[3] * m[5];
        dest.data[14] = -m[0] * m[5] * m[14] + m[0] * m[6] * m[13] + m[4] * m[1] * m[14]
			- m[4] * m[2] * m[13] - m[12] * m[1] * m[6] + m[12] * m[2] * m[5];
        dest.data[3] = -m[1] * m[6] * m[11] + m[1] * m[7] * m[10] + m[5] * m[2] * m[11]
			- m[5] * m[3] * m[10] - m[9] * m[2] * m[7] + m[9] * m[3] * m[6];
        dest.data[7] = m[0] * m[6] * m[11] - m[0] * m[7] * m[10] - m[4] * m[2] * m[11]
			+ m[4] * m[3] * m[10] + m[8] * m[2] * m[7] - m[8] * m[3] * m[6];
        dest.data[11] = -m[0] * m[5] * m[11] + m[0] * m[7] * m[9] + m[4] * m[1] * m[11]
			- m[4] * m[3] * m[9] - m[8] * m[1] * m[7] + m[8] * m[3] * m[5];
        dest.data[15] = m[0] * m[5] * m[10] - m[0] * m[6] * m[9] - m[4] * m[1] * m[10]
			+ m[4] * m[2] * m[9] + m[8] * m[1] * m[6] - m[8] * m[2] * m[5];

        float det = m[0] * dest.data[0] + m[1] * dest.data[4] + m[2] * dest.data[8] + m[3] * dest.data[12];
        if (det == 0) {
            return;
        }
        det = 1.0f / det;
        for (byte i = 0; i < 16; i++) {
            dest.data[i] = dest.data[i] * det;
        }
    }
	
	private void transpose_(float[] m,float[] dest) {
        byte t = 0;
        for (byte i = 0; i < 4; ++i) {
            for (byte j = 0; j < 4; ++j) {
                dest[t++] = m[j * 4 + i];
            }
        }
    }
}
