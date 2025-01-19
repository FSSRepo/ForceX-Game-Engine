package com.forcex.windows;

import com.forcex.FX;
import com.forcex.app.Game;
import com.forcex.app.InputListener;
import com.forcex.core.SystemDevice;
import com.forcex.io.BinaryStreamReader;
import com.forcex.io.FileSystem;
import com.forcex.utils.Image;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;

public class ForceXApp implements SystemDevice {

    private GLRenderer renderer;

    // The window handle
    protected long window;

    InputProcessor input_processor;

    public void initialize(Game game, FXAppConfig config) {
        FX.fs = new FileSystem() {
            @Override
            protected InputStream getAndroidAsset(String name) {
                return null;
            }
        };
        FileSystem.homeDirectory = config.resources_dir.length() == 0 ? "data/" : config.resources_dir;
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        PointerBuffer pb = glfwGetMonitors();
        assert pb != null;
//        long monitorId = pb.get(1);

        long primaryMonitor = glfwGetPrimaryMonitor();

        // Create the window
        window = glfwCreateWindow(config.width, config.height, config.title, config.fullscreen ? primaryMonitor : NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        if(!config.fullscreen) {
            try ( MemoryStack stack = stackPush() ) {
                IntBuffer pWidth = stack.mallocInt(1); // int*
                IntBuffer pHeight = stack.mallocInt(1); // int*
                // Get the window size passed to glfwCreateWindow
                glfwGetWindowSize(window, pWidth, pHeight);
                // Get the resolution of the primary monitor
                GLFWVidMode vidmode = glfwGetVideoMode(primaryMonitor);
                // Center the window
                glfwSetWindowPos(
                        window,
                        (vidmode.width() - pWidth.get(0)) / 2,
                        (vidmode.height() - pHeight.get(0)) / 2
                );
            }
        }

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(config.vsync ? 1 : 0);
        // Make the window visible
        glfwShowWindow(window);

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0);

        boolean profile = false;
        if (profile) {
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE);
        } else {
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        }

        if(config.show_cursor) {
            // hide cursor
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
        }

        renderer = new GLRenderer(game, this);

        FX.device = this;
        FX.al = new WindowsAL();
        FX.alc = new WindowsSound();
        input_processor = new InputProcessor(this);
        input_processor.init();

        if (config.icon_path.length() > 0) {
            int[] icons_sizes = {16, 32, 128};
            ArrayList<ByteBuffer> icons_loaded = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                ByteBuffer icon_buf = null;
                if (new File(String.format(config.icon_path, icons_sizes[i])).exists()) {
                    icon_buf = new Image(String.format(config.icon_path, icons_sizes[i])).getBuffer();
                } else {
                    BinaryStreamReader is = FX.fs.open(String.format(config.icon_path, icons_sizes[i]), FileSystem.ReaderType.MEMORY);
                    if(is != null) {
                        icon_buf = new Image(is.getData()).getBuffer();
                    } else {
                        continue;
                    }
                }
                icons_loaded.add(icon_buf);
            }
            try (GLFWImage.Buffer icons_buffer = GLFWImage.malloc(icons_loaded.size()) ) {
                for(int i = 0; i < icons_loaded.size(); i ++) {
                    icons_buffer.position(i)
                                .width(icons_sizes[i])
                                .height(icons_sizes[i])
                                .pixels(icons_loaded.get(i));
                }
                glfwSetWindowIcon(window, icons_buffer);
            }
        }

        int[] arrWidth = new int[1];
        int[] arrHeight = new int[1];
        glfwGetFramebufferSize(window, arrWidth, arrHeight);
        renderer.width = arrWidth[0];
        renderer.height = arrHeight[0];
        glfwSetFramebufferSizeCallback(window, (window, w, h) -> renderer.resize(w, h));
        renderer.create();
        renderer.loop();
    }

    protected boolean isCloseRequested() {
        return glfwWindowShouldClose(window);
    }

    protected void update() {
        glfwPollEvents();
        glfwSwapBuffers(window);
    }

    protected boolean isVisible() {
        return glfwGetWindowAttrib(window, GLFW_VISIBLE) == 1;
    }

    protected void destroyWindow() {
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
        }
    }

    @Override
    public void addInputListener(InputListener input) {
        this.input_processor.inputs.add(input);
    }


    @Override
    public void stopRender() {
        renderer.destroy = false;
    }

    @Override
    public void showInfo(String info, boolean isError) {

    }

    @Override
    public boolean isJDKDesktop() {
        return true;
    }

    @Override
    public void destroy() {
        renderer.destroy = true;
    }

    @Override
    public void setCursorState(boolean show) {
        glfwSetInputMode(window, GLFW_CURSOR, show ? GLFW_CURSOR_NORMAL : GLFW_CURSOR_DISABLED);
    }

    @Override
    public int getAndroidVersion() {
        return 0;
    }

    @Override
    public void invokeFileChooser(boolean arg0, String arg1, String arg2, OnAndroidFileStream arg3) {
        // TODO Auto-generated method stub

    }
}
