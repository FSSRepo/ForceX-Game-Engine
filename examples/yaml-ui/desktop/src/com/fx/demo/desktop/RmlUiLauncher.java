package com.fx.demo.desktop;

import com.forcex.desktop.FXAppConfig;
import com.forcex.desktop.ForceXApp;
import com.fx.demo.RmlUiGame;

public final class RmlUiLauncher extends ForceXApp {
    public static void main(String[] args) {
        RmlUiLauncher launcher = new RmlUiLauncher();
        FXAppConfig config = new FXAppConfig();
        config.title = "ForceX - RmlUi Showcase";
        config.width = 1280;
        config.height = 720;
        config.resources_dir = "assets/";
        launcher.initialize(new RmlUiGame(), config);
    }
}
