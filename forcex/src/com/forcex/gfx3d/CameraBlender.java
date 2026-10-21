package com.forcex.gfx3d;

import com.forcex.math.Vector3f;

/**
 * Smooth transition between two camera modes.
 * <p>
 * When a mode switch happens, {@link #start(Camera, float)} snapshots the current pose (position,
 * view direction and field of view). Each frame the new mode places the camera where it wants it
 * and then {@link #update(Camera, float, float)} pulls the camera back towards the snapshot,
 * releasing it over the transition time. The snapshot stays frozen while the destination keeps
 * moving, so the camera "catches up" with a moving target.
 */
public class CameraBlender {
    /** Maps the normalized transition time (0..1) to the blend weight of the new mode (0..1). */
    public interface Easing {
        float ease(float t);
    }

    /** Smooth start and end: 3t^2 - 2t^3. */
    public static final Easing SMOOTHSTEP = new Easing() {
        @Override
        public float ease(float t) {
            return t * t * (3.0f - 2.0f * t);
        }
    };

    public static final Easing LINEAR = new Easing() {
        @Override
        public float ease(float t) {
            return t;
        }
    };

    private final Vector3f from_position = new Vector3f();
    private final Vector3f from_direction = new Vector3f(0, 0, -1);
    private final Vector3f look_point = new Vector3f();
    private float from_fov = 60.0f;
    private float duration = 0.0f;
    private float time = 0.0f;
    private boolean active = false;
    private Easing easing = SMOOTHSTEP;

    public void setEasing(Easing easing) {
        this.easing = easing;
    }

    /**
     * Begins a transition from the camera's current pose.
     *
     * @param camera  Camera whose pose is captured.
     * @param seconds Transition time; zero or less is a hard cut (no transition).
     */
    public void start(Camera camera, float seconds) {
        if (seconds <= 0.0f) {
            active = false;
            return;
        }
        from_position.set(camera.position);
        from_direction.set(camera.direction);
        from_fov = camera.fov;
        duration = seconds;
        time = 0.0f;
        active = true;
    }

    public void cancel() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Normalized progress of the running transition (1 when none is running).
     */
    public float getProgress() {
        return active ? time / duration : 1.0f;
    }

    /**
     * Blends the pose the active mode just computed with the snapshot taken by {@link #start}.
     * Does nothing when no transition is running.
     *
     * @param camera     Camera already placed by the active mode.
     * @param desiredFov Field of view the active mode wants this frame.
     * @param dt         Elapsed time in seconds.
     */
    public void update(Camera camera, float desiredFov, float dt) {
        if (!active) {
            camera.setFieldOfView(desiredFov);
            return;
        }
        time += dt;
        float t = Math.min(1.0f, time / duration);
        float w = easing.ease(t);

        // position and direction towards the snapshot by (1 - w)
        camera.position.set(from_position.lerp(camera.position, w));
        Vector3f direction = from_direction.lerp(camera.direction, w);
        if (direction.length() < 1e-4f) {
            direction.set(camera.direction);
        }
        look_point.set(camera.position).addLocal(direction);
        camera.lookAt(look_point);
        camera.setFieldOfView(from_fov + (desiredFov - from_fov) * w);

        if (t >= 1.0f) {
            active = false;
        }
    }
}
