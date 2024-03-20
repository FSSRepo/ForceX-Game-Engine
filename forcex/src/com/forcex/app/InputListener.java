package com.forcex.app;

public interface InputListener {
	public void onKeyEvent(byte key,boolean down);

	public void onTouch(float x,float y,byte type,byte pointer);
}
