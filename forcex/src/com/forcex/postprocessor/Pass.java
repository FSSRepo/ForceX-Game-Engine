package com.forcex.postprocessor;

public abstract class Pass {
    public boolean render_in_framebuffer = false;

    public abstract void process(int colorTexture);

    public int getTexture() {
        return 0;
    }

    public abstract void delete();
}
