package com.forcex.utils;
import com.forcex.core.*;
import com.forcex.math.*;
import com.forcex.gfx3d.*;
import com.forcex.*;

public class GameUtils
{
	public static Vector2f getTouchNormalized(float x, float y) {
		float ny = (2 * (y / FX.gpu.getHeight()) - 1) * (FX.gpu.isOpenGLES() ? -1 : 1);
        return new Vector2f(2 * (x /  FX.gpu.getWidth()) - 1, ny);
    }
	
	public static boolean testRect(float x,float y,Vector2f position,float width,float height){
		return 
			x >= (position.x - width) && 
			x <= (position.x + width) &&    
			y >= (position.y - height) &&     
			y <= (position.y + height);
	}
	
	public static boolean isInRange(Vector3f pos1,Vector3f pos2,float range) {
		float dist = pos1.distance2(pos2);
        return (dist < range * range);
    }
	
	public static boolean isInRange(Vector2f pos1,Vector2f pos2,float range) {
        return (pos1.sub(pos2).length() < range);
    }
	
	public static float distance(Vector2f pos1,Vector2f pos2) {
        return pos1.sub(pos2).length();
    }
	
	public static float distance(Vector3f pos1,Vector3f pos2) {
        return pos1.sub(pos2).length();
    }
	
	public static Vector2f directionToPoint(Vector2f position,Vector2f target){
		return target.sub(position).normalize();
	}
	
	public static Vector3f directionToPoint(Vector3f position,Vector3f target){
		return target.sub(position).normalize();
	}
	
	public static Vector3f getLocalPosition(Vector3f center,Vector3f point,float dist){
		Vector3f dir = directionToPoint(center,point);
		return new Vector3f(
			(dir.x * dist) + center.x,
			(dir.y * dist) + center.y,
			(dir.z * dist) + center.z);
	}
}
