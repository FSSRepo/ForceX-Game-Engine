package com.forcex.gui;

import com.forcex.FX;
import com.forcex.app.EventType;
import com.forcex.core.gpu.Texture;
import com.forcex.gui.widgets.TextView;
import com.forcex.math.Vector2f;
import com.forcex.utils.Color;
import com.forcex.utils.GameUtils;

public class Dialog {
    private static short id_gen = -32000;
    private final float label_height;
    private final float close_width;
    private final Color label_color;
    private final Color border_color;
    private final int close_texture;
    private final Vector2f local_offset = new Vector2f();
    protected boolean this_is_priority, moving = false;
    protected short id = 0;
    private Layout layout;
    private byte state = -1; // -1: no show, 1: show,2: hide,3: dismiss
    private float label_width;
    private float padding = 0.01f;
    private float icon_dimens;
    private Color background_color;
    private Vector2f position, position_end; // position_end used when playing animation
    private int icon_texture = -1;
    private TextView tvTitle;
    private OnDimissListener dismiss_listener;
    private boolean useLabel = true, useCloseButton = true, finishAnim = false;
    private float time = -1;

    public Dialog(Layout layout) {
        this.layout = layout;
        layout.setToWrapContent();
        background_color = new Color();
        label_color = new Color();
        border_color = new Color(23, 210, 210);
        position = new Vector2f();
        position_end = new Vector2f();
        id = id_gen;
        id_gen++;
        this_is_priority = layout.context.isDialogEmpty();
        close_texture = Texture.load(FX.homeDirectory + "gui/close.png");
        if (!layout.context.addSlotDialog(this)) {
            Toast.error("Dialog Error:\nThere aren't slots availables.", 4f);
        }

        label_height = 0.06f;
        close_width = 0.07f;
    }

    public void setIcon(int texture) {
        icon_texture = texture;
    }

    public void show() {
        state = 1;
        finishAnim = false;
        position.set(0, 0);
        time = -1;
    }

    public void show(float x, float y) {
        state = 1;
        position.set(x, y);
        finishAnim = false;
        time = -1;
    }

    public void setOnDismissListener(OnDimissListener listener) {
        this.dismiss_listener = listener;
    }

    public void setTitle(String text) {
        if (tvTitle == null) {
            tvTitle = new TextView(UIContext.default_font);
            tvTitle.setTextSize(label_height * 0.7f);
            tvTitle.setTextColor(0, 0, 0);
        }
        tvTitle.setText(text);
    }

    public void hide() {
        state = 2;
        this_is_priority = false;
        time = -1;
        finishAnim = false;
    }

    void render(Drawer drawer) {
        if (state == 1 || state == 3 || state == 2) {
            if (finishAnim) {
                if (state == 3) {
                    // destroy
                    layout.context.removeSlotDialog(id);
                    layout.onDestroy();
                    layout = null;
                    background_color = null;
                    position_end = null;
                    position = null;
                    dismiss_listener = null;
                    FX.gl.glDeleteTexture(close_texture);
                    return;
                } else if (state == 2) {
                    return;
                }
            }
            showAnimation();
            settingDialog();
            drawer.setScale(layout.extent.x + padding, layout.extent.y + padding);
            drawer.renderQuad(position, background_color, -1);
            if (finishAnim && state == 1) {
                layout.draw(drawer);
            }
            if (useLabel) {
                float dx = layout.extent.x + padding;
                float dy = layout.extent.y + padding + label_height;
                drawer.setScale(label_width, label_height);
                drawer.renderQuad(position.add(-dx + label_width, dy), label_color, -1);
                if (useCloseButton) {
                    drawer.setScale(close_width, label_height);
                    drawer.renderQuad(position.add(dx - close_width, dy), label_color, close_texture);
                }
                if (icon_texture != -1) {
                    drawer.setScale(icon_dimens, icon_dimens * layout.getContext().getAspectRatio());
                    drawer.renderQuad(position.add(-dx + icon_dimens, dy), label_color, icon_texture);
                }
                if (tvTitle != null) {
                    tvTitle.local.set(position.x + (-dx + (icon_texture != -1 ? icon_dimens * 2.1f : 0.01f) + tvTitle.getWidth()), dy + position.y);
                    tvTitle.onDraw(drawer);
                }
                drawer.setScale(layout.extent.x + padding, layout.extent.y + padding + label_height);
                drawer.renderLineQuad(position.add(0, label_height), border_color);
            } else {
                drawer.setScale(layout.extent.x + padding, layout.extent.y + padding);
                drawer.renderLineQuad(position, border_color);
            }
        }
    }

    private void settingDialog() {
        layout.local.set(position);
        layout.settingExtentView();
        layout.predictLayoutDimens();
        layout.sortViews();
        if (useLabel) {
            label_width = (layout.extent.x + padding) - (useCloseButton ? close_width : 0.0f);
            if (label_width <= 0.07f) {
                label_width = 0.2f;
            }
            if (icon_texture != -1) {
                icon_dimens = (label_height / layout.getContext().getAspectRatio()) * 0.8f;
            }
        }
    }

    public void setUseLabel(boolean z) {
        useLabel = z;
    }

    public void setUseCloseButton(boolean z) {
        useCloseButton = z;
    }

    private void showAnimation() {
        if (!finishAnim) {
            if (time >= 0.5f) {
                finishAnim = true;
                if (state != 3 && state != 2) {
                    position.set(position_end);
                    background_color.a = (short) 255;
                    label_color.a = (short) 255;
                    border_color.a = (short) 255;
                    if (tvTitle != null) {
                        tvTitle.default_color.a = (short) 255;
                    }
                    time = 0.8f;
                }
            } else {
                if (time == -1) {
                    position_end.set(position);
                    time = 0;
                }
                float percent = (time / 0.5f);
                percent = (state == 3 || state == 2) ? (1.0f - percent) : percent;
                background_color.a = (short) (255.0f * percent);
                label_color.a = (short) (255.0f * percent);
                border_color.a = (short) (255.0f * percent);
                if (tvTitle != null) {
                    tvTitle.default_color.a = (short) (255.0f * percent);
                }
                float delta = (state == 3 || state == 2) ? ((1 - percent) * 0.1f) : -(0.1f - (percent * 0.1f));
                position.set(position_end.x, position_end.y + delta);
                time += FX.gpu.getDeltaTime();
            }
        }
    }

    public void setUsePadding(boolean z) {
        padding = z ? 0.01f : 0.0f;
    }

    void onTouch(float x, float y, byte type) {
        if (useLabel) {
            if (type == EventType.TOUCH_PRESSED && GameUtils.testRect(x, y, position.add(-(layout.extent.x + padding) + label_width, layout.extent.y + padding + label_height), label_width, label_height)) {
                moving = true;
                local_offset.set(position.x - x, position.y - y);
                return;
            } else if (moving && type == EventType.TOUCH_DRAGGING) {
                position.set(x + local_offset.x, y + local_offset.y);
                return;
            } else if (moving && type == EventType.TOUCH_DROPPED) {
                moving = false;
                return;
            }
            if (useCloseButton && type == EventType.TOUCH_DROPPED && GameUtils.testRect(x, y, position.add((layout.extent.x + padding) - close_width, (layout.extent.y + padding + label_height)), close_width, label_height)) {
                if (dismiss_listener != null) {
                    if (dismiss_listener.dismiss()) {
                        dismiss();
                    }
                } else {
                    dismiss();
                }
                return;
            }
        }
        layout.onTouch(x, y, type);
    }


    public void setThisPriority() {
        layout.context.resetPriorities();
        this_is_priority = true;
    }

    public void dismiss() {
        state = 3;
        finishAnim = false;
        time = -1;
    }

    public boolean isVisible() {
        return state == 1;
    }

    void notifyTouchOutside(float x, float y, byte type) {
        layout.notifyTouchOutside(x, y, type);
    }

    boolean testTouch(float x, float y) {
        return GameUtils.testRect(x, y, position.add(0, (useLabel ? label_height : 0.0f) + padding), layout.getExtentWidth() + padding, layout.getExtentHeight() + padding + (useLabel ? label_height : 0.0f));
    }

    public interface OnDimissListener {
        boolean dismiss();
    }
}
