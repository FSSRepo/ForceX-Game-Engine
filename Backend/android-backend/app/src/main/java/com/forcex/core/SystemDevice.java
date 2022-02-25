package com.forcex.core;

public interface SystemDevice
{
	void stopRender();
	void showInfo(String info,boolean isError);
	void destroy();
}
