package com.forcex.anim;
import com.forcex.math.*;
import java.util.*;

public class SkeletonNode
{
	public Matrix4f InverseBoneMatrix;
	public Matrix4f modelMatrix;
	public short boneID = -1;
	public short boneNum;
	public String name;
	public ArrayList<SkeletonNode> children = new ArrayList<>();
	public SkeletonNode parent;
	
	public SkeletonNode(){
		name = "";
		boneID = -1;
	}
	
	public void addChild(SkeletonNode frame){
		frame.parent = this;
		children.add(frame);
	}
	public SkeletonNode findByBoneId(short id){
		if(boneID == id){
			return this;
		}
		for(SkeletonNode n : children){
			SkeletonNode r = n.findByBoneId(id);
			if(r != null){
				return r;
			}
		}
		return null;
	}
}
