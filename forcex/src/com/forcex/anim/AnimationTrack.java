package com.forcex.anim;
import java.util.*;

/**
 * AnimationTrack class manages animation frames and timing.
 */
public class AnimationTrack {

    private ArrayList<Track> frames = new ArrayList<>();
    private float track_timer = 0.0f;
    private float duration = 0f;
    private boolean repeat = false;
    private onInterpolateListener listener;

    /**
     * Track class holds information about each animation frame.
     */
    public static class Track {
        float time;
        Object object;

        /**
         * Constructor for Track class.
         * @param time The time at which this frame occurs.
         * @param object The object representing the frame's state.
         */
        public Track(float time, Object object) {
            this.time = time;
            this.object = object;
        }
    }

    /**
     * Interface for handling interpolation between frames.
     */
    public static interface onInterpolateListener {
        /**
         * Interpolates between two frames' states.
         * @param prv The previous frame's state.
         * @param nxt The next frame's state.
         * @param p The progress between previous and next frames (0 to 1).
         */
        public void interpolate(Object prv, Object nxt, float p);
    }

    /**
     * Sets the interpolation listener.
     * @param interpolation The interpolation listener.
     */
    public void setInterpolation(onInterpolateListener interpolation) {
        listener = interpolation;
    }

    /**
     * Adds a frame to the animation.
     * @param time The time at which the frame occurs.
     * @param obj The object representing the frame's state.
     */
    public void add(float time, Object obj) {
        frames.add(new Track(time, obj));
        updateDuration();
    }

    /**
     * Updates the total duration of the animation.
     */
    public void updateDuration() {
        duration = 0;
        for (Track f : frames) {
            if (f.time > duration) {
                duration = f.time;
            }
        }
    }

    /**
     * Retrieves the previous frame.
     * @return The previous frame.
     */
    private Track getPrevius() {
        for (short f = 0; f < frames.size(); f++) {
            if (track_timer <= frames.get(f).time) {
                if (f == 0) {
                    if (frames.size() != 1) {
                        return frames.get(frames.size() - 1);
                    } else {
                        return frames.get(f);
                    }
                } else {
                    return frames.get(f - 1);
                }
            }
        }
        return frames.get(frames.size() - 1);
    }

    /**
     * Retrieves the next frame.
     * @return The next frame.
     */
    private Track getNext() {
        for (short f = 0; f < frames.size(); f++) {
            if (track_timer <= frames.get(f).time) {
                return frames.get(f);
            }
        }
        return frames.get(frames.size() - 1);
    }

    /**
     * Stops the animation.
     */
    public void stop() {
        listener = null;
        track_timer = 0;
        frames.clear();
    }

    /**
     * Sets whether the animation should repeat.
     * @param z Whether to repeat the animation.
     */
    public void setRepeat(boolean z) {
        repeat = z;
    }

    /**
     * Retrieves the current track time.
     * @return The current track time.
     */
    public float getTrackTime() {
        return track_timer;
    }

    /**
     * Retrieves the duration of the animation.
     * @return The duration of the animation.
     */
    public float getDuration() {
        return duration;
    }

    /**
     * Updates the animation based on time delta.
     * @param delta The time delta.
     */
    public void update(float delta) {
        if (listener != null) {
            if (repeat) {
                track_timer %= duration;
            } else if (track_timer > duration) {
                return;
            }
            Track prv = getPrevius();
            Track nxt = getNext();
            float totalTime = nxt.time - prv.time;
            float currentTime = track_timer - prv.time;
            listener.interpolate(prv.object, nxt.object, currentTime / totalTime);
            track_timer += delta;
        }
    }
}
