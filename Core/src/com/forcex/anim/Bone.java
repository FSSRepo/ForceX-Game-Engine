package com.forcex.anim;
import java.util.*;
import com.forcex.math.*;
import com.forcex.utils.*;

public class Bone
{
	public int boneID;
	ArrayList<KeyFrame> keyframes = new ArrayList<KeyFrame>();
	public boolean hasPosition;
	
	public Bone(int boneId,boolean hasPosition){
		this.boneID = boneId;
		this.hasPosition = hasPosition;
		
	}
	
	public void addKeyFrame(KeyFrame key){
		keyframes.add(key);
	}
	
	public int getNumKeyFrames(){
		return keyframes.size();
	}
	
	public KeyFrame getKeyFrame(int idx){
		return keyframes.get(idx);
	}
	
	public KeyFrame[] getFrameTime(float time){
		for(byte f = 0;f < keyframes.size();f++){
			if (time <= keyframes.get(f).time) {
				if (f == 0){
					if (keyframes.size() != 1) {
						return new KeyFrame[]{
							keyframes.get(keyframes.size() - 1),keyframes.get(f)};
					} else {
						return new KeyFrame[]{
							keyframes.get(f),keyframes.get(f)};
					}
				} else {
					return new KeyFrame[]{
						keyframes.get(f - 1),keyframes.get(f)};
				}
			}
		}
		KeyFrame previus = keyframes.get(keyframes.size() - 1);
		return new KeyFrame[]{previus,previus};
	}
	
	private float getTime(KeyFrame[] keys,float time){
		float totalTime = keys[1].time - keys[0].time;
		float currentTime = time - keys[0].time;
		return currentTime / totalTime;
	}
	
	public KeyFrame interpolate(float time){
		KeyFrame[] keys = getFrameTime(time);
		float pcnt = getTime(keys,time);
		KeyFrame key = new KeyFrame();
		if(hasPosition){
			key.position = keys[0].position.lerp(keys[1].position,pcnt);
		}else{
			key.position = new Vector3f();
		}
		key.rotation = keys[0].rotation.slerp(keys[1].rotation,pcnt);
		key.time = pcnt;
		return key;
	}
	
	public Matrix4f interpolateKey(float time){
		return interpolate(time).toMatrix();
	}
}
