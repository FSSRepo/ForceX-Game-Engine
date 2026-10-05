package com.forcex.gui;

import com.forcex.FX;
import com.forcex.core.CoreJni;
import com.forcex.core.GL;
import com.forcex.core.gpu.Texture;
import com.forcex.io.BinaryStreamReader;
import com.forcex.io.BinaryStreamWriter;
import com.forcex.io.FileSystem;

import java.io.FileOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class Font {
    public int font_texture;
    public float rowFactor, columnFactor;
    public byte rowPitch, startChar;
    public float[] char_widths;
    private final GL gl = FX.gl;

    public Font(String path) {
        BinaryStreamReader is = FX.fs.open(path, FileSystem.ReaderType.MEMORY);
        startChar = is.readByte();
        rowPitch = is.readByte();
        columnFactor = is.readFloat();
        rowFactor = is.readFloat();
        char_widths = is.readFloatArray(256);
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

    public static void convertFont(String bff_path, String fft_path) {
        try {
            BinaryStreamReader is = FX.fs.open(bff_path, FileSystem.ReaderType.STREAM);
            BinaryStreamWriter os = new BinaryStreamWriter(new FileOutputStream(fft_path));
            is.skip(2);
            int imgWidth = is.readInt();
            int imgHeight = is.readInt();
            int cellWidth = is.readInt();
            int cellHeight = is.readInt();
            if(is.readByte() != 8) {
                System.out.println("The bff font must be in grayscale format");
                return;
            }
            byte baseChar = is.readByte();
            byte[] charsWidth = is.readByteArray(256);
            os.writeByte(baseChar);
            os.writeByte((byte) (imgWidth / cellWidth));
            os.writeFloat((float) cellWidth / imgWidth);
            os.writeFloat((float) cellHeight / imgHeight);
            for (byte width : charsWidth) {
                os.writeFloat((float) (width & 0xff) / cellWidth);
            }
            os.writeShort(imgWidth);
            os.writeShort(imgHeight);
            byte[] imgData = is.readByteArray(imgWidth * imgHeight);
            byte[] buffer = new byte[imgWidth * imgHeight * 4];
            for (int i = 0; i < imgData.length; i++) {
                buffer[i * 4] = (byte) 0xff;
                buffer[i * 4 + 1] = (byte) 0xff;
                buffer[i * 4 + 2] = (byte) 0xff;
                buffer[i * 4 + 3] = imgData[i];
            }
            Deflater df = new Deflater(Deflater.BEST_COMPRESSION);
            // create internal copy with image format RGBA4444
            df.setInput(CoreJni.convertImageFormat(buffer, imgWidth, imgHeight, false));
            df.finish();
            int deflated_size = df.deflate(buffer);
            df.end();
            os.writeShort(deflated_size);
            os.write(buffer, deflated_size);
            is.clear();
            os.finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void delete() {
        Texture.remove(font_texture);
    }
}
