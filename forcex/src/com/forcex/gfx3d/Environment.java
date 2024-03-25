package com.forcex.gfx3d;

import com.forcex.gfx3d.effect.Light;
import com.forcex.math.Vector2f;
import com.forcex.utils.Color;

public class Environment {
    protected Light light;
    protected Vector2f fogParams;
    protected Color fogColor;

    public void setUseFog(float start, float end) {
        fogParams = new Vector2f(end, end - start);
        fogColor = new Color();
    }

    public void setLight(Light light) {
        this.light = light;
    }

    public void setFogColor(int r, int g, int b) {
        if (fogColor != null) {
            fogColor.set(r, g, b);
        }
    }
}
