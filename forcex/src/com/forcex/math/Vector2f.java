package com.forcex.math;

public class Vector2f {

    public float x, y;

    public Vector2f(float x) {
        this.x = x;
        y = x;
    }

    public Vector2f(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vector2f(float[] array, int offset) {
        x = array[offset];
        y = array[offset + 1];
    }

    public Vector2f() {
        this(0f);
    }

    public Vector2f set(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Vector2f set(float[] array, int offset) {
        x = array[offset];
        y = array[offset + 1];
        return this;
    }

    public Vector2f set(Vector2f v) {
        this.x = v.x;
        this.y = v.y;
        return this;
    }

    public Vector2f multLocal(float scl) {
        x *= scl;
        y *= scl;
        return this;
    }

    public Vector2f multLocal(float sclx, float scly) {
        x *= sclx;
        y *= scly;
        return this;
    }

    public Vector2f mult(float scl) {
        return new Vector2f(x * scl, y * scl);
    }

    public Vector2f mult(float sx, float sy) {
        return new Vector2f(x * sx, y * sy);
    }

    public Vector2f addLocal(Vector2f v) {
        x += v.x;
        y += v.y;
        return this;
    }

    public Vector2f subLocal(Vector2f v) {
        x -= v.x;
        y -= v.y;
        return this;
    }

    public Vector2f add(float x, float y) {
        return new Vector2f(this.x + x, this.y + y);
    }

    public Vector2f add(Vector2f v) {
        return new Vector2f(x + v.x, y + v.y);
    }

    public Vector2f sub(Vector2f v) {
        return new Vector2f(x - v.x, y - v.y);
    }

    public Vector2f sub(float x, float y) {
        return new Vector2f(this.x - x, this.y - y);
    }

    public Vector2f negative() {
        return new Vector2f(-x, -y);
    }

    public float dot(Vector2f v) {
        return (x * v.x) + (y * v.y);
    }

    public String toString() {
        return "[" + String.format("%.3f | %.3f", x, y) + "]";
    }

    public float length() {
        return Maths.sqrt((x * x) + (y * y));
    }

    public Vector2f normalize() {
        float invlen = 1f / length();
        multLocal(invlen);
        return this;
    }

    public boolean equals(Vector2f v) {
        return v.x == x && v.y == y;
    }

    public float angle(Vector2f other) {
        float d = dot(other);
        if (d > 1.0f) {
            d = 1.0f;
        } else if (d < -1.0f) {
            d = -1.0f;
        }
        return Maths.acos(d);
    }

    public Vector2f lerp(Vector2f end, float porcent) {
        return new Vector2f(
                x + (porcent * (end.x - x)),
                y + (porcent * (end.y - y))
        );
    }

}
