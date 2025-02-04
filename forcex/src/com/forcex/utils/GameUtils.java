package com.forcex.utils;

import com.forcex.FX;
import com.forcex.math.Vector2f;
import com.forcex.math.Vector3f;

public class GameUtils {
    public static Vector2f getTouchNormalized(float x, float y) {
        return new Vector2f(2 * (x / FX.gpu.getWidth()) - 1, -(2 * (y / FX.gpu.getHeight()) - 1));
    }

    public static boolean testRect(float x, float y, Vector2f position, float width, float height) {
        return
                x >= (position.x - width) &&
                        x <= (position.x + width) &&
                        y >= (position.y - height) &&
                        y <= (position.y + height);
    }

    public static boolean isInRange(Vector3f pos1, Vector3f pos2, float range) {
        float dist = pos1.distance2(pos2);
        return (dist < range * range);
    }

    public static boolean isInRange(Vector2f pos1, Vector2f pos2, float range) {
        return (pos1.sub(pos2).length() < range);
    }

    public static float distance(Vector2f pos1, Vector2f pos2) {
        return pos1.sub(pos2).length();
    }

    public static float distance(Vector3f pos1, Vector3f pos2) {
        return pos1.sub(pos2).length();
    }

    public static Vector2f directionToPoint(Vector2f from, Vector2f target) {
        return target.sub(from).normalize();
    }

    public static Vector3f directionToPoint(Vector3f from, Vector3f target) {
        return target.sub(from).normalize();
    }

    public static Vector3f getLocalPosition(Vector3f center, Vector3f point, float dist) {
        Vector3f dir = directionToPoint(center, point);
        return new Vector3f(
                (dir.x * dist) + center.x,
                (dir.y * dist) + center.y,
                (dir.z * dist) + center.z);
    }

    public static String trimString(String str) {
        int null_str = str.indexOf(0);
        return null_str > 0 ? str.substring(0, null_str) : str;
    }
}
