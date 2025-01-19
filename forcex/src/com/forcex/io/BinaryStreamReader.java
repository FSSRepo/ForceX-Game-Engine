package com.forcex.io;

import com.forcex.math.Quaternion;
import com.forcex.math.Vector3f;
import com.forcex.utils.GameUtils;

import java.io.IOException;
import java.io.InputStream;

public class BinaryStreamReader {
    private byte[] data; // memory read
    private int offset;
    private InputStream is; // file read

    public BinaryStreamReader(InputStream is) {
        this.is = is;
    }

    public BinaryStreamReader(byte[] data) {
        this.data = data;
    }

    public void seek(int offset) {
        if(data == null) {
            return;
        }
        this.offset = offset;
    }

    public boolean isEndOfFile() {
        if(data == null) {
            return false;
        }
        return (offset + 1) >= data.length;
    }

    public int getOffset() {
        if(data == null) {
            return -1;
        }
        return offset;
    }

    public int length() {
        if(data == null) {
            try {
                return is.available();
            }catch (Exception e) {
                e.printStackTrace();
            }
            return -1;
        }
        return data.length;
    }

    public byte[] getData() {
        return data;
    }

    public int readUnsignedShort() {
        if(data != null) {
            int val = ((data[offset] & 0xFF) | (data[offset + 1] & 0xFF) << 8);
            offset += 2;
            return val;
        }
        return ((read() & 0xFF) | (read() & 0xFF) << 8);
    }

    public short readShort() {
        if(data != null) {
            short val = (short) ((data[offset] & 0xFF) | (data[offset + 1] & 0xFF) << 8);
            offset += 2;
            return val;
        }
        return (short) (read() | (read() << 8));
    }

    public short readUnsignedByte() {
        if(data != null) {
            short val = (short) (data[offset] & 0xFF);
            offset++;
            return val;
        }
        return (short) (read() & 0xFF);
    }

    public byte readByte() {
        if(data != null) {
            byte val = data[offset];
            offset++;
            return val;
        }
        return (byte) read();
    }

    public boolean readBoolean() {
        return readByte() == 1;
    }

    public void clear() {
        if(data != null) {
            return;
        }
        try {
            is.close();
        } catch (IOException e) {
        }
    }

    public int readInt() {
        if(data != null) {
            int val = (data[offset] & 0xFF) | (data[offset + 1] & 0xFF) << 8 | (data[offset + 2] & 0xFF) << 16 | (data[offset + 3] & 0xFF) << 24;
            offset += 4;
            return val;
        }
        return (read() & 0xFF)
                | (read() & 0xFF) << 8
                | (read() & 0xFF) << 16
                | (read() & 0xFF) << 24;
    }

    public short[] readShortArray(int size) {
        short[] data_ = new short[size];
        for (int i = 0; i < size; i++) {
            data_[i] = readShort();
        }
        return data_;
    }

    public void skip(int len) {
        if(data != null) {
            offset += len;
            return;
        }
        try {
            is.skip(len);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public Quaternion readQuaternion_f16() {
        return new Quaternion(readShort() / 4096.0f, readShort() / 4096.0f, readShort() / 4096.0f, readShort() / 4096.0f);
    }

    public Vector3f readVector() {
        return new Vector3f(readFloat(), readFloat(), readFloat());
    }

    public byte[] readByteArray(int size) {
        byte[] buffer = new byte[size];
        if(data != null) {
            for (int i = 0; i < size; i++) {
                buffer[i] = readByte();
            }
        } else {
            read(buffer);
        }
        return buffer;
    }

    public void readByteArray(byte[] buffer) {
        if(data != null) {
            for (int i = 0; i < buffer.length; i++) {
                buffer[i] = readByte();
            }
        } else {
            read(buffer);
        }
    }

    public String readString(int size) {
        return GameUtils.trimString(new String(readByteArray(size)));
    }

    public String readString() {
        String result = "";
        byte inByte;
        while ((inByte = readByte()) != 0)
            result += (char) inByte;
        return result;
    }

    public void find(char[] c) {
        while (true) {
            if (readByte() == c[0] && readByte() == c[1] && readByte() == c[2]) {
                break;
            }
            seek(-2);
        }
    }

    public float[] readFloatArray(int size) {
        float[] data_ = new float[size];
        for (int i = 0; i < size; i++) {
            data_[i] = readFloat();
        }
        return data_;
    }

    private void read(byte[] data) {
        try {
            is.read(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int read() {
        try {
            return is.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
