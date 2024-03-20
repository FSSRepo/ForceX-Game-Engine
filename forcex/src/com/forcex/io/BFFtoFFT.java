package com.forcex.io;
import com.forcex.core.*;
import com.forcex.utils.*;
import java.util.zip.*;

public class BFFtoFFT
{
	public BFFtoFFT(String folder,String name){
		FileStreamWriter os = new FileStreamWriter(folder + "/" + name + ".fft");
		FileStreamReader is = new FileStreamReader(folder + "/" + name + ".bff");
		is.skip(2);
		int imgWidth = is.readInt();
		int imgHeight = is.readInt();
		int cellWidth = is.readInt();
		int cellHeight = is.readInt();
		is.skip(1);
		byte baseChar = is.readByte();
		byte[] charsWidth = is.readByteArray(256);
		os.writeByte(baseChar);
		os.writeByte((byte)(imgWidth / cellWidth));
		os.writeFloat((float)cellWidth / imgWidth);
		os.writeFloat((float)cellHeight / imgHeight);
		for(byte w : charsWidth){
			os.writeFloat((float)(w & 0xff) / cellWidth);
		}
		os.writeShort(imgWidth);
		os.writeShort(imgHeight);
		byte[] imgData = is.readByteArray(imgWidth*imgHeight);
		byte[] rgba8 = new byte[imgWidth*imgHeight*4];
		for(int i = 0;i < imgData.length;i++){
			rgba8[i*4] = (byte)0xff;
			rgba8[i*4+1] = (byte)0xff;
			rgba8[i*4+2] = (byte)0xff;
			rgba8[i*4+3] = imgData[i];
		}
		os.writeByteArray(CoreJni.convertImageFormat(rgba8,imgWidth,imgHeight,false));
		is.clear();
		os.finish();
	}
	
	public static void convert(String path){
		try{
			MemoryStreamReader is = new MemoryStreamReader(path);
			byte base = is.readByte();
			byte val1 = is.readByte();
			float[] vals = is.readFloatArray(2+256);
			short w = is.readShort(),h = is.readShort();
			byte[] data = is.readByteArray(w*h*2);
			is.clear();
			Deflater df = new Deflater(Deflater.BEST_COMPRESSION);
			df.setInput(data);
			df.finish();
			byte[] buffer = new byte[w*h];
			int i = df.deflate(buffer);
			df.end();
			FileStreamWriter os = new FileStreamWriter(path);
			os.writeByte(base);
			os.writeByte(val1); os.writeFloatArray(vals);
			os.writeShort(w); os.writeShort(h);
			os.writeShort(i);
			os.write(buffer,i);
			os.finish();
		}catch(Exception e){
			Logger.log(e);
		}
	}
}
