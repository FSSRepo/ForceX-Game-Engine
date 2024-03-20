package com.forcex.core;

public class CoreJni
{
	public static final int DXTC_1 = 						(1 << 0);
	public static final int DXTC_3 = 						(1 << 1);
	public static final int DXTC_5 = 						(1 << 2);
	public static final int DXTC_ITERATIVE_CLUSTER_FIT = 	(1 << 8);
	public static final int DXTC_CLUSTER_FIT = 				(1 << 3);
	public static final int DXTC_RANGE_FIT = 				(1 << 4);
	public static final int DXTC_METRIC_PERCETUAL = 		(1 << 5);
	public static final int DXTC_METRIC_UNIFORM = 			(1 << 6);
	public static final int DXTC_WEIGHT_ALPHA =				(1 << 7);
	public static final byte ETC1_HIGH_QUALITY = 			2;
	public static final byte ETC1_MEDIUM_QUALITY = 			1;
	public static final byte ETC1_LOW_QUALITY =				0;
	
	public static native byte[] 	dxtcompress(byte[] in,int width,int height,int flags);
	public static native byte[]		dxtdecompress(byte[] in,int width,int height,int flags,boolean lowData);
	public static native byte[]		etc1compress(byte[] in,int width,int height,int quality);
	
	public static native void 		pngencode(byte[] in,String path,int width,int height);
	public static native byte[] 	pngdecode(int[] info,String path);
	public static native byte[] 	convertImageFormat(byte[] in,int with,int height,boolean is565);
	static{
		System.loadLibrary("fxcore");
	}
}
