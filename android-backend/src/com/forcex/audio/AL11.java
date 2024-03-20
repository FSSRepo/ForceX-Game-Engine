package com.forcex.audio;
import java.nio.*;

public class AL11
{
	/*
			Constants
	*/
	public static final int AL_FALSE = 				0x0000;
	public static final int AL_TRUE = 				0x0001;
	public static final int AL_SOURCE_TYPE 		= 	0x1027;
	public static final int AL_SOURCE_ABSOLUTE  =	0x0201;
	public static final int AL_SOURCE_RELATIVE  = 	0x0202;
	public static final int AL_CONE_INNER_ANGLE = 	0x1001;
	public static final int AL_CONE_OUTER_ANGLE = 	0x1002;
	public static final int AL_SEC_OFFSET       = 	0x1024;
	public static final int AL_SAMPLE_OFFSET    =	0x1025;
	public static final int AL_BYTE_OFFSET      =	0x1026;
	public static final int AL_PITCH = 				0x1003;
	public static final int AL_POSITION = 			0x1004;
	public static final int AL_DIRECTION = 			0x1005;
	public static final int AL_VELOCITY = 			0x1006;
	public static final int AL_LOOPING = 			0x1007;
	public static final int AL_BUFFER = 			0x1009;
	public static final int AL_GAIN = 				0x100A;
	public static final int AL_MIN_GAIN = 			0x100D;
	public static final int AL_MAX_GAIN = 			0x100E;
	public static final int AL_ORIENTATION = 		0x100F,
	AL_REFERENCE_DISTANCE = 						0x1020;
	public static final int AL_ROLLOFF_FACTOR = 	0x1021;
	public static final int AL_SOURCE_STATE = 		0x1010,
	AL_INITIAL =									0x1011,
	AL_PLAYING = 									0x1012,
	AL_PAUSED = 									0x1013,
	AL_STOPPED = 									0x1014;
	public static final int AL_FORMAT_MONO8 = 		0x1100,
	AL_FORMAT_MONO16 = 								0x1101,
	AL_FORMAT_STEREO8 = 							0x1102,
	AL_FORMAT_STEREO16 =							0x1103;
	public static final int AL_VENDOR = 			0xB001;
	public static final int AL_VERSION = 			0xB002;
	public static final int AL_RENDERER = 			0xB003;
	public static final int AL_EXTENSIONS =	 		0xB004;
	
	/*
				Core
	*/
	public static native String 	alGetString(int pname);
	public static native int 		alGetError();
	public static native boolean 	alIsExtensionPresent(String pname);
	public static native void 		alDistanceModel(int model);
	/*
				Buffers
	*/
	public static native int 		alGenBuffer();
	public static native void 		alBufferData(int buffer,int format,byte[] data,int freq);
	public static native void 		alDeleteBuffer(int buffer);
	/*
				Listener
	*/
	public static native float 		alGetListenerf(int pname);
	public static native float[] 	alGetListener3f(int pname);
	public static native float[] 	alGetListenerfv(int pname,int length);
	public static native void 		alListenerf(int pname,float value);
	public static native void 		alListenerfv(int pname,float[] values);
	public static native void 		alListener3f(int pname,float v1,float v2,float v3);
	public static native void 		alListeneri(int pname,int value);
	public static native void 		alListener3i(int pname,int v1,int v2,int v3);
	/*
				Sources
	*/
	public static native int 		alGenSource();
	public static native int 		alGetSourcei(int source,int pname);
	public static native float 		alGetSourcef(int source,int pname);
	public static native float[] 	alGetSourcefv(int source,int pname,int length);
	public static native float[] 	alGetSource3f(int source,int pname);
	public static native void 		alDeleteSource(int source);
	public static native boolean 	alIsSource(int source);
	public static native void 		alSourcef(int source,int pname,float value);
	public static native void 		alSourcefv(int source,int pname,float[] values);
	public static native void 		alSource3f(int source,int pname,float v1,float v2,float v3);
	public static native void 		alSourcei(int source,int pname,int value);
	public static native void 		alSource3i(int source,int pname,int v1,int v2,int v3);
	public static native void 		alSourcePlay(int source);
	public static native void 		alSourcePause(int source);
	public static native void 		alSourceStop(int source);
}
