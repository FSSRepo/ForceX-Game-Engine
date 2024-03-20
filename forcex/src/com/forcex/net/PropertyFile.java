package com.forcex.net;
import java.util.*;
import java.net.*;
import java.io.*;
import com.forcex.utils.*;

public class PropertyFile {
	HashMap<String,Integer> integers = new HashMap<>();
	HashMap<String,String> strings = new HashMap<>();
	HashMap<String,Boolean> booleans = new HashMap<>();
	boolean loaded = false;
	
	public boolean load(String url,OnPropertyListener listener) {
		String steps = "";
		try {
			URL socket = new URL(url);
			steps += "Url: "+url+"\n";
			InputStream is = socket.openStream();
			steps += "Streaming\n";
			String text = "";
			byte data[] = new byte[1024];
			int count = 0;
			while ((count = is.read(data)) != -1) {
				text += new String(data, 0, count);
			}
			is.close();
			String[] cfg_lines = text.split("\n");
			text = null;
			for(String line : cfg_lines){
				String[] tkn = line.split("<->");
				String[] tks = tkn[1].split("=>");
				switch(tkn[0]){
					case "s":
						strings.put(tks[0],tks[1]);
						break;
					case "i":
						integers.put(tks[0],Integer.parseInt(tks[1]));
						break;
					case "b":
						booleans.put(tks[0],Boolean.parseBoolean(tks[1]));
						break;
				}
			}
			loaded = true;
			return true;
		} catch (Exception e) {
			if(listener != null){
				int error = 0;
				if(e.toString().contains("Unable to resolve host")){
					error = Download.ERROR_NO_INTERNET;
				}else if(e.toString().contains("MalformedURL")){
					error = Download.ERROR_WRONG_URL;
				}else if(e.toString().contains("IOException")){
					error = Download.ERROR_IO_ERROR;
				}
				Logger.log(e);
				listener.OnLoadError(error,steps + e.toString());
			}
			return false;
		}
	}
	
	public boolean loaded(){
		return loaded;
	}
	
	public int getInt(String id){
		if(integers.containsKey(id)){
			return integers.get(id);
		}
		return -1;
	}

	public String getString(String id){
		if(strings.containsKey(id)){
			return strings.get(id);
		}
		return "";
	}
	
	public boolean getBoolean(String id){
		if(booleans.containsKey(id)){
			return booleans.get(id);
		}
		return false;
	}
}
