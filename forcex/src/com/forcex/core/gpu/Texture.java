package com.forcex.core.gpu;

import com.forcex.FX;
import com.forcex.core.GL;
import com.forcex.utils.Image;
import java.nio.ByteBuffer;
import java.io.*;

public class Texture {
	
	public static int load(String path) {
		if(!new File(path).exists()) {
			return genTextureWhite();
		}
        GL gl = FX.gl;
        int texid = gl.glGenTexture();
        gl.glBindTexture(GL.GL_TEXTURE_2D, texid);
        Image image = new Image(path);
        gl.glTexImage2D(GL.GL_TEXTURE_2D, image.width, image.height, image.getBuffer());
		image.clear();
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
        return texid;
    }
	
    public static int load(String path,boolean mipmapping) {
		if(!new File(path).exists()){
			return genTextureWhite();
		}
        GL gl = FX.gl;
        int texid = gl.glGenTexture();
        gl.glBindTexture(GL.GL_TEXTURE_2D, texid);
        Image image = new Image(path);
        gl.glTexImage2D(GL.GL_TEXTURE_2D, image.width, image.height, image.getBuffer());
       image.clear();
        if (mipmapping) {
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
            gl.glGenerateMipmap(GL.GL_TEXTURE_2D);
        } else {
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        }
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
        return texid;
    }

    public static int load(int width, int height, ByteBuffer buffer, boolean mipmap) {
        GL gl = FX.gl;
        int texid = gl.glGenTexture();
        gl.glBindTexture(GL.GL_TEXTURE_2D, texid);
        gl.glTexImage2D(GL.GL_TEXTURE_2D, width, height, buffer);
        if (mipmap) {
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
            gl.glGenerateMipmap(GL.GL_TEXTURE_2D);
        } else {
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        }
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
        return texid;
    }
	
	public static int load(TextureBuffer[] buffers) {
        GL gl = FX.gl;
        int texid = gl.glGenTexture();
        gl.glBindTexture(GL.GL_TEXTURE_2D, texid);
		for(byte level = 0;level < buffers.length;level++){
			gl.glTexImage2D(GL.GL_TEXTURE_2D,level,buffers[level].width,buffers[level].height,buffers[level].type,buffers[level].data);
		}
		if (buffers.length > 1) {
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_NEAREST);
		}else {
		    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        }
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
        return texid;
    }
	
	public static int load(TextureBuffer buffer) {
		if(buffer == null){
			return genTextureWhite();
		}
		GL gl = FX.gl;
		int texid = gl.glGenTexture();
        gl.glBindTexture(GL.GL_TEXTURE_2D, texid);
		gl.glTexImage2D(GL.GL_TEXTURE_2D,buffer.width,buffer.height,buffer.type,buffer.data);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
		return texid;
    }
	
    public static void remove(int i) {
        if (i != -1) {
			 FX.gl.glDeleteTexture(i);
        }
    }

    public static void bind(int unit, int texid) {
        FX.gl.glActiveTexture(unit);
        FX.gl.glBindTexture(GL.GL_TEXTURE_2D, texid);
    }
	
	public static void bind(int unit,int type, int texid) {
        FX.gl.glActiveTexture(unit);
        FX.gl.glBindTexture(type, texid);
    }
	
    public static int genTextureWhite() {
        GL gl = FX.gl;
        int texid = gl.glGenTexture();
        gl.glBindTexture(GL.GL_TEXTURE_2D, texid);
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 1, 1, GL.GL_TEXTURE_RGBA, new byte[]{(byte) 255, (byte) 255, (byte) 255, (byte) 255});
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        return texid;
    }
}
