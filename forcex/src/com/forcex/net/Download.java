package com.forcex.net;
import java.net.*;
import java.io.*;
import com.forcex.utils.*;

public class Download {
	public static final int ERROR_NO_INTERNET = 8;
	public static final int ERROR_WRONG_URL = 12;
	public static final int ERROR_IO_ERROR = 14;
	boolean cancel;
	OnDownloadListener listener;
	String url;
	String output;

	public Download(String url,String output,OnDownloadListener listener){
		this.url = url;
		this.output = output;
		this.listener = listener;
		cancel = false;
	}
	
	public void cancel(){
		cancel = true;
	}
	
	public boolean process(){
		if(listener == null){
			return false;
		}
		try {
			URL socket = new URL(url);
			URLConnection conection = socket.openConnection();
			conection.connect();
			int size = conection.getContentLength();
			listener.requestFileSize(size);
			InputStream is = new BufferedInputStream(socket.openStream(), 8192);
			OutputStream os = new FileOutputStream(output);
			byte data[] = new byte[1024];
			int count = 0;
			long offset = 0;
			while ((count = is.read(data)) != -1) {
				if(cancel) {
					os.close();
					is.close();
					new File(output).delete();
					return false;
				}
				listener.onDownloadProgress(offset);
				offset += count;
				os.write(data, 0, count);
			}
			os.flush();
			os.close();
			is.close();
			return true;
		} catch (Exception e) {
			int error = 0;
			if(e.toString().contains("Unable to resolve host")){
				error = ERROR_NO_INTERNET;
			}else if(e.toString().contains("MalformedURL")){
				error = ERROR_WRONG_URL;
			}else if(e.toString().contains("IOException")){
				error = ERROR_IO_ERROR;
			}
			Logger.log(e);
			listener.onDownloadError(error,url,e.toString());
			return false;
		}
	}
} 
