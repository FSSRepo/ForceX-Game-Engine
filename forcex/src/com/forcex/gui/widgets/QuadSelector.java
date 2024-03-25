package com.forcex.gui.widgets;

import com.forcex.FX;
import com.forcex.app.EventType;
import com.forcex.core.GL;
import com.forcex.gui.Drawer;
import com.forcex.gui.View;
import com.forcex.math.Maths;
import com.forcex.math.Vector2f;
import com.forcex.utils.Color;
import com.forcex.utils.GameUtils;

public class QuadSelector extends View {
    OnSelectionListener listener;
    boolean normalize = false;
    long start = -1;
    boolean selection_fun = false;
    Vector2f point_start = new Vector2f();
    Vector2f point_end = new Vector2f();
    private Color color;
    private int vbo = -1;
    public QuadSelector(float width, float height) {
        setWidth(width);
        setHeight(height);
        color = new Color(10, 150, 190, 80);
    }

    @Override
    public void onTouch(float x, float y, byte type) {
        switch (type) {
            case EventType.TOUCH_PRESSED:
                start = System.currentTimeMillis();
                point_start.set(x, y);
                break;
            case EventType.TOUCH_DRAGGING:
                if (selection_fun) {
                    point_end.set(x, y);
                }
                break;
            case EventType.TOUCH_DROPPED:
                if (listener != null && System.currentTimeMillis() - start < 100) {
                    listener.click(new Vector2f(x, y));
                } else if (listener != null) {
                    compute();
                }
                start = -1;
                selection_fun = false;
                break;
        }
    }

    public void setNormalize(boolean z) {
        normalize = z;
    }

    public void setListener(OnSelectionListener listener) {
        this.listener = listener;
    }

    private void compute() {
        if (listener != null) {
            float w = Maths.abs(point_end.x - point_start.x) * 0.5f;
            float h = Maths.abs(point_end.y - point_start.y) * 0.5f;
            Vector2f center = new Vector2f(point_start.x + w, point_start.y - h);
            if (point_start.x > point_end.x) {
                center.x = point_start.x - w;
            }
            if (point_start.y < point_end.y) {
                center.y = point_start.y + h;
            }
            if (normalize) {
                w /= extent.x;
                h /= extent.y;
                center.x = (center.x - local.x) / extent.x;
                center.y = (center.y - local.y) / extent.y;
            }
            listener.selecting(center, w, h);
        }
    }

    @Override
    protected void notifyTouchOutside(float x, float y, byte type) {
        start = -1;
        selection_fun = false;
    }

    @Override
    protected boolean testTouch(float x, float y) {
        return isVisible() && GameUtils.testRect(x, y, local, width, height);
    }

    @Override
    public void onDraw(Drawer drawer) {
        if (selection_fun) {
            updateSelector();
            drawer.setScale(1f, 1f);
            drawer.freeRender(vbo, new Vector2f(0, 0), color, -1);
            FX.gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
        } else if (start != -1 && (System.currentTimeMillis() - start) > 100 && !selection_fun) {
            selection_fun = true;
        }
    }

    private void updateSelector() {
        float[] vertices = new float[]{
                point_start.x, point_start.y, 0, 0, 1, 1, 1,
                point_start.x, point_end.y, 0, 0, 1, 1, 1,
                point_end.x, point_start.y, 0, 0, 1, 1, 1,
                point_end.x, point_end.y, 0, 0, 1, 1, 1
        };
        if (vbo == -1) {
            vbo = Drawer.createBuffer(vertices, false, true);
        } else {
            Drawer.updateBuffer(vbo, vertices);
        }
    }

    @Override
    public void onDestroy() {
        setVisibility(INVISIBLE);
        color = null;
    }

    @Override
    public void updateExtent() {
        extent.set(width, height * (applyAspectRatio ? context.getAspectRatio() : 1.0f));
        local.set(relative);
    }

    public interface OnSelectionListener {
        void click(Vector2f center);

        void selecting(Vector2f center, float width, float height);
    }
}
