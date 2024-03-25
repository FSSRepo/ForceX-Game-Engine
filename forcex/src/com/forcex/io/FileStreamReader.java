package com.forcex.io;

import com.forcex.math.Quaternion;
import com.forcex.math.Vector3f;
import com.forcex.utils.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileStreamReader implements BinaryReader {
    InputStream is;

    public FileStreamReader(String path) {
        try {
            is = new FileInputStream(path);
        } catch (IOException e) {
            Logger.log("IOE " + e);
        }
    }

    @Override
    public int readUShort() {
        return ((read() & 0xFF) | (read() & 0xFF) << 8);
    }

    public short readShort() {
        return (short) (read() | (read() << 8));
    }

    @Override
    public short readUbyte() {
        return (short) (read() & 0xFF);
    }

    @Override
    public byte readByte() {
        return (byte) read();
    }

    @Override
    public boolean readBoolean() {
        return read() == 1;
    }

    @Override
    public void clear() {
        try {
            is.close();
        } catch (IOException e) {
        }
    }

    public void read(byte[] data) {
        try {
            is.read(data);
        } catch (IOException e) {
        }
    }

    public int read() {
        try {
            return is.read();
        } catch (IOException e) {
        }
        return -1;
    }

    @Override
    public int readInt() {
        return (read() & 0xFF)
                | (read() & 0xFF) << 8
                | (read() & 0xFF) << 16
                | (read() & 0xFF) << 24;
    }

    public void printData(int lenght) {
        int i = 0;
        while (i < lenght) {
            System.out.println("0x" + Integer.toHexString(readUbyte()).toLowerCase());
            i++;
        }
    }

    @Override
    public short[] readShortArray(int size) {
        short[] data = new short[size];
        byte[] buffer = new byte[size * 2];
        read(buffer);
        for (int i = 0; i < size; i++) {
            data[i] = (short) ((buffer[i * 2] & 0xFF) | (buffer[i * 2 + 1] & 0xFF) << 8);
        }
        return data;
    }

    @Override
    public void skip(int len) {
        try {
            is.skip(len);
        } catch (IOException e) {
        }
    }

    @Override
    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public Quaternion readQuaternion() {
        return new Quaternion(readShort() / 4096.0f, readShort() / 4096.0f, readShort() / 4096.0f, readShort() / 4096.0f);
    }

    @Override
    public Vector3f readVector() {
        return new Vector3f(readFloat(), readFloat(), readFloat());
    }

    @Override
    public byte[] readByteArray(int size) {
        byte[] data = new byte[size];
        read(data);
        return data;
    }

    @Override
    public String readString(int size) {
        return cortarnombre(new String(readByteArray(size)));
    }

    @Override
    public String scanString() {
        String result = "";
        byte inByte;
        while ((inByte = readByte()) != 0)
            result += (char) inByte;
        return result;
    }


    public void find(char[] c) {
        while (true) {
            if (
                    read() == c[0] &&
                            read() == c[1] &&
                            read() == c[2]) {
                skip(1);
                break;
            }
        }
    }

    private String cortarnombre(String str) {
        int indexOf = str.indexOf(0);
        return indexOf > 0 ? str.substring(0, indexOf) : str;
    }

    public float[] readFloatArray(int size) {
        float[] data = new float[size];
        byte[] buffer = new byte[size * 4];
        read(buffer);
        for (int i = 0; i < data.length; i++) {
            data[i] = Float.intBitsToFloat((buffer[i * 4] & 0xFF) | (buffer[i * 4 + 1] & 0xFF) << 8 | (buffer[i * 4 + 2] & 0xFF) << 16 | (buffer[i * 4 + 3] & 0xFF) << 24);
        }
        return data;
    }
}
