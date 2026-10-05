package com.forcex.gui.widgets;

import com.forcex.FX;
import com.forcex.app.EventType;
import com.forcex.gui.Drawer;
import com.forcex.gui.Layout;
import com.forcex.gui.UIContext;
import com.forcex.math.Maths;
import com.forcex.math.Vector2f;
import com.forcex.utils.GameUtils;

import java.util.ArrayList;

public abstract class GridAdapter<T extends Object> {
    protected GridView gridview;
    byte simpleViewVisible = 0;
    short num_cols = 0, col_begin = -1;
    Vector2f rectCursor;
    boolean touchCursor = false;
    float t_deltaY = 0, time = 0;
    boolean delta_first;
    long start_click = 0;
    float touch_time;
    private final ArrayList<GridItem> items;
    private Layout layout;
    private float
            curY = 0.0f, curYO = 0.0f, oy, dx, dy,
            item_height = 0.0f,
            item_height2 = 0.0f,
            rectcursorx,
            rectcursory,
            cursor_transition = 2.0f,
            extent_y2,
            residual_space;
    private UIContext ctx;
    private boolean first = true;

    public GridAdapter(UIContext context) {
        items = new ArrayList<>();
        rectCursor = new Vector2f();
        layout = new Layout(context);
        layout.setUseWidthCustom(true);
        layout.setToWrapContent();
        layout.setOrientation(Layout.VERTICAL);
        layout.beforeSetting = true;
        this.ctx = context;
    }

    public void add(T item) {
        items.add(new GridItem(item));
        if (col_begin != -1) {
            col_begin = (short) Math.ceil((float) items.size() / gridview.limitX);
            if (items.size() > 0) {
                rectcursory = gridview.getExtentHeight() * ((float) simpleViewVisible / num_cols);
            } else {
                rectcursory = gridview.getExtentHeight();
            }
        }
        cursor_transition = 2.0f;
    }

    public void remove(short index) {
        if (items.size() == 0 || index >= items.size()) {
            return;
        }
        items.remove(index);
        if (col_begin + simpleViewVisible > num_cols) {
            setToFinalList();
        }
        if (col_begin != -1 && items.size() > 0) {
            col_begin = (short) Math.round((float) items.size() / gridview.limitX);
            rectcursory = gridview.getExtentHeight() * ((float) simpleViewVisible / num_cols);
        } else {
            rectcursory = gridview.getExtentHeight();
        }
        cursor_transition = 2.0f;
    }

    public void removeLast() {
        if (items.size() == 0) {
            return;
        }
        items.remove(items.size() - 1);
        if (col_begin + simpleViewVisible > num_cols) {
            setToFinalList();
        }
        if (col_begin != -1 && items.size() > 0) {
            num_cols = (short) Math.ceil((float) items.size() / gridview.limitX);
            rectcursory = gridview.getExtentHeight() * ((float) simpleViewVisible / num_cols);
        } else {
            rectcursory = gridview.getExtentHeight();
        }
        cursor_transition = 2.0f;
    }

    public void removeAll() {
        items.clear();
        if (num_cols != -1) {
            item_height = 0.0f;
            curY = 0.0f;
            num_cols = 0;
            rectcursory = gridview.getExtentHeight();
            cursor_transition = 2.0f;
        }
    }

    public UIContext getContext() {
        return ctx;
    }

    public T getItem(short index) {
        return items.get(index).item;
    }

    protected abstract void createView(Layout container);

    protected abstract void updateView(T item, short position, Layout container);

    void onTouch(float x, float y, byte type) {
        if (first) {
            oy = y;
            first = false;
        }
        if (type == EventType.TOUCH_PRESSED) {
            curYO = curY;
            start_click = System.currentTimeMillis();
            t_deltaY = 0;
            touch_time = 0;
            cursor_transition = 2.0f;
            if (GameUtils.testRect(x, y, rectCursor, rectcursorx, gridview.getExtentHeight())) {
                touchCursor = true;
            }
        } else if (type == EventType.TOUCH_DRAGGING) {
            if (!touchCursor) {
                float delta = (y - oy) * 1.25f;
                if (curY + delta <= ((num_cols - residual_space) * item_height2)) {
                    curY += delta;
                    t_deltaY += delta;
                } else if (num_cols >= simpleViewVisible) {
                    curYO += delta;
                }
            } else {
                setBeginRow((short) ((gridview.local.y + gridview.getExtentHeight() - rectcursory - y) * num_cols / extent_y2));
            }
        } else if (gridview.listener != null && type == EventType.TOUCH_DROPPED && (Math.abs(curY - curYO) < 0.03f)) {
            dx = (gridview.local.x - gridview.getExtentWidth()) + gridview.max_item_width;
            dy = (gridview.local.y + gridview.getExtentHeight()) - item_height + curY;
            touch_time = (System.currentTimeMillis() - start_click) / 1000.0f;
            t_deltaY = 0;
            byte curx = 0;
            for (short i = 0; i < items.size(); i++) {
                if (items.get(i).show && GameUtils.testRect(x, y, layout.local.set(dx + (curx * gridview.max_item_width * 2f), dy), gridview.max_item_width, item_height)) {
                    gridview.listener.onItemClick(getItem(i), i, touch_time > 0.5f);
                    touchCursor = false;
                    break;
                }
                curx++;
                if (curx == gridview.limitX) {
                    dy -= item_height2;
                    curx = 0;
                }
            }
            touchCursor = false;
        } else if (type == EventType.TOUCH_DROPPED) {
            touch_time = (System.currentTimeMillis() - start_click) / 1000.0f;
            touch_time = touch_time > 0.9f ? 0.8f : touch_time;
            cursor_transition = 3.0f;
            touchCursor = false;
            delta_first = true;
        }
        oy = y;
    }

    void create() {
        layout.setWidth(gridview.max_item_width);
        createView(layout);
        layout.settingExtentView();
        layout.predictLayoutDimens();
        item_height = layout.getPredictHeight();
        item_height2 = item_height * 2.0f;
        residual_space = gridview.getExtentHeight() / item_height;
        simpleViewVisible = (byte) Math.ceil(residual_space);
        num_cols = (short) Math.ceil((float) items.size() / gridview.limitX);
        extent_y2 = gridview.getExtentHeight() * 2.0f;
        rectcursorx = 0.015f;
        if (items.size() > 0) {
            rectcursory = gridview.getExtentHeight() * ((float) simpleViewVisible / num_cols);
        } else {
            rectcursory = gridview.getExtentHeight();
        }
    }

    void render(Drawer drawer) {
        if (items.isEmpty()) {
            curY = 0;
            return;
        }
        if (curY < 0.0f || curY > 0.0f && items.size() < simpleViewVisible) {
            curY = 0.0f;
            curYO += 0.05f;
        }
        dx = (gridview.local.x - gridview.getExtentWidth()) + gridview.max_item_width;
        dy = (gridview.local.y + gridview.getExtentHeight()) + curY;
        if (Maths.abs(t_deltaY) > gridview.getExtentHeight() * 0.08f && touch_time > 0) {
            if (delta_first) {
                time = Maths.abs(1f - touch_time);
                time = time > 1f ? 1f : time;
                delta_first = false;
                t_deltaY = t_deltaY * touch_time;
            }
            curY += t_deltaY * time * 0.3f;
            if (time > 0) {
                time -= FX.gpu.getDeltaTime();
            } else {
                t_deltaY = 0;
            }
        }
        if (num_cols > simpleViewVisible && curY > ((num_cols - residual_space) * item_height2)) {
            curY = ((num_cols - residual_space) * item_height2);
        }
        byte curx = 0;
        testCulling();
        for (short i = 0; i < items.size(); i++) {
            if (items.get(i).show) {
                updateView(getItem(i), i, layout);
                layout.settingExtentView();
                layout.predictLayoutDimens();
                layout.local.set(dx + (curx * gridview.max_item_width * 2f), dy - layout.getExtentHeight());
                layout.sortViews();
                if (items.get(i).select) {
                    drawer.setScale(gridview.max_item_width, item_height);
                    drawer.renderQuad(rectCursor.set(layout.local.x, dy - item_height), gridview.select_color, -1);
                }
                layout.onDraw(drawer);
                if (item_height < layout.getExtentHeight()) {
                    item_height = layout.getExtentHeight();
                    item_height2 = item_height * 2.0f;
                    residual_space = gridview.getExtentHeight() / item_height;
                    simpleViewVisible = (byte) Math.ceil(residual_space);
                    num_cols = (short) Math.round((float) items.size() / gridview.limitX);
                    if (items.size() > 0) {
                        rectcursory = gridview.getExtentHeight() * ((float) simpleViewVisible / num_cols);
                    } else {
                        rectcursory = gridview.getExtentHeight();
                    }
                }
            }
            curx++;
            if (curx == gridview.limitX) {
                dy -= item_height2;
                curx = 0;
            }
        }
        if (cursor_transition > 0.0f) {
            drawer.setScale(rectcursorx, rectcursory);
            drawer.renderQuad(rectCursor.set(gridview.local.x + gridview.getExtentWidth() - rectcursorx, gridview.local.y + gridview.getExtentHeight() - (rectcursory + ((float) col_begin / num_cols) * extent_y2)),
                    gridview.interlined.setAlpha(cursor_transition * 0.5f), -1);
        }
        if (cursor_transition > 0.0f) {
            cursor_transition -= FX.gpu.getDeltaTime();
        }
    }

    private void testCulling() {
        col_begin = (short) (curY / item_height2);
        for (short r = 0; r < num_cols; r++) {
            for (short c = 0; c < gridview.limitX; c++) {
                int idx = r * gridview.limitX + c;
                if (idx < items.size()) {
                    items.get(idx).show = (r >= col_begin && (r - col_begin <= simpleViewVisible));
                }
            }
        }
    }

    public void setBeginRow(short row) {
        curY = (row * item_height2);
        float test = (num_cols - residual_space) * item_height2;
        if (curY > test) {
            curY = test;
        }
    }

    public int getNumItems() {
        return items.size();
    }

    public void setToFinalList() {
        curY = (num_cols - residual_space) * item_height2;
    }

    public void setSelect(short position, boolean z) {
        if (position != -1) {
            items.get(position).select = z;
        }
    }

    public void destroy() {
        items.clear();
        gridview = null;
        layout.onDestroy();
        layout = null;
        rectCursor = null;
        ctx = null;
    }

    public class GridItem {
        public boolean select = false;
        T item;
        boolean show = true;

        public GridItem(T itm) {
            item = itm;
        }
    }
}
