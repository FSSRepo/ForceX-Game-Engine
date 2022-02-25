package com.forcex.core;
import com.forcex.app.*;

public interface SystemDevice {
	void addInputListener(InputListener input);
	void stopRender();
	void showInfo(String info,boolean isError);
	void destroy();
	boolean isJDKDesktop();
}
