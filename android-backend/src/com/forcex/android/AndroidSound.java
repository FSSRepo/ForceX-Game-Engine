package com.forcex.android;
import com.forcex.FX;
import com.forcex.audio.*;

public class AndroidSound implements com.forcex.core.ALC
{
	public static void setupBridge() {
		FX.al = new AndroidAL();
		FX.alc = new AndroidSound();
	}

	@Override
	public boolean create(){
		return ALC.create();
	}

	@Override
	public void destroy(){
		ALC.destroy();
	}
}
