package com.forcex.math;
import java.util.*;
import com.forcex.gfx3d.*;
import com.forcex.core.gpu.*;
import com.forcex.utils.*;

public class MathGeom{
	
	public static short[] convertToTriangleList(short[] triangleStrips){
		int numTrangles = 0;
		for(int i = 0;i < triangleStrips.length - 2;i++){
			if(triangleStrips[i] == triangleStrips[i+1] ||
			   triangleStrips[i] == triangleStrips[i+2] ||
			   triangleStrips[i+1] == triangleStrips[i+2]){
				numTrangles++;
			}
		}
		short[] triangles = new short[numTrangles * 3];
		int o = 0;
		for(int i = 0;i < triangleStrips.length - 2;i++){
			if(triangleStrips[i] == triangleStrips[i+1] ||
			   triangleStrips[i] == triangleStrips[i+2] ||
			   triangleStrips[i+1] == triangleStrips[i+2]){
				continue;
			}
			triangles[o+0] = triangleStrips[i];
			triangles[o+1] = triangleStrips[i+1 + (i % 2)];
			triangles[o+2] = triangleStrips[i+2 - (i % 2)];
			o += 3;
		}
		return triangles;
	}
	
	public static float[] transformVertices(float[] vertices,Matrix4f modelMatrix){
		float[] vertex = new float[vertices.length];
		int vertexCount = vertices.length;
		Vector3f tmp = new Vector3f();
		for (int i = 0; i < vertexCount; i += 3) {
			Vector3f v = modelMatrix.mult(
				tmp.set(vertices,i));
			v.get(vertex,i);
		}
		return vertex;
	}
	
	public static float[] calculateNormals(float[] vertices,ArrayList<MeshPart> parts,boolean invertY){
		float[] normals = new float[vertices.length];
		Vector3f tmp0 = new Vector3f();
		Vector3f tmp1 = new Vector3f();
		Vector3f tmp2 = new Vector3f();
		for(MeshPart part : parts){
			short[] indx = part.index;
			for(int i = 0;i < indx.length;i += 3){
				int idx1 = indx[i + 0] * 3;
				int idx2 = indx[i + 1] * 3;
				int idx3 = indx[i + 2] * 3;
				tmp0.set(vertices,idx1);
				tmp1.set(vertices,idx2);
				tmp2.set(vertices,idx3);
				Vector3f normal = normalTriangle(tmp0,tmp1,tmp2);
				normal.y *= (invertY ? -1f : 1f);
				normal.get(normals,idx1);
				normal.get(normals,idx2);
				normal.get(normals,idx3);
			}
		}
		return normals;
	}
	
	public static float[] calculateTangents(float[] vertices,float[] texcoords,ArrayList<MeshPart> parts){
		float[] tangents = new float[vertices.length];
		Vector3f tmp0 = new Vector3f();
		Vector3f tmp1 = new Vector3f();
		Vector3f tmp2 = new Vector3f();
		Vector2f uv1 = new Vector2f();
		Vector2f uv2 = new Vector2f();
		Vector2f uv3 = new Vector2f();
		for(MeshPart part : parts){
			short[] indx = part.index;
			for(int i = 0;i < indx.length;i += 3){
				short idx1 = indx[i];
				short idx2 = indx[i+1];
				short idx3 = indx[i+2];
				tmp0.set(vertices,idx1*3);
				tmp1.set(vertices,idx2*3);
				tmp2.set(vertices,idx3*3);
				uv1.set(texcoords,idx1*2);
				uv2.set(texcoords,idx2*2);
				uv3.set(texcoords,idx3*2);
				Vector3f edge1 = tmp1.sub(tmp0); 
				Vector3f edge2 = tmp2.sub(tmp0);
				Vector2f duv1 = uv2.sub(uv1);
				Vector2f duv2 = uv3.sub(uv1);
				float r = 1.0f / (duv1.x * duv2.y - duv1.y * duv2.x);
				edge1.mult(duv2.y); edge2.mult(duv1.y);
				Vector3f tangent = edge1.sub(edge2);
				tangent.multLocal(r);
				tangent.add(tangents,idx1*3);
				tangent.add(tangents,idx2*3);
				tangent.add(tangents,idx3*3);
			}
		}
		return tangents;
	}
	
	public static float[] calculateBitangents(float[] tangents,float[] normals){
		float[] bitangents = new float[tangents.length];
		Vector3f tmp = new Vector3f();
		Vector3f tmp2 = new Vector3f();
		for(int i = 0; i < normals.length;i += 3){
			tmp.set(normals,i);
			tmp2.set(tangents,i);
			Vector3f bitangent = tmp.cross(tmp2).normalize();
			bitangent.get(bitangents,i);
		}
		return bitangents;
	}
	
	public static void setNormalMapProps(Mesh mesh,boolean invertY){
		VertexData vdata = mesh.getVertexData();
		if(vdata.normals == null){
			mesh.setNormals(calculateNormals(vdata.vertices,mesh.getParts(),invertY));
		}
		if(vdata.tangents == null){
			mesh.setTangents(calculateTangents(vdata.vertices,vdata.texcoords,mesh.getParts()));
		}
		if(vdata.bitangents == null){
			mesh.setBiTangents(calculateBitangents(vdata.normals,vdata.tangents));
		}
	}
	
	public static Vector3f crossTriangle(Vector3f v1,Vector3f v2,Vector3f v3){
		// r = cross(v1 - v2,v2 - v3);
		return v1.sub(v2).crossLocal(v2.sub(v3));
	}
	
	public static Vector3f normalTriangle(Vector3f v1,Vector3f v2,Vector3f v3){
		// r = cross(v1 - v2,v2 - v3);
		return crossTriangle(v1,v2,v3).normalize();
	}
	
	public static float areaTriangle(Vector3f v1,Vector3f v2,Vector3f v3){
		return crossTriangle(v1,v2,v3).length() * 0.5f;
	}
}
