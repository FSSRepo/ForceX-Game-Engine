package com.forcex.gui.widgets;

import com.forcex.app.EventType;
import com.forcex.gui.Drawer;
import com.forcex.gui.Font;
import com.forcex.gui.View;
import com.forcex.utils.Color;

public class ToggleButton extends View {
    private Color background, select, edge;
    private boolean toggle = false;
    private TextView textview;
    private boolean useBorder;
    private OnToggleListener listener;

    public ToggleButton(String text, Font font, float width, float height) {
        setWidth(width);
        setHeight(height);
        background = new Color(225, 225, 225);
        select = new Color(10, 110, 240);
        textview = new TextView(font);
        textview.setTextSize(height);
        textview.setText(text);
        textview.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        textview.setConstraintWidth(width);
    }

    public void setText(String text) {
        if (textview == null) return;
        textview.setText(text);
    }

    public void setTextSize(float size) {
        if (textview == null) return;
        textview.setTextSize(size);
    }

    public void setBackgroundColor(int red, int green, int blue) {
        background.set(red, green, blue);
    }

    public void setUseBorder(Color color) {
        edge = color;
        useBorder = true;
    }

    public void setOnToggleListener(OnToggleListener listener) {
        this.listener = listener;
    }

    @Override
    public void onDraw(Drawer drawer) {
        drawer.setScale(extent.x, extent.y);
        drawer.renderQuad(local, !toggle ? background : select, -1);
        if (textview != null) {
            textview.local.set(local);
            textview.onDraw(drawer);
        }
        if (useBorder) {
            drawer.setScale(extent.x, extent.y);
            drawer.renderLineQuad(local, edge);
        }
    }

    @Override
    public void onTouch(float x, float y, byte type) {
        if (type == EventType.TOUCH_PRESSED) {
            toggle = !toggle;
            if (listener != null) {
                listener.onToggle(this, toggle);
            }
        }
    }

    @Override
    public void onDestroy() {
        background = null;
        select = null;
        edge = null;
        textview.onDestroy();
        textview = null;
    }

    public void setToggle(boolean toggle) {
        this.toggle = toggle;
    }

    public boolean isToggled() {
        return toggle;
    }

    public interface OnToggleListener {
        void onToggle(ToggleButton btn, boolean z);
    }
}
