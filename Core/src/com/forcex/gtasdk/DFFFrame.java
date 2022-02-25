package com.forcex.gtasdk;
import java.util.*;
import com.forcex.math.*;
import com.forcex.utils.*;

public class DFFFrame
{
	public ArrayList<DFFFrame> childs = new ArrayList<>();
	public DFFHanim hanim;
	public String name = "";
	public int parentIdx;
	public int flags;
	public short geoAttach = -1;
	public Matrix3f rotation = new Matrix3f();
	public Vector3f position = new Vector3f(0,0,0);
	public DFFFrame parent;
	public Matrix4f ltm;
	public boolean updateLTM = true;
	public short model_id = -1;
	
	public void invalidateLTM(){
		updateLTM = true;
		for(int i = 0;i < childs.size();i++){
			childs.get(i).invalidateLTM();
		}
	}
	
	public boolean hasHanim(){
		return hanim != null;
	}
	
	public Matrix4f getModelMatrix(){
		return new Matrix4f(rotation).setLocation(position);
	}
	
	public static Matrix4f GetLocalModelMatrix(DFFFrame frame){
		if(frame.ltm == null){
			frame.ltm = new Matrix4f();
			if(frame.parent != null){
				 GetLocalModelMatrix(frame.parent).mult(frame.ltm,frame.getModelMatrix());
			}else{
				frame.ltm = frame.getModelMatrix();
			}
			frame.updateLTM = false;
			return frame.ltm;
		}
		if(frame.updateLTM){
			if(frame.parent != null){
				GetLocalModelMatrix(frame.parent).mult(frame.ltm,frame.getModelMatrix());
			}else{
				frame.ltm.set(frame.getModelMatrix());
			}
			frame.updateLTM = false;
		}
		return frame.ltm;
	}
	
	public static int GetLevel(DFFFrame frame){
		int level = 0;
		DFFFrame par = frame.parent;
		while(par != null){
			level++;
			par = par.parent;
		}
		return level;
	}
}
