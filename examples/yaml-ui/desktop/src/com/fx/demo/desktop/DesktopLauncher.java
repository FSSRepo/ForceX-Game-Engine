package com.fx.demo.desktop;

import com.forcex.desktop.FXAppConfig;
import com.forcex.desktop.ForceXApp;
import com.fx.demo.YamlUiGame;

public class DesktopLauncher extends ForceXApp {
    public static void main(String[] args) {
        DesktopLauncher launcher = new DesktopLauncher();
        FXAppConfig config = new FXAppConfig();
        config.title = "ForceX - YAML UI Demo";
        config.width = 900;
        config.height = 600;
        config.resources_dir = "assets/";
        launcher.initialize(new YamlUiGame(), config);
    }
}
