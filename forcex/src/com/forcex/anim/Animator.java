/**
 * This class represents an Animator responsible for managing animations and bone transformations.
 */
package com.forcex.anim;

import com.forcex.FX;
import com.forcex.math.Matrix4f;
import com.forcex.math.Quaternion;
import com.forcex.math.Vector3f;

import java.util.HashMap;

public class Animator {
    public AnimationControl control;
    private Animation animation;
    private HashMap<SkeletonNode, Matrix4f> animMatrices = new HashMap<SkeletonNode, Matrix4f>();
    private Matrix4f[] boneMatrices;
    private final SkeletonNode skeleton;
    private int numBone = 0;

    boolean inverse_time = false;

    float animation_duration = 0, internal_time = 0;
    boolean finished_state = false;

    // blend: the outgoing animation keeps playing while its weight fades out (like GTA SA's blend delta)
    private Animation from_animation;
    private float from_time = 0, from_speed = 1;
    private boolean from_loop = false;
    private float blend_weight = 1.0f, blend_delta = 0.0f;
    // local pose frozen when a blend is interrupted by another one; replaces from_animation as the outgoing source
    private HashMap<SkeletonNode, Matrix4f> from_pose;

    // Optional upper-body overlay, evaluated only on the selected skeleton subtree.
    private static final int LAYER_COUNT = 4;
    private final Animation[] layer_animations = new Animation[LAYER_COUNT];
    private final SkeletonNode[] layer_roots = new SkeletonNode[LAYER_COUNT];
    private final float[] layer_times = new float[LAYER_COUNT];
    private final float[] layer_weights = new float[LAYER_COUNT];

    // post-animation procedural rotation of one bone (IK-like aim), in model space around the bone origin
    private SkeletonNode twist_node;
    private final Vector3f twist_axis = new Vector3f();
    private float twist_angle = 0.0f;

    // root motion: displacement of the root bone's position track between two updates
    private boolean root_motion = false;
    private final Vector3f root_prev = new Vector3f();
    private final Vector3f root_cur = new Vector3f();
    private final Vector3f root_delta = new Vector3f();
    private boolean root_valid = false;
    // the root bone keeps its first-frame position, so the caller can move the character by the root delta instead
    private boolean root_locked = false;

    /**
     * Constructor for the Animator class.
     *
     * @param skeleton The root node of the skeleton hierarchy.
     * @param numBones The number of bones in the skeleton.
     */
    public Animator(SkeletonNode skeleton, int numBones) {
        control = new AnimationControl();
        this.skeleton = skeleton;
        this.numBone = numBones;
    }

    /**
     * Starts playing the given animation, replacing the current one at once.
     *
     * @param anim The animation to be played.
     */
    public void doAnimation(Animation anim, boolean inverse) {
        from_animation = null;
        from_pose = null;
        blend_weight = 1.0f;
        startAnimation(anim, inverse);
    }

    /**
     * Starts playing the given animation, fading it in over the current one.
     * The outgoing animation keeps advancing while its weight falls, so the blend takes
     * {@code 1 / blendDelta} seconds (4.0 = 0.25 s, 16.0 = 0.0625 s, 1.0 = 1 s).
     *
     * @param anim       The animation to be played.
     * @param inverse    Whether it plays backwards.
     * @param blendDelta Weight gained per second by the new animation; zero or less cuts at once.
     */
    public void blendTo(Animation anim, boolean inverse, float blendDelta) {
        // interrupting a running blend: freeze the visible mixed pose as the outgoing source, so it does not pop
        HashMap<SkeletonNode, Matrix4f> frozen = null;
        if (animation != null && blendDelta > 0.0f && from_animation != null && blend_weight < 1.0f) {
            frozen = new HashMap<SkeletonNode, Matrix4f>();
            capturePose(skeleton, frozen);
        }
        clearBoneLayer();
        from_pose = frozen;
        if (animation != null && blendDelta > 0.0f) {
            from_animation = animation;
            from_time = control.time;
            from_speed = control.speed;
            from_loop = control.loop;
            blend_weight = 0.0f;
            blend_delta = blendDelta;
        } else {
            from_animation = null;
            blend_weight = 1.0f;
        }
        startAnimation(anim, inverse);
    }

    /** Applies an independently timed animation over one skeleton subtree; call it every frame. */
    public void setBoneLayer(Animation anim, float time, SkeletonNode root, float weight) {
        setAnimationLayer(0, anim, time, root, weight);
    }

    /** Sets a full-body blend layer at the requested normalized animation phase. */
    public void setFullBodyAnimationLayer(int index, Animation anim, float normalizedTime, float weight) {
        float t = anim == null ? 0.0f : Math.max(0.0f, Math.min(1.0f, normalizedTime)) * anim.getDuration();
        setAnimationLayer(index, anim, t, null, weight);
    }

    /** Sets one of the ordered animation layers; later layers blend over earlier ones. */
    public void setAnimationLayer(int index, Animation anim, float time, SkeletonNode root, float weight) {
        if (index < 0 || index >= LAYER_COUNT) throw new IndexOutOfBoundsException("animation layer " + index);
        if (anim == null || weight <= 0.0f) {
            clearAnimationLayer(index);
            return;
        }
        layer_animations[index] = anim;
        layer_times[index] = Math.max(0.0f, Math.min(time, anim.getDuration()));
        layer_roots[index] = root;
        layer_weights[index] = Math.max(0.0f, Math.min(1.0f, weight));
    }

    /**
     * Rotates {@code node} (and its children) after the animation by {@code degrees} around
     * {@code modelAxis} (model space, through the bone origin). Pass a null node or 0 to disable.
     */
    public void setBoneTwist(SkeletonNode node, Vector3f modelAxis, float degrees) {
        twist_node = node;
        if (modelAxis != null) twist_axis.set(modelAxis);
        twist_angle = degrees;
    }

    /**
     * Giro (radianes, sobre el eje vertical del modelo) que la animacion actual, mezclada con la anterior, da al hueso raiz respecto
     * a las animaciones de locomocion (cuyo adelante es -X). Las de arma (Gun_stand, GunMove_*, *_fire) llevan 48-82 grados de giro:
     * quien las usa lo compensa girando el modelo en sentido contrario para que el cuerpo mire al rumbo del ped.
     */
    public float getRootYaw() {
        if (animation == null) {
            return 0.0f;
        }
        SkeletonNode root = skeleton.findByBoneId(animation.getRootBone().boneID);
        if (root == null) {
            return 0.0f;
        }
        Matrix4f m = poseLocal(root);
        double yaw = Math.atan2(m.data[Matrix4f.M11], m.data[Matrix4f.M01]) - Math.PI;
        while (yaw > Math.PI) yaw -= 2.0 * Math.PI;
        while (yaw < -Math.PI) yaw += 2.0 * Math.PI;
        return (float) yaw;
    }

    public void clearBoneLayer() {
        for (int i = 0; i < LAYER_COUNT; i++) clearAnimationLayer(i);
    }

    public void clearAnimationLayer(int index) {
        if (index < 0 || index >= LAYER_COUNT) throw new IndexOutOfBoundsException("animation layer " + index);
        layer_animations[index] = null;
        layer_roots[index] = null;
        layer_times[index] = layer_weights[index] = 0.0f;
    }

    private void startAnimation(Animation anim, boolean inverse) {
        control.time = 0;
        animation_duration = anim.getDuration();
        internal_time = 0;
        this.inverse_time = inverse;
        animation = anim;
        root_valid = false;
        if (root_motion) {
            sampleRoot(0.0f, root_prev);
        }
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
     * @return Normalized playback position of the current animation (0..1).
     */
    public float getPhase() {
        return animation_duration > 0.0f ? control.time / animation_duration : 0.0f;
    }

    /**
     * @return True while a {@link #blendTo} transition is still in progress.
     */
    public boolean isBlending() {
        return from_animation != null;
    }

    /**
     * @return Weight of the current animation in the running blend (1 when not blending).
     */
    public float getBlendWeight() {
        return blend_weight;
    }

    /**
     * Enables extraction of the root bone's position track as displacement per update
     * (see {@link #getRootDelta()}). The displacement is expressed in the animation's own axes.
     */
    public void setRootMotion(boolean enabled) {
        root_motion = enabled;
        root_valid = false;
        root_delta.set(0, 0, 0);
        if (enabled && animation != null) {
            sampleRoot(control.time, root_prev);
        }
    }

    /**
     * Pins the root bone's position track to its first frame. With {@link #setRootMotion} the displacement is then
     * reported by {@link #getRootDelta()} only, so the owner can move the character while the pose stays in place.
     */
    public void setRootLocked(boolean locked) {
        root_locked = locked;
    }

    /**
     * @return Displacement of the root bone during the last update (zero when root motion is off).
     */
    public Vector3f getRootDelta() {
        return root_delta;
    }

    /**
     * Resets the animation to its initial state.
     */
    public void reset() {
        control.reset();
        update();
    }

    /**
     * Updates the animation time based on the elapsed time and animation speed.
     */
    public void updateTime() {
        updateTime(FX.gpu.getDeltaTime());
    }

    /**
     * Same as {@link #updateTime()} with an explicit time step (seconds), so the caller can pause,
     * slow down or step the animation deterministically.
     */
    public void updateTime(float dt) {
        if (animation == null) {
            return;
        }
        float before = control.time;
        boolean wrapped = false;
        if(inverse_time) {
            internal_time += dt * control.speed;
            control.time = animation_duration - internal_time;
        } else {
            control.time += dt * control.speed;
        }

        float end_time = control.time;
        if (finished()) {
            if(control.loop) {
                control.time %= animation_duration;
                wrapped = true;
            } else {
                control.play = false;
            }
            finished_state = true;
        }

        if (!inverse_time && animation.hasEvents()) {
            if (wrapped) {
                animation.fireEvents(before, animation_duration);
                animation.fireEvents(-1.0f, control.time);
            } else {
                animation.fireEvents(before, Math.min(end_time, animation_duration));
            }
        }

        updateRootMotion(before, wrapped);
        updateBlend(dt);
    }

    private void updateBlend(float dt) {
        if (from_animation == null) {
            return;
        }
        float duration = from_animation.getDuration();
        from_time += dt * from_speed;
        if (duration > 0.0f && from_time > duration) {
            from_time = from_loop ? from_time % duration : duration;
        }
        blend_weight += blend_delta * dt;
        if (blend_weight >= 1.0f) {
            blend_weight = 1.0f;
            from_animation = null;
            from_pose = null;
        }
    }

    private void sampleRoot(float time, Vector3f dest) {
        if (animation == null || !animation.getRootBone().samplePosition(time, dest)) {
            dest.set(0, 0, 0);
        }
    }

    private void updateRootMotion(float before, boolean wrapped) {
        if (!root_motion) {
            return;
        }
        if (!root_valid) {
            sampleRoot(before, root_prev);
            root_valid = true;
        }
        if (wrapped) {
            // finish the pass up to the end of the clip, then continue from its start
            sampleRoot(animation_duration, root_cur);
            root_delta.set(root_cur).subLocal(root_prev);
            sampleRoot(0.0f, root_prev);
            sampleRoot(control.time, root_cur);
            root_delta.addLocal(root_cur).subLocal(root_prev);
        } else {
            sampleRoot(Math.min(control.time, animation_duration), root_cur);
            root_delta.set(root_cur).subLocal(root_prev);
        }
        root_prev.set(root_cur);
        if (wrapped) {
            sampleRoot(control.time, root_prev);
        }
    }

    public boolean finished() {
        return control.time > animation_duration ||
                inverse_time && internal_time > animation_duration;
    }

    public boolean finishedNotify() {
        if(finished_state) {
            finished_state = false;
            return true;
        }
        return false;
    }

    /**
     * Updates the animation state and bone transformations.
     */
    public void update() {
        update(FX.gpu.getDeltaTime());
    }

    /**
     * Same as {@link #update()} with an explicit time step (seconds).
     */
    public void update(float dt) {
        if (animation == null) {
            return;
        }
        if (control.play) {
            updateTime(dt);
            updateBonesMatrices();
        } else {
            if (boneMatrices == null) {
                control.time += dt * control.speed;
                updateBonesMatrices();
            } else if (from_animation != null) {
                // a paused clip can still be fading in over the previous one
                updateBlend(dt);
                updateBonesMatrices();
            }
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
     * Local transformation of a node for the given animation and time: the animated key when the
     * animation drives the bone, the bind pose otherwise.
     */
    private Matrix4f localMatrix(SkeletonNode frame, Animation anim, float time) {
        Bone bone = anim != null ? anim.findBone(frame.boneID) : null;
        if (bone == null) {
            return frame.modelMatrix;
        }
        Matrix4f animKey = bone.interpolateKey(time);
        if (!bone.hasPosition) {
            frame.modelMatrix.getLocation(tmp);
            animKey.setLocation(tmp.x, tmp.y, tmp.z);
        } else if (root_locked && bone == anim.getRootBone() && bone.samplePosition(0.0f, tmp)) {
            animKey.setLocation(tmp.x, tmp.y, tmp.z);
        }
        return animKey;
    }

    /**
     * Mixes two local transformations: rotation by slerp and location by lerp. {@code w} is the
     * weight of {@code to}.
     */
    private Matrix4f blendMatrices(Matrix4f from, Matrix4f to, float w) {
        Quaternion qa = from.getRotation(null);
        Quaternion qb = to.getRotation(null);
        Vector3f la = from.getLocation(null);
        Vector3f lb = to.getLocation(null);
        return Matrix4f.fromTransform(qa.slerp(qb, w), la.lerp(lb, w));
    }

    /**
     * Updates the transformation matrix for a specific skeleton node.
     *
     * @param frame The skeleton node to update.
     */
    private Matrix4f poseLocal(SkeletonNode frame) {
        Matrix4f local = localMatrix(frame, animation, control.time);
        if (from_animation != null && blend_weight < 1.0f) {
            Matrix4f other = from_pose != null && from_pose.containsKey(frame)
                    ? from_pose.get(frame) : localMatrix(frame, from_animation, from_time);
            local = blendMatrices(other, local, blend_weight);
        }
        for (int i = 0; i < LAYER_COUNT; i++) {
            Animation layerAnimation = layer_animations[i];
            if (layerAnimation != null && layer_weights[i] > 0.0f && isInSubtree(frame, layer_roots[i])
                    && layerAnimation.findBone(frame.boneID) != null) {
                Matrix4f layer = localMatrix(frame, layerAnimation, layer_times[i]);
                local = blendMatrices(local, layer, layer_weights[i]);
            }
        }
        return local;
    }

    private void capturePose(SkeletonNode frame, HashMap<SkeletonNode, Matrix4f> out) {
        Matrix4f copy = new Matrix4f();
        copy.set(poseLocal(frame));
        out.put(frame, copy);
        for (SkeletonNode child : frame.children) {
            capturePose(child, out);
        }
    }

    private void updateBoneMatrix(SkeletonNode frame) {
        Matrix4f local = poseLocal(frame);
        SkeletonNode parent = frame.parent;
        if (parent != null) {
            Matrix4f pm = animMatrices.get(parent);
            animMatrices.put(frame, pm.mult(null, local));
        } else {
            animMatrices.put(frame, local);
        }
        if (frame == twist_node && twist_angle != 0.0f) {
            Matrix4f m = animMatrices.get(frame);
            Vector3f p = m.getLocation(null);
            Matrix4f rot = Matrix4f.fromTransform(Quaternion.fromAxisAngle(twist_axis, twist_angle), new Vector3f());
            Matrix4f to_origin = Matrix4f.fromTransform(new Quaternion(), new Vector3f(-p.x, -p.y, -p.z));
            Matrix4f back = Matrix4f.fromTransform(new Quaternion(), p);
            animMatrices.put(frame, back.mult(null, rot).mult(null, to_origin).mult(null, m));
        }
        for (SkeletonNode child : frame.children) {
            updateBoneMatrix(child);
        }
    }

    private static boolean isInSubtree(SkeletonNode node, SkeletonNode root) {
        if (root == null) return true;
        for (SkeletonNode current = node; current != null; current = current.parent) {
            if (current == root) return true;
        }
        return false;
    }

    /**
     * Updates bone transformation matrices for all nodes in the skeleton hierarchy.
     */
    public void updateBonesMatrices() {
        animMatrices.clear();
        updateBoneMatrix(skeleton);
        if (boneMatrices == null || boneMatrices.length != numBone) {
            boneMatrices = new Matrix4f[numBone];
            for (int i = 0; i < numBone; i++) {
                boneMatrices[i] = new Matrix4f();
            }
        }
        for (int i = 0; i < boneMatrices.length; i++) {
            if (boneMatrices[i] == null) {
                boneMatrices[i] = new Matrix4f();
            } else {
                boneMatrices[i].setIdentity();
            }
        }
        for (SkeletonNode frame : animMatrices.keySet()) {
            if (frame.boneID == -1 || frame.InverseBoneMatrix == null) {
                continue;
            }
            if (frame.boneNum < 0 || frame.boneNum >= boneMatrices.length) {
                continue;
            }
            boneMatrices[frame.boneNum].set(animMatrices.get(frame).mult(null, frame.InverseBoneMatrix));
        }
    }

    /**
     * Deletes the Animator instance and releases associated resources.
     */
    public void delete() {
        animMatrices = null;
        boneMatrices = null;
        control = null;
        from_animation = null;
        from_pose = null;
    }
}
