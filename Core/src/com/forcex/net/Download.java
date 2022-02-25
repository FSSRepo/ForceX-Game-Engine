package com.forcex.net;
import java.net.*;
import java.io.*;

public class Download {
	public static final int ERROR_NO_INTERNET = 8;
	public static final int ERROR_WRONG_URL = 12;
	public static final int ERROR_IO_ERROR = 14;
	
	public static boolean download(String url,String out,OnDownloadListener listener) {
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
			OutputStream os = new FileOutputStream(out);
			byte data[] = new byte[1024];
			int count = 0;
			int offset = 0;
			while ((count = is.read(data)) != -1) {
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
			listener.onDownloadError(error,url,e.toString());
			return false;
		}
	}
} 
