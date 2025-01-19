package com.forcex.utils;

import com.forcex.FX;
import com.forcex.core.CoreJni;
import com.forcex.io.BinaryStreamReader;
import com.forcex.io.FileSystem;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Image {
    public int width;
    public int height;
    private byte[] data;

    public Image(String path) {
        int[] info = new int[2];
        if(new File(path).exists()) {
            data = CoreJni.imageDecodeFromPath(info, path);
        } else {
            BinaryStreamReader is = FX.fs.open(path, FileSystem.ReaderType.MEMORY);
            if(is != null) {
                data = CoreJni.imageDecodeFromMemory(info, is.getData());
            }
        }
        width = info[0];
        height = info[1];
        if (data == null) {
            data = new byte[] {
                    (byte) 0xff, (byte) 0xff,
                    (byte) 0xff, (byte) 0xff
            };
            width = 1;
            height = 1;
        }
    }

    public Image(byte[] image_file_data) {
        int[] info = new int[2];
        data = CoreJni.imageDecodeFromMemory(info, image_file_data);
        width = info[0];
        height = info[1];

        if (data == null) {
            data = new byte[] {
                    (byte) 0xff, (byte) 0xff,
                    (byte) 0xff, (byte) 0xff
            };
            width = 1;
            height = 1;
        }
    }

    public Image(byte[] rgba, int width, int height) {
        this.data = rgba;
        this.width = width;
        this.height = height;
    }

    public void save(String path, boolean jpg) {
        CoreJni.imageEncode(data, path, width, height, jpg ? 1 : 0);
    }

    public byte[] getRGBAImage() {
        return data;
    }

    public Color getRGBA(int x, int y) {
        int pixel = (x + (width * y)) * 4;
        return new Color(
                data[pixel] & 0xff, data[pixel + 1] & 0xff, data[pixel + 2] & 0xff, data[pixel + 3] & 0xff
        );
    }

    public void setRGBA(int x, int y, int r, int g, int b, int a) {
        int pixel = (x + (width * y)) * 4;
        data[pixel] = (byte) r;
        data[pixel + 1] = (byte) g;
        data[pixel + 2] = (byte) b;
        data[pixel + 3] = (byte) a;
    }

    public ByteBuffer getBuffer() {
        ByteBuffer bb = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
        bb.put(data);
        bb.flip();
        return bb;
    }

    public void clear() {
        data = null;
    }
}
