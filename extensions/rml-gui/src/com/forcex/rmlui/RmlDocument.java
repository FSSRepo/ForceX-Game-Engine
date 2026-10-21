package com.forcex.rmlui;

/**
 * Documento RML cargado en un {@link RmlContext}.
 * <p>
 * El documento se muestra automáticamente al cargarse. Después de
 * {@link #close()} o de {@link RmlUI#shutdown()} el objeto deja de ser válido.
 */
public final class RmlDocument {

    /**
     * Listener de eventos RML (click, change, submit, ...).
     */
    public interface Listener {
        /**
         * @param event evento recibido.
         * @return true para detener la propagación del evento.
         */
        boolean onEvent(RmlEvent event);
    }

    private final RmlContext context;
    private final long handle;
    private boolean closed;

    RmlDocument(RmlContext context, long handle) {
        this.context = context;
        this.handle = handle;
    }

    /** Contexto al que pertenece el documento. */
    public RmlContext getContext() {
        return context;
    }

    /** Identificador del documento (atributo {@code id} del cuerpo). */
    public String getId() {
        checkOpen();
        return nativeGetId(handle);
    }

    /** Ruta o URL de origen del documento. */
    public String getSourceUrl() {
        checkOpen();
        return nativeGetSourceUrl(handle);
    }

    /** Indica si el documento es visible (incluyendo la visibilidad de sus padres). */
    public boolean isVisible() {
        checkOpen();
        return nativeIsVisible(handle);
    }

    /** Muestra el documento. */
    public void show() {
        checkOpen();
        nativeShow(handle);
    }

    /** Oculta el documento sin descargarlo. */
    public void hide() {
        checkOpen();
        nativeHide(handle);
    }

    /**
     * Descarga el documento del contexto y libera sus listeners. La destrucción
     * efectiva se completa en la siguiente llamada a {@link RmlContext#update()}.
     */
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        nativeClose(handle);
    }

    /**
     * Registra un listener para un tipo de evento RML ({@code click},
     * {@code change}, {@code submit}, ...).
     */
    public void addEventListener(String type, Listener listener) {
        checkOpen();
        if (type == null || listener == null) {
            throw new IllegalArgumentException("type y listener no pueden ser null");
        }
        nativeAddEventListener(handle, type, listener);
    }

    /** Desvincula un listener previamente registrado con {@link #addEventListener}. */
    public void removeEventListener(String type, Listener listener) {
        checkOpen();
        if (type == null || listener == null) {
            throw new IllegalArgumentException("type y listener no pueden ser null");
        }
        nativeRemoveEventListener(handle, type, listener);
    }

    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException("El documento ya fue cerrado");
        }
    }

    private static native String nativeGetId(long handle);

    private static native String nativeGetSourceUrl(long handle);

    private static native boolean nativeIsVisible(long handle);

    private static native void nativeShow(long handle);

    private static native void nativeHide(long handle);

    private static native void nativeClose(long handle);

    private static native boolean nativeAddEventListener(long handle, String type, Listener listener);

    private static native boolean nativeRemoveEventListener(long handle, String type, Listener listener);
}
