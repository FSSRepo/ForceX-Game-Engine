package com.forcex.utils;

import com.forcex.*;
import com.forcex.io.FileSystem;

import java.io.*;
import java.text.*;
import java.util.*;

public class Logger {

    public static void log(String text) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(FileSystem.homeDirectory + "dump.log", true));
            String time = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
            out.append(time + " " + text);
            out.newLine();
            out.close();
        } catch (IOException e) {
        }
    }
	
	public static void log(Exception e) {
        try {
            PrintWriter out = new PrintWriter(new FileWriter(FileSystem.homeDirectory + "dump.log", true));
			e.printStackTrace(out);
			out.append("\n");
            out.close();
        } catch (IOException er) {}
    }
}
