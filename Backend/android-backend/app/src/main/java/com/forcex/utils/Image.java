package com.forcex.utils;
import com.forcex.core.*;
import java.nio.*;

public class Image
{
	private byte[] data;
	public int width;
	public int height;
	
	public Image(String path){
		int[] info = new int[2];
		data = CoreJni.pngdecode(info,path);
		width = info[0];
		height = info[1];
	}
	
	public Image(byte[] rgba,int width,int height){
		this.data = rgba;
		this.width = width;
		this.height = height;
	}
	
	public void save(String path){
		CoreJni.pngencode(data,path,width,height);
	}
	
	public byte[] getRGBAImage(){
		return data;
	}
	
	public byte[] getRGBA(int x,int y){
		int pixel = (x + (width * y)) * 4;
		return new byte[]{
			data[pixel],data[pixel+1],data[pixel+2],data[pixel+3]
		};
	}
	public void setRGBA(int x,int y,int r,int g,int b,int a){
		int pixel = (x + (width * y)) * 4;
		data[pixel] = (byte)r;
		data[pixel+1] = (byte)g;
		data[pixel+2] = (byte)b;
		data[pixel+3] = (byte)a;
	}
	
	public ByteBuffer getBuffer(){
		ByteBuffer bb = ByteBuffer.allocateDirect(width*height*4).order(ByteOrder.nativeOrder());
		bb.put(data);
		bb.flip();
		return bb;
	}
	
	public void clear(){
		data = null;
	}
}
