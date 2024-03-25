package com.forcex.gui.widgets;

import com.forcex.core.gpu.Texture;
import com.forcex.gui.Drawer;
import com.forcex.gui.UIContext;
import com.forcex.gui.View;
import com.forcex.utils.BufferUtils;
import com.forcex.utils.Color;
import com.forcex.utils.GameUtils;

public class ColorPicker extends View implements ProgressBar.onSeekListener {
    public float width, height;
    private ProgressBar[] colors_bar;
    private TextView tvColors;
    private OnColorPickListener listener;
    private Color sampler;

    public ColorPicker(float width, float height) {
        setWidth(width);
        setHeight(height);
        colors_bar = new ProgressBar[4];
        tvColors = new TextView(UIContext.default_font);
        tvColors.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        for (byte i = 0; i < 4; i++) {
            colors_bar[i] = new ProgressBar(width, (height - 0.05f) / 4f);
            colors_bar[i].setId(i);
            if (i == 3) {
                colors_bar[3].use2ndBackgroundTexture = true;
            }
            colors_bar[i].setProgress(100);
            colors_bar[i].useSeekBar(true);
            colors_bar[i].updateExt();
            colors_bar[i].setOnSeekListener(this);
            switch (i) {
                case 0:
                    colors_bar[i].setColor(0xffcccccc, 0xffff0000);
                    break;
                case 1:
                    colors_bar[i].setColor(0xffcccccc, 0xff00ff00);
                    break;
                case 2:
                    colors_bar[i].setColor(0xffcccccc, 0xff0000ff);
                    break;
                case 3:
                    colors_bar[i].setColor(0x00ffffff, 0xffffffff);
                    break;
            }
        }
        tvColors.setTextSize(colors_bar[0].getHeight() * 0.92f);
        sampler = new Color();
    }

    public void setColor(Color color) {
        colors_bar[0].setProgress((color.r / 255f) * 100.0f);
        colors_bar[1].setProgress((color.g / 255f) * 100.0f);
        colors_bar[2].setProgress((color.b / 255f) * 100.0f);
        colors_bar[3].setProgress((color.a / 255f) * 100.0f);
        colors_bar[3].setColor(0xff000000, color.toRGBA());
        sampler.set(color);
    }

    @Override
    public void seek(int id, float progress) {
        switch (id) {
            case 0:
                colors_bar[3].colors[1].r = (short) (255.0f * (progress / 100.0f));
                break;
            case 1:
                colors_bar[3].colors[1].g = (short) (255.0f * (progress / 100.0f));
                break;
            case 2:
                colors_bar[3].colors[1].b = (short) (255.0f * (progress / 100.0f));
                break;
            case 3:
                sampler.a = (short) (255.0f * (progress / 100.0f));
                break;
        }
        sampler.setColor(colors_bar[3].colors[1]);
        if (listener != null) {
            listener.pick(sampler.toRGBA());
        }
    }

    public void setOnColorPickListener(OnColorPickListener listener) {
        this.listener = listener;
    }

    @Override
    public void finish(float final_progress) {

    }

    @Override
    public void onTouch(float x, float y, byte type) {
        for (ProgressBar b : colors_bar) {
            if (b.testTouch(x, y)) {
                b.onTouch(x, y, type);
            }
        }
    }

    @Override
    public void onCreate(Drawer drawer) {
        int width_ = 128;
        int height_ = 32;
        byte[] gradient = new byte[(width_ * height_) * 4];
        for (short x = 0; x < width_; x++) {
            for (short y = 0; y < height_; y++) {
                int offset = (x + (width_ * y)) * 4;
                gradient[offset] = (byte) 255;
                gradient[offset + 1] = (byte) 255;
                gradient[offset + 2] = (byte) 255;
                gradient[offset + 3] = (byte) (255.0f * ((float) x / width_));
            }
        }
        int textureId = Texture.load(width_, height_, BufferUtils.createByteBuffer(gradient), false);
        colors_bar[3].texture_background_2 = textureId;
    }

    @Override
    public void onDraw(Drawer drawer) {
        float dy = (local.y + extent.y) - 0.02f - colors_bar[0].getHeight();
        for (int i = 0; i < 4; i++) {
            if (i == 3 &&
                    colors_bar[3].colors[1].r < 120 &&
                    colors_bar[3].colors[1].g < 120 &&
                    colors_bar[3].colors[1].b < 120) {
                tvColors.setTextColor(255, 255, 255);
            } else {
                tvColors.setTextColor(0, 0, 0);
            }
            colors_bar[i].local.set(local.x, dy);
            colors_bar[i].onDraw(drawer);
            tvColors.setText(getColor(i) + ":" + (int) (255.0f * (colors_bar[i].getProgress() / 100.0f)));
            tvColors.local.set(local.x, colors_bar[i].local.y);
            tvColors.onDraw(drawer);
            dy -= (colors_bar[i].getHeight() * 2f) + 0.02f;
        }
    }

    public String getColor(int i) {
        switch (i) {
            case 0:
                return "Red";
            case 1:
                return "Green";
            case 2:
                return "Blue";
            case 3:
                return "Alpha";
        }
        return "";
    }

    @Override
    public void onDestroy() {
        for (ProgressBar bar : colors_bar) {
            bar.onDestroy();
        }
        colors_bar = null;
        tvColors.onDestroy();
        tvColors = null;
        listener = null;
        sampler = null;
    }


    @Override
    protected boolean testTouch(float x, float y) {
        return isVisible() && GameUtils.testRect(x, y, local, extent.x, extent.y);
    }

    public interface OnColorPickListener {
        void pick(int color);
    }
}
