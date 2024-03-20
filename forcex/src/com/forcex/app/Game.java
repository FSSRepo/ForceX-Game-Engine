package com.forcex.app;

public class Game {

    Screen current;
	
    public void setScreen(Screen screen) {
        current = screen;
    }

    public void create() {
        if (current != null) {
            current.create();
        }
    }

    public void render(float deltaTime) {
		
        if (current != null) {
            current.render(deltaTime);
        }
		
    }

    public void resize(int width, int height) {
        if (current != null) {
            current.resize(width, height);
        }
    }

    public void resume() {
		if (current != null) {
            current.resume();
        }
    }

    public int pause(int type) {
		if (current != null) {
            return current.pause(type);
        }
		return 1;
    }

    public void destroy() {
        if (current != null) {
            current.destroy();
        }
    }
}
