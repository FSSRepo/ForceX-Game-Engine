package com.forcex.gfx3d.effect;
import com.forcex.math.*;
import com.forcex.utils.*;

public class Light
{
	private Vector3f position;
	private Color color;
	private Color ambient;
	private Matrix4f projectionMatrix = null,lookAt = null,projView = null;
	public Vector3f target;
	
	public Light(){
		position = new Vector3f(5f,5f,5f);
		color = new Color(Color.WHITE);
		ambient = new Color(45,45,45);
	}
	
	public void useInShadow(){
		projectionMatrix = Matrix4f.orthogonal(-20.0f, 20.0f, -20.0f, 20.0f, 0.1f, 300.0f);
		lookAt = new Matrix4f();
		projView = new Matrix4f();
		target = new Vector3f();
	}
	
	public void setPosition(Vector3f position){
		this.position = position;
	}
	
	public void setPosition(float x,float y,float z){
		position.set(x,y,z);
	}
	
	public Vector3f getPosition(){
		return position;
	}

	public void setColor(Color color){
		color.set(color);
	}
	
	public void setColor(int r,int g,int b){
		color.set(r,g,b);
	}
	
	public Color getColor(){
		return color;
	}

	public void setAmbient(Color ambient){
		ambient.set(ambient);
	}
	public void setAmbientColor(int r,int g,int b){
		ambient.set(r,g,b);
	}
	public Color getAmbient(){
		return ambient;
	}
	
	public void setFocalLength(float len){
		projectionMatrix.setOrthogonal(-len,len,-len,len,0.1f,400f);
	}
	
	public void updateLookAt() {
		Vector3f direction = target.sub(position).normalize();
		Vector3f up = new Vector3f(0,1,0);
		float dot = direction.dot(up); // up and direction must ALWAYS be orthonormal vectors
		if (Math.abs(dot - 1) < 0.000000001f) {
			// Collinear
			up.set(direction).multLocal(-1);
		} else if (Math.abs(dot + 1) < 0.000000001f) {
			// Collinear opposite
			up.set(direction);
		}
		Vector3f right = direction.cross(up).normalize();
		right.cross(direction,up).normalize();
        lookAt.setlookAt(position, direction, up);
		projectionMatrix.mult(projView,lookAt);
    }
	
	public Matrix4f getProjView(){
		return projView;
	}
}
