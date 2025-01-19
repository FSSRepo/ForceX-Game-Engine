package com.forcex.core;

import com.forcex.app.InputListener;

import java.io.InputStream;
import java.io.OutputStream;

public interface SystemDevice {
    void addInputListener(InputListener input);

    void stopRender();

    void showInfo(String info, boolean isError);

    void destroy();

    void setCursorState(boolean show);

    boolean isJDKDesktop();

    int getAndroidVersion();

    void invokeFileChooser(boolean open, String label, String def_name, OnAndroidFileStream listener);

    interface OnAndroidFileStream {
        void open(InputStream is, String name);

        void save(OutputStream os);
    }
}
