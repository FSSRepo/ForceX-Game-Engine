package com.forcex.gfx3d.particle;
import com.forcex.math.*;
import com.forcex.utils.*;
import com.forcex.*;

public class Particle{
	
	public float lifetime;
	private float elapsedTime;
	public Vector3f position;
	public Vector3f velocity;
	public float scale;
	public Color color;
	public float liveFactor;
	private final float GRAVITY = -9.8f;
	public Vector2f[] offsets;
	
	public Particle(Vector3f position,Vector3f velocity,int color,float scale,float lifeTime){
		this.position = position;
		this.velocity = velocity;
		this.color = new Color(color);
		this.scale = scale;
		lifetime = lifeTime;
	}
	
	public boolean update(){
		float delta = FX.gpu.getDeltaTime();
		velocity.y += GRAVITY * delta;
		position.addLocal(velocity.mult(delta));
		elapsedTime += delta;
		liveFactor = (elapsedTime / lifetime);
		return (elapsedTime < lifetime);
	}
	
	public void updateTextureInfo(int numRows){
		int numStages = numRows * numRows;
		float progress = liveFactor * numStages;
		int index1 = (int) Math.floor(progress);
		int index2 = index1 < numStages - 1 ? index1 - 1 : index1;
		
	}
}
