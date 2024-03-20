package com.forcex.rte;
import com.forcex.rte.utils.*;
import com.forcex.utils.*;

public class RTFrameBuffer {
	RTPixel[] buffer;
	int width,height;
	byte[] data;
	
	public RTFrameBuffer(int width,int height) {
		buffer = new RTPixel[width * height];
		this.width = width;
		this.height = height;
		data = new byte[buffer.length * 4];
	}
	
	protected void fill(RTPixel pixel,int x,int y) {
		buffer[y * width + x] = pixel;
	}
	
	protected void fill(RTPixel[] pixels,int x,int y,int size) {
		for(int dx = 0;dx < size; dx++) {
			for(int dy = 0;dy < size; dy++) {
				if(((x + dx) >= width || (y + dy) >= height)){
					break;
				}
				buffer[(y + dy) * width + (x + dx)] = pixels[dy * size + dx];
			}
		}
	}
	
	public byte[] getTexture() {
		for(int i = 0;i < buffer.length;i ++){
			if(buffer[i] == null){
				data[i * 4] = (byte)(0);
				data[i * 4 + 1] = (byte)(0);
				data[i * 4 + 2] = (byte)(0);
				data[i * 4 + 3] = (byte)(0);
			}else{
				data[i * 4] = (byte)(buffer[i].color.r * 255f);
				data[i * 4 + 1] = (byte)(buffer[i].color.g * 255f);
				data[i * 4 + 2] = (byte)(buffer[i].color.b * 255f);
				data[i * 4 + 3] = (byte)(buffer[i].color.a * 255f);
			}
			
		}
		return data;
	}
	
	public void save(String output,String name,boolean emission) {
		byte[] data = getTexture();
		Image img = new Image(data,width,height);
		img.save(output+"/"+name+".png");
		img.clear();
		img = null;
		if(emission){
			for(int i = 0;i < buffer.length;i ++){
				float factor = buffer[i].emission > 1f ? 1f : buffer[i].emission;
				data[i * 4] = (byte)(factor * 255f);
				data[i * 4 + 1] = (byte)(factor * 255f);
				data[i * 4 + 2] = (byte)(factor * 255f);
				data[i * 4 + 3] = (byte)(buffer[i].color.a * 255f);
			}
			img = new Image(data,width,height);
			img.save(output+"/"+name+".emission.png");
			img.clear();
		}
	}
}
