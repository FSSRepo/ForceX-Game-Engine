package com.forcex.gui.widgets;

import com.forcex.gui.Drawer;
import com.forcex.gui.View;
import com.forcex.utils.Color;
import com.forcex.utils.GameUtils;

public class ListView extends View {
    ListAdapter adapter;
    Color interline_color, select_color;
    OnItemClickListener listener;
    private Color background_color;

    public ListView(float width, float height, ListAdapter adapter) {
        setWidth(width);
        setHeight(height);
        this.adapter = adapter;
        adapter.listview = this;
        background_color = new Color(Color.WHITE);
        interline_color = new Color(Color.GREY);
        select_color = new Color(12, 190, 240, 100);
        setDebugColor(179, 5, 153);
    }

    @Override
    public void onCreate(Drawer drawer) {
        adapter.create();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setBackgroundColor(int red, int green, int blue, int alpha) {
        background_color.set(red, green, blue, alpha);
    }

    public void setInterlinedColor(int red, int green, int blue, int alpha) {
        interline_color.set(red, green, blue, alpha);
    }

    @Override
    public void onDraw(Drawer drawer) {
        drawer.setScale(extent.x, extent.y);
        drawer.renderQuad(local, background_color, -1);
        drawer.scissorArea(local.x, local.y, extent.x, extent.y);
        adapter.render(drawer);
        drawer.finishScissor();
        if (debug) {
            drawer.setScale(extent.x, extent.y);
            drawer.renderLineQuad(local, this.debug_color);
        }
    }

    @Override
    public void onTouch(float x, float y, byte type) {
        adapter.onTouch(x, y, type);
    }

    @Override
    protected void notifyTouchOutside(float x, float y, byte type) {
        adapter.touchCursor = false;
    }

    @Override
    protected boolean testTouch(float x, float y) {
        return isVisible() && GameUtils.testRect(x, y, local, extent.x, extent.y);
    }


    @Override
    public void onDestroy() {
        setVisibility(INVISIBLE);
        adapter.destroy();
        adapter = null;
        background_color = null;
        interline_color = null;
        listener = null;
    }

    void updateExt() {
        extent.set(width, height);
    }

    public interface OnItemClickListener {
        void onItemClick(ListView view, Object item, short position, boolean longclick);
    }
}
