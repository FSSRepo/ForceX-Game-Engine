package com.forcex.gtasdk;
import java.util.*;
import com.forcex.anim.*;
import com.forcex.io.*;
import com.forcex.math.*;
import com.forcex.utils.*;

public class IFPReader{
	public ArrayList<Animation> animations = new ArrayList<Animation>();
	String name;
	int endFile = 0;
	
	public IFPReader(String path){
		try{
			MemoryStreamReader is = new MemoryStreamReader(path);
			String version = is.readString(4);
			endFile = is.readInt();
			if(version.contains("ANP3")){
				name = cortarnombre(is.readString(24));
				int anims = is.readInt();
				for(int i =0;i < anims;i++){
					String name = cortarnombre(is.readString(24));
					Animation anim = new Animation(name);
					int numObjects = is.readInt();
					is.skip(8);
					for(int o = 0;o < numObjects;o++){
						is.skip(24); // name unused
						int frameType = is.readInt();
						int numFrames = is.readInt();
						int boneID = is.readInt();
						Bone object = new Bone(boneID,frameType == 4);
						if(frameType == 4){
							for(int f = 0;f < numFrames;f++){
								KeyFrame frame = new KeyFrame();
								float rx = is.readShort()/4096.0f;
								float ry = is.readShort()/4096.0f;
								float rz = is.readShort()/4096.0f;
								float rw = is.readShort()/4096.0f;
								frame.rotation = new Quaternion(rx,ry,rz,rw);
								frame.time = is.readShort()/60.0f;
								float x = is.readShort()/1024.0f;
								float y = is.readShort()/1024.0f;
								float z = is.readShort()/1024.0f;
								frame.position = new Vector3f(y,x,z);
								object.addKeyFrame(frame);
							}
						}else if(frameType == 3){
							for(int f = 0;f < numFrames;f++){
								KeyFrame frame = new KeyFrame();
								float rx = is.readShort()/4096.0f;
								float ry = is.readShort()/4096.0f;
								float rz = is.readShort()/4096.0f;
								float rw = is.readShort()/4096.0f;
								frame.rotation = new Quaternion(rx,ry,rz,rw);
								frame.time = is.readShort()/60.0f;
								object.addKeyFrame(frame);
							}
						}else{
							is.skip(numFrames * 32);
							for(int f = 0;f<numFrames;f++){
								KeyFrame frame = new KeyFrame();
								frame.rotation = new Quaternion();
								frame.time = 0.0f;
								object.addKeyFrame(frame);
							}
						}
						anim.addBone(object);
					}
					animations.add(anim);
				}
			}else{
				throw new NoSuchMethodException("IFP File version not compatible");
			}
			is.clear();
		}catch(Exception e){
			Logger.log("IFPReader -> " + e.toString());
		}
	}
	
	public Animation getAnimation(String name, boolean notRootPos){
		name = name.toLowerCase();
		for(Animation anim : animations){
			anim.setName(anim.getName().toLowerCase());
			if(anim.getName().startsWith(name)){
				if(notRootPos){
					anim.getRootBone().hasPosition = false;
				}
				return anim;
			}
		}
		return null;
	}
	private String cortarnombre(String str) {
        int indexOf = str.indexOf(0);
        return indexOf > 0 ? str.substring(0, indexOf) : str;
    }
}
