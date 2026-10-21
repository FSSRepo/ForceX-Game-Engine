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

    /**
     * Solicita texto usando el input nativo de la plataforma (en Android abre un diálogo).
     * En escritorio no se usa: el {@code EditText} recibe las teclas físicas directamente.
     */
    void requestTextInput(String current_text, String hint, boolean numbers_only, boolean password, OnTextInputListener listener);

    interface OnAndroidFileStream {
        void open(InputStream is, String name);

        void save(OutputStream os);
    }

    interface OnTextInputListener {
        /**
         * @param text texto aceptado, o {@code null} si el usuario canceló.
         */
        void onTextInput(String text);
    }
}
