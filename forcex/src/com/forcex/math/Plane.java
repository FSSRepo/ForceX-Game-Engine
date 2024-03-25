package com.forcex.math;

public class Plane {

    public Vector3f normal;
    public float distance;
    Vector3f tmp = new Vector3f();

    public Plane() {
        normal = new Vector3f();
    }

    public Plane(Vector3f normal, float dist) {
        this.normal = normal;
        distance = dist;
    }

    public void set(Vector3f v1, Vector3f v2, Vector3f v3) {
        normal.set(v1).subLocal(v2).crossLocal(v2.sub(v2)).normalize();
        distance = -v1.dot(normal);
    }

    public Vector3f reflect(Vector3f point) {
        float d = (normal.dot(point) - distance);
        tmp.set(normal).negativeLocal().multLocal(d * 2f);
        tmp.addLocal(point);
        return tmp;
    }
}
