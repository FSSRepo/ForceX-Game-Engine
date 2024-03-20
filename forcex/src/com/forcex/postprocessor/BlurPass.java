package com.forcex.postprocessor;
import com.forcex.gfx3d.shader.*;
import com.forcex.io.*;
import com.forcex.*;
import com.forcex.core.*;

public class BlurPass extends Pass{
	ShaderProgram shader;
	FrameBuffer fbo;
	public static final byte VERTICAL = 0;
	public static final byte HORIZONTAL = 1;
	
	public BlurPass(byte type,boolean x4,int width,int height){
		fbo = new FrameBuffer(width,height);
		shader = new ShaderProgram();
		String vprefix = "";
		float pixelSize = 0;
		switch(type){
			case 0:
				pixelSize = 1f / (float)height;
				vprefix += "#define blurVertical\n";
				break;
			case 1:
				pixelSize = 1f / (float)width;
				break;
		}
		if(x4){
			vprefix += "#define blurx4\n";
		}
		
		vprefix += "const float pixelSize = "+pixelSize+";\n";
		shader.createProgram(
			vprefix + FileUtils.readStringText(FX.homeDirectory+"shaders/blur.vs"),
			(x4 ? "#define blurx4\n":"") + FileUtils.readStringText(FX.homeDirectory+"shaders/blur.fs"));
		shader.attrib_position = shader.getAttribLocation("positions");
		renderfbo = true;
	}
	public void process(int texture){
		if(renderfbo){
			fbo.begin();
			render(texture);
			fbo.end();
		}else{
			render(texture);
		}
	}
	private void render(int texture){
		shader.start();
		FX.gl.glActiveTexture(GL.GL_TEXTURE0);
		FX.gl.glBindTexture(GL.GL_TEXTURE_2D,texture);
		Utils.renderQuad(shader);
		shader.stop();
	}

	public int getTexture(){
		return fbo.getTexture();
	}
	
	public void delete(){
		shader.cleanUp();
		fbo.delete();
	}
}
