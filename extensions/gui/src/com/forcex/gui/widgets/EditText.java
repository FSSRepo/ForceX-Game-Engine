package com.forcex.gui.widgets;

import com.forcex.FX;
import com.forcex.app.EventType;
import com.forcex.gui.Drawer;
import com.forcex.gui.UIContext;
import com.forcex.gui.View;
import com.forcex.math.Vector2f;
import com.forcex.utils.Color;
import com.forcex.utils.GameUtils;

public class EditText extends View {
    int cursorCharLine = 0, cursorLine = 0;
    Color background;
    float cursor_fade = 0.0f;
    byte cursor_style = 1;
    Vector2f cursor = new Vector2f();
    private boolean using = false,
            only_numbers = false,
            password = false,
            only_one_line = false;
    private TextView textview;
    private KeyBoard keyboard = null;
    private Color edge_color;
	private final Color text_color;
    private String text, process;
    private String hint;
    private onEditTextListener lt;
    private short[] multicolor;
    private int num_lines;
	private final short lines_max;
    private String[] lines;
    private boolean auto_focus;
    private boolean useEdge = true;

    public EditText(UIContext ctx) {
        this(ctx, 0.1f, 0.05f, 0.05f);
    }

    public EditText(UIContext context, float width, float height, float text_size) {
        setWidth(width);
        setHeight(height);
        text = "";
        hint = "";
        edge_color = new Color();
        text_color = new Color(0, 0, 0);
        textview = new TextView(UIContext.default_font);
        textview.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER_LEFT);
        textview.setTextSize(text_size);
        only_one_line = (height == text_size);
        lines_max = (short) (height / text_size);
        lines = new String[]{""};
        num_lines = 1;
        background = new Color(255, 255, 255);
    }

    private void usingThisEditText(boolean z) {
        if (multicolor == null) {
            if (!z) {
                edge_color.set(220, 220, 220);
            } else {
                edge_color.set(7, 128, 255);
            }
        } else {
            if (!z) {
                edge_color.set(multicolor[3], multicolor[4], multicolor[5]);
            } else {
                edge_color.set(multicolor[0], multicolor[1], multicolor[2]);
            }
        }
    }

    public void setEdgeEnabled(boolean z) {
        useEdge = z;
    }

    public void setEdgeMultiColor(int r1, int g1, int b1, int r2, int g2, int b2) {
        if (multicolor == null) {
            multicolor = new short[6];
        }
        multicolor[0] = (short) r1;
        multicolor[1] = (short) g1;
        multicolor[2] = (short) b1;
        multicolor[3] = (short) r2;
        multicolor[4] = (short) g2;
        multicolor[5] = (short) b2;
    }

    public void setPasswordMode(boolean z) {
        password = z;
    }

    public void setNumbersMode(boolean z) {
        only_numbers = z;
    }

    public void setOnEditTextListener(onEditTextListener lt) {
        this.lt = lt;
    }

    public void addFilter(int r, int g, int b, String... data) {
        textview.addFilter(new Color(r, g, b), data);
    }

    public void detachKeyBoard() {
        if (keyboard != null) {
            keyboard.solveHideView();
            keyboard.showKeyBoard = false;
            keyboard.setOnKeyBoardListener(null);
            keyboard = null;
            using = false;
        }
    }

    public String getHint() {
        return this.hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public float getNumber() {
        return Float.parseFloat(text);
    }

    public TextView getTextView() {
        return textview;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        updateTextView();
        if (only_one_line) {
            cursorCharLine = text.length();
        } else {
            cursorLine = num_lines - 1;
            cursorCharLine = lines[cursorLine].length();
        }
    }

    public boolean isEmpty() {
        return text.length() == 0;
    }

    @Override
    public void updateExtent() {
        extent.x = width;
        extent.y = height;
    }

    public void setTextColor(int r, int g, int b) {
        text_color.set(r, g, b);
    }

    public Color getTextColor() {
        return text_color;
    }

    public void setAutoFocus(boolean z) {
        auto_focus = z;
    }

    public void setBackground(int r, int g, int b, boolean transparent) {
        background.set(r, g, b, transparent ? 0 : 255);
    }

    public void setCursorStyle(byte style) {
        cursor_style = style;
    }

    @Override
    public void onDraw(Drawer drawer) {
        usingThisEditText(using);
        drawer.setScale(extent.x, extent.y);
        drawer.renderQuad(local, background, -1);
        float dy = (local.y + extent.y) - textview.getTextSize();
        if (hint.length() > 0 && text.length() == 0) {
            textview.setText(hint);
            textview.local.set((local.x - extent.x) + textview.getWidth(), dy);
            textview.setTextColor(128, 128, 128);
            textview.onDraw(drawer);
        } else {
            textview.local.set(
                    local.x - (extent.x - textview.getWidth()),
                    dy -
                            (num_lines - 1) * textview.getTextSize());
            textview.setTextColor(text_color.r, text_color.g, text_color.b);
            textview.onDraw(drawer);
            if (cursor_fade < 0.5f && using) {
                if (cursor_style == 1) {
                    cursor.set((local.x - extent.x) + getXCursor() * 2f, dy - (cursorLine * textview.getTextSize() * 2f));
                    drawer.setTransform(90.0f, 1, textview.getTextSize());
                    FX.gl.glLineWidth(2f);
                    drawer.renderLine(cursor, textview.getTextColor());
                } else if (cursor_style == 2) {
                    cursor.set((local.x - extent.x) + getXCursor() * 2f + textview.getTextWidthReal("_"), dy - (cursorLine * textview.getTextSize() * 2f) - textview.getTextSize() * 0.8f);
                    drawer.setScale(textview.getTextWidthReal("_"), 1f);
                    FX.gl.glLineWidth(3f);
                    drawer.renderLine(cursor, textview.getTextColor());
                }
            } else if (cursor_fade > 1.0f) {
                cursor_fade = 0.0f;
            }
            cursor_fade += FX.gpu.getDeltaTime();
        }
        FX.gl.glLineWidth(1.5f);
        if (useEdge) {
            drawer.setScale(extent.x, extent.y);
            drawer.renderLineQuad(local, edge_color);
        }
        if (auto_focus) {
            using = true;
            callKeyBoard();
            auto_focus = false;
        }
    }

    private float getXCursor() {
        if (cursorLine >= lines.length) {
            return 0f;
        }
        if (cursorCharLine > lines[cursorLine].length()) {
            return textview.getTextWidthReal(lines[cursorLine]);
        }
        return textview.getTextWidthReal(lines[cursorLine].substring(0, cursorCharLine));
    }

    private void updateTextView() {
        // update limits
        if (text.length() == 0) {
            textview.setText("");
            if (lines != null && lines.length > 0) {
                lines[0] = "";
            }
            return;
        }
        if (only_one_line && textview.getTextWidthReal(text) > getWidth()) {
            text = text.substring(0, text.length() - 1);
        } else if (!only_one_line) {
            process = "";
            float width = 0.0f;
            byte line_offset = 1;
            byte current_line = 0, current_char = 0;
            boolean setting = true;
            for (short i = 0; i < text.length(); i++) {
                char test = text.charAt(i);
                if (test != '\n') {
                    width += textview.getFont().char_widths[test & 0xff] * 0.5f * textview.getTextSize();
                    if (width < getWidth()) {
                        process += test;
                        current_char++;
                    } else if (line_offset < lines_max) {
                        process += "\n" + test;
                        current_char++;
                        width = textview.getFont().char_widths[test & 0xff] * 0.5f * textview.getTextSize();
                        if (setting && current_char == cursorCharLine && current_line == cursorLine) {
                            cursorLine++;
                            cursorCharLine = 1;
                            current_char = 1;
                            setting = false;
                        }
                        line_offset++;
                        current_char = 1;
                        current_line++;
                    } else {
                        text = text.substring(0, i);
                        break;
                    }
                } else {
                    if (line_offset < lines_max) {
                        process += "\n";
                        width = 0.0f;
                        line_offset++;
                        current_line++;
                    } else {
                        text = text.substring(0, i - 1);
                        break;
                    }
                }
            }
            lines = process.split("\n");
            num_lines = lines.length;
            textview.setText(process);
            return;
        }
        if (password) {
            String gen = "";
            for (int i = 0; i < text.length(); i++) {
                gen += "*";
            }
            textview.setText(gen);
            lines = gen.split("\n");
            num_lines = lines.length;
            return;
        } else {
            textview.setText(text);
        }
        lines = text.split("\n");
        num_lines = lines.length;
    }

    @Override
    public void onTouch(float x, float y, byte type) {
        if (type == EventType.TOUCH_PRESSED) {
            if (using) {
                float iy = (1f - (((y - local.y) / extent.y) * 0.5f + 0.5f)) * extent.y;
                cursorLine = (int) (iy / textview.getTextSize());
                if (cursorLine < lines.length) {
                    float ix = x - local.x;
                    float offset = -extent.x;
                    String current_line = lines[cursorLine];
                    if (ix > offset + textview.getTextWidthReal(current_line) * 2f) {
                        cursorCharLine = current_line.length();
                        return;
                    }
                    for (byte i = 0; i < current_line.length(); i++) {
                        float char_width0 = textview.getCharWidth(password ? '*' : current_line.charAt(i));
                        float char_width1 = 0;
                        if (i + 1 < current_line.length()) {
                            char_width1 = textview.getCharWidth(password ? '*' : current_line.charAt(i + 1));
                        }
                        if (i == 0 && ix < offset + char_width0) {
                            cursorCharLine = 0;
                        } else if (
                                char_width1 > 0 &&
                                        ix > offset + char_width0 &&
                                        ix < offset + (char_width0 * 2f) + char_width1) {
                            cursorCharLine = i + 1;
                        }
                        offset += char_width0 * 2f;
                    }
                }
                return;
            }
            using = true;
            callKeyBoard();
        }
    }

    private int getRealCursor() {
        if (cursorLine > 0) {
            int offset = 0, line_offset = 0;
            if (text.length() == 0) {
                return 0;
            }
            for (int i = 0; i < process.length(); i++) {
                if (offset == text.length()) {
                    return offset - 1;
                }
                if (text.charAt(offset) == process.charAt(i)) {
                    offset++;
                }
                if (process.charAt(i) == '\n') {
                    line_offset++;
                    if (cursorLine == line_offset) {
                        break;
                    }
                }
            }
            return offset + cursorCharLine;
        }
        return cursorCharLine;
    }

    @Override
    protected void notifyTouchOutside(float x, float y, byte type) {
        if (!(keyboard != null && keyboard.testTouch(x, y))) {
            using = false;
            detachKeyBoard();
        }
    }

    public KeyBoard getKeyboard() {
        return keyboard;
    }

    @Override
    protected boolean testTouch(float x, float y) {
        return isVisible() && GameUtils.testRect(x, y, local, extent.x, extent.y) || (keyboard != null && keyboard.testTouch(x, y));
    }

    @Override
    public void onDestroy() {
        detachKeyBoard();
        textview.onDestroy();
        textview = null;
        lt = null;
        text = null;
        multicolor = null;
        edge_color = null;
    }

    private void callKeyBoard() {
        if (keyboard == null) {
            keyboard = KeyBoard.instance;
            if (keyboard.ref != this && keyboard.ref != null) {
                keyboard.ref.detachKeyBoard();
                keyboard.ref.using = false;
            }
            keyboard.ref = this;
            if (keyboard.solveShowView()) {
                keyboard.showKeyBoard = true;
                keyboard.keyboardNumber = only_numbers;
                keyboard.setOnKeyBoardListener(new KeyBoard.onKeyBoardListener() {
                    @Override
                    public void onBackspace() {
                        if (text.length() > 0) {
                            if ((cursorLine == 0 && cursorCharLine == 0)) {
                                return;
                            }
                            int cursor = getRealCursor();
                            if (cursor == text.length()) {
                                if (cursorCharLine == 0 && cursorLine > 0) {
                                    cursorLine--;
                                    if (text.charAt(cursor - 1) != '\n') {
                                        cursorCharLine = lines[cursorLine].length() - 1;
                                    } else {
                                        cursorCharLine = lines[cursorLine].length();
                                    }
                                } else {
                                    cursorCharLine--;
                                }
                                text = text.substring(0, text.length() - 1);
                            } else {
                                if (cursor > 0) {
                                    text = text.substring(0, cursor - 1) + text.substring(cursor);
                                    if (cursorCharLine == 0 && cursorLine > 0) {
                                        cursorLine--;
                                        cursorCharLine = lines[cursorLine].length();
                                    } else {
                                        cursorCharLine--;
                                    }
                                } else {
                                    text = text.substring(0, cursor - 1) + text.substring(cursor);
                                }
                            }
                            updateTextView();
                            processChange(true);
                        }
                    }

                    @Override
                    public void onEnter() {
                        if (!only_one_line) {
                            if (cursorLine + 1 < lines_max) {
                                int cursor = getRealCursor();
                                if (cursor > 0) {
                                    if (cursor > text.length()) {
                                        if (only_one_line) {
                                            cursorCharLine = text.length();
                                        } else {
                                            cursorLine = num_lines - 1;
                                            cursorCharLine = lines[cursorLine].length();
                                        }
                                        cursor = getRealCursor();
                                    }
                                    text = text.substring(0, cursor) + "\n" + text.substring(cursor);
                                } else {
                                    if (text.length() == 0) {
                                        text += "\n";
                                    } else {
                                        text = "\n" + text.substring(cursor);
                                    }
                                }
                                cursorLine++;
                                cursorCharLine = 0;
                            }
                            updateTextView();
                            processChange(false);
                        } else {
                            processEnter();
                        }
                    }

                    @Override
                    public void onKeyDown(char key) {
                        if (only_numbers) {
                            if (key == '.' && text.contains(".") || key == '-' && text.contains("-")) {
                                return;
                            }
                        }
                        if (!((cursorLine + 1 == lines_max) && cursorLine < lines.length && textview.getTextWidthReal(lines[cursorLine] + key) > getWidth())) {
                            int cursor = getRealCursor();
                            if (cursor > 0) {
                                if (cursor > text.length()) {
                                    if (only_one_line) {
                                        cursorCharLine = text.length();
                                    } else {
                                        cursorLine = num_lines - 1;
                                        cursorCharLine = lines[cursorLine].length();
                                    }
                                    cursor = getRealCursor();
                                }
                                text = text.substring(0, cursor) + key + text.substring(cursor);
                            } else {
                                if (text.length() == 0) {
                                    text += key;
                                } else {
                                    text = key + text.substring(cursor);
                                }
                            }
                            cursorCharLine++;
                        }
                        updateTextView();
                        processChange(false);
                    }

                });
            }
        }
    }

    private void processChange(boolean del) {
        if (lt != null) {
            lt.onChange(this, text, del);
        }
    }

    private void processEnter() {
        if (lt != null) {
            lt.onEnter(this, text);
        }
        using = false;
        detachKeyBoard();
    }

    public interface onEditTextListener {
        void onChange(View view, String text, boolean deleting);

        void onEnter(View view, String text);
    }
}
