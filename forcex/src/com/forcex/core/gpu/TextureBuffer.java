package com.forcex.core.gpu;

public class TextureBuffer{
	public int width;
	public int height;
	public int type;
	public byte[] data;
	
	public void clear(){
		data = null;
	}
}
