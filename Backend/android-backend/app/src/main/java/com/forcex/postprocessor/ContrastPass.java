package com.forcex.postprocessor;
import com.forcex.*;
import com.forcex.core.*;
import com.forcex.gfx3d.shader.*;

public class ContrastPass extends Pass{
	ShaderProgram shader;
	
	public ContrastPass(){
		shader = new ShaderProgram();
		shader. createProgram(
			Utils.vdefault,
			"precision mediump float;\n"+
			"varying vec2 texcoords;\n"+
			"uniform sampler2D texture;\n"+
			"void main(){\n"+
			"	vec3 color = texture2D(texture,texcoords).rgb;\n"+
			"	gl_FragColor = vec4(color,1.0);\n"+
			"}"
		);
		shader. attrib_position = shader.getAttribLocation("positions");
	}
	
	public void process(int texture){
		render(texture);
	}
	
	private void render(int texture){
		shader.start();
		FX.gl.glActiveTexture(GL.GL_TEXTURE0);
		FX.gl.glBindTexture(GL.GL_TEXTURE_2D,texture);
		Utils.renderQuad(shader);
		shader.stop();
	}
	
	public void delete(){
		shader.cleanUp();
	}
}
