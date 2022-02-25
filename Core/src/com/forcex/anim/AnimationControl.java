package com.forcex.anim;

public class AnimationControl
{
	boolean loop = false;
	boolean play = true;
	
	public static final byte CMD_RESET = 0;
	public static final byte CMD_PLAY = 1;
	public static final byte CMD_PAUSE = 2;
	public static final byte CMD_LOOP = 3;
	public static final byte CMD_NO_LOOP = 4;
	
	public float time = 0.0f;
	public float speed = 1.0f;
	
	public void putCommand(byte cmd){
		switch(cmd){
			case CMD_RESET:
				time = 0.0f;
				speed = 1.0f;
				play = true;
				loop = false;
				break;
			case CMD_PLAY:
				play = true;
				break;
			case CMD_PAUSE:
				play = false;
				break;
			case CMD_LOOP:
				loop = true;
				break;
			case CMD_NO_LOOP:
				loop = false;
				break;
		}
	}
	
	public boolean isRunning(){
		return play;
	}
}
