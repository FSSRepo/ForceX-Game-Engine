package com.forcex.gtasdk;
import com.forcex.utils.*;
import com.forcex.io.*;
import com.forcex.*;

public class DFFMaterial {
	public String texture = "";
	public Color color = new Color();
	public boolean hasReflectionMat = false;
	public boolean hasRenderToRight = false;
	public boolean hasSpecularMat = false;
	public int RTRval1 = 0;
	public int RTRval2 = 0;
	public boolean hasMaterialEffect = false;
	public byte[] dataMatFx = null;
	public float[] reflectionAmount = null;
	public float reflectionIntensity = 0;
	
	public float specular_level = 0.0f;
	public String specular_name = "";
	
	public float[] surfaceProp = {
			1.0f,1.0f,1.0f
	};
	
	public boolean hasTexture(){
		return texture.length() > 0;
	}
}
