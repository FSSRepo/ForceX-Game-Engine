package com.steward.prototype;

import com.forcex.windows.ForceXApp;

public class DesktopLauncher extends ForceXApp
{
    public static void main(String[] args) {
        DesktopLauncher launcher = new DesktopLauncher();
        launcher.initialize(new PrototypeUNI(),"Prototype", false, "data/icon_%d.png");
    }
}
