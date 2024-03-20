package com.forcex.windows;

import com.forcex.FX;
import com.forcex.app.Game;
import com.forcex.core.SystemDevice;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;
import java.nio.*;
import com.forcex.utils.*;
import com.forcex.app.*;
import java.util.*;
import java.io.*;

public class ForceXApp implements SystemDevice{

    GLRenderer render;
	ArrayList<InputListener> inputs;
	
	public void initialize(Game game,String title){
		initialize(game,title,false);
	}
	
	public void initialize(Game game,String title,
						   boolean fullscreen) {
		initialize(game,title,1280,720,fullscreen,false);
	}
	
    public void initialize(Game game,String title,
						   boolean fullscreen,boolean vsync) {
		initialize(game,title,1280,720,fullscreen,vsync);
	}
	
    public void initialize(Game game,String title,int width,int height,
		boolean fullscreen, boolean vsync) {
		FX.homeDirectory = "data/";
		ContextAttribs attribs = new ContextAttribs(3,0);
        attribs.withForwardCompatible(true);
        try{
            setDisplayMode(width,height,fullscreen);
			Display.create(new PixelFormat(),attribs);
        }catch(LWJGLException e){
			e.printStackTrace();
		}
		inputs = new ArrayList<>();
        render = new GLRenderer(game,this,vsync,width,height);
        FX.device = this;
		FX.al = new WindowsAL();
		FX.alc = new WindowsSound();
		Display.setVSyncEnabled(vsync);
		Display.setTitle(title);
                
		render.create();
    }
	
	private boolean checkFiles(){
		if(!new File(FX.homeDirectory).exists()){
			return true; 
		}
		return false;
	}
	
	public void setTitle(String text){
		Display.setTitle(text);
	}

	@Override
	public void addInputListener(InputListener input) {
		this.inputs.add(input);
	}
	
	public void setIcon(String path){
		ByteBuffer[] icons = new ByteBuffer[1];
		icons[0] = new Image(path).getBuffer();
		Display.setIcon(icons);
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
				for (int i = 0;i < modes.length;i++) {
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
				targetDisplayMode = new DisplayMode(width,height);
			}
			if (targetDisplayMode == null) {
				System.out.println("Failed to find value mode: "+width+"x"+height+" fs="+fullscreen);
				return;
			}
			Display.setDisplayMode(targetDisplayMode);
			Display.setFullscreen(fullscreen);
		} catch (LWJGLException e) {
			System.out.println("Unable to setup mode "+width+"x"+height+" fullscreen="+fullscreen + e);
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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void invokeFileChooser(boolean arg0, String arg1, String arg2, OnAndroidFileStream arg3) {
		// TODO Auto-generated method stub
		
	}
}
