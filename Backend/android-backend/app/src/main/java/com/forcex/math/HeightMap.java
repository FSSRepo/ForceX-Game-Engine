package com.forcex.math;
import com.forcex.utils.*;
import com.forcex.*;

public class HeightMap {
	private Image image;
	public float[] vertices;
	public float[] texcoords;
	public float[] normals;
	public short[] indices;
	
	public HeightMap(String path,float height,float extent,boolean uvs,boolean nor){
		image = new Image(path);
		if(image.width != image.height){
			FX.device.showInfo("Error the heightmap '"+path+"' isn't cuadrade",true);
			FX.device.stopRender();
		}
		if(image.width > 128){
			FX.device.showInfo("Error the heightmap '"+path+"' must be 128 maximum width image",true);
			FX.device.stopRender();
		}
		short vertexCount = (short)(image.height * image.height);
		short offset = 0,x,y;
		for(x = 0;x < image.width;x++){
			for(y = 0;y < image.width;y++){
				vertices[offset] = -(2f * (x / ((float)image.width - 1)) - 1f) * extent;
				vertices[offset+1] = getHeight(image,x,y) * height;
				vertices[offset+2] =  (2f * (y / ((float)image.width - 1)) - 1f) * extent;
				offset += 3;

			} 
		}
		if(uvs){
			offset = 0;
			texcoords = new float[vertexCount*2];
			for(x = 0;x < image.width; x++){
				for(y = 0;y < image.width; y++){
					texcoords[offset] = (x / (float)image.width - 1f);
					texcoords[offset+1] =  (y / (float)image.width - 1f);
					offset += 2;
				}
			}
		}
		if(nor){
			offset = 0;
			normals = new float[vertexCount*3];
			Vector3f normal = new Vector3f();
			for(x = 0;x < image.width;x++){
				for(y = 0;y < image.width;y++){
					float heightL = getHeight(image,x - 1,y);
					float heightR = getHeight(image,x + 1,y);
					float heightD = getHeight(image,x,y - 1);
					float heightU = getHeight(image,x,y + 1);
					normal.set(heightL - heightR, 2f, heightD - heightU);
					normal.normalize();
					normals[offset] = normal.x;
					normals[offset+1] = normal.y;
					normals[offset+2] = normal.z;
					offset += 3;
				} 
			}
			normal = null;
		}
		indices = new short[6 * ((image.width - 1) * image.width)];
		int pointer = 0;
		for (short gz = 0; gz < image.width - 1; gz++) {
			for (short gx = 0; gx < image.width - 1; gx++) {
				int topLeft = (gz * image.width) + gx;
				int topRight = topLeft + 1;
				int bottomLeft = ((gz + 1) * image.width) + gx;
				int bottomRight = bottomLeft + 1;
				indices[pointer++] = (short)topLeft;
				indices[pointer++] = (short)bottomLeft;
				indices[pointer++] = (short)topRight;
				indices[pointer++] = (short)topRight;
				indices[pointer++] = (short)bottomLeft;
				indices[pointer++] = (short)bottomRight;
			}
		}
		image.clear();
		image = null;
	}
	
	private final float getHeight(Image img,int x,int y){
		if(x < 0 || x >= img.height || y < 0 || y >= img.height){
			return 0;
		}
		return 
			(img.getRGBA(x,y)[0] & 0xff) * 0.003921f;
	}
}
