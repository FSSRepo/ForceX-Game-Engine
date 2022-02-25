package com.forcex.windows;
import org.lwjgl.openal.*;
import com.forcex.utils.*;
import java.nio.*;

public class WindowsAL implements com.forcex.core.AL
{

	@Override
	public String alGetString(int pname)
	{
		return AL10.alGetString(pname);
	}

	@Override
	public int alGetError()
	{
		return AL10.alGetError();
	}

	@Override
	public boolean alIsExtensionPresent(String pname)
	{
		return AL10.alIsExtensionPresent(pname);
	}

	@Override
	public void alDistanceModel(int model)
	{
		AL10.alDistanceModel(model);
	}

	@Override
	public int alGenBuffer()
	{
		return AL10.alGenBuffers();
	}

	@Override
	public void alBufferData(int buffer, int format, byte[] data, int freq)
	{
		AL10.alBufferData(buffer,format,BufferUtils.createByteBuffer(data),freq);
	}

	@Override
	public void alDeleteBuffer(int buffer)
	{
		AL10.alDeleteBuffers(buffer);
	}

	@Override
	public float alGetListenerf(int pname)
	{
		return alGetListenerf(pname);
	}

	@Override
	public float[] alGetListener3f(int pname)
	{
		FloatBuffer fb = BufferUtils.createFloatBuffer(3);
		AL10.alGetListener(pname,fb);
		return fb.array();
	}

	@Override
	public float[] alGetListenerfv(int pname, int length)
	{
		FloatBuffer fb = BufferUtils.createFloatBuffer(length);
		AL10.alGetListener(pname,fb);
		return fb.array();
	}

	@Override
	public void alListenerf(int pname, float value)
	{
		AL10.alListenerf(pname,value);
	}

	@Override
	public void alListenerfv(int pname, float[] values)
	{
		AL10.alListener(pname,BufferUtils.createFloatBuffer(values));
	}

	@Override
	public void alListener3f(int pname, float x, float y, float z)
	{
		AL10.alListener3f(pname,x,y,z);
	}

	@Override
	public void alListeneri(int pname, int value)
	{
		AL10.alListeneri(pname,value);
	}

	@Override
	public void alListener3i(int pname, int x, int y, int z)
	{
		AL11.alListener3i(pname,x,y,z);
	}

	@Override
	public int alGenSource()
	{
		return AL10.alGenSources();
	}

	@Override
	public int alGetSourcei(int source, int pname)
	{
		return AL10.alGetSourcei(source,pname);
	}

	@Override
	public float alGetSourcef(int source, int pname)
	{
		return AL10.alGetSourcef(source,pname);
	}

	@Override
	public float[] alGetSourcefv(int source, int pname, int length)
	{
		FloatBuffer fb = BufferUtils.createFloatBuffer(length);
		AL10.alGetSource(source,pname,fb);
		return fb.array();
	}

	@Override
	public float[] alGetSource3f(int source, int pname)
	{
		FloatBuffer fb = BufferUtils.createFloatBuffer(3);
		AL10.alGetSource(source,pname,fb);
		return fb.array();
	}

	@Override
	public void alDeleteSource(int source)
	{
		AL10.alDeleteSources(source);
	}

	@Override
	public boolean alIsSource(int source)
	{
		return AL10.alIsSource(source);
	}

	@Override
	public void alSourcef(int source, int pname, float value)
	{
		AL10.alSourcef(source,pname,value);
	}

	@Override
	public void alSourcefv(int source, int pname, float[] values)
	{
		AL10.alSource(source,pname,BufferUtils.createFloatBuffer(values));
	}

	@Override
	public void alSource3f(int source, int pname, float x, float y, float z)
	{
		AL10.alSource3f(source,pname,x,y,z);
	}

	@Override
	public void alSourcei(int source, int pname, int value)
	{
		AL10.alSourcei(source,pname,value);
	}

	@Override
	public void alSource3i(int source, int pname, int x, int y, int z)
	{
		AL11.alSource3i(source,pname,x,y,z);
	}

	@Override
	public void alSourcePlay(int source)
	{
		AL10.alSourcePlay(source);
	}

	@Override
	public void alSourcePause(int source)
	{
		AL10.alSourcePause(source);
	}

	@Override
	public void alSourceStop(int source)
	{
		AL10.alSourceStop(source);
	}
}
