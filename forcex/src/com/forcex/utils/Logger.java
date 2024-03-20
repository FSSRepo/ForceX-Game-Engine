package com.forcex.utils;

import com.forcex.*;
import java.io.*;
import java.text.*;
import java.util.*;

public class Logger {

    public static void log(String text) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(FX.homeDirectory + "dump.log", true));
			//BufferedWriter out = new BufferedWriter(new FileWriter("/storage/emulated/0/dump.log", true));
            String time = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
            out.append(time + " " + text);
            out.newLine();
            out.close();
        } catch (IOException e) {
        }
    }
	
	public static void log(Exception e) {
        try {
            PrintWriter out = new PrintWriter(new FileWriter(FX.homeDirectory + "dump.log", true));
			e.printStackTrace(out);
			out.append("\n");
            out.close();
        } catch (IOException er) {}
    }
	
	public static void printl3(String name,float[] v){
		String text = name+"\n {";
		for(int i = 0;i < v.length;i += 3){
			text += v[i] +", "+v[i+1]+", "+v[i+2]+"\n";
		}
		text += "}";
		Logger.log(text);
	}
	
	public static void printl2(String name,float[] v){
		String text = name+"\n";
		for(int i = 0;i < v.length;i += 2){
			text += v[i] +", "+v[i+1]+"\n";
		}
		Logger.log(text);
	}
	
	public static void printl4(String name,float[] v){
		String text = name+"\n";
		for(int i = 0;i < v.length;i += 4){
			text += v[i] +", "+v[i+1]+", "+v[i+2]+", "+v[i+3]+"\n";
		}
		Logger.log(text);
	}
}
