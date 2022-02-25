package com.forcex.utils;
import java.util.*;
import com.forcex.io.*;
import java.io.*;
import com.forcex.*;

public class LanguageString {
	HashMap<String,String> lines = new HashMap<>();
	boolean print = false;
	
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
	
	public void printId(){
		print = true;
	}
	
	public static void print(String text) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(FX.homeDirectory + "dump.lsd", true));
            out.append(text);
            out.newLine();
            out.close();
        } catch (IOException e) {}
    }
	
	public String get(String id){
		if(lines.containsKey(id)){
			return lines.get(id);
		}
		if(print){
			print("!"+id);
		}
		return id;
	}
}
