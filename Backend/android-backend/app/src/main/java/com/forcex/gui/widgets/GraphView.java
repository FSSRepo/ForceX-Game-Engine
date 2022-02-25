package com.forcex.gui.widgets;
import com.forcex.gui.*;
import com.forcex.utils.*;
import com.forcex.math.*;
import com.forcex.*;
import com.forcex.core.*;

public class GraphView extends View {
	private int vbo = -1,ibo;
	private short index_count,time_lapse_current = 0;
	private Color global_color,cursor_color,default_color,background_color;
	private GL gl = FX.gl;
	private float[] timelapse;
	private String[] labels;
	private TextView tvSampler;
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

	public GraphView(float width,float height,String title,String x_max,String x_mid,String y_max,String y_mid){
		setWidth(width);
		setHeight(height);
		global_color = new Color(230,230,230);
		default_color = new Color(6,176,37,255);
		cursor_color = new Color(6,176,37,255);
		background_color = new Color(10,10,10,140);
		timelapse = new float[60];
		labels = new String[]{title,x_max,y_max,x_mid,y_mid};
	}

	@Override
	public void onCreate(Drawer drawer) {
		update();
		short[] indices = new short[60];
		for(short i = 0;i < 60;i++){
			indices[i] = i;
		}
		index_count = (short)indices.length;
        ibo = drawer.genBuffer(indices,true,false);
		tvSampler = new TextView(context.default_font);
		tvSampler.setTextSize(Maths.clamp(width * 0.09f,0.034f,0.07f));
		tvSampler.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
	}

	@Override
	public void onDraw(Drawer drawer) {
		drawer.setScale(extent.x,extent.y);
		drawer.renderQuad(local,background_color,-1);
		drawer.setScale(extent.x,0);
		drawer.renderLine(local.sub(0,extent.y),global_color);
		drawer.setTransform(90,0,extent.y);
		drawer.renderLine(local.sub(extent.x,0),global_color);
		tvSampler.setTextColor(global_color.r,global_color.g,global_color.b);
		FX.gl.glLineWidth(3.0f);
		drawer.setScale(1,1);
		drawer.freeRender(vbo,local,cursor_color,-1);
		FX.gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ibo);
        FX.gl.glDrawElements(GL.GL_LINE_STRIP,time_lapse_current);
		// title
		tvSampler.setText(labels[0]);
		tvSampler.local.set(local.x,local.y + extent.y + 0.01f + tvSampler.getHeight());
		tvSampler.onDraw(drawer);
		// X Signs
		tvSampler.setText(labels[1]);
		tvSampler.local.set(local.x + extent.x,local.y - extent.y - 0.01f - tvSampler.getHeight());
		tvSampler.onDraw(drawer);
		tvSampler.setText(labels[3]);
		tvSampler.local.set(local.x,local.y - extent.y - 0.01f - tvSampler.getHeight());
		tvSampler.onDraw(drawer);
		// Y Signs
		tvSampler.setText(labels[2]);
		tvSampler.local.set(local.x - extent.x - 0.01f - tvSampler.getWidth(),local.y + extent.y);
		tvSampler.onDraw(drawer);
		tvSampler.setText(labels[4]);
		tvSampler.local.set(local.x - extent.x - 0.01f - tvSampler.getWidth(),local.y);
		tvSampler.onDraw(drawer);
		gl.glLineWidth(1.0f);
		setMarginTop(tvSampler.getHeight() * 2f + 0.02f);
		setMarginLeft(tvSampler.getWidth() * 2f + 0.01f);
	}
	
	private void update(){
		float[] vertices = new float[60 * 4];
		float rate = extent.x / 30.0f;
		float dy = -extent.y;
		float dx = -extent.x;
		float lenght = extent.y * 2.0f;
		for(short i = 0;i < 60;i++){
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

	public void setDefaultColor(int r,int g,int b){
		default_color.set(r,g,b);
	}

	public void setSamplerColors(GraphDynamicColor... samplers){
		this.samplers = samplers;
	}

	public void next(float progress){
		if(time_lapse_current < 60){
			timelapse[time_lapse_current] = Maths.clamp(progress,0.0f,1.0f);
			time_lapse_current++;
		}else{
			for(int i = 0;i < 59;i++){
				timelapse[i] = timelapse[i + 1];
			}
			timelapse[59] = Maths.clamp(progress,0.0f,1.0f);
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
		labels = null;
		tvSampler = null;
		samplers = null;
	}
}
