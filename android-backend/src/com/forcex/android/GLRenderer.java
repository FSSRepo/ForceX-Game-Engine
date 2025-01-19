package com.forcex.android;

import android.opengl.GLSurfaceView;
import android.util.Log;

import com.forcex.FX;
import com.forcex.app.EventType;
import com.forcex.app.Game;
import com.forcex.app.threading.Task;
import com.forcex.core.GL;
import com.forcex.core.gpu.GPUDevice;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRenderer implements GLSurfaceView.Renderer, GPUDevice {

    private String TAG = "ForceXContext";
    private int width;
    private int height;
    private final Game game;
    private String[] info;
    private int fps, frames;
    private long lastTime = 0;
    private long frameStart = 0;
    private float deltaTime;
    private final ArrayList<Task> tasks = new ArrayList<>();

    boolean created = false;
    boolean running = false;
    boolean pause = false;
    boolean resume = false;
    boolean destroy = false;

    Object synch = new Object();
    private final AndroidInput input_processor;

    public GLRenderer(Game game, AndroidInput input) {
        this.game = game;
        FX.gpu = this;
		input_processor = input;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public String getOpenGLVersion() {
        return info[0];
    }

    @Override
    public String getGPUVendor() {
        return info[1];
    }

    @Override
    public String getGPUModel() {
        return info[2];
    }

    @Override
    public boolean hasOGLExtension(String extension) {
        String[] exts = info[3].split("\n");
        for (String ext : exts) {
            if (ext.contains(extension)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getFPS() {
        return fps;
    }

    @Override
    public boolean isOpenGLES() {
        return true;
    }

    @Override
    public float getDeltaTime() {
        return deltaTime;
    }

    @Override
    public void queueTask(Task task) {
        tasks.add(task);
    }

    @Override
    public int getFPSLimit() {
        return 60;
    }

    @Override
    public void setFPSLimit(int fps) {
        // not supported for android
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig p2) {
        info = new String[4];
        info[0] = gl.glGetString(GL10.GL_VERSION);
        info[1] = gl.glGetString(GL10.GL_VENDOR);
        info[2] = gl.glGetString(GL10.GL_RENDERER);
        info[3] = gl.glGetString(GL10.GL_EXTENSIONS).replace(' ', '\n');
        if (FX.gl == null) {
            FX.gl = new AndroidGL();
        }
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LESS);
        gl.glClearDepthf(1.0f);
        gl.glDepthRangef(0.0f, 1.0f);
        gl.glDepthMask(true);
        gl.glClearColor(0.4f, 0.4f, 0.4f, 1.0f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        width = w;
        height = h;
        gl.glViewport(0, 0, w, h);
        if (!created) {
            game.create();
            created = true;
            synchronized (this) {
                running = true;
            }
        }
        game.resize(w, h);
        lastTime = System.nanoTime();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        boolean ldestoy = false;
        boolean lpause = false;
        boolean lresume = false;

        synchronized (synch) {
            ldestoy = destroy;
            lpause = pause;
            lresume = resume;
            if (resume) {
                resume = false;
            }
            if (pause) {
                pause = false;
                synch.notifyAll();
            }
            if (destroy) {
                destroy = false;
                synch.notifyAll();
            }
        }

        if (lresume) {
            running = true;
            game.resume();
            Log.w(TAG, "Resumed");
        }
        if (lpause) {
            running = false;
            game.pause(EventType.PAUSE_EVENT);
            Log.w(TAG, "Paused");
        }
        if (ldestoy) {
            game.destroy();
            created = false;
            ldestoy = false;
            Log.w(TAG, "Destroyed");
            return;
        }
        if (running) {
            long time = System.nanoTime();
            deltaTime = (time - lastTime) / 1000000000.0f;
            lastTime = time;
            game.render(deltaTime);
            if ((time - frameStart) >= 1000000000) {
                fps = frames;
                frames = 0;
                frameStart = time;
            }
            frames++;
			input_processor.processEvent();
        }
        for (int i = 0; i < 5; i++) {
            if (i < tasks.size()) {
                Task task = tasks.get(i);
                if (task != null) {
                    if (task.execute()) {
                        tasks.remove(task);
                    }
                } else {
                    tasks.remove(i);
                    break;
                }
            } else {
                break;
            }
        }
    }

    @Override
    public boolean hasTaskInQueue() {
        return tasks.size() > 0;
    }


    public int onBackPressed() {
        return game.pause(EventType.BACK_BUTTON);
    }

    public void pause() {
        synchronized (synch) {
            if (!running) return;
            pause = true;
            while (pause) {
                try {
                    synch.wait(4000);
                    if (pause) {
                        Log.w(TAG, "Error pause timeout failed.");
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    public void resume() {
        synchronized (synch) {
            resume = true;
        }
    }

    @Override
    public void waitEmptyQueue() {
        while (true) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {

            }
            if (!hasTaskInQueue()) {
                return;
            }
        }
    }

    public void destroy() {
        synchronized (synch) {
            running = false;
            destroy = true;
            while (destroy) {
                try {
                    synch.wait();
                } catch (InterruptedException ex) {
                }
            }
        }
    }
}
