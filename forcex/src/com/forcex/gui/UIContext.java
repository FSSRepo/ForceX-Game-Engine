package com.forcex.gui;

import com.forcex.FX;
import com.forcex.app.EventType;
import com.forcex.gfx3d.shader.SpriteShader;
import com.forcex.gui.widgets.Button;
import com.forcex.gui.widgets.HelpTip;
import com.forcex.gui.widgets.KeyBoard;
import com.forcex.gui.widgets.SlidePanel;

import java.util.ArrayList;

public class UIContext {
    public static Font default_font;
    private final float aspectRatio;
    public View[] view_priority;
    public ArrayList<View> notifyDropTouch;
    public boolean dispose_drop = false;
    HelpTip help_tips;
    boolean release_touch = false;
    private View content;
    private final Drawer drawer;
    private SlidePanel slide_panel;
    private final Dialog[] dialogs;
    private final ArrayList<View> untouchable_views;

    public UIContext() {
        drawer = new Drawer(new SpriteShader(true, false, true));
        aspectRatio = (float) FX.gpu.getWidth() / FX.gpu.getHeight();
        default_font = new Font("fonts/windows.fft");
        Toast.create(this);
        view_priority = new View[8];
        dialogs = new Dialog[6];
        untouchable_views = new ArrayList<View>();
        notifyDropTouch = new ArrayList<View>();
    }

    public Drawer getDrawer() {
        return drawer;
    }

    public void bindKeyBoard(float width) {
        KeyBoard.physic_keyboard = !FX.gpu.isOpenGLES();
        KeyBoard.create(width, this);
    }

    public void useHelpTips() {
        help_tips = new HelpTip(this);
    }

    public HelpTip getHelpTip() {
        return help_tips;
    }

    public void setSlidePanel(SlidePanel panel) {
        slide_panel = panel;
    }

    public void setContentView(View view) {
        content = view;
        if (!content.isLayout()) {
            content.context = this;
        }
    }

    public View findViewByID(int id) {
        for (View v : view_priority) {
            if (v != null && v.getId() == id) {
                return v;
            }
        }
        if (slide_panel != null) {
            View v = slide_panel.getContent().findViewByID(id);
            if (v != null) {
                return v;
            }
        }
        if (content != null && content.getId() == id) {
            return content;
        }
        if (content != null && content.isLayout()) {
            return ((Layout) content).findViewByID(id);
        }
        return null;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    public void draw() {
        drawer.start();
        if (content != null && content.isVisible()) {
            content.draw(drawer);
        }
        if (slide_panel != null) {
            slide_panel.render(drawer);
        }
        for (View v : untouchable_views) {
            if (v.isVisible()) {
                v.draw(drawer);
            }
        }
        Dialog priori = null;
        for (Dialog dialog : dialogs) {
            if (dialog != null) {
                if (dialog.this_is_priority) {
                    priori = dialog;
                } else {
                    dialog.render(drawer);
                }
            }
        }
        if (priori != null) {
            priori.render(drawer);
        }
        if (KeyBoard.instance != null) {
            KeyBoard.instance.render(drawer);
        }
        Toast.instance.render(drawer);
        if (help_tips != null && help_tips.showing) {
            help_tips.render(drawer);
        }
        drawer.end();
    }

    public Dialog getDialogPriority() {
        for (Dialog d : dialogs) {
            if (d != null && d.this_is_priority) {
                return d;
            }
        }
        return null;
    }

    void resetPriorities() {
        for (Dialog d : dialogs) {
            if (d != null && d.this_is_priority) {
                d.this_is_priority = false;
            }
        }
    }

    public boolean isDialogEmpty() {
        for (Dialog dialog : dialogs) {
            if (dialog != null) {
                return false;
            }
        }
        return true;
    }

    public boolean testTouch(float x, float y) {
        if (help_tips != null && help_tips.showing) {
            return true;
        }
        if (Toast.instance.testTouch(x, y)) {
            return true;
        }
        if (KeyBoard.instance != null && KeyBoard.instance.testTouch(x, y)) {
            return true;
        }
        Dialog prior = getDialogPriority();
        if (prior != null && prior.testTouch(x, y)) {
            return true;
        }
        for (Dialog d : dialogs) {
            if (d != null && d.testTouch(x, y)) {
                return true;
            }
        }
        for (View view : view_priority) {
            if (view != null && view.testTouch(x, y)) {
                return true;
            }
        }
        if (slide_panel != null && slide_panel.testTouch(x, y)) return true;
        if (content != null && content.testTouch(x, y)) {
            if (content.isLayout()) {
                return ((Layout) content).testIsTouching(x, y);
            } else {
                return true;
            }
        }
        return false;
    }

    public void onTouch(final float x, final float y, final byte type) {
        if (type == EventType.TOUCH_DROPPED && dispose_drop) {
            dispose_drop = false;
            for (View v : notifyDropTouch) {
                ((Button) v).isPressing = false;
            }
            return;
        }
        release_touch = false;
        if (help_tips != null && help_tips.showing) {
            release_touch = true;
            help_tips.onTouch(x, y, type);
        }
        if (Toast.instance.testTouch(x, y) && !release_touch) {
            Toast.instance.onTouch(type == EventType.TOUCH_PRESSED);
            release_touch = true;
        }
        if (KeyBoard.instance != null && !release_touch && KeyBoard.instance.testTouch(x, y)) {
            KeyBoard.instance.onTouch(x, y, type);
            release_touch = true;
        }
        Dialog priority = getDialogPriority();
        if (priority != null && priority.isVisible()) {
            if (priority.testTouch(x, y) && !release_touch) {
                priority.onTouch(x, y, type);
                release_touch = true;
            } else {
                priority.notifyTouchOutside(x, y, type);
            }
        }
        for (Dialog dialog : dialogs) {
            if (dialog != null && dialog.isVisible()) {
                if (dialog.testTouch(x, y) && !release_touch) {
                    if (priority != null && dialog != priority) priority.this_is_priority = false;
                    dialog.this_is_priority = true;
                    release_touch = true;
                    dialog.onTouch(x, y, type);
                } else {
                    if (dialog != priority) {
                        dialog.notifyTouchOutside(x, y, type);
                    }
                }
            }
        }
        for (View view : view_priority) {
            if (view != null && view.isVisible()) {
                if (view.testTouch(x, y) && !release_touch) {
                    view.onTouch(x, y, type);
                    release_touch = true;
                } else {
                    view.notifyTouchOutside(x, y, type);
                }
            }
        }
        if (slide_panel != null && slide_panel.isShowing()) {
            if (slide_panel.testTouch(x, y) && !release_touch) {
                slide_panel.onTouch(x, y, type);
                release_touch = true;
            } else {
                slide_panel.notifyTouchOutside(type);
            }
        }
        if (content != null && content.isVisible()) {
            if (content.testTouch(x, y) && !release_touch) {
                content.onTouch(x, y, type);
                release_touch = true;
            } else {
                content.notifyTouchOutside(x, y, type);
            }
        }
    }

    public void onKeyEvent(byte key, boolean pressed) {
        if (KeyBoard.instance != null) {
            KeyBoard.instance.onKeyEvent(key, pressed);
        }
    }

    public void removeSlotPriority(int id) {
        for (byte i = 0; i < view_priority.length; i++) {
            if (view_priority[i] != null && view_priority[i].getId() == id) {
                view_priority[i] = null;
                return;
            }
        }
    }

    public boolean addSlotPriority(View view) {
        for (byte i = 0; i < view_priority.length; i++) {
            if (view_priority[i] == null) {
                view_priority[i] = view;
                return true;
            }
        }
        return false;
    }

    void removeSlotDialog(short id) {
        for (byte i = 0; i < dialogs.length; i++) {
            if (dialogs[i] != null && dialogs[i].id == id) {
                dialogs[i] = null;
                return;
            }
        }
    }

    boolean addSlotDialog(Dialog dialog) {
        for (byte i = 0; i < dialogs.length; i++) {
            if (dialogs[i] == null) {
                dialogs[i] = dialog;
                return true;
            }
        }
        return false;
    }

    public void removeUntouchableView(View view) {
        untouchable_views.remove(view);
    }

    public boolean isUntouchableView(View view) {
        for (View v : untouchable_views) {
            if (v == view) {
                return true;
            }
        }
        return false;
    }

    public void addUntouchableView(View view) {
        view.context = this;
        untouchable_views.add(view);
    }

    public void destroy() {
        default_font.delete();
    }
}