package com.forcex.core;
import com.forcex.app.*;
import java.io.*;

public interface SystemDevice {
	public static interface OnAndroidFileStream {
		void open(InputStream is,String name);
		void save(OutputStream os);
	}
	
	void addInputListener(InputListener input);
	void stopRender();
	void showInfo(String info,boolean isError);
	void destroy();
	boolean isJDKDesktop();
	int getAndroidVersion();
	void invokeFileChooser(boolean open, String label, String def_name, OnAndroidFileStream listener);
}
