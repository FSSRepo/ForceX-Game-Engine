package com.forcex.gtasdk;

public interface OnDFFStreamingListener{
	void onStreamPrint(String log);
	void onStreamProgress(float progress);
	void onStreamError(String err,boolean stop);
}
