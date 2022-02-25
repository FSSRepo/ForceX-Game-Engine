package com.forcex.core.gpu;
import java.nio.*;
import com.forcex.*;
import com.forcex.core.*;
import com.forcex.utils.*;

public class IndexBuffer{
	int buffer = -1;
	boolean dirty = true;
	GL gl = FX.gl;
	
	public IndexBuffer clone(){
		IndexBuffer buf = new IndexBuffer();
		buf.buffer = buffer;
		buf.dirty = false;
		return buf;
	}
	
	public void reset(){
		dirty = true;
		delete();
	}
	
	public void update(short[] indices){
		if(dirty){
			ShortBuffer sb = BufferUtils.createShortBuffer(indices);
			buffer = FX.gl.glGenBuffer();
			gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,buffer);
			gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER,indices.length*2,sb,GL.GL_STATIC_DRAW);
			gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,0);
			dirty = false;
		}
	}
	
	public void bind(int primitiveType,int count){
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,buffer);
		gl.glDrawElements(primitiveType,count);
	}
	
	public void delete(){
		gl.glDeleteBuffer(buffer);
		buffer = -1;
	}
}
