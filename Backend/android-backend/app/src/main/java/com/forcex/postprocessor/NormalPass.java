package com.forcex.postprocessor;
import com.forcex.*;
import com.forcex.utils.*;
import com.forcex.gfx3d.shader.*;
import com.forcex.core.*;

public class NormalPass extends Pass{
    int color;
    ShaderProgram shader;
	Color maskColor;
	
    public NormalPass(Color mask) {
		shader = new ShaderProgram();
		maskColor = mask;
        shader.createProgram(Utils.vdefault,
			"precision mediump float;\n"+
			"varying vec2 texcoords;\n"+
			"uniform sampler2D texture;\n"+
			"uniform vec3 color;\n"+
			"void main(){\n"+
			"	gl_FragColor = vec4(texture2D(texture,texcoords).rgb * color,1.0);\n"+
			"}");
        shader.attrib_position = shader.getAttribLocation("positions");
        color = shader.getUniformLocation("color");
    }

    public void process(int colorTexture) {
        GL gl = FX.gl;
      	shader.start();
        shader.setColor3(color, maskColor);
        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, colorTexture);
        Utils.renderQuad(shader);
        shader.stop();
    }

    public void delete() {
        shader.cleanUp();
    }
}
