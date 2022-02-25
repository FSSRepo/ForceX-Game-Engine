package com.forcex.gfx3d;
import com.forcex.utils.*;
import com.forcex.*;
import com.forcex.core.gpu.*;
import com.forcex.math.*;

public class PathTracing
{
	int width;
	int height;
	Color color_buffer;
	Color pixel;
	byte[] buffer;
	boolean transparent;
	
	public PathTracing(){
		color_buffer = new Color(120,120,120);
	}
	
	public void setTransparent(boolean z){
		transparent = z;
	}
	
	public void init(int w,int h){
		width = w;
		height = h;
		buffer = new byte[w*h*4];
		pixel = new Color();
	}
	
	public void process(Camera cam,ModelObject obj) {
		VertexData vd = obj.getMesh().getVertexData();
		float[] tv = MathGeom.transformVertices(vd.vertices,obj.getTransform());
		Vector4f test = new Vector4f();
		for(int x = 0;x < width;x++){
			for(int y = 0;y < height;y++){
				//Vector4f v = cam.getProjViewMatrix().mult(test.set(tv[]));
				if(!transparent){
					pixel.set(color_buffer);
				}
				rewindPixel((y*width + x) * 4);
			}
		}
		Image img = new Image(buffer,width,height);
		img.save(FX.homeDirectory+"temp.png");
	}
	
	private void rewindPixel(int offset){
		buffer[offset] = (byte)pixel.r;
		buffer[offset+1] = (byte)pixel.g;
		buffer[offset+2] = (byte)pixel.b;
		buffer[offset+3] = (byte)pixel.a;
		pixel.set(0,0,0,0);
	}
}
