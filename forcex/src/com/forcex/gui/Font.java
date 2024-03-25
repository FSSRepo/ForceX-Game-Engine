package com.forcex.gui;

import com.forcex.FX;
import com.forcex.core.GL;
import com.forcex.core.gpu.Texture;
import com.forcex.io.MemoryStreamReader;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class Font {
    public int font_texture;
    public float rowFactor, columnFactor;
    public byte rowPitch, startChar;
    public float[] charWidths;
    private GL gl = FX.gl;

    public Font(String path) {
        MemoryStreamReader is = new MemoryStreamReader(path);
        startChar = is.readByte();
        rowPitch = is.readByte();
        columnFactor = is.readFloat();
        rowFactor = is.readFloat();
        charWidths = is.readFloatArray(256);
        short width = is.readShort();
        short height = is.readShort();
        short size = is.readShort();
        byte[] buffer = new byte[width * height * 2];
        try {
            Inflater dec = new Inflater();
            dec.setInput(is.readByteArray(size));
            dec.inflate(buffer);
            dec.end();
        } catch (DataFormatException e) {
            e.printStackTrace();
        }
        is.clear();

        font_texture = gl.glGenTexture();
        gl.glBindTexture(GL.GL_TEXTURE_2D, font_texture);
        gl.glTexImage2D(GL.GL_TEXTURE_2D, width, height, GL.GL_TEXTURE_RGBA4, buffer);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
    }

    public void delete() {
        Texture.remove(font_texture);
    }
}
