package com.forcex.io;

public class BitStream {
    byte[] data;
    int off = 0;
    byte off2 = 0;

    public BitStream(int numValues) {
        data = new byte[(numValues / 7) + 1];
    }

    public BitStream(byte[] data) {
        this.data = data;
    }

    public void put(boolean z) {
        data[off] |= ((z ? 1 : 0) << 6 - off2);
        if (off2 == 6) {
            off++;
            off2 = 0;
        } else {
            off2++;
        }
    }

    public boolean next() {
        boolean z = (data[off] >> (6 - off2) & 0x1) == 1;
        if (off2 == 6) {
            off2 = 0;
            off++;
        } else {
            off2++;
        }
        return z;
    }

}
