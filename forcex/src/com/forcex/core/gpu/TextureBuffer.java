package com.forcex.core.gpu;

public class TextureBuffer{
	public short width;
	public short height;
	public int type;
	public byte[] data;
	
	public void clear(){
		data = null;
	}
}
