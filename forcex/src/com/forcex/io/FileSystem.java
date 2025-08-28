package com.forcex.io;

import com.forcex.FX;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

    private InputStream tryOpenFile(String filePath) {
        try {
            if (FX.device.isJDKDesktop()) {
                InputStream is = tryLoadFromResources(filePath);
                if (is != null) return is;

                return tryLoadFromFileSystem(filePath);
            } else {
                InputStream is = getAndroidAsset(filePath);
                if (is != null) return is;

                return tryLoadFromFileSystem(filePath);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private InputStream tryLoadFromResources(String path) {
        return FileSystem.class.getClassLoader().getResourceAsStream(path);
    }

    private InputStream tryLoadFromFileSystem(String path) throws FileNotFoundException {
        File file = new File(path);
        if (file.exists()) return new FileInputStream(file);

        file = new File(homeDirectory + path);
        if (file.exists()) return new FileInputStream(file);

        return null;
    }

    protected abstract InputStream getAndroidAsset(String name);
}
