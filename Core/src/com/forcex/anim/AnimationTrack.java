package com.forcex.anim;
import java.util.*;
import com.forcex.utils.*;

public class AnimationTrack
{
	private ArrayList<Track> frames = new ArrayList<>();
	private float track_timer = 0.0f;
	private float duration = 0f;
	private boolean repeat = false;
	
	public static class Track {
		float time;
		Object object;
		
		public Track(float time,Object object){
			this.time = time;
			this.object = object;
		}
	}
	
	public static interface onInterpolateListener{
		public void interpolate(Object prv,Object nxt,float p);
	}
	
	onInterpolateListener listener;
	
	public void setInterpolation(onInterpolateListener interpolation){
		listener = interpolation;
	}
	
	public void add(float time,Object obj){
		frames.add(new Track(time,obj));
	}
	
	public void updateDuration(){
		duration = 0;
		for(Track f : frames){
			if(f.time > duration){
				duration = f.time;
			}
		}
	}
	
	private Track getPrevius(){
		for(short f = 0;f < frames.size();f++){
			if (track_timer <= frames.get(f).time) {
				if (f == 0) { // es el primer frame?
					if (frames.size() != 1) { // retornar el ultimo frame
						return frames.get(frames.size() - 1);
					} else { // retornar el primer frame
						return frames.get(f);
					}
				} else { // retornar el frame anterior
					return frames.get(f - 1);
				}
			}
		} //retornar el ultimo frame
		return frames.get(frames.size() - 1);
	}
	
	private Track getNext(){
		for(short f = 0;f < frames.size();f++){
			if (track_timer <= frames.get(f).time) {
				return frames.get(f);
			}
		} //retornar el ultimo frame
		return frames.get(frames.size() - 1);
	}
	
	public void stop(){
		listener =  null;
		track_timer = 0;
		frames.clear();
	}
	
	public void setRepeat(boolean z){
		repeat = z;
	}
	
	public float getTackTime(){
		return track_timer;
	}
	
	public float getDuration(){
		return duration;
	}
	
	public void update(float delta){
		if(listener != null){
			if(repeat){
				track_timer %= duration;
			}else if(track_timer > duration){
				return;
			}
			Track prv = getPrevius();
			Track nxt = getNext();
			float totalTime = nxt.time - prv.time;
			float currentTime = track_timer - prv.time;
			listener.interpolate(prv.object,nxt.object,currentTime / totalTime);
			track_timer += delta;
		}
	}
}
