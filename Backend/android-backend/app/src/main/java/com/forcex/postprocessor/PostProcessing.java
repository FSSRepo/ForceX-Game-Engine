package com.forcex.postprocessor;
import com.forcex.core.*;
import com.forcex.*;
import com.forcex.utils.*;
import java.util.*;

public class PostProcessing
{
	GL gl = FX.gl;
	int vbo;
	ArrayList<Pass> passes = new ArrayList<>();
	
	public PostProcessing(){
		float[] vertices = {
			-1,1,-1,-1,1,1,1,-1
		};
		vbo = gl.glGenBuffer();
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, vertices.length * 4, BufferUtils.createFloatBuffer(vertices), GL.GL_STATIC_DRAW);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
		vertices = null;
	}
	
	public void addPass(Pass pass){
		passes.add(pass);
	}
	
	public Pass getPass(int index){
		return passes.get(index);
	}
	
	public void doProcessing(int colorTexture) {
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
		for(byte i = 0;i < passes.size();i++){
			if(i > 0){
				Pass bef = passes.get(i - 1);
				if(bef.renderfbo){
					passes.get(i).process(bef.getTexture());
					continue;
				}
			}
			passes.get(i).process(colorTexture);
		}
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
	}
	
	public void delete() {
		for(Pass pass : passes){
			pass.delete();
		}
		gl.glDeleteBuffer(vbo);
	}
}
