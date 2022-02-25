package com.forcex.gfx3d.shapes;
import com.forcex.gfx3d.*;
import com.forcex.core.*;

public class GridRectangle extends Mesh
{
	int GRID_SIZE = 20;
	public GridRectangle(float lenght){
		super(true);
		float[] vertices = new float[GRID_SIZE * GRID_SIZE * 3];
		short[] indices = new short[(GRID_SIZE - 1) * GRID_SIZE * 4];
		int j,i,vert = 0,indx = 0;
		for(i = 0;i < GRID_SIZE;i++){
			float x = 0,y = 0;
			for(j = 0;j < GRID_SIZE - 1;j++){
				x = (float)(i - (GRID_SIZE >> 1)) * lenght;
				y = (float)(j - (GRID_SIZE >> 1)) * lenght;
				vertices[vert*3] = x;
				vertices[vert*3+1] = 0;
				vertices[vert*3+2] = y;
				vert++;
				indices[indx++] = (short)(GRID_SIZE*i+j);
				indices[indx++] = (short)(GRID_SIZE*i+j+1);
				indices[indx++] = (short)(i+GRID_SIZE*j);
				indices[indx++] = (short)(i+GRID_SIZE*(j+1));
			}
			j = GRID_SIZE - 1;
			x = (float)(i - (GRID_SIZE >> 1)) * lenght;
			y = (float)(j - (GRID_SIZE >> 1)) * lenght;
			vertices[vert*3] = x;
			vertices[vert*3+1] = 0;
			vertices[vert*3+2] = y;
			vert++;
		}
		setVertices(vertices);
		addPart(new MeshPart(indices));
		lineSize = 0.1f;
		setPrimitiveType(GL.GL_LINES);
	}
}
