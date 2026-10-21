package com.forcex.anim;

/**
 * AnimationControl class manages animation playback and control.
 */
public class AnimationControl {
    boolean loop = false;   // Indicates whether the animation should loop.
    boolean play = true;    // Indicates whether the animation is currently playing.

    public float time = 0.0f;   // Current time of the animation.
    public float speed = 1.0f;  // Speed at which the animation is playing.

    /**
     * Resets the animation to its initial state (time, speed, playback and loop).
     */
    public void reset() {
        time = 0.0f;
        speed = 1.0f;
        play = true;
        loop = false;
    }

    /**
     * Starts or resumes playback.
     */
    public void play() {
        play = true;
    }

    /**
     * Pauses playback.
     */
    public void pause() {
        play = false;
    }

    /**
     * Enables or disables looping mode.
     *
     * @param value True to loop the animation, false to play it only once.
     */
    public void setLoop(boolean value) {
        loop = value;
    }

    /**
     * Check if the animation is currently running (playing).
     *
     * @return True if the animation is playing, false otherwise.
     */
    public boolean isRunning() {
        return play;
    }
}
