package com.forcex.utils;
import java.util.*;
import java.security.*;
import java.io.*;

public class SHA1Checksum {
	
	public static String getSHA1(InputStream is){
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			byte[] buffer = new byte[2048];
			int len = is.read(buffer);
			while (len != -1) {
				sha1.update(buffer, 0, len);
				len = is.read(buffer);
			}
			String hex = "";
			byte[] out = sha1.digest();
			for(int i = 0;i < out.length;i++){
				hex += Integer.toString((out[i] & 0xff) + 0x100,16).substring(1);
			}
			out = null;
			return hex;
		}catch(Exception e){
			Logger.log("SHA File Checksum Error: "+e.toString());
		}
		return "";
	}
}
