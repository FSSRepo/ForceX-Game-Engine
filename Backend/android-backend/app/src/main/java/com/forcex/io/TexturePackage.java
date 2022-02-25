package com.forcex.io;
import com.forcex.core.gpu.*;
import com.forcex.core.*;
import com.forcex.utils.*;
import com.forcex.*;

public class TexturePackage{
	public static class TextureHeader{
		String name = "";
		public int offset = 0;
		/* texture info */
		public short width;
		public short height;
		boolean alpha;
		byte type = 1;
		byte mipmap;
	}

	TextureHeader[] textures;
	String path;

	public TexturePackage(String path){
		this.path = path;
		FileStreamReader is = new FileStreamReader(path);
		short numTextures = is.readShort();
		textures = new TextureHeader[numTextures];
		int offset = 2;
		for(short i = 0;i < numTextures;i++){
			TextureHeader tex = new TextureHeader();
			byte strlen = is.readByte();
			tex.name = is.readString(strlen);
			tex.alpha = is.readBoolean();
			tex.width = is.readShort();
			tex.height = is.readShort();
			tex.type = is.readByte();
			tex.mipmap = is.readByte();
			offset += 8 + strlen;
			tex.offset = offset;
			for(byte m = 0;m < tex.mipmap;m++){
				int sz = is.readInt();
				offset += (sz + 4);
				is.skip(sz);
			}
			textures[i] = tex;
		}
		is.clear();
		is = null;
	}
	
	public TextureHeader getTexture(String name){
		for(TextureHeader tex : textures){
			if(tex.name.equals(name)){
				return tex;
			}
		}
		return null;
	}

	public TextureBuffer[] setupTextures(String texture,boolean optimal){
		for(TextureHeader tex : textures){
			if(tex.name.equals(texture)){
				FileStreamReader is = new FileStreamReader(path);
				is.skip(tex.offset);
				TextureBuffer[] bufs = new TextureBuffer[tex.mipmap];
				short w = tex.width;
				short h = tex.height;
				for(byte m = 0; m < tex.mipmap;m++){
					int sz = is.readInt();
					bufs[m] = processTexture(is.readByteArray(sz),tex);
					w /= 2;
					h /= 2;
				}
				is.clear();
				return bufs;
			}
		}
		return null;
	}

	private TextureBuffer processTexture(byte[] in,TextureHeader info){
		TextureBuffer tr = new TextureBuffer();
		switch(info.type){
			case 1: // DXT Compression
				if(FX.gpu.hasOGLExtension("GL_EXT_texture_compression_s3tc")){
					tr.type = !info.alpha ? GL.GL_TEXTURE_DXT1 : GL.GL_TEXTURE_DXT5;
					tr.data = in;
				}else{
					tr.type = GL.GL_TEXTURE_RGBA;
					tr.data = DXTC.decompress(in,info.width,info.height,info.alpha ? DXTC.Type.DXT5 : DXTC.Type.DXT1,false);
					in = null; 
				}
				break;
			case 2: // No compression RGBA 32 bits per channel
				tr.type = GL.GL_TEXTURE_RGBA;
				break;
			case 3: // No compression 16 bits unsigned short
				tr.type = !info.alpha ?  GL.GL_TEXTURE_RGB565 : GL.GL_TEXTURE_RGBA4;
				break;
			case 4: // ETC1 Compression (Exclusive to android)
				if(FX.gpu.hasOGLExtension("GL_OES_compressed_ETC1_RGB8_texture")){
					tr.type = GL.GL_TEXTURE_ETC1;
				}else{
					throw new RuntimeException("Error! A texture requires ETC1, but Open GL doesn't support ETC1 compression. ("+info.name+")");
				}
				break;
		}
		tr.width = info.width; tr.height = info.height;
		if(info.type != 1) {
			tr.data = in;
		}
		return tr;
	}

	public void delete(){
		textures = null;
	}
}
