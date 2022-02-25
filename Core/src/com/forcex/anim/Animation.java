package com.forcex.anim;
import java.util.*;

public class Animation
{
	float duration = 0;
	ArrayList<Bone> bones = new ArrayList<Bone>();
	public String name = "";

	public Animation(String name){
		this.name = name;
	}

	public void addBone(Bone bone){
		bones.add(bone);
	}

	public Bone findBone(int index){
		for(int i = 0;i < bones.size();i++){
			if(bones.get(i).boneID == index){
				return bones.get(i);
			}
		}
		return null;
	}

	public float getDuration(){
		if(duration == 0){
			for(Bone bone : bones){
				for(KeyFrame key : bone.keyframes){
					if (key.time > duration) {
						duration = key.time;
					}
				}
			}
		}
		return duration;
	}
	
	public Bone getRootBone(){
		return bones.get(0);
	}
}
