package com.forcex.gui.widgets;
import com.forcex.gui.*;
import com.forcex.utils.*;
import com.forcex.math.*;
import com.forcex.*;
import com.forcex.core.*;

public class GraphView extends View {
	private int vbo = -1,ibo;
	private short index_count,time_lapse_current = 0;
	private Color cursor_color,default_color,background_color;
	private GL gl = FX.gl;
	private float[] timelapse;
	private GraphDynamicColor[] samplers;

	public static class GraphDynamicColor{
		public float min;
		public float max;
		public Color color;

		public GraphDynamicColor(Color color,float max,float min){
			this.color = color;
			this.min = min;
			this.max = max;
		}
	}

	public GraphView(float width,float height,int samplers){
		setWidth(width);
		setHeight(height);
		default_color = new Color(6,176,37,255);
		cursor_color = new Color(6,176,37,255);
		background_color = new Color(10,10,10,140);
		timelapse = new float[samplers];
	}

	@Override
	public void onCreate(Drawer drawer) {
		update();
		short[] indices = new short[timelapse.length];
		for(short i = 0;i < timelapse.length;i++){
			indices[i] = i;
		}
		index_count = (short)indices.length;
        ibo = drawer.genBuffer(indices,true,false);
	}

	@Override
	public void onDraw(Drawer drawer) {
		drawer.setScale(extent.x,extent.y);
		drawer.renderQuad(local,background_color,-1);
		FX.gl.glLineWidth(3.0f);
		drawer.setScale(1,1);
		drawer.freeRender(vbo,local,cursor_color,-1);
		FX.gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ibo);
        FX.gl.glDrawElements(GL.GL_LINE_STRIP,time_lapse_current);
		gl.glLineWidth(1.0f);
	}
	
	private void update(){
		float[] vertices = new float[timelapse.length * 4];
		float rate = extent.x / (timelapse.length * 0.5f);
		float dy = -extent.y;
		float dx = -extent.x;
		float lenght = extent.y * 2.0f;
		for(short i = 0;i < timelapse.length;i++){
			if(i < time_lapse_current){
				vertices[i*4] = dx;
				vertices[i*4 + 1] = dy + (timelapse[i] * lenght);
				vertices[i*4 + 2] = 0;
				vertices[i*4 + 3] = 0;
				dx += rate;
			}
		}
		if(vbo == -1){
			vbo = Drawer.genBuffer(vertices,false,true);
		}else{
			Drawer.updateBuffer(vbo,vertices);
		}
		vertices = null;
	}

	public void setDefaultColor(int red,int green,int blue){
		default_color.set(red,green,blue);
	}
	
	public void setBackgroundColor(int red,int green,int blue,int alpha){
		background_color.set(red,green,blue,alpha);
	}

	public void setSamplerColors(GraphDynamicColor... samplers){
		this.samplers = samplers;
	}

	public void next(float progress){
		if(time_lapse_current < timelapse.length){
			timelapse[time_lapse_current] = Maths.clamp(progress,0.0f,1.0f);
			time_lapse_current++;
		}else{
			for(int i = 0;i < timelapse.length-1;i++){
				timelapse[i] = timelapse[i + 1];
			}
			timelapse[timelapse.length - 1] = Maths.clamp(progress,0.0f,1.0f);
		}
		if(samplers != null){
			for(GraphDynamicColor s : samplers){
				if(progress <= s.max && progress >= s.min){
					cursor_color.set(s.color);
					break;
				}else{
					cursor_color.set(default_color);
				}
			}
		}
		update();
	}

	@Override
	public void onDestroy() {
		setVisibility(INVISIBLE);
		gl.glDeleteBuffer(vbo);
		gl.glDeleteBuffer(ibo);
		timelapse = null;
		samplers = null;
	}
}
