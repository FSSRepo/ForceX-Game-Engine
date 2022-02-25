package com.forcex.anim;
import com.forcex.math.*;
import java.util.*;
import com.forcex.*;
import com.forcex.utils.*;

public class Animator
{
	public AnimationControl control;
	Animation animation;
	Map<SkeletonNode,Matrix4f> animMatrices = new HashMap<SkeletonNode,Matrix4f>();
	Matrix4f[] boneMatrices;
	SkeletonNode skeleton;
	byte numBone = 0;

	public Animator(SkeletonNode skeleton,int numBones){
		control = new AnimationControl();
		this.skeleton = skeleton;
		this.numBone = (byte)numBones;
	}

	public void doAnimation(Animation anim){
		control.setTime(0);
		animation = anim;
	}
	
	public Animation getAnimation(){
		return animation;
	}
	
	public void reset(){
		control.putCommand(AnimationControl.Companion.getCMD_RESET());
		update();
	}
	
	public void update(){
		if(animation == null){
			return;
		}
		
		if(control.getIsRunning()){
			control.setTime(control.getTime() + FX.gpu.getDeltaTime() * control.getSpeed());
				if (control.getTime() > animation.getDuration() && control.getLoop()) {
					control.setTime(control.getTime() % animation.getDuration());
				}else{
					if(control.getTime() > animation.getDuration()){
						return;
					}
				}
			updateBonesMatrices();
		}else{
			if(boneMatrices == null){
				control.setTime(control.getTime() + FX.gpu.getDeltaTime() * control.getSpeed());
				updateBonesMatrices();
			}
			return;
		}
	}
	
	public Matrix4f[] getBoneMatrices(){
		if(boneMatrices == null && animation == null){
			updateBonesMatrices();
		}
		return boneMatrices;
	}
	
	public Matrix4f getBoneMatrixUpdate(int boneId){
		return animMatrices.get(skeleton.findByBoneId((short)boneId));
	}
	
	Vector3f tmp = new Vector3f();
	
	private void updateBoneMatrix(SkeletonNode frame){
		Bone bone = null;
		if(animation != null){
			bone = animation.findBone(frame.boneID);
		}
		if (bone != null) {
			Matrix4f animKey = bone.interpolateKey(control.getTime());
			if(!bone.hasPosition){
				frame.modelMatrix.getLocation(tmp);
				animKey.setLocation(tmp.x,tmp.y,tmp.z);
			}
			SkeletonNode parent = frame.parent;
			if(parent != null){
				Matrix4f pm = animMatrices.get(parent);
				animMatrices.put(frame,pm.mult(null,animKey));
			}else{
				animMatrices.put(frame,animKey);
			}
		}else{
			SkeletonNode parent = frame.parent;
			if(parent != null){
				Matrix4f pm = animMatrices.get(parent);
				animMatrices.put(frame,pm.mult(null,frame.modelMatrix));
			}else{
				animMatrices.put(frame,frame.modelMatrix);
			}
		}
		for(SkeletonNode child : frame.children){
			updateBoneMatrix(child);
		}
	}

	private void updateBonesMatrices(){
		animMatrices.clear();
		updateBoneMatrix(skeleton);
		boneMatrices = new Matrix4f[numBone];
		for(SkeletonNode frame : animMatrices.keySet()) {
			Matrix4f transform = animMatrices.get(frame).mult(null,frame.InverseBoneMatrix);
			if (frame.boneID != -1) {
				boneMatrices[frame.boneNum] = transform;
			}
		}
	}
	
	public void delete(){
		animMatrices = null;
		boneMatrices = null;
		control 	 = null;
	}
}
