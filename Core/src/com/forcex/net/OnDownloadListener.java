package com.forcex.net;

public interface OnDownloadListener{
	void requestFileSize(int size);
	void onDownloadError(int error_code,String url,String details);
	void onDownloadProgress(int size_dowloaded);
}
