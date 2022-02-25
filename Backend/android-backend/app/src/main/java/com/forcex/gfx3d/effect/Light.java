package com.forcex.gfx3d.effect;
import com.forcex.math.*;
import com.forcex.utils.*;

public class Light
{
	private Vector3f position;
	private Color color;
	private Color ambient;
	private Matrix4f projectionMatrix = null,lookAt = null,projView = null;
	
	public Light(){
		position = new Vector3f(0,13f,13f);
		color = new Color(Color.WHITE);
		ambient = new Color(45,45,45);
	}
	
	public void useInShadow(){
		projectionMatrix = Matrix4f.orthogonal(-20.0f, 20.0f, -20.0f, 20.0f, 0.1f, 300.0f);
		lookAt = new Matrix4f();
		projView = new Matrix4f();
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
	
	public void updateLookAt() {
		Vector3f direction = position.negative().normalize();
		Vector3f right = direction.cross(Vector3f.getUpFromDirection(direction,false)).normalize();
		Vector3f up = right.cross(direction).normalize();
        lookAt.setlookAt(position, direction, up);
		projectionMatrix.mult(projView,lookAt);
    }
	
	public Matrix4f getProjView(){
		return projView;
	}
}
