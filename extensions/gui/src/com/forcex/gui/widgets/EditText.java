package com.forcex.gui.widgets;

import com.forcex.FX;
import com.forcex.app.EventType;
import com.forcex.app.Key;
import com.forcex.core.SystemDevice;
import com.forcex.gui.Drawer;
import com.forcex.gui.UIContext;
import com.forcex.gui.View;
import com.forcex.math.Vector2f;
import com.forcex.utils.Color;
import com.forcex.utils.GameUtils;

public class EditText extends View implements SystemDevice.OnTextInputListener {
    int cursorCharLine = 0, cursorLine = 0;
    Color background;
    float cursor_fade = 0.0f;
    byte cursor_style = 1;
    Vector2f cursor = new Vector2f();
    private boolean using = false,
            only_numbers = false,
            password = false,
            only_one_line = false,
            shift = false,
            capital = false,
            input_requested = false;
    private TextView textview;
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
        this(ctx, FX.gpu.getWidth() * 0.05f, FX.gpu.getHeight() * 0.025f, FX.gpu.getHeight() * 0.025f);
    }

    public EditText(UIContext context, float width, float height, float text_size) {
        this.context = context;
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

    public void clearFocus() {
        using = false;
        input_requested = false;
        if (context != null && context.getFocusedEditText() == this) {
            context.setFocusedEditText(null);
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
        float dx = local.x - extent.x;
        float dy = local.y - extent.y;
        if (hint.length() > 0 && text.length() == 0) {
            textview.setText(hint);
            textview.local.set(dx + textview.getWidth() * 0.5f, dy + textview.getHeight() * 0.5f);
            textview.setTextColor(128, 128, 128);
            textview.onDraw(drawer);
        } else {
            textview.local.set(
                    dx + textview.getWidth() * 0.5f,
                    dy + textview.getHeight() * 0.5f);
            textview.setTextColor(text_color.r, text_color.g, text_color.b);
            textview.onDraw(drawer);
            if (cursor_fade < 0.5f && using) {
                float line_y = dy + cursorLine * textview.getTextSize() + textview.getTextSize() * 0.5f;
                if (cursor_style == 1) {
                    cursor.set(dx + getXCursor(), line_y);
                    drawer.setTransform(90.0f, 1, textview.getTextSize() * 0.5f);
                    FX.gl.glLineWidth(2f);
                    drawer.renderLine(cursor, textview.getTextColor());
                } else if (cursor_style == 2) {
                    cursor.set(dx + getXCursor() + textview.getTextWidthReal("_"),
                            dy + (cursorLine + 1) * textview.getTextSize() - textview.getTextSize() * 0.1f);
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
            callInput();
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
                    width += textview.getCharWidth(test);
                    if (width < getWidth()) {
                        process += test;
                        current_char++;
                    } else if (line_offset < lines_max) {
                        process += "\n" + test;
                        current_char++;
                        width = textview.getCharWidth(test);
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
                float iy = y - (local.y - extent.y);
                cursorLine = (int) (iy / textview.getTextSize());
                if (cursorLine < lines.length && cursorLine >= 0) {
                    float ix = x - (local.x - extent.x);
                    String current_line = lines[cursorLine];
                    if (ix > textview.getTextWidthReal(current_line)) {
                        cursorCharLine = current_line.length();
                        return;
                    }
                    float offset = 0;
                    for (byte i = 0; i < current_line.length(); i++) {
                        float char_width = textview.getCharWidth(password ? '*' : current_line.charAt(i));
                        if (ix < offset + char_width) {
                            cursorCharLine = i;
                            break;
                        }
                        offset += char_width;
                    }
                }
                return;
            }
            using = true;
            callInput();
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
        clearFocus();
    }

    @Override
    protected boolean testTouch(float x, float y) {
        return isVisible() && GameUtils.testRect(x, y, local, extent.x, extent.y);
    }

    @Override
    public void onDestroy() {
        clearFocus();
        textview.onDestroy();
        textview = null;
        lt = null;
        text = null;
        multicolor = null;
        edge_color = null;
    }

    private void callInput() {
        if (FX.device == null) {
            return;
        }
        if (!FX.device.isJDKDesktop()) {
            // Android: se usa el input nativo (diálogo con EditText del sistema).
            if (input_requested) {
                return;
            }
            input_requested = true;
            FX.device.requestTextInput(text, hint, only_numbers, password, this);
        } else if (context != null) {
            // Escritorio: las teclas físicas llegan por UIContext.onKeyEvent.
            context.setFocusedEditText(this);
        }
    }

    @Override
    public void onTextInput(String value) {
        input_requested = false;
        if (value == null) {
            using = false;
            return;
        }
        if (!value.equals(text)) {
            setText(value);
            processChange(false);
        }
        // Aceptar en el diálogo nativo equivale a pulsar Enter en el teclado virtual.
        processEnter();
    }

    /**
     * Procesa una tecla física del escritorio.
     *
     * @return {@code true} si el evento fue consumido por este campo.
     */
    public boolean onKeyEvent(byte key, boolean down) {
        if (!using) {
            return false;
        }
        if (key == Key.KEY_LEFT_SHIFT || key == Key.KEY_RIGHT_SHIFT) {
            shift = down;
            return true;
        }
        if (!down) {
            return true;
        }
        switch (key) {
            case Key.KEY_CAPITAL:
                capital = !capital;
                break;
            case Key.KEY_DEL:
                onBackspace();
                break;
            case Key.KEY_ENTER:
                onEnter();
                break;
            case Key.KEY_SPACE:
                onKeyDown(' ');
                break;
            case Key.KEY_DOT:
                onKeyDown('.');
                break;
            case Key.KEY_MINUS:
                onKeyDown('-');
                break;
            default:
                if (key >= Key.A_KEY && key <= Key.Z_KEY) {
                    onKeyDown(Key.toKeyChar(key, capital || shift));
                } else if (key >= Key.KEY_0 && key <= Key.KEY_9) {
                    onKeyDown(Key.toKeyChar(key, false));
                }
                break;
        }
        return true;
    }

    private void onBackspace() {
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

    private void onEnter() {
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

    private void onKeyDown(char key) {
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

    private void processChange(boolean del) {
        if (lt != null) {
            lt.onChange(this, text, del);
        }
    }

    private void processEnter() {
        if (lt != null) {
            lt.onEnter(this, text);
        }
        clearFocus();
    }

    public interface onEditTextListener {
        void onChange(View view, String text, boolean deleting);

        void onEnter(View view, String text);
    }
}
