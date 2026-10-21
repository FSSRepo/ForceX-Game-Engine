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

    /**
     * Registers an event at the given time. The {@link Animator} fires it each time the playback
     * time crosses it (once per loop when the animation loops).
     *
     * @param time     Time in seconds, from the start of the animation.
     * @param listener Receiver of the event.
     */
    public void addEvent(float time, AnimationEventListener listener) {
        events.add(new Event(time, listener));
    }

    public void clearEvents() {
        events.clear();
    }

    /**
     * Fires the events whose time lies in (from, to]. Used by the Animator.
     */
    void fireEvents(float from, float to) {
        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);
            if (e.time > from && e.time <= to) {
                e.listener.onAnimationEvent(this, e.time);
            }
        }
    }

    boolean hasEvents() {
        return !events.isEmpty();
    }

    private final ArrayList<Event> events = new ArrayList<Event>();

    private static class Event {
        final float time;
        final AnimationEventListener listener;

        Event(float time, AnimationEventListener listener) {
            this.time = time;
            this.listener = listener;
        }
    }
}
