package com.forcex.gui.widgets;

import com.forcex.FX;
import com.forcex.app.EventType;
import com.forcex.core.GL;
import com.forcex.gui.Drawer;
import com.forcex.gui.Layout;
import com.forcex.gui.UIContext;
import com.forcex.gui.View;
import com.forcex.math.Vector2f;
import com.forcex.utils.Color;
import com.forcex.utils.GameUtils;
import com.forcex.utils.LanguageString;

import java.util.ArrayList;

public class HelpTip {
    public boolean showing;
    View focus;
    TextView text;
    Vector2f position;
    Color color, btn_color, arrow_color, drag_color;
    UIContext ctx;
    Vector2f local, btn_extents, arrow_from, arrow_to, drag_from, drag_to;
    boolean request_show = false, request_dimiss = false, left = false, inverse_arrow = false;
    float timer;
    LanguageString lang;
    int tvbo, drag_image = -1;
    QueueTip current;
    Vector2f img_location;
    ArrayList<QueueTip> tips;
    boolean dragging = false;

    public HelpTip(UIContext ctx) {
        position = new Vector2f(FX.gpu.getWidth() * 0.5f, FX.gpu.getHeight() * 0.5f);
        btn_extents = new Vector2f();
        arrow_from = new Vector2f();
        arrow_to = new Vector2f();
        drag_from = new Vector2f();
        drag_to = new Vector2f();
        color = new Color(0, 0, 0, 180);
        btn_color = new Color(225, 225, 225);
        arrow_color = new Color(0, 244, 0);
        this.ctx = ctx;
        tips = new ArrayList<QueueTip>();
        text = new TextView(UIContext.default_font);
        text.setConstraintWidth(FX.gpu.getWidth() * 0.2f);
        text.setTextColor(255, 255, 255);
        tvbo = Drawer.createBuffer(new float[]{
                0, 0.5f, 0, 0, 1, 1, 1,
                -1.5f, 0, 0, 0, 1, 1, 1,
                0, -0.5f, 0, 0, 1, 1, 1
        }, false, false);
        drag_color = new Color(0xffffffff);
        img_location = new Vector2f();
    }

    public void setLanguage(LanguageString lang) {
        this.lang = lang;
    }

    public void add(String title, String description, View view) {
        QueueTip tip = new QueueTip();
        tip.title = title;
        tip.description = description;
        tip.view = view;
        tips.add(tip);
        request_show = true;
        showing = true;
        timer = 0.0f;
    }

    public void setDragIcon(int texture) {
        drag_image = texture;
    }

    public void addAndDragX(String title, String description, View view) {
        QueueTip tip = new QueueTip();
        tip.title = title;
        tip.description = description;
        tip.view = view;
        tip.drag_x = true;
        tips.add(tip);
        request_show = true;
        showing = true;
        timer = 0.0f;
    }

    public void addAndDragY(String title, String description, View view) {
        QueueTip tip = new QueueTip();
        tip.title = title;
        tip.description = description;
        tip.view = view;
        tip.drag_y = true;
        tips.add(tip);
        request_show = true;
        showing = true;
        timer = 0.0f;
    }

    public void setReferenceImage(int image, float width, float height, boolean aspectRatio) {
        if (tips.size() > 0) {
            QueueTip t = tips.get(tips.size() - 1);
            t.reference_img = image;
            // width y height están en píxeles reales.
            t.img_height = height;
            t.img_width = width;
        }
    }

    private void show(View view) {
        this.focus = view;
        local = view.local;
        focus.setVisibility(View.INVISIBLE);
        focus.updateExtent();
    }

    public void onTouch(float x, float y, byte type) {
        if (focus != null && type == EventType.TOUCH_DROPPED && GameUtils.testRect(x, y, text.local.set(FX.gpu.getWidth() * 0.9f, FX.gpu.getHeight() * 0.9f), btn_extents.x, btn_extents.y)) {
            focus.setVisibility(View.VISIBLE);
            tips.remove(current);
            current = null;
            if (!focus.hasParent()) {
                focus.onDestroy();
            }
            focus = null;
            if (tips.size() == 0) {
                request_dimiss = true;
            }
        }
    }

    public void drawTriangle(Drawer drawer, float rot, float porcent) {
        float size = FX.gpu.getWidth() * 0.035f;
        drawer.setTransform(rot, size, size);
        drawer.freeRender(tvbo, arrow_from.lerp(arrow_to, porcent), arrow_color, -1);
        FX.gl.glDrawArrays(GL.GL_TRIANGLES, 0, 3);
    }

    public void drawDragIcon(Drawer drawer, float porcent) {
        if (inverse_arrow) {
            drag_color.a = (short) (255f * (1 - porcent));
        } else {
            drag_color.a = (short) (255f * porcent);
        }
        float size = FX.gpu.getWidth() * 0.035f;
        drawer.setScale(size, size);
        drawer.renderQuad(!inverse_arrow ? drag_from.lerp(drag_to, porcent) : drag_to, drag_color, drag_image);
    }

    public void render(Drawer drawer) {
        if (request_show) {
            color.a = (short) (170f * (timer / 0.5f));
            if (timer > 0.5f) {
                request_show = false;
                timer = 0.0f;
            }
            timer += FX.gpu.getDeltaTime();
        }
        if (request_dimiss) {
            color.a = (short) (170f * (1 - (timer / 0.5f)));
            if (timer > 0.5f) {
                request_dimiss = false;
                showing = false;
                timer = 0.0f;
            }
            timer += FX.gpu.getDeltaTime();
        }
        drawer.setScale(FX.gpu.getWidth() * 0.5f, FX.gpu.getHeight() * 0.5f);
        drawer.renderQuad(position, color, -1);
        if (!request_show && tips.size() > 0) {
            if (current == null) {
                current = tips.get(0);
                if (current.view == null || processViewSlide()) {
                    current = null;
                    return;
                }
                show(current.view);
                left = local.x <= FX.gpu.getWidth() * 0.5f;
                arrow_from.set(local.x + (focus.getExtentWidth() + FX.gpu.getWidth() * 0.05f) * (left ? 1 : -1.0f), local.y);
                arrow_to.set(local.x + (focus.getExtentWidth() + FX.gpu.getWidth() * 0.1f) * (left ? 1 : -1.0f), local.y);
                dragging = false;
                if (current.drag_x) {
                    drag_from.x = local.x - focus.getExtentWidth();
                    drag_to.x = local.x + focus.getExtentWidth();
                    drag_to.y = drag_from.y = local.y;
                    dragging = true;
                } else if (current.drag_y) {
                    drag_from.y = local.y - focus.getExtentHeight();
                    drag_to.y = local.y + focus.getExtentHeight();
                    drag_to.x = drag_from.x = local.x;
                    dragging = true;
                }
            }
            drawTriangle(drawer, left ? 0 : 180f, inverse_arrow ? (1 - (timer / 0.8f)) : (timer / 0.8f));
            if (timer > 0.8f) {
                inverse_arrow = !inverse_arrow;
                timer = 0.0f;
            }
            timer += FX.gpu.getDeltaTime();
            focus.local.set(local);
            focus.onDraw(drawer);
            if (dragging) {
                drawDragIcon(drawer, timer / 0.8f);
            }
            text.setTextColor(0, 0, 0);
            renderButton(drawer);
            text.setTextColor(255, 255, 255);
            renderTitle(drawer);
            renderDescription(drawer);
            if (current.reference_img != -1) {
                img_location.x = left
                        ? (FX.gpu.getWidth() * 0.975f - current.img_width * 0.5f)
                        : (FX.gpu.getWidth() * 0.025f + current.img_width * 0.5f);
                img_location.y = text.local.y - text.getHeight() * 0.5f - FX.gpu.getHeight() * 0.015f - current.img_height * 0.5f;
                drawer.setScale(current.img_width * 0.5f, current.img_height * 0.5f);
                drawer.renderQuad(img_location, null, current.reference_img);
            }
        }
    }

    private boolean processViewSlide() {
        View v = current.view;
        if (v.hasParent() && v.getParent().isLayout()) {
            return ((Layout) v.getParent()).usingInSlidePanel && !((Layout) v.getParent()).finishedAnim;
        }
        return false;
    }

    private void renderButton(Drawer drawer) {
        text.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        text.setTextSize(FX.gpu.getHeight() * 0.035f);
        text.setText(lang != null ? lang.get("next") : "Next");
        text.local.set(FX.gpu.getWidth() * 0.9f, FX.gpu.getHeight() * 0.9f);
        btn_extents.x = text.getWidth() * 0.75f;
        btn_extents.y = text.getHeight() * 0.6f;
        drawer.setScale(btn_extents.x, btn_extents.y);
        drawer.renderQuad(text.local, btn_color, -1);
        text.onDraw(drawer);
    }

    private void renderTitle(Drawer drawer) {
        text.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        text.setTextSize(FX.gpu.getHeight() * 0.035f);
        text.setText(current.title);
        text.local.x = FX.gpu.getWidth() * 0.025f + text.getWidth() * 0.5f;
        boolean invasion = text.local.x > focus.local.x - focus.getExtentWidth() && text.local.x < focus.local.x + focus.getExtentWidth();
        if (invasion) {
            float margin_right = (local.x + focus.getExtentWidth() + FX.gpu.getWidth() * 0.025f) + text.getWidth() * 0.5f;
            float margin_left = (local.x - focus.getExtentWidth() - FX.gpu.getWidth() * 0.025f) - text.getWidth() * 0.5f;
            if (text.local.x > focus.local.x) { // check right
                if (margin_left < FX.gpu.getWidth()) {
                    text.local.x = margin_right;
                } else {
                    text.local.x = margin_left;
                }
            } else { // is left
                if (margin_left > 0) {
                    text.local.x = margin_left;
                } else {
                    text.local.x = margin_right;
                }
            }
        }
        float title_y = FX.gpu.getHeight() * 0.01f + text.getHeight() * 0.5f;
        text.local.y = (local.y + focus.getExtentHeight()) < FX.gpu.getHeight() * 0.1f
                ? title_y
                : ((local.y - focus.getExtentHeight()) - FX.gpu.getHeight() * 0.05f - text.getHeight() * 0.5f);
        if (text.local.y > FX.gpu.getHeight()) {
            text.local.y = title_y;
        } else if (text.local.y > FX.gpu.getHeight() * 0.75f) {
            text.local.y = local.y - focus.getExtentHeight() + text.getHeight() * 0.5f;
        }
        text.onDraw(drawer);
    }

    private void renderDescription(Drawer drawer) {
        float x = text.local.x - text.getWidth() * 0.5f;
        float y = text.local.y + text.getHeight() * 0.5f;
        text.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER_LEFT);
        text.setTextSize(FX.gpu.getHeight() * 0.025f);
        text.setText(current.description);
        text.local.x = x + text.getWidth() * 0.5f;
        text.local.y = y + FX.gpu.getHeight() * 0.025f + text.getHeight() * 0.5f;
        text.onDraw(drawer);
    }

    class QueueTip {
        String title;
        String description;
        View view;
        boolean drag_x;
        boolean drag_y;
        int reference_img = -1;
        float img_width, img_height;
    }
}
