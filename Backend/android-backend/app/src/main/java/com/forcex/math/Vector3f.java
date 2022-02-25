package com.forcex.math;
import static com.forcex.math.Maths.*;

public class Vector3f {

    public float x, y, z;
	
    public Vector3f(float ix) {
        this(ix,ix,ix);
    }
	
	public Vector3f() {
        this(0);
    }
	
	public Vector3f(Vector3f v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }
	
    public Vector3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
	
	public Vector3f(float[] array,int offset){
		x = array[offset];
		y = array[offset+1];
		z = array[offset+2];
	}
    
	public void get(float[] data,int offset){
		data[offset] = x;
		data[offset+1] = y;
		data[offset+2] = z;
	}
	
	public void add(float[] data,int offset){
		data[offset] += x;
		data[offset+1] += y;
		data[offset+2] += z;
	}
	
	public Vector3f set(float[] data,int offset){
		x = data[offset];
		y = data[offset+1];
		z = data[offset+2];
		return this;
	}
	
	public static Vector3f set(float[] data,int offset,float x,float y,float z){
		data[offset] = x;
		data[offset+1] = y;
		data[offset+2] = z;
		return new Vector3f(x,y,z);
	}
	
	public float dot(float dx,float dy,float dz) {
        return (x * dx) + (y * dy) + (z * dz);
    }
	
    public float dot(Vector3f v) {
        return (x * v.x) + (y * v.y) + (z * v.z);
    }
	
	public Vector3f negative() {
        return new Vector3f(-x, -y, -z);
    }
	
	public float get(int component){
		return component == 0 ? x : (component == 1 ? y : z);
	}
	
	public Vector3f negativeLocal() {
		set(
			x == 0 ? 0 : -x,
			y == 0 ? 0 : -y,
			z == 0 ? 0 : -z);
        return this;
    }
	
	public Vector3f absoluteLocal(){
		set(abs(x),abs(y),abs(z));
		return this;
	}
	
	public float length() {
        return sqrt(sqr(x) + sqr(y) + sqr(z));
    }
	
	public Vector3f normalize() {
		float l = length();
		float invLen = 1.0f / (l > 0.0f ? l : 1.0f);
		multLocal(invLen);
        return this;
    }
	
	public float distance2(float px, float py, float pz) {
        float 
		rx = px - x,
		ry = py - y,
		rz = pz - z;
        return (sqr(rx) + sqr(ry) + sqr(rz));
    }
	
    public float distance2(Vector3f point) {
        return distance2(point.x, point.y, point.z);
    }
	public float distance(float px, float py, float pz) {
		return sqrt(distance2(px,py,pz));
    }

    public float distance(Vector3f point) {
        return distance(point.x, point.y, point.z);
    }
	public Vector3f lerp(Vector3f end, float porcent) {
		return end.sub(this).mult(porcent).add(this);
    }
	public Vector3f cross(Vector3f v) {
		return cross(v,new Vector3f());
    }
	
    public Vector3f cross(Vector3f v,Vector3f dest) {
		dest.x = (y * v.z - z * v.y);
		dest.y = (z * v.x - x * v.z);
		dest.z = (x * v.y - y * v.x);
		dest.x = dest.x == -0 ? +0 : dest.x;
		dest.y = dest.y == -0 ? +0 : dest.y;
		dest.z = dest.z == -0 ? +0 : dest.z;
        return dest;
    }
	
    public Vector3f crossLocal(Vector3f v) {
       float tx = (y * v.z - z * v.y);
       float ty = (z * v.x - x * v.z);
       float tz = (x * v.y - y * v.x);
	   set(tx,ty,tz);
       return this;
    }
	
	/* settes */
    public Vector3f set(float x, float y, float z) {
        this.x = x;
		this.y = y;
		this.z = z;
        return this;
    }
	public void add(int component,float amount){
		switch(component){
			case 0: x += amount; break;
			case 1: y += amount; break;
			case 2: z += amount; break;
		}
	}
	
	public void set(int component,float v){
		switch(component){
			case 0: x = v; break;
			case 1: y = v; break;
			case 2: z = v; break;
		}
	}
	
    public Vector3f set(Vector3f other) {
        x = other.x;
        y = other.y;
        z = other.z;
        return this;
    }
	
	public static Vector3f getUpFromDirection(Vector3f direction,boolean upZ){
		float dot = direction.dot(upZ ? new Vector3f(0,0,1) : new Vector3f(0,1,0));
		if(Maths.abs(dot + 1) < 0.000001f){
			return (upZ ? new Vector3f(0,-1,0) : new Vector3f(0,0,-1));
		}else if(Maths.abs(dot - 1) < 0.000001f){
			return (upZ ? new Vector3f(0,1,0) : new Vector3f(0,0,1));
		}
		return upZ ? new Vector3f(0,0,1) : new Vector3f(0,1,0);
	}
	
	/* this operation to multiply right,up,direction vector*/
    public Vector3f multLocal(final Matrix4f mat) {
        final float l_mat[] = mat.data;
        return this.set(
                x * l_mat[Matrix4f.M00] + y * l_mat[Matrix4f.M01] + z * l_mat[Matrix4f.M02],
                x * l_mat[Matrix4f.M10] + y * l_mat[Matrix4f.M11] + z * l_mat[Matrix4f.M12],
                x * l_mat[Matrix4f.M20] + y * l_mat[Matrix4f.M21] + z * l_mat[Matrix4f.M22]);
    }
	
	public Vector3f project(final Matrix4f mat) {
        final float l_mat[] = mat.data;
		final float l_w = 1f / (x * l_mat[Matrix4f.M30] + y * l_mat[Matrix4f.M31] + z * l_mat[Matrix4f.M32] + l_mat[Matrix4f.M33]);
        return new Vector3f(
			(x * l_mat[Matrix4f.M00] + y * l_mat[Matrix4f.M01] + z * l_mat[Matrix4f.M02] + l_mat[Matrix4f.M03]) * l_w, 
			(x * l_mat[Matrix4f.M10] + y * l_mat[Matrix4f.M11] + z * l_mat[Matrix4f.M12] + l_mat[Matrix4f.M13]) * l_w, 
			(x * l_mat[Matrix4f.M20] + y * l_mat[Matrix4f.M21] + z * l_mat[Matrix4f.M22] + l_mat[Matrix4f.M23]) * l_w);
    }
	
	public Vector3f multLocal(float scalar) {
        x *= scalar;
		y *= scalar;
		z *= scalar;
        return this;
    }
	
    public Vector3f mult(Vector3f v) {
        return new Vector3f(x * v.x, y * v.y, z * v.y);
    }
	
	public Vector3f multLocal(Vector3f v) {
        x *= v.x;
		y *= v.y;
		z *= v.z;
        return this;
    }
	
    public Vector3f mult(float scalar) {
        return new Vector3f(x * scalar, y * scalar, z * scalar);
    }
	
    public String toString() {
        return "[" + String.format("%.3f", x) + " | " + String.format("%.3f", y) + " | " + String.format("%.3f", z) +"]";
    }
	
    public Vector3f addLocal(Vector3f v) {
        x += v.x; y += v.y; z += v.z;
        return this;
    }
	
	public Vector3f addLocal(float x,float y, float z) {
        this.x += x; this.y += y; this.z += z;
        return this;
    }
	
    public Vector3f add(Vector3f v) {
        return new Vector3f(x + v.x, y + v.y, z + v.z);
    }
	
	public Vector3f add(float amount) {
        return new Vector3f(x + amount, y + amount, z + amount);
    }
	
	public Vector3f sub(Vector3f v) {
        return new Vector3f(x - v.x, y - v.y, z - v.z);
    }
	
    public Vector3f subLocal(Vector3f v) {
        x -= v.x; y -= v.y; z -= v.z;
        return this;
    }
	
	public Vector3f pointOnCircle(float radius,float angle,boolean XY){
		Vector3f p = new Vector3f();
		p.x = (radius * cos(angle * toRadians)) + x;
		p.y = !XY ?  y : ((radius * sin(angle * toRadians)) + y);
		p.z = XY ? ((radius * sin(angle * toRadians)) + z) : z;
		return p;
	}
	
	public Vector3f pointOnSphere(float radius,float angleX,float angleY,boolean XY){
		float u = angleX * toRadians, v = angleY * toRadians;
		float tx = radius * sin(u) * cos(v) + x;
		float ty = !XY ? (radius * sin(v) + y) : (radius * cos(u) * cos(v) + y);
		float tz = XY ? (radius * sin(v) + z) : (radius * cos(u) * cos(v) + z);
		return new Vector3f(tx,ty,tz);
	}
	
	public void rotateOnSphereOrigin(float radius,float angleX,float angleY,boolean XY){
		float u = angleX * toRadians, v = angleY * toRadians;
		x = radius * sin(u) * cos(v);
		y = !XY ? (radius * sin(v)) : (radius * cos(u) * cos(v));
		z = XY ? (radius * sin(v)) : (radius * cos(u) * cos(v));
	}
	
	public Vector3f toRotation(){
		float rotx = Maths.atan2(y,sqrt((x*x) + (z*z))) * toDegrees;
		float roty = -Maths.atan2(x,z) * toDegrees;
		return new Vector3f(rotx,roty,0);
	}
	
	/* rotate this vector */
    public Vector3f rotX(float angle) {
		float cs = cos(angle * toRadians);
        float sn = sin(angle * toRadians);
        float ty = (y * cs) - (z * sn);
        float tz = (y * sn) + (z * cs);
		y = ty;
		z = tz;
		return this;
    }
	
    public Vector3f rotY(float angle) {
		float cs = cos(angle * toRadians);
        float sn = sin(angle * toRadians);
		float tx = (x * cs) + (z * sn);
        float tz = (x * -sn) + (z * cs);
		x = tx;
		z = tz;
		return this;
    }
	
    public Vector3f rotZ(float angle) {
		float cs = cos(angle * toRadians);
        float sn = sin(angle * toRadians);
        float tx = (x * cs) - (y * sn);
        float ty = (x * sn) + (y * cs);
		x = tx;
		y = ty;
		return this;
    }
	
    public void rotate(float ax,float ay,float az) {
        rotX(ax); rotY(ay); rotZ(az);
    }
	
	public Vector3f reflect(Vector3f normal){
		float d = dot(normal);
		float rx = x - ((2.0f * d) * normal.x);
		float ry = y - ((2.0f * d) * normal.y);
		float rz = z - ((2.0f * d) * normal.z);
		return new Vector3f(rx,ry,rz);
	}
	
    public boolean isZero() {
        return (x == 0 && y == 0 && z == 0);
    }
	
	public Vector3f medio(Vector3f p2){
		return add(p2).mult(0.5f);
	}
	
	public Vector3f clone(){
		return new Vector3f(x,y,z);
	}
}
