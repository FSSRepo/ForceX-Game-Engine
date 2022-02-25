package com.forcex.io;
import com.forcex.utils.*;
import com.forcex.core.*;
import com.forcex.*;
import com.forcex.core.gpu.*;
import com.forcex.app.threading.*;
import java.io.*;

public class VideoClip
{
	private boolean play = false,stop = false,loop = false, first_frame = true;
	private int texture = -1,blockSize;
	private float duration,frame_time,time = 0f,speed = 1;
	private short width,height,numFrames,OldframeOffset;
	private byte framerate;
	private GL gl;
	private byte[] next_frame;
	private String video;
	private OnVideoClipListener listener;
	
	public VideoClip(String video) {
		this.video = video;
		FileStreamReader os = new FileStreamReader(video);
		width = os.readShort();
		height = os.readShort();
		blockSize = os.readInt();
		framerate = os.readByte();
		numFrames = os.readShort();
		frame_time = 1.0f / framerate;
		duration = numFrames * frame_time;
		os.clear();
		os = null;
		gl = FX.gl;
		next_frame = new byte[blockSize];
	}
	
	public int init(){
		texture = Texture.genTextureWhite();
		return texture;
	}
	
	public void play(){
		play = true;
		if(listener != null){
			listener.play();
		}
	}
	
	public void pause(){
		play = false;
		if(listener != null){
			listener.pause();
		}
	}
	
	public void stop(){
		stop = true;
	}
	
	public void setSpeed(float spd){
		speed = spd;
	}
	
	public void setLoop(boolean z){
		loop = z;
	}
	
	public Task getTask(){
		return new Task(){
			@Override
			public boolean execute() {
				short frameOffset = (short)(time / frame_time);
				if(frameOffset == 0 && first_frame){
					loadNextFrame(frameOffset);
					updateGLMemory();
					loadNextFrame((short)(frameOffset + 1));
					first_frame = false;
				}else if((frameOffset - OldframeOffset) > 0 && frameOffset < (numFrames - 1)){
					updateGLMemory();
					loadNextFrame((short)(frameOffset + 1));
				}
				OldframeOffset = frameOffset;
				return stop;
			}
		};
	}
	
	public void update(){
		if(time + FX.gpu.getDeltaTime() >= duration+0.02f){
			if(loop){
				OldframeOffset = 0;
				first_frame = true;
				time = 0;
			}else{
				time = duration;
				if(listener != null){
					listener.end();
				}
				stop = true;
			}
		}else{
			time += play && !stop && !first_frame ? FX.gpu.getDeltaTime() * speed : 0.0f;
		}
	}
	
	private void updateGLMemory(){
		FX.gpu.queueTask(new Task(){
				@Override
				public boolean execute() {
					gl.glBindTexture(GL.GL_TEXTURE_2D,texture);
					gl.glTexImage2D(GL.GL_TEXTURE_2D,width,height,GL.GL_TEXTURE_ETC1,next_frame);
					gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
					gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
					gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
					gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
					return true;
				}
		});
		FX.gpu.waitEmptyQueue();
	}
	
	private void loadNextFrame(short frameOffset) {
		try
		{
			FileInputStream os = new FileInputStream(video);
			os.skip(11 + (frameOffset * blockSize)); // Header + FrameOffset
			os.read(next_frame);
			os.close();
			os = null;
		}
		catch (Exception e)
		{
			Logger.log("ERROR: Video Streaming: "+e.toString());
		}
	}
	
	public static void convertFromSequence(String path,int framerate,int numFrames,boolean dxt) {
		FileStreamWriter os = new FileStreamWriter(path+"result.fvp");
		int blck_sz = 0;
		String append = "";
		for(int i = 1;i <= numFrames;i++){
			if(i < 10){
				append = "000";
			}else if(i >= 10 && i < 100){
				append = "00";
			}else if(i >= 100 && i < 1000){
				append = "0";
			}else if(i >= 1000){
				append = "";
			}
			Image img = new Image(path + append + i + ".png");
			if(i == 1){
				blck_sz = (img.width * img.height) / 2;
				os.writeShort(img.width); // ancho
				os.writeShort(img.height); // alto
				os.writeInt(blck_sz); // block size
				os.writeByte(framerate); // taza de frames
				os.writeShort(numFrames); // numero de Frames
			}
			if(!dxt){
				byte[] buffer = CoreJni.etc1compress(img.getRGBAImage(),img.width,img.height,CoreJni.ETC1_LOW_QUALITY);
				os.writeByteArray(buffer);
				buffer = null;
			}else{
				byte[] buffer = CoreJni.dxtcompress(img.getRGBAImage(),img.width,img.height,CoreJni.DXTC_1 | CoreJni.DXTC_RANGE_FIT);
				os.writeByteArray(buffer);
				buffer = null;
			}
			img.clear();
			img = null;
		}
		os.finish();
	}
	
	public int getTexture(){
		return texture;
	}
	
	public void setOnVideoClipListener(OnVideoClipListener listener){
		this.listener = listener;
	}
	
	public void delete(){
		gl.glDeleteTexture(texture);
		next_frame = null;
		video = null;
	}
	
	public static interface OnVideoClipListener{
		void end();
		void pause();
		void play();
	}
}
