package com.forcex.gfx3d.effect;
import com.forcex.math.*;
import com.forcex.gfx3d.*;
import com.forcex.utils.*;

public class Sun extends BillboardObject
{
	float dist = 80;
	public Sun(int tex) {
		super(tex);
		scale = 15;
		position = new Vector3f();
		color.set(0xfc,0xff,0x17);
	}
	
	public void update(Vector3f lightPos,Camera cam) {
		position.set(GameUtils.getLocalPosition(cam.position,lightPos,dist));
	}
}
