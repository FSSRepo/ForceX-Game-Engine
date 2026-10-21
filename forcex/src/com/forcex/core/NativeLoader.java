package com.forcex.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Loads the engine native libraries embedded in the jar (natives/&lt;os&gt;/&lt;arch&gt;/...),
 * following the same approach as LWJGL: the binaries are extracted to a temporary
 * folder and loaded by absolute path with System.load(), so the launcher does not
 * need to add anything to java.library.path.
 *
 * Libraries that are not embedded (for example third-party binaries shipped as
 * loose files next to the application) fall back to System.loadLibrary(), which
 * resolves them through java.library.path / the working directory.
 */
public final class NativeLoader {

    /** Folder (inside the temp dir) where the embedded natives are extracted. */
    public static final String DIR_NAME = "forcex-natives";

    private static final Set<String> LOADED = new HashSet<>();

    private NativeLoader() {
    }

    public static synchronized void init(String... libraries) {
        if (isAndroid()) {
            // on Android the .so files come from the apk jniLibs and are resolved by name
            for (String library : libraries) {
                if (LOADED.add(library)) {
                    try {
                        System.loadLibrary(library);
                    } catch (Throwable t) {
                        LOADED.remove(library);
                        t.printStackTrace();
                    }
                }
            }
            return;
        }
        try {
            File dir = new File(System.getProperty("java.io.tmpdir"), DIR_NAME);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("Could not create the natives folder: " + dir);
            }
            for (String library : libraries) {
                File out = new File(dir, System.mapLibraryName(library));
                if (LOADED.contains(out.getAbsolutePath())) {
                    continue;
                }
                if (extract(library, out)) {
                    System.load(out.getAbsolutePath());
                    LOADED.add(out.getAbsolutePath());
                } else {
                    // Not embedded (e.g. development run): resolve by name.
                    System.loadLibrary(library);
                    LOADED.add(out.getAbsolutePath());
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static boolean extract(String library, File out) throws IOException {
        String fileName = System.mapLibraryName(library);
        String os = osName();
        InputStream in = null;
        for (String arch : archCandidates()) {
            in = NativeLoader.class.getClassLoader().getResourceAsStream("natives/" + os + "/" + arch + "/" + fileName);
            if (in != null) {
                break;
            }
        }
        if (in == null) {
            return false; // not embedded
        }
        try (InputStream is = in; OutputStream osStream = new FileOutputStream(out)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                osStream.write(buffer, 0, read);
            }
        }
        return true;
    }

    private static boolean isAndroid() {
        return System.getProperty("java.vm.name", "").toLowerCase().contains("dalvik");
    }

    private static String osName() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return "windows";
        if (os.contains("mac") || os.contains("darwin")) return "macos";
        return "linux";
    }

    private static String[] archCandidates() {
        String arch = System.getProperty("os.arch", "").toLowerCase();
        if (arch.equals("amd64") || arch.equals("x86_64") || arch.equals("x64")) {
            return new String[]{"amd64", "x86_64", "x64"};
        }
        if (arch.equals("aarch64") || arch.equals("arm64")) {
            return new String[]{"aarch64", "arm64"};
        }
        if (arch.contains("86")) {
            return new String[]{"x86", "i386", "i686"};
        }
        return new String[]{arch};
    }
}
