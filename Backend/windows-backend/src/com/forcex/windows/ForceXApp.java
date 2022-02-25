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
import java.io.InputStream;
import java.util.*;
import java.io.*;

public class ForceXApp implements SystemDevice{

    GLRenderer render;
	ArrayList<InputListener> inputs;
	private SHA1Checksum.SHA1[] files = {
		new SHA1Checksum.SHA1("gui/file.png","be30926161b4aca7878ab8d8c0bd06968b2f4612"),
		new SHA1Checksum.SHA1("gui/folder.png","1cef8674b72fe1d2fde800a9d67f320ce81b864f"),
		new SHA1Checksum.SHA1("gui/close.png","adb048f45479b591b7a6df63f80f1efded271e46"),
		new SHA1Checksum.SHA1("gui/backspace.png","16988a74a31c343baaa6131e219d915809a6e1a3"),
		new SHA1Checksum.SHA1("gui/enter.png","51bcb8e7452bb54ad92de11358cff99ed25879e5"),
		new SHA1Checksum.SHA1("gui/parent.png","b572b94bc5247ed24756e6c3e7c20624e96ab2c0"),
		new SHA1Checksum.SHA1("gui/fd_icon.png","87a8efa6295f7926aa6f22616832b6344c83a7de"),
		new SHA1Checksum.SHA1("gui/circle.png","551596f4ff3122313d0a04960c826108b202c01c"),
		new SHA1Checksum.SHA1("gui/shift.png","d79ca41d4d87114eb986443e6f751d28adfddfd7"),
		new SHA1Checksum.SHA1("shaders/blur.vs","100ab1d6f3fd8b5238b00f13b3414180d71aa979"),
		new SHA1Checksum.SHA1("shaders/sprite.fs","c5700f1479adfe4a9f131dcea498d8107b93fb9b"),
		new SHA1Checksum.SHA1("shaders/water.fs","38df9f2326690fea1c84b433bc1c528af41512e8"),
		new SHA1Checksum.SHA1("shaders/default.fs","78fa3f896c2cf92f491a4b941972e14b2322f11b"),
		new SHA1Checksum.SHA1("shaders/default.vs","0de2882c2403aa395fdae69d5b34f79a55876e63"),
		new SHA1Checksum.SHA1("shaders/lens.fs","9be5bfae85d445738ca47354c3a25ce62aa2fd4d"),
		new SHA1Checksum.SHA1("shaders/water.vs","24c7449eda12c8d8d1e04e7d62d733cd4a6e888f"),
		new SHA1Checksum.SHA1("shaders/fxaa.fs","5499a2ed217d9564dd0768d875bfa129d4459121"),
		new SHA1Checksum.SHA1("shaders/fxaa.vs","7149a1aa52e897e03ec3725d341a6622495fcf11"),
		new SHA1Checksum.SHA1("shaders/sprite.vs","1c9bdbe3f986d91cbe542d8fb639c7e375b0b1df"),
		new SHA1Checksum.SHA1("shaders/shadow.fs","c2e990255c0ec645b7aff3c35f286bea351ae377"),
		new SHA1Checksum.SHA1("shaders/shadow.vs","d0158a20f7b32c9ccca3d1b977912e7332387840"),
		new SHA1Checksum.SHA1("shaders/lens.vs","a1781c3c134a98828924554695b6ef56f2acd7ae"),
		new SHA1Checksum.SHA1("shaders/blur.fs","f89f779303aefd5035bcf459b722416eaeeedbc6"),
		new SHA1Checksum.SHA1("fonts/diploma.fft","c6ebbdc3ee5a039f7ba5010d09a3603481eb1196"),
		new SHA1Checksum.SHA1("fonts/arial.fft","445dbc6c9d5982ba1cc12aa306665850cedf34c1"),
		new SHA1Checksum.SHA1("fonts/digital.fft","8144b9feaa90e04f18638d241ed85eb58bfe2849"),
		new SHA1Checksum.SHA1("fonts/windows.fft","4aa80e50fd81ee6e7c78a4f25701a45c73be8557"),
		new SHA1Checksum.SHA1("fonts/century.fft","2a90e7dc98c3eac03e3845406aab3a532bf0b449"),
		
	};
	public void initialize(Game game,String title){
		initialize(game,title,false);
	}
	
	public void initialize(Game game,String title,
						   boolean fullscreen){
		initialize(game,title,1280,720,fullscreen,false);
	}
	
    public void initialize(Game game,String title,
						   boolean fullscreen,boolean vsync){
		initialize(game,title,1280,720,fullscreen,vsync);
	}
	private String getDocumentsFolder(){
		String myDocuments = null;

		try {
			Process p =  Runtime.getRuntime().exec("reg query \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders\" /v personal");
			p.waitFor();
			InputStream in = p.getInputStream();
			byte[] b = new byte[in.available()];
			in.read(b);
			in.close();
			myDocuments = new String(b);
			myDocuments = myDocuments.split("\\s\\s+")[4];
		} catch(Throwable t) {
			t.printStackTrace();
		}
		return myDocuments.replace('\\','/');
	}
	
    public void initialize(Game game,String title,int width,int height,
		boolean fullscreen,boolean vsync){
			FX.homeDirectory = getDocumentsFolder() + "/ForceX/";
		checkEssencials();
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
		if(!SHA1Checksum.check(FX.homeDirectory,files)){
			return true;
		}
		return false;
	}
	
	private void checkEssencials(){
		if(checkFiles()){
			mkDirs("fonts","gui","shaders");
			extractAssets(files);
		}
	}
	
	public void mkDirs(String... dirs){
		File dir = new File(FX.homeDirectory);
		if(dir.exists()){
			for(String f : dirs){
				File mk = new File(dir.getAbsolutePath()+"/"+f);
				if(!mk.exists()){
					mk.mkdir();
				}
			}
		}else{
			dir.mkdir();
			for(String f : dirs){
				File mk = new File(dir.getAbsolutePath()+"/"+f);
				if(!mk.exists()){
					mk.mkdir();
				}
			}
		}
	}
	
	public void extractAssets(SHA1Checksum.SHA1[] fls){
		for(SHA1Checksum.SHA1 sha1 : fls){
			try{
				InputStream is = getClass().getClassLoader().getResourceAsStream(sha1.file);
				byte[] data = new byte[is.available()];
				is.read(data);
				OutputStream os = new FileOutputStream(FX.homeDirectory + sha1.file);
				os.write(data);
				os.close();
				is.close();
				data = null;
			}catch(Exception e){}
		}
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
}
