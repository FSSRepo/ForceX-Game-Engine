package com.forcex.io;

import com.forcex.FX;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public abstract class FileSystem {
    public static enum ReaderType {
        MEMORY,
        STREAM
    };

    public static String homeDirectory;

    private ArrayList<FXPackage> packages = new ArrayList<>();

    public void loadPackageFile(String pkg_name) {
        try {
            BinaryStreamReader is = open(pkg_name, ReaderType.STREAM);
            if(is != null) {
                packages.add(FXPackage.load(is, pkg_name));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BinaryStreamReader open(String path, ReaderType type) {
        try {
            String pkg_path = null;
            FXPackage.FXEntry entry = null;
            if(packages.size() > 0) {
                for(FXPackage pkg : packages) {
                    if(pkg.entries.containsKey(path)) {
                        entry = pkg.entries.get(path);
                        pkg_path = pkg.file_name;
                        break;
                    }
                }
            }
            InputStream is = tryOpenFile(pkg_path != null ? pkg_path : path);
            if(is == null) {
                return null;
            }
            if(entry != null) {
                is.skip(entry.offset);
            }
            if(type == ReaderType.STREAM) {
                return new BinaryStreamReader(is);
            }
            byte[] buffer = new byte[entry != null ? entry.size : is.available()];
            is.read(buffer);
            is.close();
            return new BinaryStreamReader(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private InputStream tryOpenFile(String file_name) {
        InputStream is = null;
        try {
            if(FX.device.isJDKDesktop()) {
                is = FileSystem.class.getClassLoader().getResourceAsStream(file_name);
                if(is == null) {
                    File file = new File(homeDirectory + file_name);
                    if(file.exists()) {
                        is = new FileInputStream(file);
                    } else {
                        file = new File(file_name);
                        if(file.exists()) {
                            is = new FileInputStream(file);
                        }
                    }
                }
            } else {
                is = getAndroidAsset(file_name);
                if(is == null) {
                    File file = new File(homeDirectory + file_name);
                    if(file.exists()) {
                        is = new FileInputStream(homeDirectory + file);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return is;
    }

    protected abstract InputStream getAndroidAsset(String name);
}
