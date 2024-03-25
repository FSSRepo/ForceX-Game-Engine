package com.forcex.anim;

import com.forcex.math.Matrix4f;
import com.forcex.math.Quaternion;
import com.forcex.math.Vector3f;

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
