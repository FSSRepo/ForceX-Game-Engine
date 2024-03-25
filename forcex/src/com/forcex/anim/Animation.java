package com.forcex.anim;

import java.util.ArrayList;

/**
 * Represents an animation with a collection of bones and their keyframes.
 */
public class Animation {

    // Total duration of the animation
    private float duration = 0;

    // List of bones involved in the animation
    private final ArrayList<Bone> bones = new ArrayList<Bone>();

    // Name of the animation
    public String name = "";

    /**
     * Constructor to create an Animation object with a given name.
     *
     * @param name The name of the animation.
     */
    public Animation(String name) {
        this.name = name;
    }

    /**
     * Adds a bone to the animation's collection of bones.
     *
     * @param bone The Bone object to be added.
     */
    public void addBone(Bone bone) {
        bones.add(bone);
    }

    /**
     * Finds and returns a specific bone within the animation.
     *
     * @param index The index of the bone to be searched for.
     * @return The found Bone object, or null if not found.
     */
    public Bone findBone(int index) {
        for (int i = 0; i < bones.size(); i++) {
            if (bones.get(i).boneID == index) {
                return bones.get(i);
            }
        }
        return null;
    }

    /**
     * Calculates and returns the total duration of the animation.
     *
     * @return The total duration of the animation.
     */
    public float getDuration() {
        if (duration == 0) {
            for (Bone bone : bones) {
                for (KeyFrame key : bone.keyframes) {
                    if (key.time > duration) {
                        duration = key.time;
                    }
                }
            }
        }
        return duration;
    }

    /**
     * Retrieves the root bone of the animation.
     *
     * @return The root Bone object of the animation.
     */
    public Bone getRootBone() {
        return bones.get(0);
    }
}
