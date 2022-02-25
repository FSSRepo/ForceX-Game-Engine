package com.forcex.utils;
import com.forcex.gfx3d.*;
import com.forcex.math.*;
import java.util.*;

public class CutMesh
{
	ArrayList<Integer> toSplit = new ArrayList<>();
	
	public void cut(Mesh mesh,Vector3f normal,float constant){
		float[] vertices = mesh.getVertexData().vertices;
		Vector3f a = new Vector3f();
		Vector3f b = new Vector3f();
		Vector3f c = new Vector3f();
		for(MeshPart part : mesh.getParts()){
			short[] indices = part.index;
			for(int i = 0;i < indices.length;i += 3){
				
			}
		}
	}
}
