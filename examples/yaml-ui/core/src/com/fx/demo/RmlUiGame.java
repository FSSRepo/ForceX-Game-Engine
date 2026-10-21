package com.fx.demo;

import com.forcex.FX;
import com.forcex.app.EventType;
import com.forcex.app.Game;
import com.forcex.app.InputListener;
import com.forcex.app.Key;
import com.forcex.core.GL;
import com.forcex.io.BinaryStreamReader;
import com.forcex.io.FileSystem;
import com.forcex.rmlui.RmlContext;
import com.forcex.rmlui.RmlDocument;
import com.forcex.rmlui.RmlUI;
import com.forcex.rmlui.RmlUIInput;

/** Demo RmlUi con HUD, SVG inline, Lottie y eventos Java. */
public final class RmlUiGame extends Game implements InputListener {

    private RmlContext context;
    private RmlDocument document;
    private RmlUIInput rmlInput;
    private static final float REFERENCE_HEIGHT = 720f;

    private int clickCount;
    @Override
    public void create() {
        if (!RmlUI.init(FX.gpu.getWidth(), FX.gpu.getHeight())) {
            throw new IllegalStateException("No se pudo inicializar RmlUi");
        }

        RmlUI.loadFont("fonts/LatoLatin-Regular.ttf");
        context = RmlUI.getContext();
        if (context == null) {
            throw new IllegalStateException("RmlUi no creó el contexto principal");
        }
        applyScale(FX.gpu.getHeight());

        BinaryStreamReader rmlFile = FX.fs.open("rml/main.rml", FileSystem.ReaderType.MEMORY);
        if (rmlFile == null) {
            throw new IllegalStateException("No se encontró assets/rml/main.rml");
        }
        String rmlSource = new String(rmlFile.getData(), java.nio.charset.StandardCharsets.UTF_8);
        if (rmlSource.startsWith("\uFEFF")) {
            rmlSource = rmlSource.substring(1);
        }
        document = context.loadDocument(rmlSource);
        if (document == null) {
            throw new IllegalStateException("No se pudo cargar assets/rml/main.rml");
        }

        document.addEventListener("click", event -> {
            if ("play".equals(event.getTargetId())) {
                clickCount++;
                context.setElementClass("glass-modal", "is-open", true);
                context.setInnerRml("status", "Modal glass abierto — backdrop-filter activo.");
            }
            if ("modal-close".equals(event.getTargetId()) || "modal-confirm".equals(event.getTargetId())
                    || "glass-scrim".equals(event.getTargetId())) {
                context.setElementClass("glass-modal", "is-open", false);
                context.setInnerRml("status", "Modal glass cerrado. Clics: " + clickCount);
            }
            if ("quit".equals(event.getTargetId())) {
                FX.device.destroy();
            }
            return false;
        });

        document.show();
        rmlInput = new RmlUIInput(context);
        FX.device.addInputListener(this);
    }

    /** La UI se diseñó para 720 px de alto; las pantallas mayores escalan las medidas en dp. */
    private void applyScale(int height) {
        if (context != null && height > 0) {
            context.setDensityIndependentPixelRatio(Math.max(1f, height / REFERENCE_HEIGHT));
        }
    }

    @Override
    public void render(float deltaTime) {
        FX.gl.glClearColor(0.035f, 0.055f, 0.09f, 1f);
        FX.gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT);

        if (context != null) {
            context.update();
            context.render();
        }
    }

    @Override
    public void resize(int width, int height) {
        RmlUI.resize(width, height);
        applyScale(height);
    }

    public void onTouch(float x, float y, byte type, int pointer) {
        if (rmlInput == null) {
            return;
        }
        if (type == EventType.MOUSE_HOVER || type == EventType.MOUSE_SCROLL ||
                type == EventType.TOUCH_PRESSED || type == EventType.TOUCH_DROPPED ||
                type == EventType.TOUCH_DRAGGING || type == EventType.TOUCH_PRESSED_2 ||
                type == EventType.TOUCH_DROPPED_2 || type == EventType.TOUCH_DRAGGING_2) {
            rmlInput.onTouch(x, y, type, pointer);
        }
    }

    public void onKeyEvent(byte key, boolean down) {
        if (key == Key.KEY_ESC && down) {
            FX.device.destroy();
            return;
        }
        if (rmlInput != null) {
            rmlInput.onKeyEvent(key, down);
        }
    }

    @Override
    public void destroy() {
        if (document != null) {
            document.close();
            document = null;
        }
        if (context != null) {
            context.update();
        }
        RmlUI.shutdown();
        context = null;
        rmlInput = null;
    }
}
