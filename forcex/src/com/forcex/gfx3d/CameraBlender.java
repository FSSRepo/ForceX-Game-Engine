package com.forcex.gfx3d;

import com.forcex.math.Vector3f;

/**
 * Transition between two camera modes, with the same two-phase scheme as GTA SA's
 * {@code CCamera::StartTransition}.
 * <p>
 * Each frame the active mode computes where it wants the camera ({@code position}), what it looks at
 * (target point) and its field of view, and {@link #update} replaces them with the blended values.
 * Position, target and FOV are blended with the cosine curve {@code 0.5 - 0.5 cos(pi x)}:
 * <ul>
 *   <li>Phase A (first {@code interToStop} of the time): from the pose of the previous mode towards the
 *   pose the new mode had when the transition began.</li>
 *   <li>Phase B (the remaining {@code catchUp}): from that pose to the new mode's live pose, so the
 *   camera catches up with a moving target.</li>
 * </ul>
 * The previous pose is the result of the last {@code update}, so call {@code update} every frame, also
 * when no transition is running.
 */
public class CameraBlender {
    private final Vector3f last_position = new Vector3f();
    private final Vector3f last_target = new Vector3f();
    private float last_fov = 60.0f;
    private boolean has_last = false;

    private final Vector3f from_position = new Vector3f();
    private final Vector3f from_target = new Vector3f();
    private float from_fov;
    private final Vector3f base_position = new Vector3f();
    private final Vector3f base_target = new Vector3f();
    private float base_fov;
    private boolean first_frame = false;

    private float duration = 0.0f;
    private float time = 0.0f;
    private float inter_to_stop = 0.25f;
    private float catch_up = 0.75f;
    private boolean active = false;

    private final Vector3f tmp = new Vector3f();
    private final Vector3f live = new Vector3f();

    /**
     * Begins a transition from the pose produced by the last {@link #update}. Call it the frame the mode
     * changes, before that frame's {@code update}.
     *
     * @param seconds     Transition time; zero or less is a hard cut.
     * @param interToStop Fraction of the time spent moving from the old pose to the new mode's starting pose
     *                    (SA's "InterToStopMoving").
     * @param catchUp     Fraction spent catching up with the new mode's live pose (SA's "CatchUp").
     */
    public void start(float seconds, float interToStop, float catchUp) {
        if (seconds <= 0.0f || !has_last) {
            active = false;
            return;
        }
        from_position.set(last_position);
        from_target.set(last_target);
        from_fov = last_fov;
        duration = seconds;
        time = 0.0f;
        if (!Float.isFinite(seconds) || !Float.isFinite(interToStop) || !Float.isFinite(catchUp)
                || interToStop < 0.0f || catchUp < 0.0f || interToStop + catchUp <= 0.0f) {
            throw new IllegalArgumentException("Transition duration and phase fractions must be finite and nonnegative");
        }
        float total = interToStop + catchUp;
        inter_to_stop = interToStop / total;
        catch_up = catchUp / total;
        first_frame = true;
        active = true;
    }

    public void cancel() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    /** Copies the last interpolated look target into a caller-owned vector. */
    public Vector3f getTarget(Vector3f result) {
        return result.set(last_target);
    }

    /** Records a post-collision camera pose so interrupted transitions start at the visible pose. */
    public void captureOutput(Camera camera, Vector3f target) {
        last_position.set(camera.position);
        last_target.set(target);
        last_fov = camera.fov;
        has_last = true;
    }

    /**
     * Normalized progress of the running transition (1 when none is running).
     */
    public float getProgress() {
        return active ? time / duration : 1.0f;
    }

    private static float cosine(float x) {
        return 0.5f - 0.5f * (float) Math.cos(Math.PI * x);
    }

    /**
     * Blends the pose the active mode just computed and applies the result to the camera.
     *
     * @param camera Camera already placed by the active mode ({@code camera.position} is the desired position).
     * @param target Point the active mode looks at.
     * @param fov    Field of view the active mode wants (degrees).
     * @param dt     Elapsed time in seconds.
     */
    public void update(Camera camera, Vector3f target, float fov, float dt) {
        float out_fov = fov;
        if (active) {
            if (first_frame) {
                base_position.set(camera.position);
                base_target.set(target);
                base_fov = fov;
                // Without phase A, catch up directly from the previous output (aim mode).
                if (inter_to_stop == 0.0f) {
                    base_position.set(from_position);
                    base_target.set(from_target);
                    base_fov = from_fov;
                }
                first_frame = false;
            }
            time += Math.max(0.0f, dt);
            float t = Math.min(1.0f, time / duration);
            if (t >= 1.0f) {
                // Finish at the live pose even when catchUp is zero or dt overshoots.
                active = false;
            } else if (t <= inter_to_stop) {
                // phase A: old pose -> pose of the new mode at the start
                float s = inter_to_stop > 0.0f ? (inter_to_stop - t) / inter_to_stop : 0.0f;
                float w = cosine(s);
                camera.position.set(base_position).addLocal(tmp.set(from_position).subLocal(base_position).multLocal(w));
                tmp.set(from_target).subLocal(base_target).multLocal(w);
                target = tmp.addLocal(base_target);
                out_fov = base_fov + (from_fov - base_fov) * w;
            } else {
                // phase B: that pose -> live pose of the new mode
                float u = catch_up > 0.0f ? Math.min(1.0f, (t - inter_to_stop) / catch_up) : 1.0f;
                float w = cosine(u);
                live.set(camera.position);
                camera.position.set(base_position).addLocal(tmp.set(live).subLocal(base_position).multLocal(w));
                tmp.set(target).subLocal(base_target).multLocal(w);
                target = tmp.addLocal(base_target);
                out_fov = base_fov + (fov - base_fov) * w;
            }
        }
        camera.lookAt(target);
        camera.setFieldOfView(out_fov);
        last_position.set(camera.position);
        last_target.set(target);
        last_fov = out_fov;
        has_last = true;
    }
}
