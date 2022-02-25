package com.forcex.gfx3d;
import com.forcex.gfx3d.effect.*;
import com.forcex.utils.*;

public class Environment
{
	Light light;
	float fogStart;
	float fogEnd;
	Color fogColor;
	
	public void setUseFog(float start,float end){
		fogStart = start;
		fogEnd = end;
		fogColor = new Color();
	}
	
	public void setLight(Light light){
		this.light = light;
	}
	
	public void setFogColor(int r,int g,int b){
		if(fogColor != null){
			fogColor.set(r,g,b);
		}
	}
}
