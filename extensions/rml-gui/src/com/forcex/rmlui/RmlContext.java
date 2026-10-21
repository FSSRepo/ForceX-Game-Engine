package com.forcex.rmlui;

/**
 * Contexto de RmlUi: agrupa los documentos de una superficie/ventana.
 * <p>
 * Normalmente se obtiene con {@link RmlUI#getContext()} y no se instancia
 * directamente.
 */
public final class RmlContext {

    private final long handle;

    RmlContext(long handle) {
        this.handle = handle;
    }

    /** Nombre del contexto (el principal se llama {@code main}). */
    public String getName() {
        return nativeGetName(handle);
    }

    /** Cambia el tamaño lógico del contexto; conviene acompañarlo de {@link RmlUI#resize}. */
    public void setDimensions(int width, int height) {
        nativeSetDimensions(handle, width, height);
    }

    /**
     * Factor de escala de la unidad {@code dp} en RCSS (1.0 = 1 dp por píxel). Úsalo para
     * adaptar la UI a pantallas de alta resolución; las medidas en {@code px} no escalan.
     */
    public void setDensityIndependentPixelRatio(float ratio) {
        nativeSetDensityIndependentPixelRatio(handle, ratio);
    }

    /**
     * Carga un documento desde una cadena RML.
     *
     * @return el documento cargado, o null si hubo errores.
     */
    public RmlDocument loadDocument(String rml) {
        long document = nativeLoadDocument(handle, rml);
        return document == 0L ? null : new RmlDocument(this, document);
    }

    /**
     * Carga un documento desde un archivo resuelto por {@code FX.fs}. Las rutas
     * relativas de sus recursos se resuelven respecto al directorio del archivo.
     *
     * @return el documento cargado, o null si hubo errores.
     */
    public RmlDocument loadDocumentFromFile(String path) {
        long document = nativeLoadDocumentFromFile(handle, path);
        return document == 0L ? null : new RmlDocument(this, document);
    }

    /** Obtiene el elemento por id y reemplaza su contenido con markup RML. */
    public void setInnerRml(String elementId, String rml) {
        nativeSetInnerRml(handle, elementId, rml);
    }

    /**
     * Activa o desactiva una clase CSS en el elemento indicado. Útil para
     * mostrar/ocultar paneles ({@code is-open}) sin tocar el layout inline.
     */
    public void setElementClass(String elementId, String className, boolean enable) {
        nativeSetElementClass(handle, elementId, className, enable);
    }

    /** Envía movimiento de ratón/touch en coordenadas lógicas de pantalla. */
    public void processMouseMove(int x, int y, int modifiers) {
        nativeProcessMouseMove(handle, x, y, modifiers);
    }

    /** Envía un botón RmlUi: 0 izquierdo, 1 derecho, 2 central. */
    public void processMouseButton(int button, boolean down, int modifiers) {
        nativeProcessMouseButton(handle, button, down, modifiers);
    }

    /** Envía el delta de rueda horizontal/vertical. */
    public void processMouseWheel(float x, float y, int modifiers) {
        nativeProcessMouseWheel(handle, x, y, modifiers);
    }

    /** Envía una tecla usando el identificador RmlUi y sus modificadores. */
    public void processKey(int key, boolean down, int modifiers) {
        nativeProcessKey(handle, key, down, modifiers);
    }

    /** Envía un punto de código Unicode al control de texto enfocado. */
    public void processText(int codePoint) {
        nativeProcessText(handle, codePoint);
    }

    /** Actualiza el contexto (entrada, animaciones, layout). */
    public void update() {
        nativeUpdate(handle);
    }

    /** Dibuja el contexto en el framebuffer actual. */
    public void render() {
        nativeRender(handle);
    }

    long getHandle() {
        return handle;
    }

    private static native String nativeGetName(long handle);

    private static native void nativeSetDimensions(long handle, int width, int height);

    private static native void nativeSetDensityIndependentPixelRatio(long handle, float ratio);

    private static native long nativeLoadDocument(long handle, String rml);

    private static native long nativeLoadDocumentFromFile(long handle, String path);

    private static native void nativeSetInnerRml(long handle, String elementId, String rml);

    private static native void nativeSetElementClass(long handle, String elementId, String className, boolean enable);

    private static native void nativeProcessMouseMove(long handle, int x, int y, int modifiers);

    private static native void nativeProcessMouseButton(long handle, int button, boolean down, int modifiers);

    private static native void nativeProcessMouseWheel(long handle, float x, float y, int modifiers);

    private static native void nativeProcessKey(long handle, int key, boolean down, int modifiers);

    private static native void nativeProcessText(long handle, int codePoint);

    private static native void nativeUpdate(long handle);

    private static native void nativeRender(long handle);
}
