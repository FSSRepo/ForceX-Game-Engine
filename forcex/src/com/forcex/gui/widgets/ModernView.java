package com.forcex.gui.widgets;
import com.forcex.gui.*;
import com.forcex.utils.*;
import com.forcex.core.*;
import com.forcex.*;
import com.forcex.math.*;

public class ModernView extends View {
	private int vbo, ibo, t2_vbo, t_vbo;
	private short index_offset,index_count,segments,triangle_show;
	private Color triangle_color,start_color,end_color,power_color;
	float duration,ivk_lapse,hd_wait;
	boolean start_interpolation;
	boolean power_mode = false,invoking,hiding,multiple_power;
	Vector2f t1,t2,t3;
	
	public class Warp {
		public float lapse, velocity, rotation;
		public boolean inverse, flip_every;
		
		public Warp(float lapse,float velocity,boolean inverse){
			this.lapse = lapse;
			this.velocity = velocity;
			this.inverse = inverse;
		}
	}
	
	GL gl = FX.gl;
	Warp[] warps = new Warp[7];

	public ModernView(float radius,int segments){
		setWidth(radius);
		setHeight(radius);
		this.segments = (short)segments;
		index_count = (short)(segments * 6);
		start_color = new Color(0,225,0);
		end_color = new Color(255,0,0);
		triangle_color = new Color(255,0,0);
		setApplyAspectRatio(true);
		warps[0]= new Warp(0.5f,90f,true);
		warps[1]= new Warp(0.6f,120f,false);
		warps[2]= new Warp(0.2f,150f,true);
		warps[3]= new Warp(0.7f,180f,true);
		warps[4]= new Warp(0.4f,120f,true);
		warps[5]= new Warp(0.1f,360f,false);
		warps[6]= new Warp(0.999f,0f,true);
		power_color = new Color(240,210,8,255);
	}

	@Override
	public void onCreate(Drawer drawer) {
		float[] vertices = new float[segments * 2 * 7];
		float step_rate = Maths.PI_2 / segments;
		int vertexOffset = 0;
		float angle = 0;
		t_vbo = drawer.createBuffer(new float[]{
			2f,1.2f,0,0,1,1,1,
			-2f,1.2f,0,0,1,1,1,
			0.0f,-2f,0,0,1,1,1,
			2f,1.2f,0,0,1,1,1
		},false,false);
		t2_vbo = drawer.createBuffer(new float[]{
									 0.2f,0.13f,0,0,1,1,1,
									 -0.2f,0.13f,0,0,1,1,1,
									 0.0f,-0.2f,0,0,1,1,1
								 },false,false);
		for(short i = 0;i < segments;i++){
			vertices[vertexOffset] = Maths.sin(angle);
			vertices[vertexOffset + 1] = Maths.cos(angle);
			vertices[vertexOffset + 2] = 0;
			vertices[vertexOffset + 3] = 0;
			vertices[vertexOffset + 4] = 1;
			vertices[vertexOffset + 5] = 1;
			vertices[vertexOffset + 6] = 1;
			vertexOffset += 7;
			vertices[vertexOffset] = Maths.sin(angle) * 0.9f;
			vertices[vertexOffset + 1] = Maths.cos(angle) * 0.9f;
			vertices[vertexOffset + 2] = 0;
			vertices[vertexOffset + 3] = 0;
			vertices[vertexOffset + 4] = 1;
			vertices[vertexOffset + 5] = 1;
			vertices[vertexOffset + 6] = 1;
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
		t1 = new Vector2f(1.5f,0.9f);
		t2 = new Vector2f(-1.5f,0.9f);
		t3 = new Vector2f(0,-1.5f);
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
	
	public void setTriangleColor(int r,int g,int b){
		triangle_color.set(r,g,b);
	}
	
	public void flipColor(){
		start_color.set(end_color);
	}
	
	public void setStartColor(int r,int g,int b){
		start_color.set(r,g,b);
	}
	
	public void setEndColor(int r,int g,int b){
		end_color.set(r,g,b);
	}
	
	public boolean compare(Color color){
		return end_color.equals(color);
	}
	
	public boolean hasColorAnimation(){
		return start_interpolation;
	}
	
	public void setInterpolation(float duration){
		if(start_interpolation)return;
		this.duration = duration;
		start_interpolation  = true;
	}
	
	public void invoke(float timelapse){
		if(invoking)return;
		ivk_lapse = timelapse;
		invoking = true;
		hiding = false;
	}
	
	public void hide(float timelapse){
		if(hiding)return;
		hd_wait = timelapse;
		hiding = true;
		invoking = false;
	}
	
	private void setWrapper(float progress){
		triangle_show = (short)((progress * 0.01f) * index_count);
		if(progress > 0){
			triangle_show += 6 - (triangle_show % 6);
		}
	}
	
	Color anim_color = new Color();
	float time = 0.0f,time1 = 0.0f,time2=0.0f,time3=0.0f,porcent_color = 1.0f;
	boolean init_hide = false;
	float circle_scale = 1.0f;
	
	public void setCircleScale(float scale){
		circle_scale = scale;
	}
	
	public void setVelocity(int idx,float velocity){
		warps[idx].velocity = velocity;
	}
	
	public void setFlipRotation(int idx,boolean inverse){
		warps[idx].inverse = inverse;
	}
	
	public void usePower(){
		if(power_mode)return;
		power_mode = true;
		time1 = 0.0f;
	}
	
	public void setTransparency(float porcent){
		triangle_color.a = (short)(255.0f * porcent);
		anim_color.a = (short)(255.0f * porcent);
		power_color.a = (short)(255.0f * porcent);
	}
	
	public Warp getWarp(int index){
		return warps[index];
	}
	
	public void indefinedHide(){
		hiding = false;
		setTransparency(1.0f);
	}
	
	public void multiplePower(boolean z){
		multiple_power = z;
	}
	
	public void setWaitHide(float timelapse){
		hd_wait = timelapse;
	}
	
	public boolean isInvoking(){
		return invoking;
	}
	
	public boolean isHiding(){
		return hiding;
	}
	
	public boolean isShowing(){
		return anim_color.a >= 255;
	}
	
	boolean wave_effect = false, wave_expand = false;
	
	public void setWaveEffect(boolean z){
		wave_effect = z;
		if(!z)circle_scale = 1.0f;
	}
	
	@Override
	public void onDraw(Drawer drawer) {
		gl.glLineWidth(2f);
		drawer.setScale(extent.x,extent.y);
		drawer.freeRender(t_vbo,local,triangle_color,-1);
		gl.glDrawArrays(GL.GL_LINE_STRIP,0,4);
		drawer.setScale(extent.x*1.1f,extent.y*1.1f);
		drawer.freeRender(t_vbo,local,triangle_color,-1);
		gl.glDrawArrays(GL.GL_LINE_STRIP,0,4);
		gl.glLineWidth(1f);
		if(start_interpolation){
			time += FX.gpu.getDeltaTime();
			if(time > duration){
				duration = 0;
				start_interpolation = false;
				porcent_color = 1.0f;
				time = 0.0f;
			}else{
				porcent_color = (time / duration);
			}
		}
		if(power_mode) {
			time1 += FX.gpu.getDeltaTime();
			float porcent = 1.0f - (time1  / 0.5f);
			drawTriangle(drawer,45,t1,porcent);
			drawTriangle(drawer,-45,t2,porcent);
			drawTriangle(drawer,180,t3,porcent);
			if(time1 > 0.5f){
				power_mode = multiple_power;
				time1 = 0.0f;
			}
		}
		if(invoking && !hiding){
			time2 += FX.gpu.getDeltaTime();
			setTransparency(time2 / ivk_lapse);
			if(time2 > ivk_lapse){
				invoking = false;
				time2 = 0.0f;
			}
		}
		if (hiding && !invoking) {
			time2 += FX.gpu.getDeltaTime();
			if(init_hide){
				setTransparency(1.0f - time2);
				if(time2 > 1f){
					hiding = false;
					time2 = 0.0f;
					init_hide = false;
				}
			}else{
				init_hide = time2 > hd_wait;
				if(init_hide)time2 = 0;
			}
		}
		if (wave_effect) {
			time3 += FX.gpu.getDeltaTime();
			if(time3 > 0.5f){
				wave_expand = !wave_expand;
				time3 = 0.0f;
			}
			circle_scale = 1.0f + (wave_expand ? time3 / 0.5f : (1 - (time3 / 0.5f)))*0.4f;
		}
		for(int i = 0;i < 7;i++) {
			Warp wrap = warps[i];
			setWrapper(100f * wrap.lapse);
			drawer.setTransform(wrap.rotation,extent.x * ((i + 1) / 7f) * circle_scale,extent.y * ((i + 1) / 7f) * circle_scale);
			drawer.freeRender(vbo,local,anim_color.mix(start_color,end_color,porcent_color),-1);
			gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,ibo);
			gl.glDrawElements(GL.GL_TRIANGLES,triangle_show);
			wrap.rotation += FX.gpu.getDeltaTime() * wrap.velocity * (wrap.inverse ? -1f : 1f);
			if(wrap.flip_every && Maths.abs(wrap.rotation) > 360f){
				wrap.inverse = !wrap.inverse;
			}
			wrap.rotation %= 360f;
		}
	}
	
	public void drawTriangle(Drawer drawer, float rot,Vector2f v,float porcent){
		drawer.setTransform(rot,extent.x,extent.y);
		if(anim_color.a >= 250){
			power_color.a = (short)(255.0f * porcent);
		}
		drawer.freeRender(t2_vbo,local.add(v.mult(extent.x,extent.y).multLocal(porcent)),power_color,-1);
		gl.glDrawArrays(GL.GL_TRIANGLES,0,3);
	}

	@Override
	public void onDestroy() {
		setVisibility(INVISIBLE);
		gl.glDeleteBuffer(vbo);
		gl.glDeleteBuffer(ibo);
		end_color = null;
		start_color = null;
		anim_color = null;
		triangle_color = null;
	}
}
