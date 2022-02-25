package com.forcex.postprocessor;
import com.forcex.gfx3d.shader.*;
import com.forcex.io.*;
import com.forcex.*;
import com.forcex.core.*;
import com.forcex.math.*;

public class FXAAFilter extends Pass
{
	ShaderProgram shader;
	Vector2f screenSize;
	int ssz;
	
	public FXAAFilter(int width,int height){
		shader = new ShaderProgram("shaders/fxaa.vs","shaders/fxaa.fs");
		screenSize = new Vector2f(1.0f / width,1.0f / height);
		shader.attrib_position = shader.getAttribLocation("positions");
		ssz = shader.getUniformLocation("pixs");
	}
	
	public void process(int texture){
		shader.start();
		shader.setVector2(ssz,screenSize);
		FX.gl.glActiveTexture(GL.GL_TEXTURE0);
		FX.gl.glBindTexture(GL.GL_TEXTURE_2D,texture);
		Utils.renderQuad(shader);
		shader.stop();
	}
	
	public void delete(){
		shader.cleanUp();
	}
}
