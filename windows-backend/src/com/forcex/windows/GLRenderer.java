package com.forcex.windows;

import com.forcex.FX;
import com.forcex.app.EventType;
import com.forcex.app.Game;
import com.forcex.app.InputListener;
import com.forcex.app.Key;
import com.forcex.app.threading.Task;
import com.forcex.core.GL;
import com.forcex.core.gpu.GPUDevice;
import com.forcex.utils.PoolArray;


public class GLRenderer implements GPUDevice {
    Game game;
    protected int width;
    protected int height;
    private float deltaTime;
    private int fps, frames;
    private long lastTime = 0, frameStart = 0;
    String[] info;
    PoolArray<Task> tasks;
    Object synch = new Object();
    // states
    private boolean created = false, pause = false, resume = false;
    protected boolean destroy = false;
    private ForceXApp app;
    private boolean runningInBackground = false;
    protected boolean running = false;
    private int fps_limit = 120;


    public GLRenderer(Game game, ForceXApp app) {
        this.game = game;
        FX.gpu = this;
        this.app = app;
        tasks = new PoolArray<>(10);
    }

    void create() {
        org.lwjgl.opengl.GL.createCapabilities();
        if (FX.gl == null) {
            FX.gl = new WindowsGL();
        }
        GL gl = FX.gl;

        info = new String[4];
        info[0] = gl.glGetString(GL.GL_VERSION);
        info[1] = gl.glGetString(GL.GL_VENDOR);
        info[2] = gl.glGetString(GL.GL_RENDERER);
        info[3] = gl.glGetString(GL.GL_EXTENSIONS).replace(' ', '\n');

        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LESS);
        gl.glClearDepthf(1.0f);
        gl.glDepthRangef(0.0f, 1.0f);
        gl.glDepthMask(true);
        gl.glClearColor(0.4f, 0.4f, 0.4f, 1.0f);

        game.create();
        resize(width, height);
    }

    void loop() {
        lastTime = System.nanoTime();
        running = true;

        while (true) {
            long start_time = System.nanoTime();
            processStates();
            created = true;
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
                lastTime = System.nanoTime();
                game.resume();
                System.out.println("resumed");
            }
            if (lpause) {
                running = false;
                game.pause(0);
                System.out.println("Paused");
            }
            if (ldestoy || app.isCloseRequested()) {
                game.destroy();
                created = false;
                running = false;
                break;
            }
            if (running) {
                game.render(deltaTime);
                app.update();
                long time = System.nanoTime();
                deltaTime = (time - lastTime) / 1_000_000_000.0f;
                lastTime = time;
                if (time - frameStart >= 1_000_000_000) {
                    fps = frames;
                    frames = 0;
                    frameStart = time;
                }
                frames++;
            }
            Task task = null;
            while ((task = tasks.pop()) != null) {
                if (!task.execute()) {
                    tasks.push(task);
                }
            }
            if(fps_limit != -1) {
                long cap_time = 1_000_000_000 / fps_limit;
                long frame_time = lastTime - start_time;
                long time_wait = cap_time - frame_time;
                if(time_wait > 0) {
                    while((System.nanoTime() - lastTime) < time_wait) {
                    }
                }
            }
        }
        System.out.println("Destroyed");
        app.destroyWindow();
    }

    void resize(int width, int height) {
        FX.gl.glViewport(width, height);
        this.width = width;
        game.resize(width, height);
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
        String[] extensions = info[3].split("\n");
        for (String ext : extensions) {
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
        return false;
    }

    @Override
    public float getDeltaTime() {
        return deltaTime;
    }

    @Override
    public void waitEmptyQueue() {
        while (true) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
            }
            if (!hasTaskInQueue()) {
                return;
            }
        }
    }

    @Override
    public void queueTask(Task task) {
        tasks.push(task);
    }

    @Override
    public boolean hasTaskInQueue() {
        return tasks.count() > 0;
    }

    private void processStates() {
        if (created && app.isVisible() && runningInBackground) {
            runningInBackground = false;
            resume = true;
        } else if (created && !app.isVisible() && !runningInBackground) {
            if (!running) return;
            runningInBackground = true;
            pause = true;
        }
    }

    @Override
    public int getFPSLimit() {
        return fps_limit;
    }

    @Override
    public void setFPSLimit(int limit) {
        fps_limit = limit;
    }
}
