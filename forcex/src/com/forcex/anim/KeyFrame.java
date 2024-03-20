package com.forcex.anim;
import com.forcex.math.*;
import com.forcex.utils.*;

/**
 * Represents a keyframe for animation.
 */
public class KeyFrame {
    public Quaternion rotation;  // Rotation quaternion for the keyframe.
    public Vector3f position;    // Position vector for the keyframe.
    public float time;           // Time at which the keyframe occurs.

    /**
     * Converts the keyframe's rotation and position to a transformation matrix.
     *
     * @return The transformation matrix representing the keyframe.
     */
    public Matrix4f toMatrix() {
        Matrix4f m = new Matrix4f(rotation.toMatrix());
        m.multLocal(Matrix4f.translation(position));
        return m;
    }
}
