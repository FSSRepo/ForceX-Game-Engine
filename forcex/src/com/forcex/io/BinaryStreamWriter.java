package com.forcex.io;

import com.forcex.math.Quaternion;
import com.forcex.math.Vector3f;
import com.forcex.utils.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class BinaryStreamWriter {
    private OutputStream os;
    private byte[] data;
    private int offset = 0;

    public BinaryStreamWriter(int reserve, OutputStream os) {
        this.data = new byte[reserve];
        this.os = os;
    }

    public BinaryStreamWriter(OutputStream os) {
        this.os = os;
    }

    private void write(byte[] data) {
        try {
            os.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(byte[] data, int length) {
        try {
            os.write(data, 0, length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeFloatArray(float[] data) {
        for (int i = 0; i < data.length; i++) {
            writeFloat(data[i]);
        }
    }

    public void writeByteArray(byte[] data) {
        write(data);
    }

    public void writeShortArray(short[] data) {
        for (int i = 0; i < data.length; i++) {
            writeShort(data[i]);
        }
    }

    public void writeQuaternion(Quaternion qt) {
        writeShort((short) (qt.x * 4096.0f));
        writeShort((short) (qt.y * 4096.0f));
        writeShort((short) (qt.z * 4096.0f));
        writeShort((short) (qt.w * 4096.0f));
    }

    public void writeFloat2(float x, float y) {
        writeFloat(x);
        writeFloat(y);
    }

    public void writeFloat3(float x, float y, float z) {
        writeFloat(x);
        writeFloat(y);
        writeFloat(z);
    }

    public void writeVector3(Vector3f v) {
        writeFloat(v.x);
        writeFloat(v.y);
        writeFloat(v.z);
    }

    public void writeString(String text) {
        writeByteArray(text.getBytes());
    }

    public void writeStringFromSize(int size, String text) {
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            if (i <= text.length() - 1) {
                data[i] = (byte) text.charAt(i);
            } else {
                break;
            }
        }
        writeByteArray(data);
    }

    public void writeFloat(float value) {
        writeInt(Float.floatToIntBits(value));
    }

    public void writeInt(int value) {
        if(data != null) {
            data[offset] = (byte) (value & 0xFF);
            data[offset + 1] = (byte) ((value >> 8) & 0xFF);
            data[offset + 2] = (byte) ((value >> 16) & 0xFF);
            data[offset + 3] = (byte) ((value >> 24) & 0xFF);
            offset += 4;
            return;
        }
        byte[] data = new byte[4];
        data[0] = (byte) (value & 0xFF);
        data[1] = (byte) ((value >> 8) & 0xFF);
        data[2] = (byte) ((value >> 16) & 0xFF);
        data[3] = (byte) ((value >> 24) & 0xFF);
        write(data);
    }

    public void writeShort(int value) {
        if(data != null) {
            data[offset] = (byte) (value & 0xFF);
            data[offset + 1] = (byte) ((value >> 8) & 0xFF);
            offset += 2;
            return;
        }
        byte[] data = new byte[2];
        data[0] = (byte) (value & 0xFF);
        data[1] = (byte) ((value >> 8) & 0xFF);
        write(data);
    }

    public void writeByte(int value) {
        if(data != null) {
            data[offset] = (byte) (value & 0xFF);
            offset++;
        }
        write(new byte[] {
                (byte) (value & 0xFF)
        });
    }

    public void allocate(int size) {
        byte[] newData = new byte[data.length + size];
        int data_offset = 0;
        for (int i = 0; i < offset; i++) {
            newData[data_offset] = data[i];
            data_offset++;
        }
        for (int i = 0; i < size; i++) {
            newData[data_offset] = 0;
            data_offset++;
        }
        data = newData;
    }

    public void finish() {
        try {
            if(data != null) {
                os.write(data);
            }
            os.close();
        } catch (Exception e) {
            Logger.log(e.toString());
        }
    }
}
