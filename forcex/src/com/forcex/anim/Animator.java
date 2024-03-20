/**
 * This class represents an Animator responsible for managing animations and bone transformations.
 */
package com.forcex.anim;

import com.forcex.FX;
import com.forcex.math.Matrix4f;
import com.forcex.math.Vector3f;
import java.util.HashMap;

public class Animator {
    public AnimationControl control;
    Animation animation;
    HashMap<SkeletonNode, Matrix4f> animMatrices = new HashMap<SkeletonNode, Matrix4f>();
    Matrix4f[] boneMatrices;
    SkeletonNode skeleton;
    byte numBone = 0;

    /**
     * Constructor for the Animator class.
     *
     * @param skeleton The root node of the skeleton hierarchy.
     * @param numBones The number of bones in the skeleton.
     */
    public Animator(SkeletonNode skeleton, int numBones) {
        control = new AnimationControl();
        this.skeleton = skeleton;
        this.numBone = (byte) numBones;
    }

    /**
     * Starts playing the given animation.
     *
     * @param anim The animation to be played.
     */
    public void doAnimation(Animation anim) {
        control.time = 0;
        animation = anim;
    }

    /**
     * Retrieves the current animation.
     *
     * @return The current animation.
     */
    public Animation getAnimation() {
        return animation;
    }

    /**
     * Resets the animation to its initial state.
     */
    public void reset() {
        control.putCommand(AnimationControl.CMD_RESET);
        update();
    }

    /**
     * Updates the animation time based on the elapsed time and animation speed.
     */
    public void updateTime() {
        if (animation == null) {
            return;
        }
        control.time += FX.gpu.getDeltaTime() * control.speed;
        if (control.time > animation.getDuration() && control.loop) {
            control.time %= animation.getDuration();
        } else {
            if (control.time > animation.getDuration()) {
                control.play = false;
                return;
            }
        }
    }

    /**
     * Updates the animation state and bone transformations.
     */
    public void update() {
        if (animation == null) {
            return;
        }
        if (control.play) {
            updateTime();
            updateBonesMatrices();
        } else {
            if (boneMatrices == null) {
                control.time += FX.gpu.getDeltaTime() * control.speed;
                updateBonesMatrices();
            }
            return;
        }
    }

    /**
     * Retrieves a map of bone transformations for each node in the skeleton.
     *
     * @return The map of bone transformations.
     */
    public HashMap<SkeletonNode, Matrix4f> getBoneMap() {
        return animMatrices;
    }

    /**
     * Retrieves an array of bone transformation matrices.
     *
     * @return The array of bone transformation matrices.
     */
    public Matrix4f[] getBoneMatrices() {
        if (boneMatrices == null && animation == null) {
            updateBonesMatrices();
        }
        return boneMatrices;
    }

    /**
     * Retrieves the updated transformation matrix for a specific bone.
     *
     * @param boneId The ID of the bone.
     * @return The updated transformation matrix for the specified bone.
     */
    public Matrix4f getBoneMatrixUpdate(int boneId) {
        return animMatrices.get(skeleton.findByBoneId((short) boneId));
    }

    Vector3f tmp = new Vector3f();

    /**
     * Updates the transformation matrix for a specific skeleton node.
     *
     * @param frame The skeleton node to update.
     */
    private void updateBoneMatrix(SkeletonNode frame) {
        Bone bone = null;
        if (animation != null) {
            bone = animation.findBone(frame.boneID);
        }
        if (bone != null) {
            Matrix4f animKey = bone.interpolateKey(control.time);
            if (!bone.hasPosition) {
                frame.modelMatrix.getLocation(tmp);
                animKey.setLocation(tmp.x, tmp.y, tmp.z);
            }
            SkeletonNode parent = frame.parent;
            if (parent != null) {
                Matrix4f pm = animMatrices.get(parent);
                animMatrices.put(frame, pm.mult(null, animKey));
            } else {
                animMatrices.put(frame, animKey);
            }
        } else {
            SkeletonNode parent = frame.parent;
            if (parent != null) {
                Matrix4f pm = animMatrices.get(parent);
                animMatrices.put(frame, pm.mult(null, frame.modelMatrix));
            } else {
                animMatrices.put(frame, frame.modelMatrix);
            }
        }
        for (SkeletonNode child : frame.children) {
            updateBoneMatrix(child);
        }
    }

    /**
     * Updates bone transformation matrices for all nodes in the skeleton hierarchy.
     */
    public void updateBonesMatrices() {
        animMatrices.clear();
        updateBoneMatrix(skeleton);
        boneMatrices = new Matrix4f[numBone];
        for (SkeletonNode frame : animMatrices.keySet()) {
            Matrix4f transform = animMatrices.get(frame).mult(null, frame.InverseBoneMatrix);
            if (frame.boneID != -1) {
                boneMatrices[frame.boneNum] = transform;
            }
        }
    }

    /**
     * Deletes the Animator instance and releases associated resources.
     */
    public void delete() {
        animMatrices = null;
        boneMatrices = null;
        control = null;
    }
}
