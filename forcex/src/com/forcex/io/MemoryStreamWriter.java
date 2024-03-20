package com.forcex.io;
import java.io.*;
import com.forcex.utils.*;
import com.forcex.math.*;

public class MemoryStreamWriter
{
	byte[] data;
	int offset = 0;
	
	public MemoryStreamWriter(String path){
		try{
			InputStream is = new FileInputStream(path);
			data = new byte[is.available()];
			is.read(data);
			is.close();
		}
		catch(Exception e){
			Logger.log(e);
		}
	}
	
	public MemoryStreamWriter(int initial_size){
		data = new byte[initial_size];
	}
	
	public void seek(int offset){
		this.offset = offset;
	}

	public void writeFloatArray(float[] data) {
        for (int ofs = 0; ofs < data.length; ofs++) {
            writeFloat(data[ofs]);
        }
    }

	public void writeByteArray(byte[] dat) {
        for(int i = 0;i < dat.length;i++){
			writeByte(dat[i]);
		}
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
        byte[] dat = new byte[size];
        for(short i = 0;i < size;i++){
			if(i <= text.length() - 1){
				dat[i] = (byte)text.charAt(i);
			}else{
				break;
			}
		}
		writeByteArray(data);
		dat = null;
    }

    public void writeFloat(float val) {
        writeInt(Float.floatToIntBits(val));
    }

    public void writeInt(int values) {
		data[offset] = (byte) (values & 0xFF);
        data[offset+1] = (byte) ((values >> 8) & 0xFF);
        data[offset+2] = (byte) ((values >> 16) & 0xFF);
        data[offset+3] = (byte) ((values >> 24) & 0xFF);
        offset += 4;
    }

    public void writeShort(int values) {
		data[offset] = (byte) (values & 0xFF);
        data[offset+1] = (byte) ((values >> 8) & 0xFF);
        offset += 2;
    }

    public void writeByte(int values) {
        data[offset] = (byte) (values & 0xFF);
		offset++;
    }

	public void allocate(int size) {
		byte[] newData = new byte[data.length + size];
		int dataoffset = 0;
		for(int i = 0;i < offset;i++){
			newData[dataoffset] = data[i];
			dataoffset++;
		}
		for(int i = 0;i < size;i++){
			newData[dataoffset] = 0;
			dataoffset++;
		}
		data = null;
		data = newData;
	}
	
	public void finish(String path) {
		try{
			OutputStream os = new FileOutputStream(path);
			os.write(data);
			os.close();
		}catch(Exception e){}
		data = null;
	}
}
