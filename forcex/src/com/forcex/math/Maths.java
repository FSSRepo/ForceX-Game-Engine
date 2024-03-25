package com.forcex.math;

public class Maths {
    public static final float toRadians = 0.0174532924f;
    public static final float toDegrees = 57.295779513f;
    public static final float PI_2 = 6.28318530f;
    public static final float PI = 3.14159265f;
    public static final float epsilon = 0.00001f;

    public static float cos(float v) {
        return (float) Math.cos(v);
    }

    public static float sin(float v) {
        return (float) Math.sin(v);
    }

    public static float tan(float v) {
        return (float) Math.tan(v);
    }

    public static float asin(float v) {
        return (float) Math.asin(v);
    }

    public static float acos(float v) {
        return (float) Math.acos(v);
    }

    public static float atan(float v) {
        return (float) Math.atan(v);
    }

    public static float atan2(float v1, float v2) {
        return (float) Math.atan2(v1, v2);
    }

    public static float mod(float v1, float v2) {
        return v1 % v2;
    }

    public static float abs(float v) {
        return (float) Math.abs(v);
    }

    public static float pow(float v1, float v2) {
        return (float) Math.pow(v1, v2);
    }

    public static float min(float v, float v2) {
        return (float) Math.min(v, v2);
    }

    public static float max(float v, float v2) {
        return (float) Math.max(v, v2);
    }

    static public float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

    public static float sqrt(float v) {
        return (float) Math.sqrt(v);
    }

    public static float sqr(float v) {
        return v * v;
    }
}
