package com.forcex.gui.widgets;

import com.forcex.gui.Drawer;
import com.forcex.gui.View;
import com.forcex.utils.Color;
import com.forcex.utils.GameUtils;

public class GridView extends View {
    Color interlined, select_color;
    float max_item_width;
    int limitX;
    OnItemClickListener listener;
    private Color background_color;
    private GridAdapter adapter;

    public GridView(float width, float height, int limitX, GridAdapter adapter) {
        setWidth(width);
        setHeight(height);
        background_color = new Color(255, 255, 255);
        interlined = new Color(170, 170, 170);
        select_color = new Color(12, 190, 240, 130);
        max_item_width = width / limitX;
        this.limitX = limitX;
        this.adapter = adapter;
        this.adapter.gridview = this;
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

    @Override
    public void onDraw(Drawer drawer) {
        drawer.setScale(extent.x, extent.y);
        drawer.renderQuad(local, background_color, -1);
        drawer.scissorArea(local.x, local.y, extent.x, extent.y);
        adapter.render(drawer);
        drawer.finishScissor();
    }

    @Override
    public void onTouch(float x, float y, byte type) {
        adapter.onTouch(x, y, type);
    }

    @Override
    public void onDestroy() {
        setVisibility(INVISIBLE);
        background_color = null;
        interlined = null;
        select_color = null;
        adapter.destroy();
        adapter = null;
    }

    @Override
    protected void notifyTouchOutside(float x, float y, byte type) {
        adapter.touchCursor = false;
    }

    @Override
    protected boolean testTouch(float x, float y) {
        return isVisible() && GameUtils.testRect(x, y, local, extent.x, extent.y);
    }

    public interface OnItemClickListener {
        void onItemClick(Object item, short position, boolean longclick);
    }
}
