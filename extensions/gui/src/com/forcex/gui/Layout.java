package com.forcex.gui;

import com.forcex.FX;
import com.forcex.math.Maths;
import com.forcex.utils.Color;

import java.util.ArrayList;

public class Layout extends View {
    public static final byte VERTICAL = 0x1;
    public static final byte HORIZONTAL = 0x2;
    public static final byte FILL_PARENT = 0x1;
    public static final byte WRAP_CONTENT = 0x3;
    public static final byte CENTER = 0xA;
    public static final byte RIGHT = 0xB;
    public static final byte LEFT = 0xC;

    private final ArrayList<View> children = new ArrayList<>();
    public boolean beforeSetting = false, widthCustom = false;
    public boolean usingInSlidePanel = false, finishedAnim = false;
    protected byte orientation;
    private boolean isRelative = false, heightCustom = false;
    private float padding = 0.0f;
    private Color background;

    public Layout(UIContext context) {
        orientation = VERTICAL;
        width_type = FILL_PARENT;
        height_type = FILL_PARENT;
        this.context = context;
        setDebugColor(73, 138, 19);
    }

    public void add(View view) {
        if (view.parent == null) {
            view.parent = this;
            view.context = context;
            children.add(view);
            if (view.isLayout()) {
                ((Layout) view).usingInSlidePanel = usingInSlidePanel;
            }
        }
    }

    public void add(View view, int index) {
        if (view.parent == null) {
            view.parent = this;
            view.context = context;
            children.add(index, view);
        }
    }

    public int indexOf(View view) {
        return children.indexOf(view);
    }

    public View getView(int index) {
        return children.get(index);
    }

    public void remove(View view) {
        if (view.parent == this) {
            children.remove(view);
            view.parent = null;
            view.previous = null;
            view.next = null;
        }
    }

    public void removeAll(boolean destroy) {
        for (View view : children) {
            if (destroy) {
                view.onDestroy();
            }
            view.parent = null;
        }
        children.clear();
    }

    public void setIsRelative(boolean z) {
        isRelative = z;
        if (z) {
            width_type = WRAP_CONTENT;
            height_type = WRAP_CONTENT;
        } else {
            setToWrapContent();
        }
    }

    public void setUseWidthCustom(boolean z) {
        widthCustom = z;
    }

    public void setUseHeightCustom(boolean z) {
        heightCustom = z;
    }

    public void setPadding(float padding) {
        this.padding = padding;
    }

    public void setBackgroundColor(int r, int g, int b, int a) {
        if (background == null) {
            background = new Color();
        }
        background.set(r, g, b, a);
    }

    public void setOrientation(byte orientation) {
        this.orientation = orientation;
    }

    public void setToWrapContent() {
        width_type = WRAP_CONTENT;
        height_type = WRAP_CONTENT;
    }

    @Override
    void draw(Drawer drawer) {
        if (!created) {
            onCreate(drawer);
            created = true;
        }
        onDraw(drawer);
    }

    @Override
    public void onDraw(Drawer drawer) {
        if (!beforeSetting) {
            settingLayout();
        }
        if (background != null) {
            drawer.setScale(extent.x, extent.y);
            drawer.renderQuad(local, background, -1);
        }
        for (short i = 0; i < children.size(); i++) {
            View view = children.get(i);
            if (view.isVisible()) {
                view.draw(drawer);
                view.debug = debug;
            }
        }
        if (debug) {
            drawer.setScale(extent.x, extent.y);
            drawer.renderLineQuad(local, debug_color);
        }
    }

    public boolean testIsTouching(float x, float y) {
        for (short i = 0; i < children.size(); i++) {
            if (children.get(i).isVisible() && !children.get(i).ignoreTouch) {
                if (children.get(i).testTouch(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onTouch(float x, float y, byte type) {
        for (short i = 0; i < children.size(); i++) {
            if (children.get(i).isVisible() && !children.get(i).ignoreTouch) {
                if (children.get(i).testTouch(x, y)) {
                    children.get(i).onTouch(x, y, type);
                } else {
                    children.get(i).notifyTouchOutside(x, y, type);
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        for (View v : children) {
            v.onDestroy();
        }
        children.clear();
    }

    public boolean isRoot() {
        return !hasParent();
    }

    @Override
    protected void notifyTouchOutside(float x, float y, byte type) {
        for (short i = 0; i < children.size(); i++) {
            if (children.get(i).isVisible() && !children.get(i).ignoreTouch) {
                children.get(i).notifyTouchOutside(x, y, type);
            }
        }
    }

    public void settingLayout() {
        settingExtentView();
        predictLayoutDimens();
        sortViews();
    }

    public void predictLayoutDimens() {
        if (isRoot()) {
            if (width_type == FILL_PARENT) {
                extent.x = FX.gpu.getWidth() * 0.5f;
            } else {
                extent.x = widthCustom ? width * 0.5f : getPredictWidth() * 0.5f;
            }
            if (height_type == FILL_PARENT) {
                extent.y = FX.gpu.getHeight() * 0.5f;
            } else {
                extent.y = heightCustom ? height * 0.5f : getPredictHeight() * 0.5f;
            }
        } else {
            // only layouts the parent is a layout
            if (width_type != WRAP_CONTENT) {
                extent.x = parent.extent.x;
            } else {
                extent.x = widthCustom ? width * 0.5f : getPredictWidth() * 0.5f;
            }
            if (height_type != WRAP_CONTENT) {
                extent.y = parent.extent.y;
            } else {
                extent.y = heightCustom ? height * 0.5f : getPredictHeight() * 0.5f;
            }
        }
    }

    public void settingExtentView() {
        for (short i = 0; i < children.size(); i++) {
            View view = children.get(i);
            if (i - 1 >= 0) {
                view.previous = children.get(i - 1);
            }
            if (i + 1 < children.size()) {
                view.next = children.get(i + 1);
            }
            if (view.isLayout()) {
                ((Layout) view).settingExtentView();
                ((Layout) view).predictLayoutDimens();
                continue;
            }
            view.updateExtent();
            if (view.width_type != WRAP_CONTENT) {
                view.extent.x = extent.x;
            }
            if (view.height_type != WRAP_CONTENT) {
                view.extent.y = extent.y;
            }
        }
    }

    public void sortViews() {
        if (children.size() == 0) return;
        float left = local.x - extent.x + padding;
        float top = local.y - extent.y + padding;
        float cursorX = left;
        float cursorY = top;
        for (short i = 0; i < children.size(); i++) {
            View view = children.get(i);
            if (view.getVisibility() == View.GONE) continue;
            if (!isRelative) {
                if (view.isLayout()) {
                    ((Layout) view).predictLayoutDimens();
                }
                if (orientation == VERTICAL) {
                    switch (view.alignment) {
                        case CENTER:
                            view.local.x = local.x;
                            break;
                        case RIGHT:
                            view.local.x = (local.x + extent.x) - padding - view.getExtentWidth() - view.margin_right;
                            break;
                        case LEFT:
                            view.local.x = left + view.getExtentWidth() + view.margin_left;
                            break;
                    }
                    if (!view.noApplyYConstaint) {
                        cursorY += view.margin_top + view.getExtentHeight();
                        view.local.y = cursorY;
                        cursorY += view.getExtentHeight() + view.margin_bottom;
                    } else {
                        view.local.y = cursorY;
                    }
                } else {
                    cursorX += view.margin_left + view.getExtentWidth();
                    view.local.x = cursorX;
                    cursorX += view.getExtentWidth() + view.margin_right;
                    view.local.y = top + view.margin_top + view.getExtentHeight();
                }
            } else {
                view.local.x = local.x + view.relative.x;
                view.local.y = local.y + view.relative.y;
            }
        }
    }

    public float getPredictWidth() {
        float predicted_width = 0.0f;
        for (View view : children) {
            if (view.getVisibility() == View.GONE) continue;
            if (!isRelative) {
                if (orientation == VERTICAL) {
                    float width = view.getExtentWidth() * 2f + view.margin_left + view.margin_right;
                    if (predicted_width < width) {
                        predicted_width = width;
                    }
                } else {
                    predicted_width += view.margin_left + view.getExtentWidth() * 2f + view.margin_right;
                }
            } else {
                float test = (Maths.abs(view.relative.x) + view.getExtentWidth()) * 2f;
                if (predicted_width < test) {
                    predicted_width = test;
                }
            }
        }
        return predicted_width + padding * 2f;
    }

    public float getPredictHeight() {
        float predicted_height = 0.0f;
        for (View view : children) {
            if (view.getVisibility() == View.GONE || view.noApplyYConstaint) continue;
            if (!isRelative) {
                if (orientation == VERTICAL) {
                    predicted_height += view.margin_top + view.getExtentHeight() * 2f + view.margin_bottom;
                } else {
                    float test = view.margin_top + view.getExtentHeight() * 2f + view.margin_bottom;
                    if (predicted_height < test) {
                        predicted_height = test;
                    }
                }
            } else {
                float test = (Maths.abs(view.relative.y) + view.getExtentHeight()) * 2f;
                if (predicted_height < test) {
                    predicted_height = test;
                }
            }
        }
        return predicted_height + padding * 2f;
    }

    public View findViewByID(int id) {
        for (View v : children) {
            if (v.getId() == id) {
                return v;
            } else if (v.isLayout()) {
                View result = ((Layout) v).findViewByID(id);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
