package com.forcex.rte.objects;
import com.forcex.gfx3d.*;
import com.forcex.rte.*;
import com.forcex.math.*;
import java.util.*;

public class RTMesh extends RTObject
{
	ArrayList<RTTriangle> triangles = new ArrayList<>();
	public Vector3f hit_normal;
	public Vector2f hit_uv;
	public RTMaterial hit_material;
	Vector3f min,max;
	
	public boolean intersectBox(Ray ray) {
		float t1,t2,tnear = Float.NEGATIVE_INFINITY,tfar = Float.POSITIVE_INFINITY,temp;
		boolean intersectFlag = true;
		for(int i = 0;i < 3; i++){
            if(ray.direction.get(i) == 0){
                if(ray.origin.get(i) < min.get(i) || ray.origin.get(i) > max.get(i))
                    intersectFlag = false;
            }
            else{
                t1 = (min.get(i) - ray.origin.get(i)) / ray.direction.get(i);
                t2 = (max.get(i) - ray.origin.get(i)) / ray.direction.get(i);
                if(t1 > t2){
                    temp = t1;
                    t1 = t2;
                    t2 = temp;
                }
                if(t1 > tnear)
                    tnear = t1;
                if(t2 < tfar)
                    tfar = t2;
                if(tnear > tfar)
                    intersectFlag = false;
                if(tfar < 0)
                    intersectFlag = false;
            }
        }
		return intersectFlag;
	}
	
	@Override
	public Vector3f intersect(Ray ray) {
		if(!intersectBox(ray)){
			return null;
		}
		for(RTTriangle tr : triangles) {
			Vector3f e1 = tr.p1.sub(tr.p2);
			Vector3f e2 = tr.p3.sub(tr.p1);
			hit_normal = e1.cross(e2);
			Vector3f c = tr.p1.sub(ray.origin);
			Vector3f r = ray.direction.cross(c);
			float invDet = 1.0f / hit_normal.dot(ray.direction);
			float u = r.dot(e2) * invDet;
			float v = r.dot(e1) * invDet;
			if (u >= 0.0f && v >= 0.0f && u + v <= 1.0f) {
				float t = hit_normal.dot(c) * invDet;
				if (t >= 0.0f) {
					hit_uv = new Vector2f(u,v);
					hit_normal.normalize();
					hit_material = materials.get(tr.material_index);
					return ray.origin.add(ray.direction.mult(t));
				}
			}
			
		}
		return null;
	}

	@Override
	public Vector3f getNormal(Vector3f point) {
		return null;
	}
	
	public RTMesh(Mesh mesh,Matrix4f transform) {
		super(new Vector3f());
		arePrimitives = false;
		min = new Vector3f(1000,1000,1000);
		max = new Vector3f(-1000,-1000,-1000);
		float[] trans = MathGeom.transformVertices(mesh.getVertexData().vertices,transform);
		for(int i = 0;i < trans.length;i += 3){
			min.x = Maths.min(min.x,trans[i]);
			min.y = Maths.min(min.y,trans[i+1]);
			min.z = Maths.min(min.z,trans[i+2]);
			max.x = Maths.max(max.x,trans[i]);
			max.y = Maths.max(max.y,trans[i+1]);
			max.z = Maths.max(max.z,trans[i+2]);
		}
		for(int j = 0;j < mesh.getParts().list.size();j++){
			MeshPart p  = mesh.getPart(j);
			for(int i = 0;i < p.index.length;i += 3){
				RTTriangle t = new RTTriangle();
				t.p1 = new Vector3f(trans,(p.index[i] & 0xff) * 3);
				t.p2 = new Vector3f(trans,(p.index[i + 1] & 0xff) * 3);
				t.p3 = new Vector3f(trans,(p.index[i + 2] & 0xff) * 3);
				t.material_index = j;
				triangles.add(t);
			}
			RTMaterial mat = new RTMaterial();
			mat.color.set(p.material.color);
			mat.roughness = p.material.reflection;
			mat.specular = p.material.specular;
			materials.add(mat);
		}
		trans = null;
	}
	
	public class RTTriangle{
		Vector3f p1,p2,p3;
		int material_index;
	}
}
