package com.forcex.gui.cmd;

public abstract class Command {
	public static final byte CMD_ERROR = 0xC;
	public static final byte CMD_FINISHED = 0xA;
	public abstract byte exec(CommandLine cmd,String[] args);
}
