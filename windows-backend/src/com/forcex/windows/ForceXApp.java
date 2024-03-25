package com.forcex.windows;

import com.forcex.FX;
import com.forcex.app.Game;
import com.forcex.app.InputListener;
import com.forcex.core.SystemDevice;
import com.forcex.utils.Image;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class ForceXApp implements SystemDevice {

    GLRenderer render;
    ArrayList<InputListener> inputs;

	private static final int DEFAULT_WIDTH = 1280;
	private static final int DEFAULT_HEIGHT = 1280;

    public void initialize(Game game, String title) {
        initialize(game, title, false);
    }

    public void initialize(Game game, String title, String icon_path) {
        initialize(game, title, false, icon_path);
    }

    public void initialize(Game game, String title,
                           boolean fullscreen) {
        initialize(game, title, DEFAULT_WIDTH, DEFAULT_HEIGHT, fullscreen, false, "");
    }

    public void initialize(Game game, String title,
                           boolean fullscreen, String icon_path) {
        initialize(game, title, DEFAULT_WIDTH, DEFAULT_HEIGHT, fullscreen, false, icon_path);
    }

    public void initialize(Game game, String title,
                           boolean fullscreen, boolean vsync) {
        initialize(game, title, DEFAULT_WIDTH, DEFAULT_HEIGHT, fullscreen, vsync, "");
    }

    public void initialize(Game game, String title, int width, int height, boolean fullscreen, boolean vsync, String icon_path) {
        FX.homeDirectory = "data/";
        ContextAttribs attribs = new ContextAttribs(3, 0);
        attribs.withForwardCompatible(true);
        try {
            setDisplayMode(width, height, fullscreen);
            Display.create(new PixelFormat(), attribs);
        } catch (LWJGLException e) {
            e.printStackTrace();
        }
        inputs = new ArrayList<>();
        render = new GLRenderer(game, this, vsync, width, height);
        FX.device = this;
        FX.al = new WindowsAL();
        FX.alc = new WindowsSound();
        Display.setVSyncEnabled(vsync);
        Display.setTitle(title);
        if (icon_path.length() > 0) {
            int[] icons_sizes = {16, 32, 128};
            ByteBuffer[] icons = new ByteBuffer[3];
            for (int i = 0; i < 3; i++) {
                if (new File(String.format(icon_path, icons_sizes[i])).exists()) {
                    icons[i] = new Image(String.format(icon_path, icons_sizes[i])).getBuffer();
                } else {
                    icons[i] = ByteBuffer.allocate(1).order(ByteOrder.nativeOrder());
                }
            }
            Display.setIcon(icons);
        }
        render.create();
    }

    public void setTitle(String text) {
        Display.setTitle(text);
    }

    @Override
    public void addInputListener(InputListener input) {
        this.inputs.add(input);
    }

    private void setDisplayMode(int width, int height, boolean fullscreen) {
        // return if requested DisplayMode is already set
        if ((Display.getDisplayMode().getWidth() == width) &&
                (Display.getDisplayMode().getHeight() == height) &&
                (Display.isFullscreen() == fullscreen)) {
            return;
        }
        try {
            DisplayMode targetDisplayMode = null;

            if (fullscreen) {
                DisplayMode[] modes = Display.getAvailableDisplayModes();
                int freq = 0;
                for (int i = 0; i < modes.length; i++) {
                    DisplayMode current = modes[i];

                    if ((current.getWidth() == width) && (current.getHeight() == height)) {
                        if ((targetDisplayMode == null) || (current.getFrequency() >= freq)) {
                            if ((targetDisplayMode == null) || (current.getBitsPerPixel() > targetDisplayMode.getBitsPerPixel())) {
                                targetDisplayMode = current;
                                freq = targetDisplayMode.getFrequency();
                            }
                        }

                        if ((current.getBitsPerPixel() == Display.getDesktopDisplayMode().getBitsPerPixel()) &&
                                (current.getFrequency() == Display.getDesktopDisplayMode().getFrequency())) {
                            targetDisplayMode = current;
                            break;
                        }
                    }
                }
            } else {
                targetDisplayMode = new DisplayMode(width, height);
            }
            if (targetDisplayMode == null) {
                System.out.println("Failed to find value mode: " + width + "x" + height + " fs=" + fullscreen);
                return;
            }
            Display.setDisplayMode(targetDisplayMode);
            Display.setFullscreen(fullscreen);
        } catch (LWJGLException e) {
            System.out.println("Unable to setup mode " + width + "x" + height + " fullscreen=" + fullscreen + e);
        }
    }

    @Override
    public void stopRender() {
        render.running = false;
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
        render.destroy = true;
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
