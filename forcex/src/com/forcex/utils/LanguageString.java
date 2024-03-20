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
	
	public Set<String> getIds() {
		return lines.keySet();
	}
	
	public String get(String id,Object... data){
		// s[format] i[id] i[]=obj
		if(lines.containsKey(id)){
			String process = "";
			String eval = lines.get(id);
			int obj_offset = 0;
			for(int i = 0;i < eval.length();i++){
				char start = eval.charAt(i);
				if(i + 1 >= eval.length()){
					process += start;
					return process;
				}else if(
					(start == 's' || start == 'i' || start == 'm') && 
					eval.charAt(i + 1) == '['){
					i += 2;
					String d = "";
					for(int j = i;j < eval.length();j++){
						if(j >= eval.length()){
							return process;
						}else if(eval.charAt(j) == ']'){
							i += d.length();
							break;
						}else{
							d += eval.charAt(j);
						}
					}
					switch(start){
						case 's': {
								if(obj_offset >= data.length){
									process += "s["+d+"]";
								}else{
									process += String.format(d,data[obj_offset]);
									obj_offset++;
								}
						}
							break;
						case 'i': {
								if(d.length() == 0){
									if(obj_offset >= data.length){
										process += "i[]";
									}else{
										process += data[obj_offset];
									}
									obj_offset++;
								}else{
									process += get(d);
								}
							}
							break;
						case 'm': {
								String t = get(d);
								process += t.equals(d) ? d.toUpperCase() : t.toUpperCase();
							}
							break;
					}
				}else{
					process += start;
				}
			}
			return process;
		}
		return id;
	}
}
