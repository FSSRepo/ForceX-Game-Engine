package com.forcex.gui.widgets;

import com.forcex.app.EventType;
import com.forcex.gui.Drawer;
import com.forcex.gui.UIContext;
import com.forcex.gui.View;
import com.forcex.math.Vector2f;
import com.forcex.utils.Color;
import com.forcex.utils.GameUtils;

import java.util.ArrayList;

public class TabView extends View {
    private final ArrayList<String> tabs = new ArrayList<>();
    private int selected = 0;
    private final TextView tvTitleTab;
    private float constraint_width = 0;
    private final Color tab_color = new Color(230, 230, 230);
	private final Color tab_selected = new Color(23, 180, 240);
    private byte max_visible = 0;
    private OnTabListener listener;
    private long time = 0;
    private final Vector2f separator = new Vector2f();

    public TabView(float max_tab_width, float width, float height) {
        tvTitleTab = new TextView(UIContext.default_font);
        tvTitleTab.setTextSize(height * 0.9f);
        tvTitleTab.setAnimationScroll(true);
        tvTitleTab.setConstraintWidth(max_tab_width);
        setWidth(width);
        setHeight(height);
        this.constraint_width = max_tab_width;
        max_visible = (byte) (width / max_tab_width);
    }

    public void addTab(String tab) {
        tabs.add(tab);
    }

    public void removeAll() {
        tabs.clear();
    }

    public void removeIndex(String tab) {
        tabs.remove(tab);
    }

    public void removeTab(String tab) {
        tabs.remove(tab);
    }

    public void setSelect(int index) {
        selected = index;
    }

    public void setOnTabListener(OnTabListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(Drawer drawer) {
        super.onCreate(drawer);
    }

    @Override
    public void onTouch(float x, float y, byte type) {
        if (type == EventType.TOUCH_PRESSED) {
            time = System.currentTimeMillis();
        }
        if (type == EventType.TOUCH_DROPPED) {
            float dx = local.x - extent.x;
            for (byte i = 0; i < tabs.size(); i++) {
                if (GameUtils.testRect(x, y, separator.set(dx + constraint_width, local.y), constraint_width, extent.y)) {
                    if (listener != null) {
                        listener.onTabClick(tabs.get(i), i, (System.currentTimeMillis() - time) > 400);
                    }
                    selected = i;
                }
                dx += constraint_width * 2f;
            }
            time = 0;
        }
    }

    @Override
    protected boolean testTouch(float x, float y) {
        return GameUtils.testRect(x, y, local, extent.x, extent.y);
    }

    @Override
    protected void notifyTouchOutside(float x, float y, byte type) {
        time = -1;
    }

    @Override
    public void onDraw(Drawer drawer) {
        drawer.setScale(extent.x, extent.y);
        drawer.renderQuad(local, tab_color, -1);
        float x = local.x - extent.x;
        for (byte i = 0; i < tabs.size(); i++) {
            if (i >= max_visible) {
                break;
            }
            tvTitleTab.setText(tabs.get(i));
            tvTitleTab.local.set(
                    x + (tvTitleTab.isConstraintUse() ? constraint_width : tvTitleTab.text_anim_width), local.y);
            tvTitleTab.onDraw(drawer);
            if (selected == i) {
                float test = extent.y * 0.08f;
                drawer.setScale(constraint_width, test);
                drawer.renderQuad(separator.set(x + constraint_width, (local.y - extent.y) + test), tab_selected, -1);
            }
            x += constraint_width * 2f;
            if (i < tabs.size()) {
                drawer.setTransform(90, 0, extent.y);
                drawer.renderLine(separator.set(x, local.y), tab_selected);
            }
        }
    }

    public interface OnTabListener {
        void onTabClick(String text, byte position, boolean long_click);
    }
}
