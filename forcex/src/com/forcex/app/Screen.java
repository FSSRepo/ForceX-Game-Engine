package com.forcex.app;

public class Screen {
    public void create() {}
    public void render(float deltaTime) {}
    public void resume() {}
    public int pause(int type) {
		return 1;
    }
    public void resize(int width, int height) {}
    public void destroy() {}
}
