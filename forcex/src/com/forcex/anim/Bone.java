/**
 * Represents a bone in an animation.
 */
package com.forcex.anim;

import com.forcex.math.Matrix4f;
import com.forcex.math.Vector3f;

import java.util.ArrayList;

public class Bone {
    public int boneID; // Unique identifier for the bone
    ArrayList<KeyFrame> keyframes = new ArrayList<KeyFrame>(); // List of keyframes for the bone
    public boolean hasPosition; // Indicates if the bone has position data

    /**
     * Constructor for Bone class.
     *
     * @param boneId      The unique identifier for the bone.
     * @param hasPosition Indicates if the bone has position data.
     */
    public Bone(int boneId, boolean hasPosition) {
        this.boneID = boneId;
        this.hasPosition = hasPosition;
    }

    /**
     * Adds a new keyframe to the bone.
     *
     * @param key The KeyFrame to be added.
     */
    public void addKeyFrame(KeyFrame key) {
        keyframes.add(key);
    }

    /**
     * Gets the number of keyframes for the bone.
     *
     * @return The number of keyframes.
     */
    public int getNumKeyFrames() {
        return keyframes.size();
    }

    /**
     * Retrieves a specific KeyFrame at the given index.
     *
     * @param idx The index of the KeyFrame to retrieve.
     * @return The KeyFrame at the specified index.
     */
    public KeyFrame getKeyFrame(int idx) {
        return keyframes.get(idx);
    }

    /**
     * Retrieves the two closest KeyFrames for the given time.
     *
     * @param time The time at which to find KeyFrames.
     * @return An array containing the two closest KeyFrames.
     */
    public KeyFrame[] getFrameTime(float time) {
        for (byte f = 0; f < keyframes.size(); f++) {
            if (time <= keyframes.get(f).time) {
                if (f == 0) {
                    if (keyframes.size() != 1) {
                        return new KeyFrame[]{
                                keyframes.get(keyframes.size() - 1), keyframes.get(f)};
                    } else {
                        return new KeyFrame[]{
                                keyframes.get(f), keyframes.get(f)};
                    }
                } else {
                    return new KeyFrame[]{
                            keyframes.get(f - 1), keyframes.get(f)};
                }
            }
        }
        KeyFrame previus = keyframes.get(keyframes.size() - 1);
        return new KeyFrame[]{previus, previus};
    }

    /**
     * Calculates the interpolation factor based on given KeyFrames and time.
     *
     * @param keys An array containing the two closest KeyFrames.
     * @param time The time at which to interpolate.
     * @return The interpolation factor.
     */
    private float getTime(KeyFrame[] keys, float time) {
        float totalTime = keys[1].time - keys[0].time;
        float currentTime = time - keys[0].time;
        return currentTime / totalTime;
    }

    /**
     * Interpolates a KeyFrame for the given time.
     *
     * @param time The time at which to interpolate.
     * @return The interpolated KeyFrame.
     */
    public KeyFrame interpolate(float time) {
        KeyFrame[] keys = getFrameTime(time);
        float pcnt = getTime(keys, time);
        KeyFrame key = new KeyFrame();
        if (hasPosition) {
            key.position = keys[0].position.lerp(keys[1].position, pcnt);
        } else {
            key.position = new Vector3f();
        }
        key.rotation = keys[0].rotation.slerp(keys[1].rotation, pcnt);
        key.time = pcnt;
        return key;
    }

    /**
     * Interpolates a KeyFrame for the given time and returns its transformation matrix.
     *
     * @param time The time at which to interpolate.
     * @return The transformation matrix of the interpolated KeyFrame.
     */
    public Matrix4f interpolateKey(float time) {
        return interpolate(time).toMatrix();
    }
}
