package com.forcex.gtasdk;

import com.forcex.math.Vector3f;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class RWSection {
    ArrayList<RWSection> children = new ArrayList<>();
    private final int id;
    private int offset;
    private byte[] data = new byte[12];

    public RWSection(int id, int version) {
        this.id = id;
        writeInt(id);
        writeInt(0);
        writeInt(version);
    }

    public RWSection addSection(RWSection section) {
        children.add(section);
        return this;
    }

    public void UpdateData() {
        for (RWSection child : children) {
            child.UpdateData();
            allocate(child.data.length);
            writeByteArray(child.data);
            child.data = null;
        }
        offset = 0;
        writeInt(id);
        writeInt(data.length - 12);
        offset = data.length;
    }

    public void seek(int offset) {
        this.offset = offset;
    }

    public void skip(int len) {
        offset += len;
    }

    public int getOffset() {
        return offset;
    }

    public void writeVector(Vector3f vec) {
        if (data.length < (offset + 12)) {
            allocate(12);
        }
        writeFloat(vec.x);
        writeFloat(vec.y);
        writeFloat(vec.z);
    }

    public void writeFloatArray(float[] buffer) {
        if (data.length < (offset + buffer.length * 4)) {
            allocate(buffer.length * 4);
        }
        for (int ofs = 0; ofs < buffer.length; ofs++) {
            writeFloat(buffer[ofs]);
        }
    }

    public void writeShortArray(short[] buffer) {
        if (data.length < (offset + buffer.length * 2)) {
            allocate(buffer.length * 2);
        }
        for (int ofs = 0; ofs < buffer.length; ofs++) {
            writeShort(buffer[ofs]);
        }
    }

    public void writeByteArray(byte[] buffer) {
        if (data.length < (offset + buffer.length)) {
            allocate(buffer.length);
        }
        for (int ofs = 0; ofs < buffer.length; ofs++) {
            writeByte(buffer[ofs]);
        }
    }

    public void writeString(String text) {
        writeByteArray(text.getBytes());
    }

    public void writeFloat(float val) {
        writeInt(Float.floatToIntBits(val));
    }

    public void writeInt(int values) {
        if (data.length < (offset + 4)) {
            allocate(4);
        }
        data[offset] = (byte) (values & 0xFF);
        data[offset + 1] = (byte) ((values >> 8) & 0xFF);
        data[offset + 2] = (byte) ((values >> 16) & 0xFF);
        data[offset + 3] = (byte) ((values >> 24) & 0xFF);
        offset += 4;
    }

    public void writeShort(int values) {
        if (data.length < (offset + 2)) {
            allocate(2);
        }
        data[offset] = (byte) (values & 0xFF);
        data[offset + 1] = (byte) ((values >> 8) & 0xFF);
        offset += 2;
    }

    public void writeByte(int values) {
        if (data.length < (offset + 1)) {
            allocate(1);
        }
        data[offset] = (byte) (values & 0xFF);
        offset++;
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

    public byte write(String path, boolean nextExt) {
        try {
            if (!nextExt) {
                UpdateData();
            }
            try {
                FileOutputStream os = new FileOutputStream(path);
                os.write(data);
                os.close();
            } catch (IOException e) {
                return 1;
            }
        } catch (OutOfMemoryError out) {
            return -1;
        }
        return 0;
    }
}
