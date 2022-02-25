package com.forcex.postprocessor;
import com.forcex.*;
import com.forcex.core.*;
import com.forcex.gfx3d.shader.*;

public class ContrastPass extends Pass{
	ShaderProgram shader;
	int value;
	float amount;
	
	public ContrastPass(){
		shader = new ShaderProgram();
		shader. createProgram(
			Utils.vdefault,
			(FX.gpu.isOpenGLES()?"precision mediump float;\n":"")+
			"varying vec2 texcoords;\n"+
			"uniform sampler2D texture;\n"+
			"uniform float value;"+
			"void main(){\n"+
			"	vec3 color = texture2D(texture,texcoords).rgb;\n"+
			"	gl_FragColor = vec4(color * (color * value),1.0);\n"+
			"}"
		);
		shader. attrib_position = shader.getAttribLocation("positions");
		value = shader.getUniformLocation("value");
		amount = 1.1f;
	}
	
	public void process(int texture){
		render(texture);
	}
	
	public void setValue(float amount){
		this.amount = amount;
	}
	
	private void render(int texture){
		shader.start();
		shader.setFloat(value,amount);
		FX.gl.glActiveTexture(GL.GL_TEXTURE0);
		FX.gl.glBindTexture(GL.GL_TEXTURE_2D,texture);
		Utils.renderQuad(shader);
		shader.stop();
	}
	
	public void delete(){
		shader.cleanUp();
	}
}
