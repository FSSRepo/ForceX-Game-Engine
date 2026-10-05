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

public class ProgressBar extends View {
    protected int
            texture_background = -1,
            texture_background_2 = -1,
            progress_buffer = -1;
    protected boolean use2ndBackgroundTexture = false;
    Color[] colors;
    boolean indeterminate, use_seek, update = false;
    onSeekListener listener;
    boolean asProgress = false;
    float all_size = 0, anim_width = 0, porcent_indeterminate = 0.25f, rotation = 0;
    boolean useDynamicColor = false;
    float[] vertexs;
    boolean seeking = false;
    float time = 0;
    boolean rewind = true;
    float[] anims;
    Color start_c = new Color();
    Color end = new Color();
    float lapse_s = 0, lapse_e = 0.2f;
    private float end_time;
	private float half_time;
	private final float seek_width;
    private float duration = 5f;
    private final Color edge_color;
    private final Color pivot_seek;
    private float progress_status = 0.0f;
    private boolean useBorder = true;

    public ProgressBar(float width, float height) {
        setWidth(width);
        setHeight(height);
        seek_width = width * 0.04f;
        setProgress(0);
        colors = new Color[]{
                new Color(230, 230, 230, 255),
                new Color(10, 190, 40, 240)
        };
        pivot_seek = new Color(120, 120, 120);
        edge_color = new Color(170, 170, 170);
    }

    public void useSeekBar(boolean z) {
        use_seek = z;
        setIndeterminate(false);
    }

    public void setBackGroundColor(int color) {
        colors[0].set(color);
    }

    public void setOnSeekListener(onSeekListener listener) {
        this.listener = listener;
    }

    public void setColor(int background, int bar) {
        colors[0].set(background);
        colors[1].set(bar);
    }

    public void setColor(Color background, Color bar) {
        colors[0] = background;
        colors[1] = bar;
    }

    public float getProgress() {
        return progress_status;
    }

    public void setProgress(float porcent) {
        if (indeterminate) {
            porcent_indeterminate = porcent * 0.01f;
            return;
        }
        progress_status = Maths.clamp(porcent, 0, 100.0f);
        if (!use2ndBackgroundTexture) {
            float p = (0.01f * progress_status) * 2f - 1f;
            if (p <= 1.0f) {
                vertexs = new float[]{
                        -1, 1, 0, 0, 1, 1, 1,
                        -1, -1, 0, 0, 1, 1, 1,
                        p, 1, 0, 0, 1, 1, 1,
                        p, -1, 0, 0, 1, 1, 1
                };
                update = true;
            }
        }
    }

    public void setRotation(float rot) {
        rotation = rot;
    }

    @Override
    protected boolean testTouch(float x, float y) {
        return isVisible() && GameUtils.testRect(x, y, local, extent.x, extent.y);
    }

    @Override
    public void onTouch(float x, float y, byte type) {
        if (type == EventType.TOUCH_PRESSED) {
            if (use_seek) {
                seeking = true;
            }
        }
        if (seeking && type == EventType.TOUCH_DRAGGING) {
            setProgress(((((x - local.x) * (1f / extent.x)) * 0.5f) + 0.5f) * 100f);
            if (listener != null) {
                listener.seek(getId(), progress_status);
            }
        }
        if (type == EventType.TOUCH_DROPPED) {
            seeking = false;
            if (listener != null) {
                listener.finish(progress_status);
            }
        }
    }

    public void setUseProgressTexture(int texId, boolean randomColor) {
        this.texture_background_2 = texId;
        use2ndBackgroundTexture = true;
        asProgress = true;
        useDynamicColor = randomColor;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public void setIndeterminate(boolean z) {
        if (z != indeterminate) {
            time = 0;
            rewind = true;
        }
        indeterminate = z;
    }

    @Override
    public void onDraw(Drawer drawer) {
        if (indeterminate && rewind) {
            all_size = width * porcent_indeterminate;
            half_time = porcent_indeterminate * 0.5f;
            end_time = 1.0f - half_time;
        }
        if (update) {
            if (progress_buffer == -1) {
                progress_buffer = Drawer.createBuffer(vertexs, false, true);
            } else {
                Drawer.updateBuffer(progress_buffer, vertexs);
            }
            update = false;
            vertexs = null;
        }
        drawer.setTransform(rotation, extent.x, extent.y);
        drawer.renderQuad(local, colors[0], texture_background);
        if (!indeterminate && !use2ndBackgroundTexture) {
            drawer.freeRender(progress_buffer, local, colors[1], -1);
            FX.gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
        } else if (use2ndBackgroundTexture && !asProgress) {
            drawer.renderQuad(local, colors[1], texture_background_2);
        } else {
            float porcent = (time / duration);
            if (useDynamicColor) {
                if (change(porcent)) {
                    start_c.set(colors[1]);
                    end.set((int) (Math.random() * 255f), (int) (Math.random() * 255f), (int) (Math.random() * 255f));
                    lapse_s += 0.2f;
                    lapse_e += 0.2f;
                    if (lapse_e > 1f) {
                        lapse_s = 0;
                        lapse_e = 0.2f;
                    }
                }
                colors[1].mix(start_c, end, (porcent - lapse_s) / 0.2f);
            }
            float start = (local.x - extent.x);
            float animx = start + (porcent * ((local.x + extent.x) - start));
            if (porcent < half_time) {
                anim_width = (porcent / half_time) * all_size;
                if (use2ndBackgroundTexture && asProgress)
                    colors[1].a = (short) ((porcent / half_time) * 255f);
            }
            if (porcent > end_time) {
                anim_width = (1 - ((porcent - end_time) / half_time)) * all_size;
                if (use2ndBackgroundTexture && asProgress)
                    colors[1].a = (short) ((1 - ((porcent - end_time) / half_time)) * 255f);
            }
            drawer.setScale(anim_width, extent.y);
            drawer.renderQuad(new Vector2f(animx, local.y), colors[1], use2ndBackgroundTexture && asProgress ? texture_background_2 : -1);
            time += FX.gpu.getDeltaTime();
            time %= duration;
        }
        if (useBorder) {
            drawer.setScale(extent.x, extent.y);
            drawer.renderLineQuad(local, edge_color);
        }
    }

    private boolean change(float percent) {
        boolean range = percent <= lapse_e && percent >= lapse_s;
        return !range;
    }

    public void setUseBorder(boolean z) {
        useBorder = z;
    }

    void updateExt() {
        extent.set(width, height);
    }

    public interface onSeekListener {
        void seek(int id, float progress);

        void finish(float final_progress);
    }
}
