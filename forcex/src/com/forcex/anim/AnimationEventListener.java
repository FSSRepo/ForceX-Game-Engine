package com.forcex.anim;

/**
 * Receives events placed at a given time inside an {@link Animation}
 * (see {@link Animation#addEvent(float, AnimationEventListener)}).
 */
public interface AnimationEventListener {
    /**
     * Called by the {@link Animator} the frame its playback time crosses the event time.
     *
     * @param animation The animation that raised the event.
     * @param time      The time (seconds) the event was registered at.
     */
    void onAnimationEvent(Animation animation, float time);
}
