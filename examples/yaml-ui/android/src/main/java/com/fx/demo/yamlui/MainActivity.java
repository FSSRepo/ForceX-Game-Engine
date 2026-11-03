package com.fx.demo.yamlui;

import android.os.Bundle;

import com.forcex.android.ForceXApp;
import com.fx.demo.YamlUiGame;

/** Demo de interfaz declarativa YAML corriendo sobre el backend Android del engine. */
public class MainActivity extends ForceXApp {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(new YamlUiGame(), true);
    }
}
