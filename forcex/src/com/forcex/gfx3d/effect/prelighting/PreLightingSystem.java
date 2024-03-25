package com.forcex.gfx3d.effect.prelighting;
import java.util.*;
import com.forcex.gfx3d.*;
import com.forcex.core.gpu.*;
import com.forcex.math.*;

public class PreLightingSystem
{
	ArrayList<PreLight> lights = new ArrayList<PreLight>();
	int MAX_LIGHT = 5;
	Vector3f tmpcolor = new Vector3f();
	
	public void addLight(PreLight light) {
		if(lights.size() == MAX_LIGHT) {
			return;
		}
		lights.add(light);
	}
	
	public byte[] applyLighting(ModelObject obj) {
		Mesh mesh = obj.getMesh();
		VertexData vertex_data = mesh.getVertexData();
		float[] vertices = vertex_data.vertices;
		float[] normals = vertex_data.normals;
		Matrix4f mm = obj.getTransform();
		byte[] colors = new byte[mesh.getVertexInfo().vertexCount * 4];
		int offset = 0;
		for(int i = 0;i < vertices.length; i += 3) {
			Vector3f world = mm.mult(new Vector3f(vertices,i));
			Vector3f N = mm.mult(new Vector4f(normals,i,0)).xyz().normalize();
			tmpcolor.set(0,0,0);
			for(PreLight light : lights) {
				Vector3f L = light.position.sub(world).normalize();
				float diff = Maths.max(N.dot(L),0.0f);
				tmpcolor.addLocal(light.ambient.add(light.diffuse.mult(diff)).multLocal(0.5f));
			}
			colors[offset] = (byte)(tmpcolor.x * 255);
			colors[offset + 1] = (byte)(tmpcolor.y * 255);
			colors[offset + 2] = (byte)(tmpcolor.z * 255);
			colors[offset + 3] = (byte)(255);
			offset += 4;
		}
		mesh.setVertexColor(colors);
		return colors;
	}
}
