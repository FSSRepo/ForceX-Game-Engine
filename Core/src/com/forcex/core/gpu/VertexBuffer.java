package com.forcex.core.gpu;
import java.util.*;
import java.nio.*;
import com.forcex.*;
import com.forcex.core.*;
import com.forcex.utils.*;

public class VertexBuffer{
	private boolean dirty = true;
	private boolean isStatic = true;
	private int buffer = -1;
	public VertexData vdata;
	public VertexInfo info;
	private GL gl = FX.gl;
	
	public VertexBuffer(
		VertexData data,
		VertexInfo info,
		boolean isStatic){
			
		this.isStatic = isStatic;
		this.vdata = data;
		this.info = info;
	}
	
	public void reset(){
		dirty = true;
	}
	
	public VertexBuffer clone(){
		VertexBuffer buf = new VertexBuffer(null,info.clone(),isStatic);
		buf.buffer = buffer;
		buf.dirty = false;
		return buf;
	}
	
	public void update(){
		if(dirty){
			FloatBuffer data = vdata.convert(info);
			if(buffer == -1){ // init
				buffer = gl.glGenBuffer();
				gl.glBindBuffer(GL.GL_ARRAY_BUFFER,buffer);
				gl.glBufferData(GL.GL_ARRAY_BUFFER,info.dataSize,data,!isStatic ? GL.GL_DYNAMIC_DRAW: GL.GL_STATIC_DRAW);
				gl.glBindBuffer(GL.GL_ARRAY_BUFFER,0);
				dirty = false;
				data = null;
				return;
			}
			if(!isStatic){
				gl.glBindBuffer(GL.GL_ARRAY_BUFFER,buffer);
				gl.glBufferSubData(GL.GL_ARRAY_BUFFER,info.dataSize,data);
				gl.glBindBuffer(GL.GL_ARRAY_BUFFER,0);
				data = null;
			}else{
				gl.glDeleteBuffer(buffer);
				buffer = FX.gl.glGenBuffer();
				gl.glBindBuffer(GL.GL_ARRAY_BUFFER,buffer);
				gl.glBufferData(GL.GL_ARRAY_BUFFER,info.dataSize,data,GL.GL_STATIC_DRAW);
				gl.glBindBuffer(GL.GL_ARRAY_BUFFER,0);
				data = null;
			}
			dirty = false;
		}
	}
	
	public void bind(){
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, buffer);
	}
	
	public void EnableVertexAttrib(int indx,int dimen,int offset){
		gl.glVertexAttribPointer(indx,dimen,GL.GL_FLOAT,false,info.stride,offset);
		gl.glEnableVertexAttribArray(indx);
	}
	
	public int getBuffer(){
		return buffer;
	}
	
	public void delete(){
		if(buffer != -1){
			FX.gl.glDeleteBuffer(buffer);
		}
		vdata.delete();
		vdata = null;
		info = null;
	}
}
