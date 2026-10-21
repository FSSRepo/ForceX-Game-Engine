package com.fx.demo;

import com.forcex.FX;
import com.forcex.app.Game;
import com.forcex.app.InputListener;
import com.forcex.app.Key;
import com.forcex.core.GL;
import com.forcex.gui.UIContext;
import com.forcex.gui.UiInflater;
import com.forcex.gui.View;
import com.forcex.gui.widgets.EditText;
import com.forcex.gui.widgets.ProgressBar;
import com.forcex.gui.widgets.TextView;

/**
 * Ejemplo mínimo: toda la interfaz se declara en assets/layouts/main.yaml.
 */
public class YamlUiGame extends Game implements InputListener {
    private UIContext context;
    private TextView tvStatus;
    private EditText etName;
    private ProgressBar progress;
    private int clicks = 0;

    @Override
    public void create() {
        context = new UIContext();
        UiInflater inflater = new UiInflater(context);

        inflater.setOnClickListener("onPlay", view -> {
            clicks++;
            if (tvStatus != null) {
                tvStatus.setText("Boton pulsado " + clicks + (clicks == 1 ? " vez" : " veces"));
            }
            if (progress != null) {
                progress.setProgress(Math.min(clicks * 10, 100));
            }
        });
        inflater.setOnClickListener("onGreet", view -> {
            if (tvStatus == null) {
                return;
            }
            String name = etName == null ? "" : etName.getText().trim();
            tvStatus.setText(name.isEmpty() ? "Escribe tu nombre primero" : "Hola, " + name);
        });

        View root = inflater.inflateFile("layouts/main.yaml");
        tvStatus = (TextView) inflater.findViewById("status");
        etName = (EditText) inflater.findViewById("name");
        progress = (ProgressBar) inflater.findViewById("progress");
        context.setContentView(root);

        FX.device.addInputListener(this);
    }

    @Override
    public void render(float deltaTime) {
        FX.gl.glClearColor(0.08f, 0.09f, 0.12f, 1f);
        FX.gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        context.draw();
    }

    @Override
    public void onTouch(float x, float y, byte type, int pointer) {
        if (context.testTouch(x, y)) {
            context.onTouch(x, y, type);
        }
    }

    @Override
    public void onKeyEvent(byte key, boolean down) {
        if (down && key == Key.KEY_ESC) {
            FX.device.destroy();
            return;
        }
        context.onKeyEvent(key, down);
    }

    @Override
    public void destroy() {
        context.destroy();
    }
}
