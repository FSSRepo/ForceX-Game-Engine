package com.forcex.rte.utils;
import com.forcex.utils.*;

public class RTPixel {
	public RTColor color;
	public float emission;
	public float depth;
	
	public RTPixel(){
		color = new RTColor(0,0,0);
		emission = 0;
		depth = 0;
	}
}
