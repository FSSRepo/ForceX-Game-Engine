package com.forcex.io;
import java.io.*;
import com.forcex.utils.*;
import com.forcex.math.*;

public class MemoryStreamReader implements BinaryReader
{
	byte[] data;
	int offset;
	
	public int getOffset(){
		return offset;
	}
	
	public MemoryStreamReader(String path){
		try{
			InputStream is = new FileInputStream(path);
			data = new byte[is.available()];
			is.read(data);
			is.close();
			is = null;
		}catch(Exception e){
			Logger.log("Error: "+e.toString());
		}
	}
	
	public MemoryStreamReader(byte[] data){
		this.data = data;
	}
	
	@Override
	public String readString(int len){
		return cortarnombre(new String(readByteArray(len)));
	}
	
	private String cortarnombre(String str) {
        int indexOf = str.indexOf(0);
        return indexOf > 0 ? str.substring(0, indexOf) : str;
    }
	
	@Override
	public short[] readShortArray(int len){
		short[] sdata = new short[len];
        for (int i = 0; i < len; i++) {
            sdata[i] = (short)readUShort();
        }
        return sdata;
	}
	
	@Override
	public int readUShort(){
		int val = ((data[offset] & 0xFF) | (data[offset + 1] & 0xFF) << 8);
		offset += 2;
		return val;
	}
	
	@Override
	public short readShort(){
		short val = (short)((data[offset] & 0xFF) | (data[offset + 1] & 0xFF) << 8);
		offset += 2;
		return val;
	}
	
	@Override
	public float readFloat(){
		return Float.intBitsToFloat(readInt());
	}
	
	@Override
	public float[] readFloatArray(int len){
		float[] fdata = new float[len];
        for (int i = 0; i < len; i++) {
            fdata[i] = readFloat();
        }
        return fdata;
	}
	
	@Override
	public short readUbyte(){
		short val = (short) (data[offset] & 0xFF);
        offset++;
        return val;
	}
	
	@Override
	public byte readByte(){
		byte val = data[offset];
        offset++;
        return val;
	}

	@Override
	public boolean readBoolean(){
		return readByte() == 1;
	}
	
	@Override
	public byte[] readByteArray(int len){
		byte[] bdata = new byte[len];
        for (int i = 0; i < len; i++) {
            bdata[i] = (byte) readUbyte();
        }
        return bdata;
	}
	
	@Override
	public int readInt(){
		int val = (data[offset] & 0xFF) | (data[offset + 1] & 0xFF) << 8 | (data[offset + 2] & 0xFF) << 16 | (data[offset + 3] & 0xFF) << 24;
        offset += 4;
        return val;
	}

	@Override
	public void skip(int len){
		offset += len;
	}
	
	@Override
	public Quaternion readQuaternion()
	{
		return new Quaternion(readShort() / 4096.0f,readShort() / 4096.0f,readShort() / 4096.0f,readShort() / 4096.0f);
	}

	@Override
	public Vector3f readVector() {
		return new Vector3f(readFloat(),readFloat(),readFloat());
	}
	
	public void seek(int offset){
		this.offset = offset;
	}
	
	public boolean isEndOfFile(){
		return (offset + 1) >= data.length;
	}

	@Override
	public String scanString()
	{String result = new String();
		byte inByte;
		while ((inByte = readByte()) != 0)
			result += (char) inByte;
		return result;
	}

	
	@Override
	public void clear(){
		data = null;
		offset = 0;
	}
}
