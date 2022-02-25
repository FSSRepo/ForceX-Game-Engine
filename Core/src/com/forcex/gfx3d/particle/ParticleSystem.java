package com.forcex.gfx3d.particle;
import com.forcex.math.*;
import com.forcex.*;
import com.forcex.utils.*;

public class ParticleSystem
{
	float lifeTime;
	float particlesPerSecond;
	float speed;
	
	public ParticleSystem(float lifeTime,float pss,float speed){
		this.lifeTime = lifeTime;
		particlesPerSecond = pss;
		this.speed = speed;
	}
	
	public void generate(ParticleRenderer render,float delta,float x,float y,float z){
		int count = (int)Math.floor(particlesPerSecond * (delta*1.5));
		Vector3f center = new Vector3f(x,y,z);
		for(int i = 0;i < count;i++){
			float dx = (float)Math.random() * 2f - 1f;
			float dz = (float)Math.random() * 2f - 1f;
			Vector3f vel = new Vector3f(dx,1,dz).normalize();
			vel.multLocal(generateValue(speed,0.3f));
			render.addParticle(new Particle(center,vel,Color.BLACK,generateValue(1f,0.3f),generateValue(lifeTime,0.4f)));
		}
	}
	
	private float generateValue(float average, float errorMargin) {
		float offset = (float)(Math.random() - 0.5f) * 2f * errorMargin;
		return average + offset;
	}
}
