package com.forcex.rmlui;

import com.forcex.FX;
import com.forcex.core.NativeLoader;
import com.forcex.io.BinaryStreamReader;
import com.forcex.io.FileSystem;

/**
 * Entry point of the RmlUi extension (HTML/CSS for game interfaces).
 * <p>
 * All methods must be invoked from the thread that owns the engine's
 * OpenGL/GLES context: RmlUi creates and updates textures (glyphs, SVG, Lottie)
 * during update and render.
 * <p>
 * Files (.rml, .rcss, fonts, images and animations) are loaded through
 * {@link FX#fs}, so they work the same from jar resources, FX packages or
 * Android assets.
 */
public final class RmlUI {

    static {
        NativeLoader.init("fxrmlui");
    }

    private RmlUI() {
    }

    /** RmlUi version compiled into the native library. */
    public static String getVersion() {
        return nativeGetVersion();
    }

    /**
     * Initializes RmlUi, installs the native interfaces and creates the main
     * context with the given size.
     *
     * @return true if initialization succeeded (or was already initialized).
     */
    public static boolean init(int width, int height) {
        return nativeInit(width, height);
    }

    /** Releases RmlUi and all its documents. Java handles become invalid. */
    public static void shutdown() {
        nativeShutdown();
    }

    /** Updates the renderer viewport and the size of the main context. */
    public static void resize(int width, int height) {
        nativeResize(width, height);
    }

    /**
     * Loads a font so that it can be used in stylesheets.
     *
     * @param path path of the .ttf/.otf file resolved by {@link FX#fs}.
     * @return true if the font was registered successfully.
     */
    public static boolean loadFont(String path) {
        return nativeLoadFont(path);
    }

    /** Main context created by {@link #init(int, int)}. */
    public static RmlContext getContext() {
        long handle = nativeGetContextHandle();
        return handle == 0L ? null : new RmlContext(handle);
    }

    /** Loads an RML document from a string into the main context. */
    public static RmlDocument loadDocument(String rml) {
        RmlContext context = getContext();
        return context == null ? null : context.loadDocument(rml);
    }

    /** Updates the RML content of an element in the main context. */
    public static void setInnerRml(String elementId, String rml) {
        RmlContext context = getContext();
        if (context != null) {
            context.setInnerRml(elementId, rml);
        }
    }

    /** Updates the main context (events, animations, data bindings). */
    public static void update() {
        nativeUpdate();
    }

    /** Draws the main context into the current framebuffer. */
    public static void render() {
        nativeRender();
    }

    // ------------------------------------------------------------ internal ----

    /**
     * Native FileInterface bridge: opens a file through the engine's file
     * system. Returns null if the file does not exist.
     */
    static byte[] readFile(String path) {
        if (FX.fs == null) {
            return null;
        }
        BinaryStreamReader reader = FX.fs.open(path, FileSystem.ReaderType.MEMORY);
        return reader == null ? null : reader.getData();
    }

    /** Factory used by the bindings to deliver events to Java. */
    static RmlEvent newEvent(String type, String targetId, String currentId) {
        return new RmlEvent(type, targetId, currentId);
    }

    private static native String nativeGetVersion();

    private static native boolean nativeInit(int width, int height);

    private static native void nativeShutdown();

    private static native void nativeResize(int width, int height);

    private static native boolean nativeLoadFont(String path);

    private static native long nativeGetContextHandle();

    private static native void nativeUpdate();

    private static native void nativeRender();
}
