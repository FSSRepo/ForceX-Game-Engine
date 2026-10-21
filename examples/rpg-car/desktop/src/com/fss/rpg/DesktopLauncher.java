package com.fss.rpg;

import com.forcex.desktop.FXAppConfig;
import com.forcex.desktop.ForceXApp;

public class DesktopLauncher extends ForceXApp
{
    public static void main(String[] args) {
        DesktopLauncher launcher = new DesktopLauncher();
        FXAppConfig config = new FXAppConfig();
        config.title = "RPG Game";
        config.fullscreen = false;
        config.width = 1280;
        config.height = 720;
        launcher.initialize(new RPGCarGame(), config);
    }
}
