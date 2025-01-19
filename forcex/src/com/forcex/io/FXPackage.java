package com.forcex.io;

import java.util.HashMap;

public class FXPackage {
    protected HashMap<String, FXEntry> entries = new HashMap<>();
    protected String file_name;

    public static class FXEntry {
        public int size;
        public int offset;
    }

    public FXPackage(String file_name) {
        this.file_name = file_name;
    }

    public static FXPackage load(BinaryStreamReader is, String file_name) {
        FXPackage pkg = new FXPackage(file_name);
        int numEntries = is.readShort();
        for (int i = 0; i < numEntries; i++) {
            short str_len = is.readUnsignedByte();
            FXEntry entry = new FXEntry();
            entry.size = is.readInt();
            entry.offset = is.readInt();
            pkg.entries.put(is.readString(str_len), entry);
        }
        is.clear();
        return pkg;
    }
}
