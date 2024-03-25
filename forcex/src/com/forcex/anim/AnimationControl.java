package com.forcex.anim;

/**
 * AnimationControl class manages animation playback and control.
 */
public class AnimationControl {
    boolean loop = false;   // Indicates whether the animation should loop.
    boolean play = true;    // Indicates whether the animation is currently playing.

    // Command constants for controlling the animation.
    public static final byte CMD_RESET = 0;    // Reset the animation to its initial state.
    public static final byte CMD_PLAY = 1;     // Start or resume playback.
    public static final byte CMD_PAUSE = 2;    // Pause playback.
    public static final byte CMD_LOOP = 3;     // Enable looping mode.
    public static final byte CMD_NO_LOOP = 4;  // Disable looping mode.

    public float time = 0.0f;   // Current time of the animation.
    public float speed = 1.0f;  // Speed at which the animation is playing.

    /**
     * Executes a command to control the animation playback.
     *
     * @param cmd The command to execute (CMD_RESET, CMD_PLAY, CMD_PAUSE, CMD_LOOP, CMD_NO_LOOP).
     */
    public void putCommand(byte cmd) {
        switch (cmd) {
            case CMD_RESET:
                time = 0.0f;       // Reset animation time to start.
                speed = 1.0f;      // Reset animation speed.
                play = true;       // Resume playback.
                loop = false;      // Disable looping.
                break;
            case CMD_PLAY:
                play = true;       // Resume playback.
                break;
            case CMD_PAUSE:
                play = false;      // Pause playback.
                break;
            case CMD_LOOP:
                loop = true;       // Enable looping.
                break;
            case CMD_NO_LOOP:
                loop = false;      // Disable looping.
                break;
        }
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
