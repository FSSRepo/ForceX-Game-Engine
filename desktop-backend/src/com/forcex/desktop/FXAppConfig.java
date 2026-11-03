package com.forcex.desktop;

public class FXAppConfig {
    public int width = 1280;
    public int height = 720;
    public boolean vsync = false;
    public boolean fullscreen = false;
    public String title = "";
    public String icon_path = "";
    /**
     * The user can resize the window by dragging its edges or maximizing it. {@code Game.resize(w, h)} is called
     * with the new framebuffer size. Ignored in fullscreen and headless modes.
     */
    public boolean resizable = false;
    /** Minimum window size while {@link #resizable}; 0 means no limit on that axis. */
    public int min_width = 0;
    public int min_height = 0;
    public boolean show_cursor = false;
    public String resources_dir = "";
    /**
     * Headless: the OpenGL context is created on a hidden window that is never shown (no visible window,
     * no focus, no icon, never fullscreen). The game loop, rendering to framebuffers and input polling keep
     * working. Enabled by default with {@code -Dforcex.headless=true}.
     */
    public boolean headless = Boolean.getBoolean("forcex.headless");
}
