
package com.forcex.core;
import java.nio.Buffer;

public interface GL {
    public static final int GL_DEPTH_BUFFER_BIT = 0x00000100;
    public static final int GL_COLOR_BUFFER_BIT = 0x00004000;
    public static final int GL_BLEND_COLOR = 32773;
    public static final int GL_BLEND_DST_ALPHA = 32970;
    public static final int GL_BLEND_DST_RGB = 32968;
    public static final int GL_BLEND_EQUATION = 32777;
    public static final int GL_BLEND_EQUATION_ALPHA = 34877;
    public static final int GL_BLEND_EQUATION_RGB = 32777;
    public static final int GL_BLEND_SRC_ALPHA = 32971;
    public static final int GL_BLEND_SRC_RGB = 32969;
    public static final int GL_FRONT = 0x0404,
            GL_BACK = 0x0405,
            GL_FRONT_AND_BACK = 0x0408;
    public static final int GL_LESS = 513;
    public static final int GL_TRUE = 1;
    public static final int GL_FALSE = 0;

    public static final int GL_ONE = 1;
    public static final int GL_ONE_MINUS_CONSTANT_ALPHA = 32772;
    public static final int GL_ONE_MINUS_CONSTANT_COLOR = 32770;
    public static final int GL_ONE_MINUS_DST_ALPHA = 773;
    public static final int GL_ONE_MINUS_DST_COLOR = 775;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 771;
    public static final int GL_ONE_MINUS_SRC_COLOR = 769;
    public static final int GL_SRC_ALPHA = 770;
    public static final int GL_SRC_ALPHA_SATURATE = 776;
    public static final int GL_SRC_COLOR = 768;
    public static final int GL_POINTS = 0x0000;
    public static final int GL_LINES = 0x0001;
    public static final int GL_LINE_LOOP = 0x0002;
    public static final int GL_LINE_STRIP = 0x0003;
    public static final int GL_TRIANGLES = 0x0004;
    public static final int GL_TRIANGLE_STRIP = 0x0005;
    public static final int GL_ARRAY_BUFFER = 0x8892;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    public static final int GL_ARRAY_BUFFER_BINDING = 0x8894;
    public static final int GL_ELEMENT_ARRAY_BUFFER_BINDING = 0x8895;
    public static final int GL_STREAM_DRAW = 0x88E0;
    public static final int GL_STATIC_DRAW = 0x88E4;
    public static final int GL_DYNAMIC_DRAW = 0x88E8;
    public static final int GL_TEXTURE_2D = 0x0DE1;
    public static final int GL_CULL_FACE = 0x0B44;
    public static final int GL_BLEND = 0x0BE2;
    public static final int GL_DEPTH_TEST = 0x0B71;
    public static final int GL_UNSIGNED_BYTE = 0x1401;
    public static final int GL_UNSIGNED_SHORT = 0x1403;
    public static final int GL_UNSIGNED_INT = 0x1405;
    public static final int GL_FLOAT = 0x1406;
    public static final int GL_RGB = 0x1907;
    public static final int GL_RGBA = 0x1908;
    public static final int GL_LUMINANCE = 0x1909;
    public static final int GL_LUMINANCE_ALPHA = 0x190A;
    public static final int GL_UNSIGNED_SHORT_4_4_4_4 = 0x8033;
    public static final int GL_UNSIGNED_SHORT_5_5_5_1 = 0x8034;
    public static final int GL_UNSIGNED_SHORT_5_6_5 = 0x8363;
    public static final int GL_FRAGMENT_SHADER = 0x8B30;
    public static final int GL_VERTEX_SHADER = 0x8B31;
    public static final int GL_VENDOR = 0x1F00;
    public static final int GL_RENDERER = 0x1F01;
    public static final int GL_VERSION = 0x1F02;
    public static final int GL_EXTENSIONS = 0x1F03;
    public static final int GL_NEAREST = 0x2600;
    public static final int GL_LINEAR = 0x2601;
    public static final int GL_NEAREST_MIPMAP_NEAREST = 0x2700;
    public static final int GL_LINEAR_MIPMAP_NEAREST = 0x2701;
    public static final int GL_NEAREST_MIPMAP_LINEAR = 0x2702;
    public static final int GL_LINEAR_MIPMAP_LINEAR = 0x2703;
    public static final int GL_TEXTURE_MAG_FILTER = 0x2800;
    public static final int GL_TEXTURE_MIN_FILTER = 0x2801;
    public static final int GL_TEXTURE_WRAP_S = 0x2802;
    public static final int GL_TEXTURE_WRAP_T = 0x2803;
    public static final int GL_TEXTURE0 = 0x84C0;
    public static final int GL_TEXTURE1 = 0x84C1;
    public static final int GL_TEXTURE2 = 0x84C2;
	public static final int GL_TEXTURE3 = 0x84C3;
	public static final int GL_TEXTURE4 = 0x84C4;
    public static final int GL_REPEAT = 0x2901;
    public static final int GL_CLAMP_TO_EDGE = 0x812F;
    public static final int GL_MIRRORED_REPEAT = 0x8370;
    public static final int GL_COMPILE_STATUS = 0x8B81;
    public static final int GL_TEXTURE_DXT1 = 0x00D1;
    public static final int GL_TEXTURE_DXT5 = 0x00D5;
    public static final int GL_TEXTURE_RGBA = 0x8888;
	public static final int GL_TEXTURE_RGBA4 = 0x4444;
    public static final int GL_TEXTURE_RGB565 = 0x0565;
	public static final int GL_TEXTURE_ETC1 = 0x0EC1;
	
    public static final int GL_TEXTURE_CUBE_MAP = 34067;
    public static final int GL_TEXTURE_CUBE_MAP_NEGATIVE_X = 34070;
    public static final int GL_TEXTURE_CUBE_MAP_NEGATIVE_Y = 34072;
    public static final int GL_TEXTURE_CUBE_MAP_NEGATIVE_Z = 34074;
    public static final int GL_TEXTURE_CUBE_MAP_POSITIVE_X = 34069;
    public static final int GL_TEXTURE_CUBE_MAP_POSITIVE_Y = 34071;
    public static final int GL_TEXTURE_CUBE_MAP_POSITIVE_Z = 34073;
    public static final int GL_COLOR_ATTACHMENT0 = 36064;
    public static final int GL_DEPTH_COMPONENT = 6402;
    public static final int GL_DEPTH_COMPONENT16 = 33189;
    public static final int GL_FRAMEBUFFER = 36160;
    public static final int GL_RENDERBUFFER = 36161;
    public static final int GL_DEPTH_ATTACHMENT = 36096;
    public static final int GL_FRAMEBUFFER_COMPLETE = 36053;
	public static final int GL_MAX_RENDERBUFFER_SIZE = 0x84E8;
	public static final int GL_MAX_VERTEX_ATTRIBS = 0x8869;
	public static final int GL_MAX_VERTEX_UNIFORM_VECTORS = 0x8DFB;
	public static final int GL_MAX_VARYING_VECTORS = 0x8DFC;
	public static final int GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS = 0x8B4D;
	public static final int GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS = 0x8B4C;
	public static final int GL_MAX_TEXTURE_IMAGE_UNITS = 0x8872;
	public static final int GL_MAX_FRAGMENT_UNIFORM_VECTORS = 0x8DFD;
	public static final int 
	GL_COMPRESSED_RGB_S3TC_DXT1_EXT  =                 0x83F0,
	GL_COMPRESSED_RGBA_S3TC_DXT1_EXT  =                0x83F1,
	GL_COMPRESSED_RGBA_S3TC_DXT3_EXT  =                0x83F2,
	GL_COMPRESSED_RGBA_S3TC_DXT5_EXT  =                0x83F3,
	GL_ETC1_RGB8_OES =          0x8D64;
	public static final int GL_NEVER = 0x0200;
	public static final int GL_EQUAL = 0x0202;
	public static final int GL_LEQUAL = 0x0203;
	public static final int GL_GREATER = 0x0204;
	public static final int GL_NOTEQUAL = 0x0205;
	public static final int GL_GEQUAL = 0x0206;
	public static final int GL_ALWAYS = 0x0207;
	public static final int GL_KEEP = 0x1E00;
	public static final int GL_REPLACE = 0x1E01;
	public static final int GL_STENCIL_BUFFER_BIT = 0x00000400;
	public static final int GL_STENCIL_TEST = 0x0B90;
	public static final int GL_SCISSOR_TEST = 0x0C11;
	public void glScissor (int x, int y, int width, int height);
	
    public void glDisableVertexAttribArray(int indx);

    public void glEnableVertexAttribArray(int indx);

    public void glVertexAttribPointer(int indx, int size, int type, boolean normalize, int stride, int offset);

    /*	GL capabilities 	*/
    public void glEnable(int cap);

    public void glDisable(int cap);

    public String glGetString(int pname);

    public int glGetInteger(int pname);

    public void glViewport(int width, int height);
	
	public int glGetError();
	
    /*	GL colorbuffer		*/
    public void glClear(int params);

    public void glClearColor(float red, float green, float blue, float alpha);

    /*	GL cullface	*/
    public void glFrontFace(int param);

    public void glCullFace(int mode);

    /*	GL depthbuffer	*/
    public void glDepthFunc(int func);

    public void glDepthRangef(float zNear, float zFar);

    public void glDepthMask(boolean mask);

    public void glClearDepthf(float param);

    /*	GL blending	*/
    public void glBlendFunc(int sfactor, int dfactor);

    /*	GL framebuffer	*/
    public int glGenFramebuffer();

    public void glBindFramebuffer(int target, int fbo);

    public void glFramebufferTexture2D(int target, int dataformat, int targetTexture, int textureId);

    public void glFramebufferRenderbuffer(int target, int dataformat, int targetRbo, int rbo);

    public int glCheckFramebufferStatus(int target);

    /*	GL renderbuffer	*/
    public int glGenRenderbuffer();

    public void glBindRenderbuffer(int target, int rbo);

    public void glRenderbufferStorage(int target, int format, int width, int height);

    /*	GL buffers	*/
    public void glBindBuffer(int target, int buffer);

    public int glGenBuffer();

    public void glDeleteBuffer(int buffer);

    public void glBufferData(int target, int size, Buffer data, int mode);

    public void glBufferSubData(int target, int size, Buffer data);

    /*	GL drawbuffer	*/
    public void glDrawArrays(int mode, int first, int count);

    public void glDrawElements(int mode, int count);

    public void glLineWidth(float size);

    /*	GL texture	*/
    public void glBindTexture(int target, int textureId);

    public void glActiveTexture(int texture);

    public int glGenTexture();

    public void glDeleteTexture(int textureId);
	
	public void glDeleteFrameBuffer(int fbo);
	
	public void glDeleteRenderBuffer(int rbo);
	
    public void glGenerateMipmap(int target);

    public void glEmptyTexture(int target, int width, int height, int internalFormat, int format, int dataFormat);

    public void glTexParameteri(int target, int pname, int param);
	
	public void glTexImage2D(int target,int level, int width, int height, int type, byte[] data);
	
    public void glTexImage2D(int target, int width, int height, int type, byte[] data);
	
	public void glTexImage2D(int target, int width, int height, Buffer data);
	
	public void glReadPixel(int x,int y,int width,int height,int format, Buffer data);
	
    /*	GL shader	*/
    public void glBindAttribLocation(int program, int indx, String name);

    public int glGetAttribLocation(int program, String name);

    public int glGetShaderi(int shader, int pname);

    public String glGetShaderInfoLog(int shader);

    public int glGetUniformLocation(int program, String name);

    public void glUseProgram(int program);

    public int glCreateShader(int type, String source);
	
    public int glCreateProgram(int vertex_shader, int fragment_shader);

    public void glCleanProgram(int program, int vertex_shader_id, int fragment_shader_id);

    public void glUniform1i(int location, int x);

    public void glUniform1f(int location, float x);

    public void glUniform2f(int location, float x, float y);

    public void glUniform3f(int location, float x, float y, float z);

    public void glUniform4f(int location, float x, float y, float z, float w);

    public void glUniformMatrix4f(int location, int count, float[] m);
	
	public void glUniformMatrix3f(int location, int count, float[] m);
	
	public void glUniformMatrix2f(int location, int count, float[] m);
	
	public void glStencilFunc (int func, int ref, int mask);

	public void glStencilMask (int mask);

	public void glStencilOp (int fail, int zfail, int zpass);
}
