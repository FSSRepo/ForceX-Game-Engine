package com.forcex.gui.widgets;
import com.forcex.gui.*;
import com.forcex.utils.*;
import com.forcex.math.*;
import com.forcex.app.*;
import com.forcex.*;
import com.forcex.core.*;

public class ProgressBar extends View {
    private float end_time,half_time,seek_width;
	private float duration = 5f;
	Color[] colors;
	boolean indeterminate,use_seek;
	private Color edge_color;
	private Color pivot_seek;
	private float progress_status = 0.0f;
	onSeekListener listener;
	protected int 
		texture_background = -1,
		texture_background_2 = -1,
		progress_buffer = -1;
	protected boolean use2ndBackgroundTexture = false;
	float all_size = 0,anim_width = 0,porcent_indeterminate = 0.25f;
	
    public ProgressBar(float width, float height) {
		setWidth(width);
		setHeight(height);
        seek_width = width * 0.04f;
        setProgress(0);
		colors = new Color[]{
			new Color(230,230,230,255),
			new Color(6, 176, 37,230)
		};
		pivot_seek = new Color(120,120,120);
		edge_color = new Color(170,170,170);
	}

	public void useSeekBar(boolean z){
		use_seek = z;
		setIndeterminate(false);
	}

	public void setBackGroundColor(int color){
		colors[0].set(color);
	}

	public void setOnSeekListener(onSeekListener listener){
		this.listener = listener;
	}

	public void setColor(int background,int bar){
		colors[0].set(background);
		colors[1].set(bar);
	}

	public void setColor(Color background,Color bar){
		colors[0] = background;
		colors[1] = bar;
	}
	
    public void setProgress(float porcent) {
		if(indeterminate){
			return;
		}
		progress_status = Maths.clamp(porcent,0,100.0f);
		if(!use2ndBackgroundTexture){
			float p = (0.01f * progress_status) * 2f - 1f;
			if(p <= 1){
				float[] vertexs = new float[]{
					-1,1,0,0,
					-1,-1,0,0,
					p,1,0,0,
					p,-1,0,0
				};
				if(progress_buffer == -1){
					progress_buffer = Drawer.genBuffer(vertexs,false,true);
				}else{
					Drawer.updateBuffer(progress_buffer,vertexs);
				}
			}
		}
    }
	
	public float getProgress(){
		return progress_status;
	}

	@Override
	protected boolean testTouch(float x, float y) {
		return isVisible() && GameUtils.testRect(x,y,local,extent.x,extent.y);
	}

	boolean seeking = false;

	@Override
	public void onTouch(float x, float y, byte type) {
		if(type == EventType.TOUCH_PRESSED){
			if(use_seek){
				seeking = true;
			}
		}
		if(seeking && type == EventType.TOUCH_DRAGGING){
			setProgress(((((x - local.x) * (1f / extent.x)) * 0.5f) + 0.5f) * 100f);
			if(listener != null){
				listener.seek(getId(),progress_status);
			}
		}
		if(type == EventType.TOUCH_DROPPED){
			seeking = false;
			if(listener != null){
				listener.finish(progress_status);
			}
		}
	}

	float time = 0;
	boolean rewind = true;
	float[] anims;

	public void setIndeterminate(boolean z){
		if(z != indeterminate){
			time = 0;
			rewind = true;
		}
		indeterminate = z;
	}
	
	@Override
	public void onDraw(Drawer drawer) {
		if(indeterminate && rewind){
			float[] vertexs = new float[]{
				-1,1,0,0,
				-1,-1,0,0,
				1,1,0,0,
				1,-1,0,0
			};
			Drawer.updateBuffer(progress_buffer,vertexs);
			all_size = width * porcent_indeterminate;
			half_time = porcent_indeterminate * 0.5f;
			end_time = 1.0f - half_time;
		}
		drawer.setScale(extent.x,extent.y);
		drawer.renderQuad(local,colors[0],texture_background);
		if(!indeterminate && !use2ndBackgroundTexture){
			drawer.freeRender(progress_buffer,local,colors[1],-1);
			FX.gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
		}else if(use2ndBackgroundTexture){
			drawer.renderQuad(local,colors[1],texture_background_2);
		}else{
			float porcent = (time / duration);
			float start = (local.x - extent.x);
			float animx =  start + (porcent * ((local.x + extent.x) - start));
			if(porcent < half_time){
				anim_width = (porcent / half_time) * all_size;
			}
			if(porcent > end_time){
				anim_width = (1 - ((porcent - end_time) / half_time)) * all_size;
			}
			drawer.setScale(anim_width,extent.y);
			drawer.renderQuad(new Vector2f(animx,local.y), colors[1],-1);
			time += FX.gpu.getDeltaTime();
			time %= 5f;
		}
		drawer.setScale(extent.x,extent.y);
		drawer.renderLineQuad(local,edge_color);
		if(use_seek){
			drawer.setScale(seek_width,extent.y);
			drawer.renderQuad(local.add((-getExtentWidth()) + ((0.01f * progress_status * 2f * width)),0),pivot_seek,-1);
		}
	}
	
	void updateExt(){
		extent.set(width,height);
	}

	public static interface onSeekListener{
	 	void seek(int id,float progress);
		void finish(float final_progress);
	}
}
