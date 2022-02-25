package com.forcex.postprocessor;

public abstract class Pass
{
	public boolean renderfbo = false;
	public abstract void process(int colorTexture);
	public int getTexture(){return 0;}
	public abstract void delete();
}
