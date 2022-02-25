package com.forcex.io;
import java.io.*;
import com.forcex.core.*;

public class WavFile{
	public byte[] data;
	public int samplerate;
	public int format;
	public float time;

	public WavFile(String path){
		byte[] wav = FileUtils.readBinaryData(path);
		int channel = 0,bitrate = 0, offset = 0;
		if(readString(wav,offset,4).equals("RIFF")){
			offset += 8;
			if(readString(wav,offset,4).equals("WAVE")){
				offset += 8;
				if(readInt(wav,offset) != 16){
					throw new RuntimeException("WavFile: not PCM format, wave format unsupported :(");
				}
				offset += 4;
				if(readShort(wav,offset) != 1){
					throw new RuntimeException("WavFile: not PCM format :(");
				}
				offset += 2; 
				channel = readShort(wav,offset);
				offset += 2;
				samplerate = readInt(wav,offset);
				offset += 10;
				bitrate = readShort(wav,offset);
				while(true){
					if(wav[offset] == 'd' && wav[offset+1] == 'a' && wav[offset+2] == 't'){
						break;
					}
					offset++;
				}
				offset += 4;
				int dataSize = readInt(wav,offset);
				data = new byte[dataSize];
				if(bitrate == 16){
					for(int i = offset,j = 0;i < dataSize;i += 2,j += 2){
						data[j] = wav[i];
						data[j + 1] = wav[i + 1];
					}
				}else{
					for(int i = offset,j = 0;i < dataSize;i++,j++){
						data[j] = wav[i];
					}
				}
			}
		}
		time = (float)data.length / (samplerate * channel * bitrate / 8f);
		if(bitrate == 16){
			if(channel == 2){
				format = AL.AL_FORMAT_STEREO16;
			}
			else{
				format = AL.AL_FORMAT_MONO16;
			}
		}else{
			if(channel == 2){
				format = AL.AL_FORMAT_STEREO8;
			}
			else{
				format = AL.AL_FORMAT_MONO8;
			}
		}
	}

	public int readShort(byte[] DATA,int fileoffset){
		int i = (DATA[fileoffset] & 0xFF) | 
			(DATA[fileoffset+1] & 0xFF) << 8;
		return i;
	}
	public String readString(byte[] DATA,int offset,int size){
		byte[] data = new byte[size];
		for(int i = 0;i < size;i++){
			data[i] = DATA[offset + i];
		}
		return new String(data);
	}
	public int readInt(byte[] DATA,int fileoffset){
		int i = (DATA[fileoffset] & 0xFF) | 
			(DATA[fileoffset+1] & 0xFF) << 8| 
			(DATA[fileoffset+2] & 0xFF) << 16|
			(DATA[fileoffset+3] & 0xFF) << 24;
		return i;
	}
	public String toTimeReal(){
		int t = (int)time % 60;
		return ((int)time / 60)+":"+t;
	}
}
