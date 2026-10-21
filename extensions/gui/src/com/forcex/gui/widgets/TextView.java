package com.forcex.gui.widgets;

import com.forcex.FX;
import com.forcex.core.GL;
import com.forcex.core.gpu.Texture;
import com.forcex.gui.Drawer;
import com.forcex.gui.Font;
import com.forcex.gui.UIContext;
import com.forcex.gui.View;
import com.forcex.math.Vector2f;
import com.forcex.utils.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextView extends View {
    public static final byte TEXT_ALIGNMENT_CENTER = 0x3;
    public static final byte TEXT_ALIGNMENT_CENTER_RIGHT = 0x4;
    public static final byte TEXT_ALIGNMENT_CENTER_LEFT = 0x5;
    public Color default_color;
    float text_anim_width;
    ArrayList<TextColorSpan> color_span = new ArrayList<>();
    HashMap<Pattern[], Color> span_text = new HashMap<>();
    Vector2f anim_step = new Vector2f();
    float deltax = 0.0f;
    private Font font;
    private byte text_alignment;
    private short num_lines;
    private String text = "";
    private int text_vbo = -1, text_ibo = -1, index_length;
    private float constraint_width = 0.0f, max_line, text_size;
    private boolean animationScroll;

	private GL gl = FX.gl;

    public TextView() {
        this.font = UIContext.default_font;
    }

    public TextView(Font font) {
        this.font = font;
        text_size = 24f;
        resolveFont();
        text_alignment = TEXT_ALIGNMENT_CENTER;
        setText("");
        default_color = new Color(Color.BLACK);
        setDebugColor(30, 199, 0);
    }

    private void processSpanner() {
        try {
            String process = "";
            short offset = 0;
            // {|r,g,b:=text|}
            for (int i = 0; i < text.length(); i++) {
                if (i + 1 >= text.length()) {
                    process += text.charAt(i);
                    break;
                } else if (text.charAt(i) == '{' && text.charAt(i + 1) == '|') {
                    String d = "";
                    i += 2;
                    for (int j = i; j < text.length(); j++) {
                        if (j + 1 >= text.length()) {
                            process += text.charAt(j);
                            return;
                        }
                        if (text.charAt(j) == '|' && text.charAt(j + 1) == '}') {
                            i += d.length() + 1;
                            break;
                        } else {
                            d += text.charAt(j);
                        }
                    }
                    String[] sp = d.split(":=");
                    String[] sc = sp[0].split(",");
                    TextColorSpan span = new TextColorSpan();
                    span.color = new Color(Integer.parseInt(sc[0]), Integer.parseInt(sc[1]), Integer.parseInt(sc[2]));
                    span.start_index = offset;
                    process += sp[1];
                    offset += sp[1].length();
                    span.end_index = offset;
                    color_span.add(span);
                } else {
                    process += text.charAt(i);
                    offset++;
                }
            }
            text = process;
            for (Pattern[] patterns : span_text.keySet()) {
                Color color = span_text.get(patterns);
                for (Pattern pattern : patterns) {
                    Matcher m = pattern.matcher(text);
                    while (m.find()) {
                        TextColorSpan span = new TextColorSpan();
                        span.start_index = (short) m.start();
                        span.end_index = (short) m.end();
                        span.color = color;
                        color_span.add(span);
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    public void addFilter(Color color, String... data) {
        Pattern[] pattern = new Pattern[data.length];
        for (int i = 0; i < data.length; i++) {
            pattern[i] = Pattern.compile(data[i]);
        }
        span_text.put(pattern, color);
    }

    @Override
    public void onDraw(Drawer drawer) {
        // Los vértices ya se construyen en píxeles: transform identidad.
        drawer.setScale(1f, 1f);
        drawer.shader.setFontMode(true);
        if (!animationScroll || (text_anim_width < constraint_width)) {
            drawer.setupBuffer(text_vbo, local);
            drawer.shader.setSpriteColor(color_span.isEmpty() ? default_color : null);
            Texture.bind(GL.GL_TEXTURE0, font.font_texture);
            gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, text_ibo);
            gl.glDrawElements(GL.GL_TRIANGLES, index_length);
        } else {
            float left = local.x - extent.x;
            anim_step.set(left + text_anim_width + deltax, local.y);
            drawer.scissorArea(left, local.y - extent.y, constraint_width, height);
            drawer.setupBuffer(text_vbo, anim_step);
            drawer.shader.setSpriteColor(color_span.isEmpty() ? default_color : null);
            Texture.bind(GL.GL_TEXTURE0, font.font_texture);
            gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, text_ibo);
            gl.glDrawElements(GL.GL_TRIANGLES, index_length);
            drawer.finishScissor();
            deltax -= text_anim_width * 0.1f * FX.gpu.getDeltaTime();
            if (deltax < -text_anim_width * 2.0f) {
                deltax = constraint_width * 2.0f;
            }
        }
        drawer.shader.setFontMode(false);
        if (debug) {
            setDebugColor(255, 0, 0);
            drawer.setScale(extent.x, extent.y);
            drawer.renderLineQuad(local, this.debug_color);
        }
    }

    public void setConstraintWidth(float constaint) {
        constraint_width = constaint;
    }

    public void setAnimationScroll(boolean enable) {
        animationScroll = enable;
    }

    public void setTextAlignment(byte alignment) {
        this.text_alignment = alignment;
    }

    public void setTextColor(int r, int g, int b) {
        default_color.set(r, g, b);
    }

    public Color getTextColor() {
        return default_color;
    }

    public void setText(String text) {
        this.text = text;
        color_span.clear();
        processSpanner();
        updateText();
    }

    private void updateText() {
        String process = "";
        if (constraint_width > 0.0f && !animationScroll) {
            float width = 0.0f;
            String[] lines = text.split("\n");
            for (String line : lines) {
                String[] words = line.split(" ");
                if (words.length > 1) {
                    for (int i = 0; i < words.length; i++) {
                        float w = getTextWidthReal(words[i]);
                        if ((width + w) > constraint_width) {
                            process += "\n" + words[i] + (i < words.length - 1 ? " " : "");
                            width = w + (i < words.length - 1 ? getTextWidthReal(" ") : 0);
                        } else {
                            process += words[i] + (i < words.length - 1 ? " " : "");
                            width += w + (i < words.length - 1 ? getTextWidthReal(" ") : 0);
                        }
                    }
                } else if (words.length == 1) {
                    for (short i = 0; i < words[0].length(); i++) {
                        String sampler = words[0].charAt(i) + "";
                        float w = getTextWidthReal(sampler);
                        if ((width + w) > constraint_width) {
                            process += "\n" + sampler + (i < words.length - 1 ? " " : "");
                            width = w + (i < words.length - 1 ? getTextWidthReal(" ") : 0);
                        } else {
                            process += sampler + (i < words.length - 1 ? " " : "");
                            width += w + (i < words.length - 1 ? getTextWidthReal(" ") : 0);
                        }
                    }
                }
                if (lines.length > 1) {
                    process += "\n";
                    width = 0.0f;
                }
            }
        } else {
            process = text;
        }
        String[] lines = process.split("\n");
        max_line = getLineMaxWidth(lines);
        num_lines = (short) lines.length;
        int text_lenght = getDrawTextLenght(process);
        float[] vertices = new float[text_lenght * 28];
        short[] indices = new short[text_lenght * 6];
        index_length = indices.length;
        // text_size es la altura de línea; los glifos se escalan desde el tamaño horneado.
        float scale = getScale();
        float cursorY = -num_lines * text_size * 0.5f;
        int offset = 0, index_offset = 0, vertex_offset = 0;
        int text_offset = 0;
        for (String line : lines) {
            float line_width = getTextWidth(line);
            float baseline = cursorY + font.getAscent() * scale;
            float cursorX;
            switch (text_alignment) {
                case TEXT_ALIGNMENT_CENTER_LEFT:
                    cursorX = max_line * -0.5f;
                    break;
                case TEXT_ALIGNMENT_CENTER_RIGHT:
                    cursorX = (max_line * 0.5f) - line_width;
                    break;
                case TEXT_ALIGNMENT_CENTER:
                default:
                    cursorX = -line_width * 0.5f;
                    break;
            }
            for (char c : line.toCharArray()) {
                Color def = getColorSpan(text_offset);
                float c_r = def.r * 0.003921f;
                float c_g = def.g * 0.003921f;
                float c_b = def.b * 0.003921f;
                if (c != ' ') {
                    indices[index_offset] = (short) (vertex_offset);
                    indices[index_offset + 1] = (short) (vertex_offset + 1);
                    indices[index_offset + 2] = (short) (vertex_offset + 3);
                    indices[index_offset + 3] = (short) (vertex_offset);
                    indices[index_offset + 4] = (short) (vertex_offset + 2);
                    indices[index_offset + 5] = (short) (vertex_offset + 3);
                    vertex_offset += 4;
                    index_offset += 6;
                    Font.Glyph g = font.getGlyph(c);
                    float x0 = cursorX + g.xoff * scale;
                    float y0 = baseline + g.yoff * scale;
                    float x1 = x0 + g.w * scale;
                    float y1 = y0 + g.h * scale;
                    // y crece hacia abajo: arriba = v0.
                    float[][] corners = {
                        {x0, y0, g.u0, g.v0},
                        {x0, y1, g.u0, g.v1},
                        {x1, y0, g.u1, g.v0},
                        {x1, y1, g.u1, g.v1}
                    };
                    for (float[] v : corners) {
                        vertices[offset] = v[0];
                        vertices[offset + 1] = v[1];
                        vertices[offset + 2] = v[2];
                        vertices[offset + 3] = v[3];
                        vertices[offset + 4] = c_r;
                        vertices[offset + 5] = c_g;
                        vertices[offset + 6] = c_b;
                        offset += 7;
                    }
                }
                if (text_offset < text.length() && text.charAt(text_offset) == c) {
                    text_offset++;
                }
                cursorX += font.getAdvance(c) * scale;
            }
            if (text_offset < text.length() && text.charAt(text_offset) == '\n') {
                text_offset++;
            }
            cursorY += text_size;
        }
        if (text_vbo != -1) {
            FX.gl.glDeleteBuffer(text_vbo);
            FX.gl.glDeleteBuffer(text_ibo);
        }
        text_vbo = Drawer.createBuffer(vertices, false, false);
        text_ibo = Drawer.createBuffer(indices, true, false);
        if (!animationScroll) {
            setWidth(max_line);
        } else {
            setWidth(constraint_width);
            text_anim_width = max_line;
        }
        setHeight(num_lines * text_size);
    }

    public boolean isConstraintUse() {
        return animationScroll && (text_anim_width > constraint_width);
    }

    public float getTextWidth(String text) {
        float text_width = 0.0f;
        for (short i = 0; i < text.length(); i++) {
            text_width += font.getAdvance(text.charAt(i)) * getScale();
        }
        return text_width;
    }

    public float getTextWidthReal(String text) {
        return getTextWidth(text);
    }

    public float getCharWidth(char c) {
        return font.getAdvance(c) * getScale();
    }

    private Color getColorSpan(int text_offset) {
        for (TextColorSpan s : color_span) {
            if (text_offset >= s.start_index && text_offset < s.end_index) {
                return s.color;
            }
        }
        return color_span.isEmpty() ? new Color(255, 255, 255) : default_color;
    }

    // Usa la instancia rasterizada más cercana al tamaño pedido para mantener la nitidez.
    private void resolveFont() {
        if (font != null && text_size > 0f) {
            font = Font.forSize(font.getPath(), text_size);
        }
    }

    private float getScale() {
        return text_size / font.getLineHeight();
    }

    public float getTextSize() {
        return text_size;
    }

    public void setTextSize(float size) {
        text_size = size;
        resolveFont();
        updateText();
    }

    public float getLineMaxWidth(String[] lines) {
        float max_width = 0.0f;
        for (short i = 0; i < lines.length; i++) {
            float x = getTextWidth(lines[i]);
            if (max_width < x) {
                max_width = x;
            }
        }
        return max_width;
    }

    private int getDrawTextLenght(String process) {
        int i = 0;
        for (char c : process.toCharArray()) {
            if (c != '\n' && c != ' ') {
                i++;
            }
        }
        return i;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
        resolveFont();
        setText(text);
    }

    @Override
    public void onDestroy() {
        setVisibility(INVISIBLE);
        font = null;
        default_color = null;
        gl.glDeleteBuffer(text_vbo);
        gl.glDeleteBuffer(text_ibo);
        text = null;
    }

    private static class TextColorSpan {
        public short start_index;
        public short end_index;
        public Color color;
    }
}
