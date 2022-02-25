package com.forcex.gfx3d.shapes;
import com.forcex.gfx3d.*;
import com.forcex.core.*;

public class WireFrameObject extends Mesh {
	public WireFrameObject(Mesh src){
		super(true);
		setVertices(src.getVertexData().vertices);
		int totalTriangles = 0;
		for(MeshPart part : src.getParts()){
			totalTriangles += part.index.length / 3;
		}
		short[] dest = new short[totalTriangles * 6];
		int j = 0;
		for(MeshPart part : src.getParts()){
			short[] indices = part.index;
			for(int i = 0;i < indices.length;){
				dest[j+0] = indices[i+0];
				dest[j+1] = indices[i+1];
				dest[j+2] = indices[i+1];
				dest[j+3] = indices[i+2];
				dest[j+4] = indices[i+2];
				dest[j+5] = indices[i+0];
				i += 3;
				j += 6;
			}
		}
		addPart(new MeshPart(dest));
		setPrimitiveType(GL.GL_LINES);
	}
}
