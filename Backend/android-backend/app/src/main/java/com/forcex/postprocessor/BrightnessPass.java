package com.forcex.postprocessor;
import com.forcex.gfx3d.shader.*;
import com.forcex.*;
import com.forcex.core.*;

public class BrightnessPass extends Pass
{
	ShaderProgram shader;
	FrameBuffer fbo;
	
	public BrightnessPass(int width,int height){
		fbo = new FrameBuffer(width,height);
		shader = new ShaderProgram();
		shader.createProgram(
			Utils.vdefault,
			"precision mediump float;\n"+
			"varying vec2 texcoords;\n"+
			"uniform sampler2D texture;\n"+
			"void main(){\n"+
			"	vec4 color = texture2D(texture,texcoords);\n"+
			"	float bightness = (color.r * 0.30) + (color.g * 0.45) + (color.b * 0.15);\n"+
			"	gl_FragColor = color * bightness;"+
			"}");
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
