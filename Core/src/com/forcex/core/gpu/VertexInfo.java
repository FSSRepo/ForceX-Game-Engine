package com.forcex.core.gpu;
import com.forcex.utils.*;

public class VertexInfo
{
	int flags;
	public static final int HAS_NORMALS = 		(1<<1);
	public static final int HAS_TEXTURECOORDS = (1<<2);
	public static final int HAS_BONES = 		(1<<3);
	public static final int HAS_COLORS = 		(1<<4);
	public static final int HAS_TANGENTS = 		(1<<5);

	public int stride = 0;
	public int texcoord_ofs = 0;
	public int normal_ofs = 0;
	public int color_ofs = 0;
	public int bone_w_ofs = 0;
	public int bone_i_ofs = 0;
	public int vertexCount;
	public int dataSize = 0;
	public int tangent_ofs = 0;
	public int bitangent_ofs = 0;

	public VertexInfo clone(){
		VertexInfo info = new VertexInfo();
		info.stride = stride;
		info.texcoord_ofs = texcoord_ofs;
		info.normal_ofs = normal_ofs;
		info.color_ofs = color_ofs;
		info.bone_w_ofs = bone_w_ofs;
		info.bone_i_ofs = bone_i_ofs;
		info.vertexCount = vertexCount;
		info.tangent_ofs = tangent_ofs;
		info.bitangent_ofs = bitangent_ofs;
		info.flags = flags;
		return info;
	}
	
	public void setVertexCount(int vcount){
		vertexCount = vcount;
	}

	public void addFlag(int flag){
		if((flags & flag) != 0){
			return;
		}
		this.flags |= flag;
	}

	public void removeFlag(int flag){
		if((flags & flag) != 0){
			this.flags -= flags;
		}
	}

	public int getDataSize(){
		stride = 3; 		// 3 floats
		if(hasTextureCoords()){
			texcoord_ofs = stride*4;
			stride += 2; // 2 floats
		}
		if(hasNormals()){
			normal_ofs = stride*4;
			stride += 3; // 3 floats
		}
		if(hasColors()){
			color_ofs = stride*4;
			stride += 4; // 4 floats
		}
		if(hasBones()){
			bone_w_ofs = stride*4;
			stride += 4; // 4 floats
			bone_i_ofs = stride*4;
			stride += 4; // 4 floats
		}
		if(hasTangents()){
			tangent_ofs = stride*4;
			stride += 3; // 3 floats
			bitangent_ofs = stride*4;
			stride += 3; // 3 floats
		}
		stride *= 4;
		dataSize = vertexCount * stride;
		return dataSize;
	}
	
	public boolean hasTextureCoords(){
		return (flags & HAS_TEXTURECOORDS) != 0;
	}
	
	public boolean hasColors(){
		return (flags & HAS_COLORS) != 0;
	}
	
	public boolean hasBones(){
		return (flags & HAS_BONES) != 0;
	}
	
	public boolean hasNormals(){
		return (flags & HAS_NORMALS) != 0;
	}
	
	public boolean hasTangents(){
		return (flags & HAS_TANGENTS) != 0;
	}
}
