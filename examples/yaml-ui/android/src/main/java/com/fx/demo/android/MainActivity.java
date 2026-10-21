package com.fx.demo.android;

import android.os.Bundle;

import com.forcex.android.ForceXApp;
import com.fx.demo.RmlUiGame;

/** Demo de RmlUi (HTML/CSS) corriendo sobre el backend Android del engine. */
public class MainActivity extends ForceXApp {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(new RmlUiGame(), true);
    }
}
