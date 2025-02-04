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

public abstract class ListAdapter<T extends Object> {
    ListView listview;
    float t_deltaY = 0, timelapse = 0;
    boolean delta_first;
    long start_click = 0;
    float touch_time;
    Filter filter;
    boolean touchCursor = false;
    private ArrayList<ListItem> items;
    private Layout layout;
    private boolean first = true;
    private float cursorY = 0.0f, cursorYOld = 0.0f,
            item_height = 0.0f,
            item_height2 = 0.0f,
            dy, oy, excedent,
            rectcursorx,
            rectcursory,
            cursor_transition,
            extent_y2;
    private final UIContext ctx;
    private byte simpleViewVisible;
    private short index_start = -1;
    private Vector2f rectCursor;

    public ListAdapter(UIContext ctx) {
        items = new ArrayList<>();
        layout = new Layout(ctx);
        layout.setUseWidthCustom(true);
        layout.setToWrapContent();
        layout.setOrientation(Layout.VERTICAL);
        layout.beforeSetting = true;
        rectCursor = new Vector2f();
        this.ctx = ctx;
    }

    public void setFilter(Filter filter) {
        filter.adapter = this;
        this.filter = filter;
    }

    public void add(T item) {
        items.add(new ListItem(item));
        if (index_start != -1) {
            if (items.size() > 0) {
                rectcursory = listview.getExtentHeight() * ((float) simpleViewVisible / items.size());
            } else {
                rectcursory = listview.getExtentHeight();
            }
        }
        cursor_transition = 2.0f;
    }

    public void remove(int index) {
        if (items.size() == 0 || index >= items.size()) {
            return;
        }
        items.remove(index);
        if (index_start + simpleViewVisible > items.size()) {
            setToFinalList();
        }
        if (index_start != -1 && items.size() > 0) {
            rectcursory = listview.getExtentHeight() * ((float) simpleViewVisible / items.size());
        } else {
            rectcursory = listview.getExtentHeight();
        }
        cursor_transition = 2.0f;
    }

    public void removeLast() {
        if (items.size() == 0) {
            return;
        }
        items.remove(items.size() - 1);
        if (index_start + simpleViewVisible > items.size()) {
            setToFinalList();
        }
        if (index_start != -1 && items.size() > 0) {
            rectcursory = listview.getExtentHeight() * ((float) simpleViewVisible / items.size());
        } else {
            rectcursory = listview.getExtentHeight();
        }
        cursor_transition = 3.0f;
    }

    public void removeAll() {
        if (items.size() == 0) {
            cursor_transition = 0.0f;
            return;
        }
        items.clear();
        cursorY = 0;
        rectcursory = listview.getExtentHeight();
        cursor_transition = 3.0f;
    }

    public UIContext getContext() {
        return ctx;
    }

    public T getItem(int index) {
        return items.get(index).item;
    }

    public int getNumItem() {
        return items.size();
    }

    protected abstract void createView(Layout container);

    protected abstract void updateView(T item, short position, Layout container);

    public void destroyView() {
    }

    void onTouch(float x, float y, byte type) {
        if (first) {
            oy = y;
            first = false;
        }
        if (type == EventType.TOUCH_PRESSED) {
            cursorYOld = cursorY;
            start_click = System.currentTimeMillis();
            t_deltaY = 0;
            touch_time = 0;
            if (GameUtils.testRect(x, y, rectCursor, rectcursorx, listview.getHeight())) {
                touchCursor = true;
            }
        } else if (items.size() > simpleViewVisible && type == EventType.TOUCH_DRAGGING) {
            if (!touchCursor) {
                float delta = (y - oy) * 1.25f;
                if ((index_start + simpleViewVisible >= items.size())) {
                    if (cursorY + delta > ((items.size() - excedent) * item_height2)) {
                        cursorYOld += 0.05f;
                    } else {
                        cursorY += delta;
                        t_deltaY += delta;
                    }
                } else {
                    cursorY += delta;
                    t_deltaY += delta;
                }
            } else {
                setBeginIndex((short) ((listview.local.y + listview.getExtentHeight() - rectcursory - y) * items.size() / extent_y2));
            }
        } else if (listview.listener != null && type == EventType.TOUCH_DROPPED && Math.abs(cursorY - cursorYOld) < 0.04f) {
            dy = (listview.local.y + listview.getExtentHeight()) - item_height + cursorY;
            touch_time = (System.currentTimeMillis() - start_click) / 1000.0f;
            for (short i = 0; i < items.size(); i++) {
                if (items.get(i).show && GameUtils.testRect(x, y, layout.local.set(listview.local.x, dy), listview.getExtentWidth(), item_height)) {
                    T item = getItem(i);
                    listview.listener.onItemClick(listview, item, filter != null ? (short) filter.getOriginalPosition(item) : i, touch_time > 0.45f);
                    break;
                }
                dy -= item_height2;
            }
        } else if (type == EventType.TOUCH_DROPPED) {
            touch_time = (System.currentTimeMillis() - start_click) / 1000.0f;
            if (Maths.abs(t_deltaY) < listview.getExtentHeight() * 0.08f) {
                touch_time = 0.0f;
            } else {
                touch_time = touch_time > 0.9f ? 0.7f : touch_time;
            }
            cursor_transition = 3.0f;
            touchCursor = false;
            delta_first = true;
        }
        oy = y;
    }

    void create() {
        createView(layout);
        layout.settingExtentView();
        layout.predictLayoutDimens();
        item_height = layout.getPredictHeight();
        item_height2 = item_height * 2.0f;
        rectcursorx = 0.015f;
        excedent = listview.getExtentHeight() / item_height;
        simpleViewVisible = (byte) Math.round(excedent);
        extent_y2 = listview.getExtentHeight() * 2.0f;
        if (items.size() > 0) {
            rectcursory = listview.getExtentHeight() * ((float) simpleViewVisible / items.size());
        } else {
            rectcursory = listview.getExtentHeight();
        }
        layout.setWidth(listview.getExtentWidth());
        if (filter != null) {
            filter.update();
        }
    }

    void render(Drawer drawer) {
        if (items.size() == 0) {
            cursorY = 0.0f;
            index_start = 0;
            return;
        }
        if (cursorY < 0.0f || cursorY > 0.0f && items.size() < simpleViewVisible) {
            cursorY = 0.0f;
            cursorYOld += 0.05f;
        }
        if (items.size() > simpleViewVisible && cursorY > ((items.size() - excedent) * item_height2)) {
            cursorY = ((items.size() - excedent) * item_height2);
        }
        if (Maths.abs(t_deltaY) > listview.getExtentHeight() * 0.08f && touch_time > 0) {
            if (delta_first) {
                timelapse = Maths.abs(1.5f - touch_time);
                timelapse = timelapse > 1.5f ? 1.5f : timelapse;
                delta_first = false;
                t_deltaY = t_deltaY * touch_time;
            }
            cursorY += t_deltaY * timelapse * 0.3f;
            if (timelapse > 0) {
                timelapse -= FX.gpu.getDeltaTime();
            } else {
                t_deltaY = 0;
            }
        }
        testCulling();
        dy = (listview.local.y + listview.getExtentHeight()) - item_height + cursorY;
        for (short i = 0; i < items.size(); i++) {
            if (items.get(i).show) {
                updateView(getItem(i), i, layout);
                layout.settingExtentView();
                layout.local.set(listview.local.x, dy);
                layout.predictLayoutDimens();
                layout.sortViews();
                if (items.get(i).select) {
                    drawer.setScale(listview.getExtentWidth(), item_height);
                    drawer.renderQuad(layout.local, listview.select_color, -1);
                }
                layout.onDraw(drawer);
                drawer.setScale(listview.getExtentWidth(), 0);
                if (i == 0) {
                    drawer.renderLine(layout.relative.set(listview.local.x, layout.local.y - item_height), listview.interline_color.setAlpha(1.0f));
                } else {
                    drawer.renderLine(layout.relative.set(listview.local.x, layout.local.y + item_height), listview.interline_color.setAlpha(1.0f));
                }
            }
            dy -= item_height2;
        }
        if (cursor_transition > 0.0f) {
            drawer.setScale(rectcursorx, rectcursory);
            drawer.renderQuad(rectCursor.set(listview.local.x + listview.getExtentWidth() - rectcursorx, listview.local.y + listview.getExtentHeight() - (rectcursory + ((float) index_start / items.size()) * extent_y2)),
                    listview.interline_color.setAlpha(cursor_transition * 0.5f), -1);
        }
        if (cursor_transition > 0.0f) {
            cursor_transition -= FX.gpu.getDeltaTime();
        }
    }

    void destroy() {
        if (filter != null) {
            filter.delete();
            filter = null;
        }
        destroyView();
        items.clear();
        items = null;
        layout.onDestroy();
        rectCursor = null;
        layout = null;
    }

    private void testCulling() {
        index_start = (short) (cursorY / item_height2);
        for (short i = 0; i < items.size(); i++) {
            items.get(i).show = i >= index_start && (i - index_start <= simpleViewVisible);
        }
    }

    public void setBeginIndex(short index) {
        cursorY = (index * item_height2);
        float test = (items.size() - excedent) * item_height2;
        if (cursorY > test) {
            cursorY = test;
        }
    }

    public void setToFinalList() {
        cursorY = (items.size() - excedent) * item_height2;
    }

    public void setSelectItem(short index, boolean select) {
        items.get(index).select = select;
    }

    public ListItem getActualSelectedItem() {
        for (ListItem it : items) {
            if (it.select) {
                return it;
            }
        }
        return null;
    }

    private class ListItem {
        public boolean select = false;
        T item;
        boolean show = true;

        public ListItem(T itm) {
            item = itm;
        }
    }
}
