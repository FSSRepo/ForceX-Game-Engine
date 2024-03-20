package com.forcex.android;
import android.opengl.*;
import com.forcex.core.*;
import java.nio.*;
import com.forcex.*;

public class AndroidGL implements GL {
	@Override
	public void glUniformMatrix3f(int location, int count, float[] m) {
		GLES20.glUniformMatrix3fv(location, count, false, m, 0);
	}

	@Override
	public int glGetError() {
		return GLES20.glGetError();
	}
	
	@Override
	public void glUniformMatrix2f(int location, int count, float[] m) {
		GLES20.glUniformMatrix2fv(location, count, false, m, 0);
	}

	@Override
	public void glDeleteRenderBuffer(int rbo) {
		GLES20.glDeleteRenderbuffers(1, new int[]{rbo}, 0);
	}

	@Override
	public void glDeleteFrameBuffer(int fbo)
	{
		GLES20.glDeleteFramebuffers(1, new int[]{fbo}, 0);

	}

	@Override
	public void glDisableVertexAttribArray(int index)
	{
		GLES20.glDisableVertexAttribArray(index);

	}

	@Override
	public void glEnableVertexAttribArray(int index)
	{
		GLES20.glEnableVertexAttribArray(index);

	}

	@Override
	public void glVertexAttribPointer(int index, int size, int type, boolean normalize, int stride, int offset)
	{
		GLES20.glVertexAttribPointer(index, size, type, normalize, stride, offset);
	}

	@Override
	public void glEnable(int cap)
	{
		GLES20.glEnable(cap);
	}

	@Override
	public void glDisable(int cap)
	{
		GLES20.glDisable(cap);
	}

	@Override
	public String glGetString(int pname)
	{

		return GLES20.glGetString(pname);
	}

	@Override
	public int glGetInteger(int pname)
	{
		int[] v = new int[1];
		GLES20.glGetIntegerv(pname, v, 0);
		return v[0];
	}

	@Override
	public void glViewport(int width, int height)
	{
		GLES20.glViewport(0, 0, width, height);
	}

	@Override
	public void glClear(int params)
	{
		GLES20.glClear(params);
	}

	@Override
	public void glClearColor(float red, float green, float blue, float alpha)
	{
		GLES20.glClearColor(red, green, blue, alpha);
	}

	@Override
	public void glFrontFace(int param)
	{
		GLES20.glFrontFace(param);
	}

	@Override
	public void glCullFace(int mode)
	{
		GLES20.glCullFace(mode);
	}

	@Override
	public void glDepthFunc(int func)
	{
		GLES20.glDepthFunc(func);
	}

	@Override
	public void glDepthRangef(float zNear, float zFar)
	{
		GLES20.glDepthRangef(zNear, zFar);
	}

	@Override
	public void glDepthMask(boolean mask)
	{
		GLES20.glDepthMask(mask);
	}

	@Override
	public void glClearDepthf(float depth)
	{
		GLES20.glClearDepthf(depth);
	}

	@Override
	public void glBlendFunc(int sfactor, int dfactor)
	{
		GLES20.glBlendFunc(sfactor, dfactor);
	}

	@Override
	public int glGenFramebuffer()
	{
		int[] v = new int[1];
		GLES20.glGenFramebuffers(1, v, 0);
		return v[0];
	}

	@Override
	public void glBindFramebuffer(int target, int fbo)
	{
		GLES20.glBindFramebuffer(target, fbo);

	}

	@Override
	public void glFramebufferTexture2D(int target, int dataformat, int targetTexture, int textureId)
	{
		GLES20.glFramebufferTexture2D(target, dataformat, targetTexture, textureId, 0);

	}

	@Override
	public void glFramebufferRenderbuffer(int target, int dataformat, int targetRbo, int rbo)
	{

		GLES20.glFramebufferRenderbuffer(target, dataformat, targetRbo, rbo);
	}

	@Override
	public int glCheckFramebufferStatus(int target)
	{

		return GLES20.glCheckFramebufferStatus(target);
	}

	@Override
	public int glGenRenderbuffer()
	{

		int[] v = new int[1];
		GLES20.glGenRenderbuffers(1, v, 0);
		return v[0];
	}

	@Override
	public void glBindRenderbuffer(int target, int rbo)
	{
		GLES20.glBindRenderbuffer(target, rbo);
	}

	@Override
	public void glRenderbufferStorage(int target, int format, int width, int height)
	{
		GLES20.glRenderbufferStorage(target, format, width, height);
	}

	@Override
	public void glBindBuffer(int target, int buffer)
	{
		GLES20.glBindBuffer(target, buffer);
	}

	@Override
	public int glGenBuffer()
	{
		int[] v = new int[1];
		GLES20.glGenBuffers(1, v, 0);
		return v[0];
	}

	@Override
	public void glDeleteBuffer(int buffer)
	{
		GLES20.glDeleteBuffers(1, new int[]{buffer}, 0);
	}

	@Override
	public void glBufferData(int target, int size, Buffer data, int mode)
	{
		GLES20.glBufferData(target, size, data, mode);
	}

	@Override
	public void glBufferSubData(int target, int size, Buffer data)
	{
		GLES20.glBufferSubData(target, 0, size, data);
	}

	@Override
	public void glDrawArrays(int mode, int first, int count)
	{
		GLES20.glDrawArrays(mode, first, count);
	}

	@Override
	public void glDrawElements(int mode, int count)
	{
		GLES20.glDrawElements(mode, count, GL_UNSIGNED_SHORT, 0);
	}

	@Override
	public void glLineWidth(float size)
	{
		GLES20.glLineWidth(size);
	}

	@Override
	public void glBindTexture(int target, int textureId)
	{
		GLES20.glBindTexture(target, textureId);
	}

	@Override
	public void glActiveTexture(int texture)
	{
		GLES20.glActiveTexture(texture);
	}

	@Override
	public int glGenTexture()
	{
		int[] v = new int[1];
		GLES20.glGenTextures(1, v, 0);
		return v[0];
	}

	@Override
	public void glDeleteTexture(int textureId)
	{
		GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
	}

	@Override
	public void glGenerateMipmap(int target)
	{
		GLES20.glGenerateMipmap(target);
	}

	@Override
	public void glEmptyTexture(int target, int width, int height, int internalFormat, int format, int dataFormat)
	{
		GLES20.glTexImage2D(target, 0, internalFormat, width, height, 0, format, dataFormat, null);
	}

	@Override
	public void glTexParameteri(int target, int pname, int param)
	{
		GLES20.glTexParameteri(target, pname, param);
	}

	@Override
	public void glTexImage2D(int target, int width, int height, int type, byte[] data)
	{
		glTexImage2D(target, 0, width, height, type, data);
	}

	/*
	 Input Texture Compression in glCompressedTexImage2D if the texture 
	 compression is available
	*/

	@Override
	public void glTexImage2D(int target, int level, int width, int height, int type, byte[] data)
	{
		ByteBuffer output = ByteBuffer.allocateDirect(data.length).order(ByteOrder.nativeOrder());
		output.put(data);
		output.position(0);
		/* OpenGL supports DXT1/DXT5 Textures */
		if((type == GL_TEXTURE_DXT1 || type == GL_TEXTURE_DXT5) && FX.gpu.hasOGLExtension("GL_EXT_texture_compression_s3tc")){
			GLES20.glCompressedTexImage2D(target,level,type == GL_TEXTURE_DXT1 ? GL_COMPRESSED_RGB_S3TC_DXT1_EXT : GL_COMPRESSED_RGBA_S3TC_DXT5_EXT,width,height,0,data.length,output);
		}
		// OpenGL supports ETC1 Textures
		else if(type == GL_TEXTURE_ETC1 && FX.gpu.hasOGLExtension("GL_OES_compressed_ETC1_RGB8_texture")){
			GLES20.glCompressedTexImage2D(target,level,GL_ETC1_RGB8_OES,width,height,0,output.limit(),output);
		}
		// OpenGL doesn't support Textures compressed
		else if(type == GL_TEXTURE_RGBA4 || type == GL_TEXTURE_RGBA || type == GL_TEXTURE_RGB565){
			GLES20.glTexImage2D(target, level,type != GL_TEXTURE_RGB565 ? GL_RGBA : GL_RGB, width, height, 0, type != GL_TEXTURE_RGB565 ? GL_RGBA : GL_RGB, type == GL_TEXTURE_RGB565 ? GL_UNSIGNED_SHORT_5_6_5 : (type == GL_TEXTURE_RGBA4 ? GL_UNSIGNED_SHORT_4_4_4_4 : GL_UNSIGNED_BYTE), output);
		}
	}

	@Override
	public void glTexImage2D(int target, int width, int height, Buffer data)
	{
		GLES20.glTexImage2D(target, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
	}

	@Override
	public void glBindAttribLocation(int program, int index, String name)
	{
		GLES20.glBindAttribLocation(program, index, name);
	}

	@Override
	public int glGetAttribLocation(int program, String name)
	{
		return GLES20.glGetAttribLocation(program, name);
	}

	@Override
	public int glGetShaderi(int shader, int pname)
	{
		int[] v = new int[1];
		GLES20.glGetShaderiv(shader, pname, v, 0);
		return v[0];
	}

	@Override
	public String glGetShaderInfoLog(int shader)
	{
		return GLES20.glGetShaderInfoLog(shader);
	}

	@Override
	public int glGetUniformLocation(int program, String name)
	{

		return GLES20.glGetUniformLocation(program, name);
	}

	@Override
	public void glUseProgram(int program)
	{
		GLES20.glUseProgram(program);
	}

	@Override
	public int glCreateShader(int type, String source)
	{
		int shader = GLES20.glCreateShader(type);
		if (shader == 0){
			return -1;
		}
		GLES20.glShaderSource(shader, source);
		GLES20.glCompileShader(shader);
		return shader;
	}
	
	@Override
	public int glCreateProgram(int vertex_shader, int fragment_shader)
	{
		int program = GLES20.glCreateProgram();
		if (program != 0)
		{
			GLES20.glAttachShader(program, vertex_shader);
			GLES20.glAttachShader(program, fragment_shader);
			GLES20.glLinkProgram(program);
			return program;
		}
		return 0;
	}

	@Override
	public void glCleanProgram(int program, int vertex_shader_id, int fragment_shader_id)
	{
		GLES20.glDetachShader(program, fragment_shader_id);
		GLES20.glDetachShader(program, vertex_shader_id);
		GLES20.glDeleteShader(fragment_shader_id);
		GLES20.glDeleteShader(vertex_shader_id);
		GLES20.glDeleteProgram(program);
	}

	@Override
	public void glUniform1i(int location, int x)
	{
		GLES20.glUniform1i(location, x);
	}

	@Override
	public void glUniform1f(int location, float x)
	{
		GLES20.glUniform1f(location, x);
	}

	@Override
	public void glUniform2f(int location, float x, float y)
	{
		GLES20.glUniform2f(location, x, y);
	}

	@Override
	public void glUniform3f(int location, float x, float y, float z)
	{
		GLES20.glUniform3f(location, x, y, z);
	}

	@Override
	public void glUniform4f(int location, float x, float y, float z, float w)
	{
		GLES20.glUniform4f(location, x, y, z, w);
	}

	@Override
	public void glUniformMatrix4f(int location, int count, float[] m)
	{
		GLES20.glUniformMatrix4fv(location, count, false, m, 0);
	}

	@Override
	public void glStencilOp(int fail, int zfail, int zpass) {
		GLES20.glStencilOp(fail,zfail,zpass);
	}
	
	@Override
	public void glStencilMask(int mask) {
		GLES20.glStencilMask(mask);
	}

	@Override
	public void glStencilFunc(int func, int ref, int mask) {
		GLES20.glStencilFunc(func, ref, mask);
	}

	@Override
	public void glScissor(int x, int y, int width, int height)
	{
		GLES20.glScissor(x, y, width, height);
	}

	@Override
	public void glReadPixel(int x,int y,int width, int height, int format, Buffer data) {
		GLES20.glReadPixels(x, y, width, height, format, GLES20.GL_UNSIGNED_BYTE, data);
	}
}
