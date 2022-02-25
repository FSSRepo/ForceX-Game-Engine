package com.forcex.gfx3d.shapes;
import com.forcex.gfx3d.*;
import com.forcex.core.*;

public class WireBox extends Mesh
{
	public WireBox(float xExt,float yExt,float zExt){
		super(true);
		setVertices( new float[]{
			-xExt, -yExt,  zExt,
			xExt, -yExt,  zExt,
			xExt,  yExt,  zExt,
			-xExt,  yExt,  zExt,

			-xExt, -yExt, -zExt,
			xExt, -yExt, -zExt,
			xExt,  yExt, -zExt,
			-xExt,  yExt, -zExt
		});
		addPart(new MeshPart(new short[]{0, 1,
								   1, 2,
								   2, 3,
								   3, 0,

								   4, 5,
								   5, 6,
								   6, 7,
								   7, 4,

								   0, 4,
								   1, 5,
								   2, 6,
								   3, 7}));
		setPrimitiveType(GL.GL_LINES);
	}
	
	public void update(float xExt,float yExt,float zExt){
		getVertexData().vertices = null;
		setVertices(new float[]{
						-xExt, -yExt,  zExt,
						xExt, -yExt,  zExt,
						xExt,  yExt,  zExt,
						-xExt,  yExt,  zExt,

						-xExt, -yExt, -zExt,
						xExt, -yExt, -zExt,
						xExt,  yExt, -zExt,
						-xExt,  yExt, -zExt
					});
	}
}
