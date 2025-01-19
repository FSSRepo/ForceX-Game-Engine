package com.fss.ai;

import com.forcex.windows.FXAppConfig;
import com.forcex.windows.ForceXApp;

public class DesktopLauncher extends ForceXApp
{
    public static void main(String[] args) {
        DesktopLauncher launcher = new DesktopLauncher();
        FXAppConfig config = new FXAppConfig();
        config.title = "Super AI";
        config.fullscreen = true;
        config.width = 1920;
        config.height = 1080;
        launcher.initialize(new AIScreen(), config);
    }
}
