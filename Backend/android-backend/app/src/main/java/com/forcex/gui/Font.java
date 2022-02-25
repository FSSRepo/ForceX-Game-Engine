package com.forcex.gui;
import com.forcex.io.*;
import com.forcex.core.*;
import com.forcex.*;
import com.forcex.core.gpu.*;
import com.forcex.utils.*;

public class Font {
	public int font_texture;
	boolean loaded = false;
	public float rowFactor,columnFactor;
	public byte rowPitch,startChar;
	public float[] charWidths;
	GL gl;
	
	public Font(String path){
		MemoryStreamReader is = new MemoryStreamReader(path);
		startChar = is.readByte();
		rowPitch = is.readByte();
		columnFactor = is.readFloat();
		rowFactor = is.readFloat();
		charWidths = is.readFloatArray(256);
		short width = is.readShort();
		short height = is.readShort();
		byte[] img_data = is.readByteArray(width*height*2);
		is.clear();
		gl = FX.gl;
		font_texture = gl.glGenTexture();
        gl.glBindTexture(GL.GL_TEXTURE_2D, font_texture);
        gl.glTexImage2D(GL.GL_TEXTURE_2D, width, height,GL.GL_TEXTURE_RGBA4,img_data);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
	}
	
	public void delete(){
		Texture.remove(font_texture);
	}
}
