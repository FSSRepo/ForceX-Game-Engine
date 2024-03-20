package com.forcex.gfx3d;
import com.forcex.gfx3d.effect.*;
import com.forcex.utils.*;
import com.forcex.math.*;

public class Environment
{
	Light light;
	Vector2f fogParams;
	Color fogColor;
	
	public void setUseFog(float start,float end){
		fogParams = new Vector2f(end,end - start);
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
