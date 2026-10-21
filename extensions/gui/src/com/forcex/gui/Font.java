package com.forcex.gui;

import com.forcex.FX;
import com.forcex.core.CoreJni;
import com.forcex.core.GL;
import com.forcex.core.gpu.Texture;
import com.forcex.io.BinaryStreamReader;
import com.forcex.io.FileSystem;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

public class Font {
    private static final int FIRST_CHAR = 32;
    private static final int NUM_CHARS = 224; // 32..255 (Latin-1)
    private static final int ATLAS_MIN_SIZE = 512;
    private static final int ATLAS_MAX_SIZE = 2048;
    // Tamaños de rasterizado disponibles para forSize().
    private static final float[] BUCKETS = {24f, 48f, 96f};
    private static final HashMap<String, Font> cache = new HashMap<>();

    public int font_texture;
    private final String path;
    private final float pixelSize;
    private final int atlasSize;
    private final Glyph[] glyphs = new Glyph[NUM_CHARS];
    private final float ascent;
    private final float descent;
    private final float lineHeight;
    private final GL gl = FX.gl;

    private Font(String path, float pixelSize) {
        this.path = path;
        this.pixelSize = pixelSize;

        BinaryStreamReader is = FX.fs.open(path, FileSystem.ReaderType.MEMORY);
        if (is == null) {
            throw new RuntimeException("Font not found: " + path);
        }
        byte[] ttf = is.getData();
        is.clear();

        float[] glyphData = new float[NUM_CHARS * 9];
        float[] metrics = new float[3];
        // El atlas crece hasta que caben todos los glifos (a 96 px no caben en 512x512).
        byte[] atlas = null;
        int size = ATLAS_MIN_SIZE;
        while (atlas == null && size <= ATLAS_MAX_SIZE) {
            atlas = CoreJni.fontBake(ttf, pixelSize, FIRST_CHAR, NUM_CHARS, size, size, glyphData, metrics);
            if (atlas == null) {
                size *= 2;
            }
        }
        if (atlas == null) {
            throw new RuntimeException("Failed to bake font: " + path);
        }
        atlasSize = size;

        ascent = metrics[0];
        descent = metrics[1];
        lineHeight = ascent - descent + metrics[2];

        // Atlas de 1 canal: el shader de sprites (fontMode) usa .r como cobertura.
        ByteBuffer atlasBuffer = ByteBuffer.allocateDirect(atlas.length).order(ByteOrder.nativeOrder());
        atlasBuffer.put(atlas).position(0);

        font_texture = gl.glGenTexture();
        gl.glBindTexture(GL.GL_TEXTURE_2D, font_texture);
        gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_LUMINANCE, atlasSize, atlasSize, GL.GL_LUMINANCE, GL.GL_UNSIGNED_BYTE, atlasBuffer);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);

        for (int i = 0; i < NUM_CHARS; i++) {
            float[] g = new float[9];
            System.arraycopy(glyphData, i * 9, g, 0, 9);
            glyphs[i] = new Glyph(g, atlasSize);
        }
    }

    public static Font get(String path, float pixelSize) {
        String key = path + ":" + pixelSize;
        return cache.computeIfAbsent(key, k -> new Font(path, pixelSize));
    }

    /**
     * Devuelve la instancia más pequeña que permite dibujar una línea de {@code lineHeight} px
     * sin ampliar el atlas (si ninguna alcanza, la mayor).
     */
    public static Font forSize(String path, float lineHeight) {
        Font reference = get(path, BUCKETS[1]);
        float needed = lineHeight * BUCKETS[1] / reference.lineHeight;
        for (float bucket : BUCKETS) {
            if (bucket >= needed) {
                return get(path, bucket);
            }
        }
        return get(path, BUCKETS[BUCKETS.length - 1]);
    }

    /** Libera todas las instancias en caché. */
    public static void clearCache() {
        for (Font font : new java.util.ArrayList<>(cache.values())) {
            font.delete();
        }
    }

    public Glyph getGlyph(char c) {
        int idx = (int) c - FIRST_CHAR;
        if (idx < 0 || idx >= NUM_CHARS) {
            return glyphs[0]; // fallback a espacio
        }
        return glyphs[idx];
    }

    public float getAdvance(char c) {
        return getGlyph(c).advance;
    }

    public float getTextWidth(String text) {
        float width = 0.0f;
        for (int i = 0; i < text.length(); i++) {
            width += getAdvance(text.charAt(i));
        }
        return width;
    }

    public float getLineHeight() {
        return lineHeight;
    }

    public float getAscent() {
        return ascent;
    }

    public float getDescent() {
        return descent;
    }

    public float getPixelSize() {
        return pixelSize;
    }

    public String getPath() {
        return path;
    }

    public void delete() {
        Texture.remove(font_texture);
        cache.remove(path + ":" + pixelSize);
    }

    public static class Glyph {
        public final float u0, v0, u1, v1; // UV normalizados
        public final float xoff, yoff;     // offset desde el pen (píxeles)
        public final float w, h;           // tamaño del quad (píxeles)
        public final float advance;        // avance horizontal (píxeles)

        Glyph(float[] d, int atlas_size) {
            u0 = d[0] / atlas_size;
            v0 = d[1] / atlas_size;
            u1 = d[2] / atlas_size;
            v1 = d[3] / atlas_size;
            xoff = d[4];
            yoff = d[5];
            advance = d[6];
            w = d[7] - d[4];
            h = d[8] - d[5];
        }
    }
}
