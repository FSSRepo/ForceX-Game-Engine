package com.forcex.gtasdk;
import java.util.*;

public class DFFGeometry
{
	public ArrayList<DFFIndices> splits = new ArrayList<DFFIndices>();
	public ArrayList<DFFMaterial> materials = new ArrayList<>();
	public float[] vertices;
	public float[] texcoords;
	public float[] normals;
	public byte[] colors;
	public byte[] nightColors;
	public int vertexCount;
	public int frameIdx = -1;
	public String name = "";
	public int flags;
	public int uvsets;
	public float diffuse,specular,ambient;
	public boolean isTriangleStrip,hasMeshExtension;
	public DFFSkin skin;
	public static final int GEOMETRY_FLAG_TRISTRIP = 			0x01;
	public static final int GEOMETRY_FLAG_POSITIONS = 			0x02;
	public static final int GEOMETRY_FLAG_TEXCOORDS = 			0x04;
	public static final int GEOMETRY_FLAG_COLORS  = 			0x08;
	public static final int GEOMETRY_FLAG_NORMALS = 			0x10;
	public static final int GEOMETRY_FLAG_DYNAMICLIGHTING = 	0x20;
	public static final int GEOMETRY_FLAG_MODULATEMATCOLOR = 	0x40;
	public static final int GEOMETRY_FLAG_MULTIPLEUVSETS = 		0x80;
	
	public boolean hasNormals(){
		return normals != null;
	}
	
	public int getNumTrangles(){
		if(isTriangleStrip){
			return 0;
		}
		int total = 0;
		for(DFFIndices idx : splits){
			total += (idx.index.length / 3);
		}
		return total;
	}
	
	public byte[] getRGB(int type,int idx){
		switch(type){
			case 0:
				return new byte[]{
					colors[idx+0],colors[idx+1],colors[idx+2],colors[idx+3]
				};
			case 1:
				return new byte[]{
					nightColors[idx+0],nightColors[idx+1],nightColors[idx+2],nightColors[idx+3]
				};
		}
		return null;
	}
	
	public void changeUVChannel(){
		if(uvsets == 2){
			uvsets = 1;
			float[] uvs = new float[vertexCount*2];
			for(int i = 0;i < vertexCount*2;i++){
				uvs[i] = texcoords[i];
			}
			flags -= GEOMETRY_FLAG_MULTIPLEUVSETS;
			texcoords = uvs;
		}else{
			uvsets = 2;
			float[] uvs = new float[(vertexCount*2) * 2];
			int i;
			for(i = 0;i < vertexCount*2;i++){
				uvs[i] = texcoords[i];
			}
			for(;i < uvs.length;i++){
				uvs[i] = 0.0f;
			}
			flags |= GEOMETRY_FLAG_MULTIPLEUVSETS;
			texcoords = uvs;
		}
	}
	
	public boolean isModulateMaterial(){
		return ((flags & GEOMETRY_FLAG_MODULATEMATCOLOR) != 0);
	}
	
	public void setModulateMaterial(){
		if((flags & GEOMETRY_FLAG_MODULATEMATCOLOR) != 0){
			flags -= GEOMETRY_FLAG_MODULATEMATCOLOR;
		}else{
			flags |= GEOMETRY_FLAG_MODULATEMATCOLOR;
		}
	}
	
}
