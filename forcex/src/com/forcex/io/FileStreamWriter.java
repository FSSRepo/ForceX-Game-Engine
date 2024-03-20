package com.forcex.io;
import java.io.*;
import com.forcex.utils.*;
import com.forcex.math.*;

public class FileStreamWriter
{
	OutputStream out;
	
	public FileStreamWriter(String path){
		try{
			out = new FileOutputStream(path);
		}
		catch(Exception e){
			Logger.log(e.toString());
		}
	}
	
	private void write(byte[] data){
		try{
			out.write(data);
		}
		catch(Exception e){
			Logger.log(e.toString());
		}
	}
	
	public void write(byte[] data,int lenght){
		try{
			out.write(data,0,lenght);
		}
		catch(Exception e){
			Logger.log(e.toString());
		}
	}
	
	public void writeFloatArray(float[] data) {
        for (int ofs = 0; ofs < data.length; ofs++) {
            writeFloat(data[ofs]);
        }
    }
	
	public void writeByteArray(byte[] data) {
        write(data);
    }
	
	public void writeShortArray(short[] data) {
        for (int ofs = 0; ofs < data.length; ofs++) {
            writeShort(data[ofs]);
        }
    }
	
	public void writeQuaternion(Quaternion qt) {
        writeShort((short)(qt.x * 4096.0f));
		writeShort((short)(qt.y * 4096.0f));
		writeShort((short)(qt.z * 4096.0f));
		writeShort((short)(qt.w * 4096.0f));
    }
	
	public void writeFloat2(float x,float y){
		writeFloat(x);
		writeFloat(y);
	}
	
	public void writeFloat3(float x,float y,float z){
		writeFloat(x);
		writeFloat(y);
		writeFloat(z);
	}
	
	public void writeVector3(Vector3f v){
		writeFloat(v.x);
		writeFloat(v.y);
		writeFloat(v.z);
	}
	
    public void writeString(String text) {
        writeByteArray(text.getBytes());
    }
	
	public void writeStringFromSize(int size, String text) {
        byte[] data = new byte[size];
        for(short i = 0;i < size;i++){
			if(i <= text.length() - 1){
				data[i] = (byte)text.charAt(i);
			}else{
				break;
			}
		}
		writeByteArray(data);
    }
	
    public void writeFloat(float val) {
        writeInt(Float.floatToIntBits(val));
    }

    public void writeInt(int values) {
		byte[] data = new byte[4];
        data[0] = (byte) (values & 0xFF);
        data[1] = (byte) ((values >> 8) & 0xFF);
        data[2] = (byte) ((values >> 16) & 0xFF);
        data[3] = (byte) ((values >> 24) & 0xFF);
        write(data);
    }

    public void writeShort(int values) {
		byte[] data = new byte[2];
        data[0] = (byte) (values & 0xFF);
        data[1] = (byte) ((values >> 8) & 0xFF);
        write(data);
    }

    public void writeByte(int values) {
        write(new byte[]{
				(byte) (values & 0xFF)
			});
    }
	
	public void finish(){
		try{
			out.close();
		}
		catch(Exception e){
			Logger.log(e.toString());
		}
	}
}
