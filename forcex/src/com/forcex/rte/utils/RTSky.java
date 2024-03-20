package com.forcex.rte.utils;
import com.forcex.utils.*;
import com.forcex.math.*;

public class RTSky
{
	Image img;
	
	public RTSky(String path) {
		img = new Image(path);
	}
	
	public RTColor getColor(Vector3f direction) {
		float u = (float) (0.5 + Math.atan2(direction.y, direction.x) / (2 * Math.PI));
        float v = (float) (0.5 - Math.asin(direction.z) / Math.PI);
		return new RTColor( img.getRGBA((int)(u * (img.width - 1)), (int)(v * (img.height - 1))));
	}
}
