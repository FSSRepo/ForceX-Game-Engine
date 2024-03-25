package com.forcex.gui.widgets;

import com.forcex.FX;
import com.forcex.core.GL;
import com.forcex.gui.Drawer;
import com.forcex.gui.View;
import com.forcex.math.Maths;
import com.forcex.utils.Color;

public class GraphView extends View {
    private int vbo = -1, ibo;
    private short index_count, time_lapse_current = 0;
    private final Color cursor_color;
	private final Color default_color;
	private final Color background_color;
    private final GL gl = FX.gl;
    private float[] data_samples;
    private GraphDynamicColor[] samples_custom;

    public GraphView(float width, float height, int samples) {
        setWidth(width);
        setHeight(height);
        default_color = new Color(6, 176, 37, 255);
        cursor_color = new Color(6, 176, 37, 255);
        background_color = new Color(10, 10, 10, 140);
        data_samples = new float[samples];
    }

    @Override
    public void onCreate(Drawer drawer) {
        update();
        short[] indices = new short[data_samples.length];
        for (short i = 0; i < data_samples.length; i++) {
            indices[i] = i;
        }
        index_count = (short) indices.length;
        ibo = Drawer.createBuffer(indices, true, false);
    }

    @Override
    public void onDraw(Drawer drawer) {
        drawer.setScale(extent.x, extent.y);
        drawer.renderQuad(local, background_color, -1);
        FX.gl.glLineWidth(3.0f);
        drawer.setScale(1, 1);
        drawer.freeRender(vbo, local, cursor_color, -1);
        FX.gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ibo);
        FX.gl.glDrawElements(GL.GL_LINE_STRIP, time_lapse_current);
        gl.glLineWidth(1.0f);
    }

    private void update() {
        float[] vertices = new float[data_samples.length * 7];
        float rate = extent.x / (data_samples.length * 0.5f);
        float dy = -extent.y;
        float dx = -extent.x;
        float length = extent.y * 2.0f;
        for (short i = 0; i < data_samples.length; i++) {
            if (i < time_lapse_current) {
                vertices[i * 7] = dx;
                vertices[i * 7 + 1] = dy + (data_samples[i] * length);
                vertices[i * 7 + 2] = 0;
                vertices[i * 7 + 3] = 0;
                vertices[i * 7 + 4] = 1;
                vertices[i * 7 + 5] = 1;
                vertices[i * 7 + 6] = 1;
                dx += rate;
            }
        }
        if (vbo == -1) {
            vbo = Drawer.createBuffer(vertices, false, true);
        } else {
            Drawer.updateBuffer(vbo, vertices);
        }
    }

    public void setDefaultColor(int red, int green, int blue) {
        default_color.set(red, green, blue);
    }

    public void setBackgroundColor(int red, int green, int blue, int alpha) {
        background_color.set(red, green, blue, alpha);
    }

    public void setSamplerColors(GraphDynamicColor... samplers) {
        this.samples_custom = samplers;
    }

    public void next(float progress) {
        if (time_lapse_current < data_samples.length) {
            data_samples[time_lapse_current] = Maths.clamp(progress, 0.0f, 1.0f);
            time_lapse_current++;
        } else {
            for (int i = 0; i < data_samples.length - 1; i++) {
                data_samples[i] = data_samples[i + 1];
            }
            data_samples[data_samples.length - 1] = Maths.clamp(progress, 0.0f, 1.0f);
        }
        if (samples_custom != null) {
            for (GraphDynamicColor s : samples_custom) {
                if (progress <= s.max && progress >= s.min) {
                    cursor_color.set(s.color);
                    break;
                } else {
                    cursor_color.set(default_color);
                }
            }
        }
        update();
    }

    @Override
    public void onDestroy() {
        setVisibility(INVISIBLE);
        gl.glDeleteBuffer(vbo);
        gl.glDeleteBuffer(ibo);
        data_samples = null;
		samples_custom = null;
    }

    public static class GraphDynamicColor {
        public float min;
        public float max;
        public Color color;

        public GraphDynamicColor(Color color, float max, float min) {
            this.color = color;
            this.min = min;
            this.max = max;
        }
    }
}
