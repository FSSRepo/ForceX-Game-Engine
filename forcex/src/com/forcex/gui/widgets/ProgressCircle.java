package com.forcex.gui.widgets;
import com.forcex.utils.*;
import com.forcex.gui.*;
import com.forcex.math.*;
import com.forcex.core.*;
import com.forcex.*;

public class ProgressCircle extends View {
	int vbo,ibo;
	short index_offset,index_count,segments,triangle_show;
	Color background_color,progress_color;
	GL gl = FX.gl;
	float progress,indeterminate_prog,radius_inner,speed_cycle = 80;
	boolean indeterminate;

	public ProgressCircle(float radius,float radius_inner,int segments){
		setWidth(radius);
		setHeight(radius);
		this.segments = (short)segments;
		index_count = (short)(segments * 6);
		background_color = new Color(225,225,225);
		progress_color = new Color(50,170,20);
		indeterminate_prog = 20.0f;
		this.radius_inner = radius_inner;
	}

	@Override
	public void onCreate(Drawer drawer)
	{
		float[] vertices = new float[segments * 2 * 7];
		float step_rate = Maths.PI_2 / segments;
		int vertexOffset = 0;
		float angle = 0;
		for(short i = 0;i < segments;i++){
			vertices[vertexOffset] = Maths.sin(angle);
			vertices[vertexOffset + 1] = Maths.cos(angle);
			vertices[vertexOffset + 2] = 0;
			vertices[vertexOffset + 3] = 0;
			vertices[vertexOffset + 4] = 1f;
			vertices[vertexOffset + 5] = 1f;
			vertices[vertexOffset + 6] = 1f;
			vertexOffset += 7;
			vertices[vertexOffset] = Maths.sin(angle) * radius_inner;
			vertices[vertexOffset + 1] = Maths.cos(angle) * radius_inner;
			vertices[vertexOffset + 2] = 0;
			vertices[vertexOffset + 3] = 0;
			vertices[vertexOffset + 4] = 1f;
			vertices[vertexOffset + 5] = 1f;
			vertices[vertexOffset + 6] = 1f;
			vertexOffset += 7;
			angle += step_rate;
		}
		short[] indices = new short[index_count];
		for (int i = 2; i <= segments; i++) {
			int a = i*2 - 3 - 1;
			int b = i*2 - 2 - 1;
			int c = i*2 - 1 - 1;
			int d = i*2 - 0 - 1;
			addQuad(indices,a,b,c,d);
		}		
		int a = segments*2 - 1 - 1; // ... connect last segment
		int b = segments*2 - 0 - 1;
		int c = 0;
		int d = 1;
		addQuad(indices,a,b,c,d);
		vbo = drawer.createBuffer(vertices,false,true);
		ibo = drawer.createBuffer(indices,true,false);
		vertices = null;
		indices = null;
	}

	private void addQuad(short[] indices,int ul, int bl, int ur, int br) {
		addFace(indices,ur,br,ul);
		addFace(indices,br,bl,ul);
	}

	private void addFace(short[] indices,int a,int b,int c){
		indices[index_offset*3] = (short)a;
		indices[index_offset*3+1] = (short)b;
		indices[index_offset*3+2] = (short)c;
		index_offset++;
	}

	public void setSpeedPerCycle(float speed){
		speed_cycle = speed;
	}

	public void setProgressColor(int r,int g,int b){
		progress_color.set(r,g,b);
	}
	
	public void setBackgroundColor(Color color) {
		background_color.set(color);
	}
	
	public void setIndeterminate(boolean z){
		indeterminate = z;
		if(z){
			triangle_show = (short)((indeterminate_prog * 0.01f) * index_count);
			triangle_show += 6 - (triangle_show % 6);
		}else{
			setProgress(0);
		}
	}

	public void setProgress(float progress){
		if(indeterminate){
			this.indeterminate_prog = progress;
			return;
		}
		this.progress = progress;
		triangle_show = (short)((progress * 0.01f) * index_count);
		if(progress > 0){
			triangle_show += 6 - (triangle_show % 6);
		}
	}

	float rotation = 0;

	@Override
	public void onDraw(Drawer drawer) {
		drawer.setScale(extent.x,extent.y);
		drawer.freeRender(vbo,local,background_color,-1);
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ibo);
        gl.glDrawElements(GL.GL_TRIANGLES,index_count);
		if(indeterminate){
			drawer.setTransform(rotation,extent.x,extent.y);
			rotation += speed_cycle * FX.gpu.getDeltaTime();
		}
		drawer.freeRender(vbo,local,progress_color,-1);
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ibo);
		gl.glDrawElements(GL.GL_TRIANGLES,triangle_show);
	}

	@Override
	public void onDestroy() {
		setVisibility(INVISIBLE);
		gl.glDeleteBuffer(vbo);
		gl.glDeleteBuffer(ibo);
		background_color = null;
		progress_color = null;
	}
}
