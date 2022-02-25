package com.forcex.windows;

import com.forcex.core.*;
import com.forcex.utils.*;
import java.nio.*;
import org.lwjgl.opengl.*;
import com.forcex.*;

public class WindowsGL implements GL{

    @Override
    public void glDisableVertexAttribArray(int indx) {
        GL20.glDisableVertexAttribArray(indx);
    }

    @Override
    public void glEnableVertexAttribArray(int indx) {
        GL20.glEnableVertexAttribArray(indx);
    }

    @Override
    public void glVertexAttribPointer(int indx, int size, int type, boolean normalize, int stride, int offset) {
        GL20.glVertexAttribPointer(indx, size, type, normalize, stride, offset);
    }

    @Override
    public void glEnable(int cap) {
        GL11.glEnable(cap);
    }

    @Override
    public void glDisable(int cap) {
        GL11.glDisable(cap);
    }

    @Override
    public String glGetString(int pname) {
        return GL11.glGetString(pname);
    }

    @Override
    public int glGetInteger(int pname) {
        IntBuffer buf = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
        GL11.glGetInteger(pname, buf);
        return buf.get(0);
    }

    @Override
    public void glViewport(int width, int height) {
        GL11.glViewport(0, 0, width, height);
    }

    @Override
    public void glClear(int params) {
        GL11.glClear(params);
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        GL11.glClearColor(red, green, blue, alpha);
    }

    @Override
    public void glFrontFace(int param) {
       GL11.glFrontFace(param);
    }

    @Override
    public void glCullFace(int param) {
        GL11.glCullFace(param);
    }

    @Override
    public void glDepthFunc(int param) {
       GL11.glDepthFunc(param);
    }

    @Override
    public void glDepthRangef(float param1, float param2) {
       GL11.glDepthRange(param1, param2);
    }

    @Override
    public void glDepthMask(boolean mask) {
        GL11.glDepthMask(mask);
    }

    @Override
    public void glClearDepthf(float param) {
       GL11.glClearDepth(param);
    }

    @Override
    public void glBlendFunc(int param1, int param2) {
            GL11.glBlendFunc(param1, param2);
    }

    @Override
    public int glGenFramebuffer() {
        IntBuffer buf = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
        EXTFramebufferObject.glGenFramebuffersEXT(buf);
        return buf.get(0);
    }

    @Override
    public void glBindFramebuffer(int target, int fbo) {
       EXTFramebufferObject.glBindFramebufferEXT(target, fbo);
    }

    @Override
    public void glFramebufferTexture2D(int target, int dataformat, int targetTexture, int textureId) {
        EXTFramebufferObject.glFramebufferTexture2DEXT(target, dataformat, targetTexture, textureId, 0);
    }

    @Override
    public void glFramebufferRenderbuffer(int target, int dataformat, int targetRbo, int rbo) {
        EXTFramebufferObject.glFramebufferRenderbufferEXT(target, dataformat, targetRbo, rbo);
    }

    @Override
    public int glCheckFramebufferStatus(int target) {
        return EXTFramebufferObject.glCheckFramebufferStatusEXT(target);
    }

    @Override
    public int glGenRenderbuffer() {
       IntBuffer buf = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
        EXTFramebufferObject.glGenFramebuffersEXT(buf);
        return buf.get(0);
    }

    @Override
    public void glBindRenderbuffer(int target, int rbo) {
        EXTFramebufferObject.glBindRenderbufferEXT(target, rbo);
    }

    @Override
    public void glRenderbufferStorage(int target, int format, int width, int height) {
        EXTFramebufferObject.glRenderbufferStorageEXT(target, format, width, height);
    }

    @Override
    public void glBindBuffer(int target, int buffer) {
        GL15.glBindBuffer(target, buffer);
    }

    @Override
    public int glGenBuffer() {
       return GL15.glGenBuffers();
    }

    @Override
    public void glDeleteBuffer(int buffer) {
        GL15.glDeleteBuffers(buffer);
    }

    @Override
    public void glBufferData(int target, int size, Buffer data, int mode) {
        if(data instanceof ByteBuffer){
             GL15.glBufferData(target, (ByteBuffer)data, mode);
        }else if(data instanceof FloatBuffer){
             GL15.glBufferData(target, (FloatBuffer)data, mode);
        }else if(data instanceof ShortBuffer){
             GL15.glBufferData(target, (ShortBuffer)data, mode);
        }
    }

    @Override
    public void glBufferSubData(int target, int size, Buffer data) {
         if(data instanceof ByteBuffer){
             GL15.glBufferSubData(target, 0, (ByteBuffer)data);
        }else if(data instanceof FloatBuffer){
             GL15.glBufferSubData(target, 0, (FloatBuffer)data);
        }else if(data instanceof ShortBuffer){
             GL15.glBufferSubData(target, 0, (ShortBuffer)data);
        }
    }

    @Override
    public void glDrawArrays(int mode, int first, int count) {
        GL11.glDrawArrays(mode, first, count);
    }

    @Override
    public void glDrawElements(int mode, int count) {
        GL11.glDrawElements(mode, count, GL11.GL_UNSIGNED_SHORT, 0);
    }

    @Override
    public void glLineWidth(float size) {
        GL11.glLineWidth(size);
    }

    @Override
    public void glBindTexture(int target, int textureId) {
       GL11.glBindTexture(target, textureId);
    }

    @Override
    public void glActiveTexture(int texture) {
        GL13.glActiveTexture(texture);
    }

    @Override
    public int glGenTexture() {
       return GL11.glGenTextures();
    }

    @Override
    public void glDeleteTexture(int textureId) {
       GL11.glDeleteTextures(textureId);
    }

    @Override
    public void glDeleteFrameBuffer(int fbo) {
        EXTFramebufferObject.glDeleteFramebuffersEXT(fbo);
    }

    @Override
    public void glDeleteRenderBuffer(int rbo) {
        EXTFramebufferObject.glDeleteRenderbuffersEXT(rbo);
    }

    @Override
    public void glGenerateMipmap(int target) {
        EXTFramebufferObject.glGenerateMipmapEXT(target);
    }

    @Override
    public void glEmptyTexture(int target, int width, int height, int internalFormat, int format, int dataFormat) {
        GL11.glTexImage2D(target, 0, internalFormat, width, height, 0, format, dataFormat, (ByteBuffer)null);
    }

    @Override
    public void glTexParameteri(int target, int pname, int param) {
        GL11.glTexParameteri(target, pname, param);
    }
    @Override
    public int glGetError() {
         return GL11.glGetError();
    }
    @Override
    public void glTexImage2D(int target, int level, int width, int height, int type, byte[] data) {
        ByteBuffer output = ByteBuffer.allocateDirect(data.length).order(ByteOrder.nativeOrder());
		output.put(data);
		output.position(0);
		/* OpenGL supports DXT1/DXT5 Textures */
		if((type == GL_TEXTURE_DXT1 || type == GL_TEXTURE_DXT5) && FX.gpu.hasOGLExtension("GL_EXT_texture_compression_s3tc")){
                        System.out.println("Detectado");
			GL13.glCompressedTexImage2D(target,level,type == GL_TEXTURE_DXT1 ? GL_COMPRESSED_RGB_S3TC_DXT1_EXT : GL_COMPRESSED_RGBA_S3TC_DXT5_EXT,width,height,0,output);
                        
		}
		// OpenGL supports ETC1 Textures
		else if(type == GL_TEXTURE_ETC1 && FX.gpu.hasOGLExtension("GL_OES_compressed_ETC1_RGB8_texture")){
			GL13.glCompressedTexImage2D(target,level,GL_ETC1_RGB8_OES,width,height,data.length,output);
		}
		// OpenGL doesn't support Textures compressed
		else if(type == GL_TEXTURE_RGBA4 || type == GL_TEXTURE_RGBA || type == GL_TEXTURE_RGB565){
			GL11.glTexImage2D(target, level,type != GL_TEXTURE_RGB565 ? GL_RGBA : GL_RGB, width, height, 0, type != GL_TEXTURE_RGB565 ? GL_RGBA : GL_RGB, type == GL_TEXTURE_RGB565 ? GL_UNSIGNED_SHORT_5_6_5 : (type == GL_TEXTURE_RGBA4 ? GL_UNSIGNED_SHORT_4_4_4_4 : GL_UNSIGNED_BYTE), output);
		}
	}

    @Override
    public void glTexImage2D(int target, int width, int height, int type, byte[] data) {
        glTexImage2D(target, 0, width, height, type, data);
    }

    @Override
    public void glTexImage2D(int target, int width, int height, Buffer data) {
        if(data instanceof ByteBuffer){
              GL11.glTexImage2D(target, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)data);
        }else if(data instanceof IntBuffer){
              GL11.glTexImage2D(target, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (IntBuffer)data);
        }
    }

    @Override
    public void glBindAttribLocation(int program, int indx, String name) {
        GL20.glBindAttribLocation(program, indx, name);
    }

    @Override
    public int glGetAttribLocation(int program, String name) {
        return GL20.glGetAttribLocation(program, name);
    }

    @Override
    public int glGetShaderi(int shader, int pname) {
        return GL20.glGetShader(shader, pname);
    }

    @Override
    public String glGetShaderInfoLog(int shader) {
        return GL20.glGetShaderInfoLog(shader, 2048);
    }

    @Override
    public int glGetUniformLocation(int program, String name) {
       return GL20.glGetUniformLocation(program, name);
    }

    @Override
    public void glUseProgram(int program) {
        GL20.glUseProgram(program);
    }

    @Override
    public int glCreateShader(int type, String source) {
        int shader = GL20.glCreateShader(type);
        if(shader == 0){
            return -1;
        }
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        return shader;
    }

    @Override
    public int glCreateProgram(int vertex_shader, int fragment_shader) {
        int program = GL20.glCreateProgram();
        if(program != 0){
            GL20.glAttachShader(program, vertex_shader);
            GL20.glAttachShader(program, fragment_shader);
            GL20.glLinkProgram(program);
            return program;
        }
        return 0;
    }

    @Override
    public void glCleanProgram(int program, int vertex_shader_id, int fragment_shader_id) {
        GL20.glDetachShader(program, fragment_shader_id);
		GL20.glDetachShader(program, vertex_shader_id);
		GL20.glDeleteShader(fragment_shader_id);
		GL20.glDeleteShader(vertex_shader_id);
		GL20.glDeleteProgram(program);
    }

    @Override
    public void glUniform1i(int location, int x) {
		GL20.glUniform1i(location,x);
    }

    @Override
    public void glUniform1f(int location, float x) {
		GL20.glUniform1f(location,x);
	}

    @Override
    public void glUniform2f(int location, float x, float y) {
		GL20.glUniform2f(location,x,y);
	}

    @Override
    public void glUniform3f(int location, float x, float y, float z) {
        GL20.glUniform3f(location,x,y,z);
    }

    @Override
    public void glUniform4f(int location, float x, float y, float z, float w) {
        GL20.glUniform4f(location,x,y,z,w);
    }

    @Override
    public void glUniformMatrix4f(int location, int count, float[] m) {
        GL20.glUniformMatrix4(location,false,BufferUtils.createFloatBuffer(m));
    }

    @Override
    public void glUniformMatrix3f(int location, int count, float[] m) {
        GL20.glUniformMatrix3(location,false,BufferUtils.createFloatBuffer(m));
    }

    @Override
    public void glUniformMatrix2f(int location, int count, float[] m) {
		GL20.glUniformMatrix2(location,false,BufferUtils.createFloatBuffer(m));
    }

	@Override
	public void glScissor(int x, int y, int width, int height) {
		GL11.glScissor(x,y,width,height);
	}

	@Override
	public void glStencilFunc(int func, int ref, int mask)
	{
		GL11.glStencilFunc(func,ref,mask);
	}

	@Override
	public void glStencilMask(int mask)
	{
		GL11.glStencilMask(mask);
	}

	@Override
	public void glStencilOp(int fail, int zfail, int zpass)
	{
		GL11.glStencilOp(fail,zfail,zpass);
	}
}
