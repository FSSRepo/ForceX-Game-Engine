package com.forcex.core.gpu;
import com.forcex.app.threading.*;

public interface GPUDevice{
	int getWidth();
	int getHeight();
	int getFPSLimit();
	String getOpenGLVersion();
	String getGPUVendor();
	String getGPUModel();
	boolean hasOGLExtension(String extension);
	int getFPS();
	float getDeltaTime();
	boolean isOpenGLES();
	void queueTask(Task task);
	boolean hasTaskInQueue();
	void waitEmptyQueue();
	void setFPSLimit(int fps);
}
