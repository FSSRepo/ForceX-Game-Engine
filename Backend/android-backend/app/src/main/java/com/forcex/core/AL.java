package com.forcex.core;

public interface AL
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
	public static final int 
	AL_FORMAT_MONO8 = 								0x1100,
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
	public String 	alGetString(int pname);
	public int 		alGetError();
	public boolean 	alIsExtensionPresent(String pname);
	public void 	alDistanceModel(int model);
	/*
	 Buffers
	 */
	public int 		alGenBuffer();
	public void 	alBufferData(int buffer,int format,byte[] data,int freq);
	public void 	alDeleteBuffer(int buffer);
	/*
	 Listener
	 */
	public float 	alGetListenerf(int pname);
	public float[] 	alGetListener3f(int pname);
	public float[] 	alGetListenerfv(int pname,int length);
	public void 	alListenerf(int pname,float value);
	public void 	alListenerfv(int pname,float[] values);
	public void 	alListener3f(int pname,float x,float y,float z);
	public void 	alListeneri(int pname,int value);
	public void 	alListener3i(int pname,int x,int y,int z);
	/*
	 Sources
	 */
	public int 		alGenSource();
	public int 		alGetSourcei(int source,int pname);
	public float 	alGetSourcef(int source,int pname);
	public float[] 	alGetSourcefv(int source,int pname,int length);
	public float[] 	alGetSource3f(int source,int pname);
	public void 	alDeleteSource(int source);
	public boolean 	alIsSource(int source);
	public void 	alSourcef(int source,int pname,float value);
	public void 	alSourcefv(int source,int pname,float[] values);
	public void 	alSource3f(int source,int pname,float x,float y,float z);
	public void 	alSourcei(int source,int pname,int value);
	public void 	alSource3i(int source,int pname,int x,int y,int z);
	public void 	alSourcePlay(int source);
	public void 	alSourcePause(int source);
	public void 	alSourceStop(int source);
}
