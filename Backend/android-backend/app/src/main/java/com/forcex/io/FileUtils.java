package com.forcex.io;
import java.io.*;
import com.forcex.utils.*;

public class FileUtils
{
	public static void writeBinaryData(String path,byte[] data) {
         try {
            OutputStream fo = new FileOutputStream(path);
            fo.write(data);
			fo.close();
        } catch (Exception e) {
			Logger.log(e.toString());
        }
    }
	public static byte[] readBinaryData(String path) {
        byte[] buffer = null;
        try {
            FileInputStream fi = new FileInputStream(path);
            buffer = new byte[fi.available()];
            fi.read(buffer);
            fi.close();
        } catch (Exception e) {
			Logger.log(e.toString());
        }
        return buffer;
    }
	
	public static String readStringText(String path) {
        String text = "";
        try {
            FileInputStream fi = new FileInputStream(path);
            byte[] b = new byte[fi.available()];
            fi.read(b);
            fi.close();
            text = new String(b);
        } catch (IOException e) {
        }
        return text;
    }
}
