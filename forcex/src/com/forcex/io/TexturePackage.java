package com.forcex.io;

import com.forcex.FX;
import com.forcex.core.GL;
import com.forcex.core.gpu.TextureBuffer;
import com.forcex.utils.DXTC;

public class TexturePackage {
    private TextureHeader[] textures;
    private String path;

    public TexturePackage(String path) {
        this.path = path;
        BinaryStreamReader is = FX.fs.open(path, FileSystem.ReaderType.STREAM);
        short numTextures = is.readShort();
        textures = new TextureHeader[numTextures];
        int offset = 2;
        for (short i = 0; i < numTextures; i++) {
            TextureHeader tex = new TextureHeader();
            byte str_len = is.readByte();
            tex.name = is.readString(str_len);
            tex.alpha = is.readBoolean();
            tex.width = is.readShort();
            tex.height = is.readShort();
            tex.type = is.readByte();
            tex.mipmap = is.readByte();
            offset += 8 + str_len;
            tex.offset = offset;

            for (byte m = 0; m < tex.mipmap; m++) {
                int sz = is.readInt();
                offset += (sz + 4);
                is.skip(sz);
            }
            textures[i] = tex;
        }
        is.clear();
        is = null;
    }

    public TextureHeader getTexture(String name) {
        for (TextureHeader tex : textures) {
            if (tex.name.equals(name)) {
                return tex;
            }
        }
        return null;
    }

    public TextureHeader getTexture(int index) {
        return textures[index];
    }

    public int numTextures() {
        return textures.length;
    }

    public TextureBuffer[] setupTextures(String texture) {
        for (TextureHeader texture_header : textures) {
            if (texture_header.name.equals(texture)) {
                BinaryStreamReader is = FX.fs.open(path, FileSystem.ReaderType.STREAM);
                is.skip(texture_header.offset);
                TextureBuffer[] buffers = new TextureBuffer[texture_header.mipmap];
                int width = texture_header.width;
                int height = texture_header.height;
                for (byte m = 0; m < texture_header.mipmap; m++) {
                    int size = is.readInt();
                    buffers[m] = processTexture(is.readByteArray(size), texture_header);
                    buffers[m].width = width;
                    buffers[m].height = height;
                    width /= 2;
                    height /= 2;
                }
                is.clear();
                return buffers;
            }
        }
        return null;
    }

    private TextureBuffer processTexture(byte[] data, TextureHeader info) {
        TextureBuffer texture_buffer = new TextureBuffer();
        switch (info.type) {
            case 1: // DXT Compression
                if (FX.gpu.hasOGLExtension("GL_EXT_texture_compression_s3tc")) {
                    texture_buffer.type = !info.alpha ? GL.GL_TEXTURE_DXT1 : GL.GL_TEXTURE_DXT5;
                    texture_buffer.data = data;
                } else {
                    texture_buffer.type = GL.GL_TEXTURE_RGBA;
                    texture_buffer.data = DXTC.decompress(data, info.width, info.height, info.alpha ? DXTC.Type.DXT5 : DXTC.Type.DXT1, false);
                }
                break;
            case 2: // No compression RGBA 32 bits per channel
                texture_buffer.type = GL.GL_TEXTURE_RGBA;
                break;
            case 3: // No compression 16 bits unsigned short
                texture_buffer.type = !info.alpha ? GL.GL_TEXTURE_RGB565 : GL.GL_TEXTURE_RGBA4;
                break;
            case 4: // ETC1 Compression (Exclusive to android)
                if (FX.gpu.hasOGLExtension("GL_OES_compressed_ETC1_RGB8_texture")) {
                    texture_buffer.type = GL.GL_TEXTURE_ETC1;
                } else {
                    throw new RuntimeException("Error! A texture requires ETC1, but Open GL doesn't support ETC1 compression. (" + info.name + ")");
                }
                break;
        }
        if (info.type != 1) {
            texture_buffer.data = data;
        }
        return texture_buffer;
    }

    public void delete() {
        textures = null;
    }

    public static class TextureHeader {
        public String name = "";
        public int offset = 0;

        /* texture info */
        public short width;
        public short height;

        boolean alpha;
        byte type = 1;
        byte mipmap;
    }
}
