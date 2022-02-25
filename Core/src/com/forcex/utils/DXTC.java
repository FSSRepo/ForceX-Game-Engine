package com.forcex.utils;
import com.forcex.core.*;

public class DXTC {
	public static enum Type{
		DXT1,
		DXT5
	}
	public static enum CompressAlgoritm{
		RANGE_FIT,
		CLUSTER_FIT
	}
	
	public static byte[] compress(byte[] rgba,int width,int height,Type type,CompressAlgoritm comp){
		int flags = 0;
		switch(type){
			case DXT1:
				flags |= CoreJni.DXTC_1;
				break;
			case DXT5:
				flags |= CoreJni.DXTC_5;
				break;
		}
		switch(comp){
			case RANGE_FIT:
				flags |= CoreJni.DXTC_RANGE_FIT;
				break;
			case CLUSTER_FIT:
				flags |= CoreJni.DXTC_CLUSTER_FIT;
				break;
		}
		
		return CoreJni.dxtcompress(rgba,width,height,flags);
	}
	
	public static byte[] decompress(byte[] dxtblocks,int width,int height,Type type,boolean lowData){
		int flags = 0;
		
		switch(type){
			case DXT1:
				flags |= CoreJni.DXTC_1;
				break;
			case DXT5:
				flags |= CoreJni.DXTC_5;
				break;
		}
		return CoreJni.dxtdecompress(dxtblocks,width,height,flags,lowData);
	}
}
