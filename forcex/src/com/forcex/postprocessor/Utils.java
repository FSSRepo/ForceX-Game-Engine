package com.forcex.postprocessor;
import com.forcex.gfx3d.shader.*;
import com.forcex.*;
import com.forcex.core.*;

public class Utils
{
	public static String vdefault = 
	"attribute vec2 positions;\n"+
	"varying vec2 texcoords;\n"+
	"void main(){\n"+
	"	gl_Position = vec4(positions,0.0,1.0);\n"+
	"	texcoords = positions * 0.5 + 0.5;\n"+
	"}";
	
	public static void renderQuad(ShaderProgram shader){
		GL gl = FX.gl;
		gl.glVertexAttribPointer(shader.attrib_position,2,GL.GL_FLOAT,false,0,0);
		gl.glEnableVertexAttribArray(shader.attrib_position);
		gl.glDisable(GL.GL_DEPTH_TEST);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
		gl.glDrawArrays(GL.GL_TRIANGLE_STRIP,0,4);
		gl.glEnable(GL.GL_DEPTH_TEST);
	}
}
