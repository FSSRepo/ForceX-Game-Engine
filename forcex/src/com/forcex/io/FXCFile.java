package com.forcex.io;

import com.forcex.utils.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class FXCFile {
    ArrayList<Entry> entries = new ArrayList<>();
    ArrayList<String> directories = new ArrayList<>();
    String path;
    public FXCFile(String path) {
        this.path = path;
    }

    public void read() {
        try {
            FileStreamReader is = new FileStreamReader(path);
            int numEntries = is.readShort();
            int numDirectories = is.readUbyte();
            int i = 0;
            int offset = 3;
            for (; i < numDirectories; i++) {
                short strlen = is.readUbyte();
                directories.add(is.readString(strlen));
                offset += strlen + 1;
            }
            for (i = 0; i < numEntries; i++) {
                short strlen = is.readUbyte();
                Entry e = new Entry();
                e.name = is.readString(strlen);
                for (int j = e.name.length() - 1; j >= 0; j--) {
                    if (e.name.charAt(j) == '/') {
                        e.parent = e.name.substring(0, j + 1);
                        e.name = e.name.substring(j + 1);
                        break;
                    }
                }
                strlen = is.readUbyte();
                e.sha1 = is.readString(strlen);
                e.deflate = is.readInt();
                e.inflate = is.readInt();
                offset +=
                        e.name.length() +
                                e.parent.length() +
                                e.sha1.length() + 10;
                e.offset = offset;
                offset += e.deflate != -1 ? e.deflate : e.inflate;
                is.skip(e.deflate != -1 ? e.deflate : e.inflate);
                entries.add(e);
            }
            is.clear();
        } catch (Exception e) {
            Logger.log(e);
        }
    }

    public void add(File file, ArrayList<String> ignore) {
        if (file.isDirectory()) {
            serializeFolder(file, ignore, file.getName());
        } else {
            addBuffer(FileUtils.readBinaryData(file.toString()), "", file.getName());
        }
    }

    public void add(File file) {
        add(file, null);
    }

    public void addBuffer(byte[] buffer, String filepath, String name) {
        Entry e = new Entry();
        e.name = name;
        e.parent = filepath;
        deflateFile(e, buffer);
        if (!directories.contains(filepath)) {
            directories.add(filepath);
        }
        e.operation = 1;
        entries.add(e);
        buffer = null;
    }

    public void replaceFile(String file, Entry entry) {
        deflateFile(entry, FileUtils.readBinaryData(file));
        entry.operation = 1;
    }

    public ArrayList<String> getDirectories() {
        return directories;
    }

    public ArrayList<Entry> getEntries() {
        return entries;
    }

    public int getNumEntries() {
        return entries.size();
    }

    public void printInfo() {
        System.out.println("Content:");
        for (String d : directories) {
            System.out.println("Directory: " + d);
            for (Entry e : entries) {
                if (e.parent.equals(d)) {
                    System.out.println("Name: " + e.name + "\nSHA-1: " + e.sha1);
                }
            }
        }
    }

    public Entry getEntry(String name) {
        for (Entry en : entries) {
            if (en.name.contains(name) || (en.parent + en.name).endsWith(name)) {
                return en;
            }
        }
        return null;
    }

    public void extractFolder(String dest, String dir) {
        String parent = "";
        for (int i = dir.length() - 2; i >= 0; i--) {
            if (dir.charAt(i) == '/') {
                parent = dir.substring(0, i) + "/";
                break;
            }
        }
        boolean exist = false;
        for (String d : directories) {
            if (d.startsWith(dir)) {
                File destDir = new File(dest + "/" + d.replace(parent, ""));
                if (!destDir.exists()) {
                    destDir.mkdir();
                }
                for (Entry e : entries) {
                    if (e.parent.equals(d)) {
                        extract(destDir.toString(), e);
                    }
                }
                exist = true;
            }
        }
        if (!exist) {
            // Error folder not found
		}
    }

    public void extract(String dest, Entry entry) {
        if (entry == null) {
            return;
        }
        try {
            FileOutputStream os = new FileOutputStream(dest + "/" + entry.name);
            FileStreamReader is = new FileStreamReader(path);
            is.skip(entry.offset);
            byte[] data = new byte[entry.inflate];
            if (entry.deflate != -1) {
                Inflater inf = new Inflater();
                inf.setInput(is.readByteArray(entry.deflate));
                inf.inflate(data);
                inf.end();
            } else {
                is.read(data);
            }
            os.write(data);
            os.close();
            data = null;
        } catch (Exception e) {
            Logger.log(e);
        }
    }

    public void close() {
        entries.clear();
        entries = null;
        directories.clear();
        directories = null;
        path = null;
    }

    public void update() {
        try {
            RandomAccessFile is = null;
            FileStreamWriter os = null;
            if (new File(path).exists()) {
                is = new RandomAccessFile(path, "r");
                os = new FileStreamWriter(path + "_temp");
            } else {
                os = new FileStreamWriter(path);
            }
            int offset = 3;
            os.writeShort(entries.size());
            os.writeByte(directories.size());
            for (String d : directories) {
                os.writeByte(d.length());
                os.writeString(d);
                offset += d.length() + 1;
            }
            ListIterator<Entry> eit = entries.listIterator();
            while (eit.hasNext()) {
                Entry e = eit.next();
                String ip = e.parent + e.name;
                os.writeByte(ip.length());
                os.writeString(ip);
                os.writeByte(e.sha1.length());
                os.writeString(e.sha1);
                os.writeInt(e.deflate);
                os.writeInt(e.inflate);
                offset += 10 + e.sha1.length() + e.name.length();
                if (e.operation == 1 || e.operation == 2) {
                    os.writeByteArray(e.deflated_tmp);
                    e.deflated_tmp = null;
                    e.offset = offset;
                    offset += e.deflate != -1 ? e.deflate : e.inflate;
                } else {
                    is.seek(e.offset);
                    byte[] buffer = new byte[e.deflate];
                    is.read(buffer);
                    os.writeByteArray(buffer);
                    e.offset = offset;
                    offset += e.deflate != -1 ? e.deflate : e.inflate;
                    buffer = null;
                }
            }
            os.finish();
            if (is != null) {
                new File(path + "_temp").renameTo(new File(path));
            }
        } catch (Exception e) {
            Logger.log(e);
        }
    }

    private void deflateFile(Entry e, byte[] buffer) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(buffer, 0, buffer.length);
            byte[] out = sha1.digest();
            for (int i = 0; i < out.length; i++) {
                e.sha1 += Integer.toString((out[i] & 0xff) + 0x100, 16).substring(1);
            }
            out = null;
        } catch (Exception ex) {
        }
        if (e.name.endsWith(".png")) {
            e.deflate = -1;
            e.inflate = buffer.length;
            e.deflated_tmp = buffer;
            return;
        }
        Deflater df = new Deflater(Deflater.BEST_COMPRESSION);
        df.setInput(buffer);
        df.finish();
        byte[] temp = new byte[buffer.length];
        e.deflate = df.deflate(temp);
        e.inflate = buffer.length;
        df.end();
        e.deflated_tmp = new byte[e.deflate];
		System.arraycopy(temp, 0, e.deflated_tmp, 0, e.deflate);
    }

    private boolean isIgnored(ArrayList<String> ignore, String cur_dir) {
        for (String ign : ignore) {
            if (cur_dir.startsWith(ign)) {
                return true;
            }
        }
        return false;
    }

    private void serializeFolder(File dir, ArrayList<String> ignore, String root_path) {
        String[] ls = dir.list();
        String fxpath = root_path + "/";
        if (ignore != null && isIgnored(ignore, fxpath)) {
            return;
        } else {
            directories.add(fxpath);
        }
        for (String d : ls) {
            File test = new File(dir + "/" + d);
            if (test.isDirectory()) {
                serializeFolder(test, ignore, root_path + "/" + d);
            } else {
                addBuffer(FileUtils.readBinaryData(test.toString()), root_path + "/", d);
            }
        }
    }

    public static class Entry {
        public String name;
        public String parent = "";
        public String sha1 = "";
        public int deflate;
        public int inflate;
        public int offset;
        int operation = -1; // 1 = (add, replace), 3 = delete
        byte[] deflated_tmp = null;
    }
}
