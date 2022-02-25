package com.forcex.utils;
import java.util.*;
import com.forcex.io.*;

public class LanguageString {
	HashMap<String,String> lines = new HashMap<>();
	
	public LanguageString(String path){
		try{
			String[] lns = FileUtils.readStringText(path).split("\n");
			for(String line : lns){
				if(line.length() == 0 || line.startsWith("#")){
					continue;
				}
				line = line.replace(" = ","=");
				String[] token = line.split("=");
				lines.put(token[0].replace(" ",""),token[1].replace("-nl","\n"));
			}
			lns = null;
		}catch(Exception e){
			Logger.log(e.toString());
		}
	}
	
	public String get(String id){
		if(lines.containsKey(id)){
			return lines.get(id);
		}
		return id;
	}
}
