package com.forcex.gfx3d.effect;

import com.forcex.math.Vector3f;
import com.forcex.utils.*;

public class BillboardObject {
    public Vector3f position;
    public float scale = 4;
	public Color color;
    int texture;

    public BillboardObject(int tex) {
        this.texture = tex;
		color = new Color(Color.WHITE);
    }
}
