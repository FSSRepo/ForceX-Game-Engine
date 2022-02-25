package com.forcex.windows;

import com.forcex.app.Game;
import com.forcex.app.threading.Task;
import com.forcex.core.gpu.GPUDevice;
import org.lwjgl.opengl.*;
import org.lwjgl.*;
import java.util.*;
import com.forcex.*;
import com.forcex.core.*;
import org.lwjgl.input.*;
import com.forcex.app.*;
import com.forcex.utils.*;

public class GLRenderer implements GPUDevice{
	Game game;
	int width;
	int height;
	float deltaTime;
	short fps,frames;
	long lastTime = 0;
	long frameStart = 0;
	String[] info;
	PoolArray<Task> tasks;
	Object synch = new Object();
	boolean created = false;
	boolean running = false;
	boolean pause = false;
	boolean resume = false;
	boolean destroy = false;
	boolean vsync = false;
	int FPS_LIMIT = 240;
	ForceXApp app;
	
    public GLRenderer(Game game,ForceXApp app,boolean vsync,int w,int h){
		width = w;
		height = h;
		this.vsync = vsync;
        this.game = game;
		FX.gpu = this;
		this.app = app;
		tasks = new PoolArray<>(10);
    }
	
   	void create(){
		if(FX.gl == null){
			FX.gl = new WindowsGL();
		}
		GL gl = FX.gl;
		info = new String[4];
		info[0] = gl.glGetString(GL.GL_VERSION);
		info[1] = gl.glGetString(GL.GL_VENDOR);
		info[2] = gl.glGetString(GL.GL_RENDERER);
		info[3] = gl.glGetString(GL.GL_EXTENSIONS).replace(' ','\n');
		gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LESS);
        gl.glClearDepthf(1.0f);
        gl.glDepthRangef(0.0f, 1.0f);
        gl.glDepthMask(true);
        gl.glClearColor(0.4f, 0.4f, 0.4f, 1.0f);
		game.create();
		FX.gl.glViewport(width,height);
		game.resize(width,height);
		lastTime = System.nanoTime();
		running = true;

        while(true){
			processStates();
			created = true;
			boolean ldestoy = false;
			boolean lpause = false;
			boolean lresume = false;
			synchronized(synch){
				ldestoy = destroy;
				lpause = pause;
				lresume = resume;
				if(resume){
					resume = false;
				}
				if(pause){
					pause = false;
					synch.notifyAll();
				}
				if(destroy){
					destroy = false;
					synch.notifyAll();
				}
			}

			if(lresume){
				running = true;
				lastTime = System.nanoTime();
				game.resume();
				System.out.println("resumed");
			}
			if(lpause){
				running = false;
				game.pause(0);
				System.out.println("Paused");
			}
			if(ldestoy || Display.isCloseRequested()){
				game.destroy();
				created = false;
				ldestoy = false;
				System.out.println("Destroyed");
				Display.destroy();
				break;
			}
			if(running){
				game.render(deltaTime);
				long time = System.nanoTime();
				deltaTime = (time - lastTime) / 1000000000.0f;
				lastTime = time;
				if(time - frameStart >= 1000000000){
					fps = frames;
					frames = 0;
					frameStart = time;
				}
				frames++;
			}
			Task t = null;
			while((t = tasks.pop()) != null){
				if(!t.execute()){
					tasks.push(t);
				}
			}
			if(!vsync){
				Display.sync(FPS_LIMIT);
			}
			updateInput();
			Display.update();
		}
    }
	
	@Override
	public int getWidth(){
		return width;
	}

	@Override
	public int getHeight(){
		return height;
	}

	@Override
	public String getOpenGLVersion(){
		return info[0];
	}

	@Override
	public String getGPUVendor(){
		return info[1];
	}

	@Override
	public String getGPUModel(){
		return info[2];
	}

	@Override
	public boolean hasOGLExtension(String extension){
		String[] exts = info[3].split("\n");
		for(String ext : exts){
			if(ext.contains(extension)){
				return true;
			}
		}
		return false;
	}

	@Override
	public int getFPS(){
		return fps;
	}

	@Override
	public boolean isOpenGLES(){
		return false;
	}

	@Override
	public float getDeltaTime(){
		return deltaTime;
	}

	@Override
	public void waitEmptyQueue()
	{
		/* caso especial por que si no existe 
			tiempo de espera por alguna razon este bucle 
			nunca termina */
		while(true){
			try{
				Thread.sleep(2);
			}
			catch (InterruptedException e)
			{}
			if (!hasTaskInQueue()) {
				return;
			}
		}
	}

	
	@Override
	public void queueTask(Task task){
		tasks.push(task);
	}
	
	@Override
	public boolean hasTaskInQueue(){
		return tasks.count() > 0;
	}
	
	boolean dpd = false;
	
	private void processStates(){
		if(created && Display.isVisible() && dpd){
			dpd = false;
			resume = true;
		}else if(created && !Display.isVisible() && !dpd){
			if(!running) return;
			dpd = true;
			pause = true;
		}
	}
	
	private boolean pressed;
	
	private void updateInput(){
		updateMouse();
		updateKeyBoard();
	}
	
	public byte getFxKey(int key){
		switch(key){
			case Keyboard.KEY_A:
				return Key.A_KEY;
			case Keyboard.KEY_B:
				return Key.B_KEY;
			case Keyboard.KEY_C:
				return Key.C_KEY;
			case Keyboard.KEY_D:
				return Key.D_KEY;
			case Keyboard.KEY_E:
				return Key.E_KEY;
			case Keyboard.KEY_F:
				return Key.F_KEY;
			case Keyboard.KEY_G:
				return Key.G_KEY;
			case Keyboard.KEY_H:
				return Key.H_KEY;
			case Keyboard.KEY_I:
				return Key.I_KEY;
			case Keyboard.KEY_J:
				return Key.J_KEY;
			case Keyboard.KEY_K:
				return Key.K_KEY;
			case Keyboard.KEY_L:
				return Key.L_KEY;
			case Keyboard.KEY_M:
				return Key.M_KEY;
			case Keyboard.KEY_N:
				return Key.N_KEY;
			case Keyboard.KEY_O:
				return Key.O_KEY;
			case Keyboard.KEY_P:
				return Key.P_KEY;
			case Keyboard.KEY_Q:
				return Key.Q_KEY;
			case Keyboard.KEY_R:
				return Key.R_KEY;
			case Keyboard.KEY_S:
				return Key.S_KEY;
			case Keyboard.KEY_T:
				return Key.T_KEY;
			case Keyboard.KEY_V:
				return Key.V_KEY;
			case Keyboard.KEY_U:
				return Key.U_KEY;
			case Keyboard.KEY_W:
				return Key.W_KEY;
			case Keyboard.KEY_X:
				return Key.X_KEY;
			case Keyboard.KEY_Y:
				return Key.Y_KEY;
			case Keyboard.KEY_Z:
				return Key.Z_KEY;
			case Keyboard.KEY_UP:
				return Key.KEY_UP;
			case Keyboard.KEY_DOWN:
				return Key.KEY_DOWN;
			case Keyboard.KEY_RIGHT:
				return Key.KEY_RIGHT;
			case Keyboard.KEY_LEFT:
				return Key.KEY_LEFT;
			case Keyboard.KEY_0:
				return Key.KEY_0;
			case Keyboard.KEY_1:
				return Key.KEY_1;
			case Keyboard.KEY_2:
				return Key.KEY_2;
			case Keyboard.KEY_3:
				return Key.KEY_3;
			case Keyboard.KEY_4:
				return Key.KEY_4;
			case Keyboard.KEY_5:
				return Key.KEY_5;
			case Keyboard.KEY_6:
				return Key.KEY_6;
			case Keyboard.KEY_7:
				return Key.KEY_7;
			case Keyboard.KEY_8:
				return Key.KEY_8;
			case Keyboard.KEY_9:
				return Key.KEY_9;
			case Keyboard.KEY_ESCAPE:
				return Key.KEY_ESC;
			case Keyboard.KEY_BACK:
				return Key.KEY_DEL;
			case Keyboard.KEY_RETURN:
				return Key.KEY_ENTER;
			case Keyboard.KEY_SPACE:
				return Key.KEY_SPACE;
			case Keyboard.KEY_CAPITAL:
				return Key.KEY_CAPITAL;
			case Keyboard.KEY_PERIOD:
				return Key.KEY_DOT;
			case Keyboard.KEY_LSHIFT:
			case Keyboard.KEY_RSHIFT:
				return Key.KEY_SHIFT;
			case Keyboard.KEY_MINUS:
				return Key.KEY_MINUS;
				
		}
		return -1;
	}
	
	void updateKeyBoard(){
		if(Keyboard.isCreated()){
			while(Keyboard.next()){
				int key = Keyboard.getEventKey();
				byte kfx = getFxKey(key);
				if(kfx != -1){
					for(InputListener input : app.inputs){
						input.onKeyEvent(kfx,Keyboard.getEventKeyState());
					}
				}else{
					continue;
				}
			}
		}
	}
	
	void updateMouse(){
		if(Mouse.isCreated()){
			float mX = Mouse.getX();
            float mY = Mouse.getY();
            while(Mouse.next()){
                if(Mouse.getEventButton() == -1){
					if(pressed){
                        for(InputListener input : app.inputs){
							input.onTouch(mX,mY,EventType.TOUCH_DRAGGING,(byte)0);
						}
					}
				}else{
					if(Mouse.getEventButtonState()){
                        for(InputListener input : app.inputs){
                            
                    
							input.onTouch(mX,mY,EventType.TOUCH_PRESSED,(byte)0);
						}
						pressed = true;
					} else{
						for(InputListener input : app.inputs){
							input.onTouch(mX,mY,EventType.TOUCH_DROPPED,(byte)0);
						}
						pressed = false;
					}
				}
            }
		}
	}

	@Override
	public void setFPSLimit(int fps) {
		FPS_LIMIT =  fps;
	}

	@Override
	public int getFPSLimit() {
		return FPS_LIMIT;
	}
}
