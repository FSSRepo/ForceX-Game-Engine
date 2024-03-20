package com.forcex.gui.widgets;
import com.forcex.gui.*;
import com.forcex.math.*;
import com.forcex.*;
import com.forcex.utils.*;
import com.forcex.core.*;

public class FunctionGraph extends View {
	int vbo,ibo,vbo_cur = -1,ibo_cur;
	float interval,a,b,distance;
	Function func;
	float max = 0;
	Color fx_color,cur_color,tangent_color;
	int index_count;
	float cursorX = 0;
	float angle = 0;
	TextView tv_tangent;
	boolean calculateArea;
	
	public FunctionGraph(float width,float height,Function func,float a,float b,float interval){
		this.func = func;
		this.interval = interval;
		this.a = a;
		this.b = b;
		setWidth(width);
		setHeight(height);
		fx_color = new Color(210,234,34);
		cur_color = new Color(224,20,24);
		tangent_color = new Color(230,55,243);
		distance = Maths.abs(b - a);
	}

	@Override
	public void onCreate(Drawer drawer) {
		calculateMax();
		tv_tangent = new TextView(context.default_font);
		tv_tangent.setTextSize(0.04f);
		tv_tangent.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER_LEFT);
		if(calculateArea){
			tv_tangent.setText("Scanning");
		}
		int numSteps = (int)(distance / interval);
		float[] vertexs = new float[(numSteps + 1) * 7];
		int offset = 0,current_step = 0;
		for(float x = a;x <= b;x += interval){
			vertexs[offset] = (-extent.x) + (((float)current_step / numSteps) * extent.x * 2.0f);
			vertexs[offset + 1] = (-extent.y) + ((func.f(x) / max) * extent.y * 2f);
			vertexs[offset + 2] = 
			vertexs[offset + 3] = 0.0f;
			vertexs[offset + 4] = 1f;
			vertexs[offset + 5] = 1f;
			vertexs[offset + 6] = 1f;
			offset += 7;
			current_step++;
		}
		short[] indices = new short[(numSteps - 1) * 2];
		offset = 0;
		for(int i = 0;i < indices.length;i += 2){
			indices[i + 0] = (short)(offset);
			indices[i + 1] = (short)(offset + 1);
			offset++;
		}
		index_count = indices.length;
		vbo = drawer.createBuffer(vertexs,false,false);
		ibo = drawer.createBuffer(indices,true,false);
		indices = null;
		indices = new short[]{0,1};
		ibo_cur = drawer.createBuffer(indices,true,false);
	}
	
	public void integrate(){
		calculateArea = true;
		area = 0.0f;
		float n = 0.001f * interval;
		for(float x = a;x <= b;x += n){
			area += (func.f(x) * n);
		}
	}
	
	Vector2f rect_tangent = new Vector2f();
	byte anim_step = 3;
	
	@Override
	public void onDraw(Drawer drawer) {
		FX.gl.glLineWidth(2.0f);
		drawer.setScale(1,1);
		drawer.freeRender(vbo,local,fx_color,-1);
		FX.gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ibo);
        FX.gl.glDrawElements(GL.GL_LINES,index_count);
		stepCursor();
		drawer.freeRender(vbo_cur,local,cur_color,-1);
		FX.gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ibo_cur);
        FX.gl.glDrawElements(GL.GL_LINES,2);
		if(!calculateArea){
			drawer.setTransform(angle,extent.x*0.5f,extent.y*0.5f);
			drawer.renderLine(rect_tangent,tangent_color);
		}
		tv_tangent.local.set(local.x,local.y+(extent.y*0.5f));
		tv_tangent.onDraw(drawer);
		if(cursorX < 1f){
			cursorX += calculateArea ? 0.15f * FX.gpu.getDeltaTime() : 0.008f * FX.gpu.getDeltaTime();
		}
		if(calculateArea){
			if(cursorX > 1){
				anim_step--;
			}
			if(anim_step == 2){
				tv_tangent.setText(String.format("Area:%.4f",area));
			}
			calculateArea = anim_step > 0;
		}
		cursorX %= 1;
	}
	
	// calcular el punto maximo de la funcion
	private void calculateMax(){
		for(float x = a;x < b;x += interval){
			float y = func.f(x);
			if(max < y){
				max = y;
			}
		}
	}
	
	float area = 0;
	
	private void stepCursor(){
		float[] vertexs = new float[14];
		vertexs[0] = (-extent.x) + (cursorX * extent.x * 2.0f);
		vertexs[1] = (-extent.y);
		vertexs[2] = 
		vertexs[3] = 0.0f;
		vertexs[4] = 1f;
		vertexs[5] = 1f;
		vertexs[6] = 1f;
		float x = a + (distance * cursorX);
		float y = func.f(x);
		float m = derivate(x,y);
		angle = Maths.atan(m) * Maths.toDegrees;
		vertexs[7] = (-extent.x) + (cursorX * extent.x * 2.0f);
		vertexs[8] = (-extent.y) + ((y / max) * extent.y * 2f);
		vertexs[9] = 
		vertexs[10] = 0.0f;
		vertexs[11] = 1f;
		vertexs[12] = 1f;
		vertexs[13] = 1f;
		rect_tangent.set(vertexs[4] + local.x,vertexs[5] + local.y);
		if(vbo_cur == -1){
			vbo_cur = Drawer.createBuffer(vertexs,false,true);
		}else{
			Drawer.updateBuffer(vbo_cur,vertexs);
		}
		if(!calculateArea){
			tv_tangent.setText(String.format("f(%.2f)=%.2f\npendiente=%.2f\nangulo=%.2f",x,func.f(x),m,angle));
		}
		vertexs = null;
	}
	
	public void destroy() {
		setVisibility(INVISIBLE);
		FX.gl.glDeleteBuffer(vbo);
		FX.gl.glDeleteBuffer(vbo_cur);
		FX.gl.glDeleteBuffer(ibo);
		FX.gl.glDeleteBuffer(ibo_cur);
	}
	
	
	final float h = 0.00001f;
	private float derivate(float x,float y){
		return (func.f(x + h) - y) / h;
	}
	
	public static interface Function{
		float f(float x); // -> y axis
	} 
	
}
