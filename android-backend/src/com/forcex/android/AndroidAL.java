package com.forcex.android;
import com.forcex.FX;
import com.forcex.core.*;
import com.forcex.audio.*;

public class AndroidAL implements AL
{
	@Override
	public String alGetString(int pname){
		return AL11.alGetString(pname);
	}

	@Override
	public int alGetError(){
		return AL11.alGetError();
	}

	@Override
	public boolean alIsExtensionPresent(String pname){
		return AL11.alIsExtensionPresent(pname);
	}

	@Override
	public void alDistanceModel(int model){
		AL11.alDistanceModel(model);
	}

	@Override
	public int alGenBuffer(){
		return AL11.alGenBuffer();
	}

	@Override
	public void alBufferData(int buffer,int format,byte[] data,int freq){
		AL11.alBufferData(buffer,format,data,freq);
	}

	@Override
	public void alDeleteBuffer(int buffer){
		AL11.alDeleteBuffer(buffer);
	}

	@Override
	public float alGetListenerf(int pname){
		return AL11.alGetListenerf(pname);
	}

	@Override
	public float[] alGetListener3f(int pname){
		return AL11.alGetListener3f(pname);
	}

	@Override
	public float[] alGetListenerfv(int pname,int length){
		return AL11.alGetListenerfv(pname,length);
	}

	@Override
	public void alListenerf(int pname,float value){
		AL11.alListenerf(pname,value);
	}

	@Override
	public void alListenerfv(int pname,float[] values){
		AL11.alListenerfv(pname,values);
	}

	@Override
	public void alListener3f(int pname,float x,float y,float z){
		AL11.alListener3f(pname,x,y,z);
	}

	@Override
	public void alListeneri(int pname,int value){
		AL11.alListeneri(pname,value);
	}

	@Override
	public void alListener3i(int pname,int x,int y,int z){
		AL11.alListener3i(pname,x,y,z);
	}

	@Override
	public int alGenSource(){
		return AL11.alGenSource();
	}

	@Override
	public int alGetSourcei(int source,int pname){
		return AL11.alGetSourcei(source,pname);
	}

	@Override
	public float alGetSourcef(int source,int pname){
		return AL11.alGetSourcef(source,pname);
	}

	@Override
	public float[] alGetSourcefv(int source,int pname,int length){
		return AL11.alGetSourcefv(source,pname,length);
	}

	@Override
	public float[] alGetSource3f(int source,int pname){
		return AL11.alGetSource3f(source,pname);
	}

	@Override
	public void alDeleteSource(int source){
		AL11.alDeleteSource(source);
	}

	@Override
	public boolean alIsSource(int source){
		return AL11.alIsSource(source);
	}

	@Override
	public void alSourcef(int source,int pname,float value){
		AL11.alSourcef(source,pname,value);
	}

	@Override
	public void alSourcefv(int source,int pname,float[] values){
		AL11.alSourcefv(source,pname,values);
	}

	@Override
	public void alSource3f(int source,int pname,float x,float y,float z){
		AL11.alSource3f(source,pname,x,y,z);
	}

	@Override
	public void alSourcei(int source,int pname,int value){
		AL11.alSourcei(source,pname,value);
	}

	@Override
	public void alSource3i(int source,int pname,int x,int y,int z){
		AL11.alSource3i(source,pname,x,y,z);
	}

	@Override
	public void alSourcePlay(int source){
		AL11.alSourcePlay(source);
	}

	@Override
	public void alSourcePause(int source){
		AL11.alSourcePause(source);
	}

	@Override
	public void alSourceStop(int source){
		AL11.alSourceStop(source);
	}
}
