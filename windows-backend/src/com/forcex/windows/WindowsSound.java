package com.forcex.windows;

import org.lwjgl.openal.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class WindowsSound implements com.forcex.core.ALC
{
	private long context;
	private long device;

	@Override
	public boolean create() {
		device = alcOpenDevice((ByteBuffer) null);
		if (device == NULL) {
			throw new IllegalStateException("Failed to open the default OpenAL device.");
		}
		ALCCapabilities deviceCaps = ALC.createCapabilities(device);
		this.context = alcCreateContext(device, (IntBuffer) null);
		if (context == NULL) {
			throw new IllegalStateException("Failed to create OpenAL context.");
		}
		alcMakeContextCurrent(context);
		AL.createCapabilities(deviceCaps);
		return true;
	}

	@Override
	public void destroy()
	{
		alcDestroyContext(context);
		alcCloseDevice(device);
	}
}
