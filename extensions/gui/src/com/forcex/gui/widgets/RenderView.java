package com.forcex.gui.widgets;

import com.forcex.FX;
import com.forcex.core.GL;
import com.forcex.gui.Drawer;
import com.forcex.gui.View;
import com.forcex.postprocessor.FrameBuffer;
import com.forcex.utils.Color;
import com.forcex.utils.GameUtils;

public class RenderView extends View {
    public Color mix_color, edge_color;
    boolean update_framebuffer;
    boolean first = true;
    private onRenderListener listener;
    private FrameBuffer fbo;
    private int vbo;
    private float ox, oy;

    public RenderView(float width, float height) {
        this(width, height, null);
    }

    public RenderView(float width, float height, onRenderListener listener) {
        setDimens(width, height);
        fbo = new FrameBuffer((int) (width * FX.gpu.getWidth()), (int) (height * FX.gpu.getHeight()));
        update_framebuffer = false;
        mix_color = new Color(Color.WHITE);
        edge_color = new Color(Color.BLACK);
        this.listener = listener;
    }

    @Override
    public void onCreate(Drawer drawer) {
        vbo = Drawer.createBuffer(new float[]{
                -1, 1, 0, 1, 1, 1, 1,
                -1, -1, 0, 0, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1,
                1, -1, 1, 0, 1, 1, 1
        }, false, false);
        if (applyAspectRatio) {
            fbo.delete();
            fbo.create((int) (extent.x * FX.gpu.getWidth()), (int) (extent.y * FX.gpu.getHeight()));
        }
    }

    public void setDimens(float width, float height) {
        setWidth(width);
        setHeight(height);
        update_framebuffer = true;
    }

    public void begin() {
        fbo.begin();
    }

    public void end() {
        fbo.end();
    }

    @Override
    public void onDraw(Drawer drawer) {
        if (update_framebuffer) {
            fbo.delete();
            fbo.create((int) (extent.x * FX.gpu.getWidth()), (int) (extent.y * FX.gpu.getHeight()));
            update_framebuffer = false;
        }
        drawer.setScale(extent.x, extent.y);
        drawer.freeRender(vbo, local, mix_color, fbo.getTexture());
        FX.gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
        drawer.renderLineQuad(local, edge_color);
    }

    public void setOnRenderListener(onRenderListener listener) {
        this.listener = listener;
    }

    @Override
    public void onDestroy() {
        setVisibility(INVISIBLE);
        FX.gl.glDeleteBuffer(vbo);
        mix_color = null;
        edge_color = null;
        listener = null;
        fbo.delete();
        fbo = null;
    }

    @Override
    public void onTouch(float x, float y, byte type) {
        if (listener != null) {
            if (first) {
                ox = x;
                oy = y;
                first = false;
            }
            listener.touch(this, x, y, ox, oy, type);
            ox = x;
            oy = y;
        }
    }

    @Override
    public boolean testTouch(float x, float y) {
        return isVisible() && GameUtils.testRect(x, y, local, getExtentWidth(), getExtentHeight());
    }

    public interface onRenderListener {
        void touch(RenderView v, float x, float y, float ox, float oy, byte type);
    }
}
