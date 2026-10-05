package com.forcex.gui;

import com.forcex.FX;
import com.forcex.math.Vector2f;
import com.forcex.utils.Color;

public class View {
    public static final byte VISIBLE = 0xA;
    public static final byte INVISIBLE = 0xB;
    public static final byte GONE = 0xC;

    private static int id_gen = Integer.MIN_VALUE;

    public Vector2f local, relative;
    protected boolean debug;
    protected Vector2f extent;
    protected OnClickListener listener;
    protected UIContext context;

    protected boolean created = false,
			applyAspectRatio = false,
			ignoreTouch = false,
			noApplyYConstaint = false;
    protected float margin_right = 0.0f,
            margin_left = 0.0f,
            margin_top = 0.0f,
            margin_bottom = 0.0f,
    // View extents
    		width = 0.0f,
            height = 0.0f;
    protected byte alignment, width_type, height_type;
    protected Color debug_color;
    View parent, previous, next;
    private byte visibility;
    private int id;

    public View() {
        local = new Vector2f();
        extent = new Vector2f();
        relative = new Vector2f();
        visibility = VISIBLE;
        width_type = Layout.WRAP_CONTENT;
        height_type = Layout.WRAP_CONTENT;
        id = id_gen;
        id_gen++;
        alignment = Layout.LEFT;
        debug_color = new Color();
    }

    protected void setDebugColor(int r, int g, int b) {
        debug_color.set(r, g, b);
    }

    public void setRelativePosition(float x, float y) {
        relative.set(x, y);
    }

    public void setAlignment(byte alignment) {
        this.alignment = alignment;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isVisible() {
        return visibility == VISIBLE;
    }

    public byte getVisibility() {
        return visibility;
    }

    public void setVisibility(byte visibility) {
        this.visibility = visibility;
    }

    public void setDebugMode(boolean z) {
        debug = z;
    }

    public void onCreate(Drawer drawer) {
    }

    public void onDraw(Drawer drawer) {
    }

    public void onDestroy() {
    }

    public void onTouch(float x, float y, byte type) {
    }

    protected void notifyTouchOutside(float x, float y, byte type) {
    }

    public void updateExtent() {
    }

    void draw(Drawer drawer) {
        if (!hasParent()) {
            extent.set(width, height * (applyAspectRatio ? context.getAspectRatio() : 1.0f));
            local.set(relative);
        }
        if (!created) {
            onCreate(drawer);
            created = true;
        }
        onDraw(drawer);
    }

    public boolean isLayout() {
        return this instanceof Layout;
    }

    public boolean hasParent() {
        return parent != null;
    }

    public boolean hasPrevius() {
        return previous != null;
    }

    public boolean hasNext() {
        return next != null;
    }

    public View getParent() {
        return parent;
    }

    public View getPrevious() {
        return previous;
    }

    public View getNext() {
        return next;
    }

    public void setOnClickListener(OnClickListener listener) {
        this.listener = listener;
    }

    public void setIgnoreTouch(boolean z) {
        this.ignoreTouch = z;
    }

    public void setApplyAspectRatio(boolean z) {
        applyAspectRatio = z;
    }

    public float getExtentWidth() {
        return extent.x;
    }

    public float getExtentHeight() {
        return extent.y;
    }

    public int getDisplayWidth() {
        return (int) (FX.gpu.getWidth() * extent.x);
    }

    public int getDisplayHeight() {
        return (int) (FX.gpu.getHeight() * extent.y);
    }

    public int getDisplayLocalX() {
        return (int) ((FX.gpu.getWidth() * ((local.x - width) + 1.0f)) * 0.5f);
    }

    public int getDisplayLocalY() {
        return (int) ((FX.gpu.getHeight() * ((local.y - height) + 1.0f)) * 0.5f);
    }

    public void setWidthType(byte type) {
        width_type = type;
    }

    public void setHeightType(byte type) {
        height_type = type;
    }

    public void setMarginTop(float margin) {
        margin_top = margin;
    }

    public void setMarginRight(float margin) {
        margin_right = margin;
    }

    public void setMarginLeft(float margin) {
        margin_left = margin;
    }

    public void setMarginBottom(float margin) {
        margin_bottom = margin;
    }

    public void setNoApplyConstraintY(boolean z) {
        noApplyYConstaint = z;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
        width_type = Layout.WRAP_CONTENT;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
        height_type = Layout.WRAP_CONTENT;
    }

    public UIContext getContext() {
        return context;
    }

    protected boolean testTouch(float x, float y) {
        return isVisible() &&
                x >= (local.x - extent.x) &&
                x <= (local.x + extent.x) &&
                y >= (local.y - extent.y) &&
                y <= (local.y + extent.y);
    }

    public interface OnClickListener {
        void OnClick(View view);
    }
}
