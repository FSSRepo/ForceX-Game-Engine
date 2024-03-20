package com.forcex.core.gpu;

import java.nio.*;
import com.forcex.utils.*;

public class VertexData{
	public float[] 
		vertices,texcoords,normals,
		tangents,bitangents,
		boneweights;
	public byte[] colors, boneindices;
	
	public FloatBuffer convert(VertexInfo info){
		FloatBuffer fb = BufferUtils.createByteBuffer(info.getDataSize()).asFloatBuffer();
		if(bitangents == null && info.hasTangents()) {
			info.removeFlag(VertexInfo.HAS_TANGENTS);
		}
		for(int i = 0;i < info.vertexCount;i++){
			fb.put(vertices[i*3]);
			fb.put(vertices[i*3+1]);
			fb.put(vertices[i*3+2]);
			if(info.hasTextureCoords()){
				fb.put(texcoords[i*2]);
				fb.put(texcoords[i*2+1]);
			}
			if(info.hasNormals()){
				fb.put(normals[i*3]);
				fb.put(normals[i*3+1]);
				fb.put(normals[i*3+2]);
			}
			if(info.hasColors()){
				fb.put((colors[i*4] & 0xff) * 0.003921f);
				fb.put((colors[i*4+1] & 0xff) * 0.003921f);
				fb.put((colors[i*4+2] & 0xff) * 0.003921f);
				fb.put((colors[i*4+3] & 0xff) * 0.003921f);
			}
			if(info.hasBones()){
				fb.put(boneweights[i*4]);
				fb.put(boneweights[i*4+1]);
				fb.put(boneweights[i*4+2]);
				fb.put(boneweights[i*4+3]);
				fb.put(boneindices[i*4] & 0xff);
				fb.put(boneindices[i*4+1] & 0xff);
				fb.put(boneindices[i*4+2] & 0xff);
				fb.put(boneindices[i*4+3] & 0xff);
			}
			if(info.hasTangents()){
				fb.put(tangents[i*3]);
				fb.put(tangents[i*3+1]);
				fb.put(tangents[i*3+2]);
			}
			if(info.hasTangents()){
				fb.put(bitangents[i*3]);
				fb.put(bitangents[i*3+1]);
				fb.put(bitangents[i*3+2]);
			}
		}
		fb.position(0);
		return fb;
	}
	
	public void delete(){
		vertices = null;
		texcoords = null;
		boneweights = null;
		boneindices = null;
		colors = null;
		normals = null;
		tangents = null;
		bitangents = null;
	}
}
