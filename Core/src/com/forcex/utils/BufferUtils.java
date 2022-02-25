package com.forcex.utils;
import java.nio.*;

public class BufferUtils
{
	public static FloatBuffer createFloatBuffer(float[] data){
		FloatBuffer fb = createFloatBuffer(data.length);
		fb.put(data);
		fb.position(0);
		return fb;
	}
	
	public static FloatBuffer createFloatBuffer(int length){
		FloatBuffer fb = ByteBuffer.allocateDirect(length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		return fb;
	}
	
	public static ShortBuffer createShortBuffer(short[] data){
		ShortBuffer fb = ByteBuffer.allocateDirect(data.length*2).order(ByteOrder.nativeOrder()).asShortBuffer();
		fb.put(data);
		fb.position(0);
		return fb;
	}
	public static ShortBuffer createShortBuffer(int length){
		ShortBuffer fb = ByteBuffer.allocateDirect(length*2).order(ByteOrder.nativeOrder()).asShortBuffer();
		return fb;
	}
	public static ByteBuffer createByteBuffer(byte[] data){
		
		ByteBuffer fb = ByteBuffer.allocateDirect(data.length).order(ByteOrder.nativeOrder());
		fb.put(data);
		fb.position(0);
		return fb;
	}
	public static ByteBuffer createByteBuffer(int length){
		ByteBuffer fb = ByteBuffer.allocateDirect(length).order(ByteOrder.nativeOrder());
		return fb;
	}
}
