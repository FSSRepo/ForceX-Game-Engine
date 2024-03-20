package com.forcex.windows;
import com.forcex.core.*;
import org.lwjgl.openal.*;
import org.lwjgl.*;

public class WindowsSound implements ALC
{
	@Override
	public boolean create() {
		try
		{
			org.lwjgl.openal.AL.create();
			if(AL10.alGetError() == AL10.AL_NO_ERROR){
				return true;
			}
		}
		catch (LWJGLException e)
		{
			return false;
		}
		return false;
	}

	@Override
	public void destroy()
	{
		org.lwjgl.openal.AL.destroy();
	}
}
