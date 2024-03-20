package com.forcex.audio;

public class ALC
{
	public static native boolean create();
	public static native void destroy();
	
	static {
        System.loadLibrary("fxaudio");
    }
}
