package com.forcex.utils;
import java.util.*;
import java.security.*;
import java.io.*;

public class SHA1Checksum {
	public static String generateFromFolder(String path,String filter){
		File folder = new File(path);
		String sample = "";
		for(String n : folder.list()){
			File f = new File(path+"/"+n);
			if(f.length() < (1536 * 1024) && n.endsWith(filter) || filter.contains("*")){
				System.out.println("\""+folder.getName()+"/"+n+"\",");
				sample += "new SHA1Checksum.SHA1(\""+folder.getName()+"/"+n+"\",\""+checkSha1(f)+"\"),\n";
			}
		}
		return sample;
	}
	
	public static boolean check(String path,SHA1[] files){
		for(SHA1 sha1 : files){
			File fl =new File(path + sha1.file);
			if(fl.exists()){
				if(!checkSha1(fl).contains(sha1.sha1)){
					return false;
				}
			}else{
				return false;
			}
		}
		return true;
	}
	
	private static String checkSha1(File file){
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			InputStream input = new FileInputStream(file);
			byte[] buffer = new byte[8192];
			int len = input.read(buffer);
			while (len != -1) {
				sha1.update(buffer, 0, len);
				len = input.read(buffer);
			}
			String hex = "";
			byte[] out = sha1.digest();
			for(int i = 0;i < out.length;i++){
				hex += Integer.toString((out[i] & 0xff) + 0x100,16).substring(1);
			}
			out = null;
			return hex;
		}catch(Exception e){
		}
		return null;
	}
	
	public static class SHA1{
		public String file;
		public String sha1;
		
		public SHA1(String file,String sha1){
			this.file = file;
			this.sha1 = sha1;
		}
	}
}
