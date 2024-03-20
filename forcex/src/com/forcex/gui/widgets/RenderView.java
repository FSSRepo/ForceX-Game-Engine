package com.forcex.gui.widgets;
import com.forcex.gui.*;
import com.forcex.utils.*;
import com.forcex.postprocessor.*;
import com.forcex.*;
import com.forcex.core.*;

public class RenderView extends View {
	public Color mix_color,edge_color;
	private onRenderListener listener;
	private FrameBuffer fbo;
	private boolean use_edges = false;
	private int vbo;
	private float ox,oy;
	boolean updateFBO = false;
	
	public RenderView(float width,float height){
		this(width,height,null);
	}

	public RenderView(float width,float height,onRenderListener listener){
		setDimens(width,height);
		fbo = new FrameBuffer((int)(width  * FX.gpu.getWidth()),(int)(height * FX.gpu.getHeight()));
		updateFBO = false;
		mix_color = new Color(Color.WHITE);
		edge_color = new Color(Color.BLACK);
		this.listener = listener;
	}

	@Override
	public void onCreate(Drawer drawer) {
		vbo = drawer.createBuffer(new float[]{
			-1,1,0,1,1,1,1,
			-1,-1,0,0,1,1,1,
			1,1,1,1,1,1,1,
			1,-1,1,0,1,1,1
		},false,false);
		if(applyAspectRatio){
			fbo.delete();
			fbo.create((int)(extent.x * FX.gpu.getWidth()),(int)(extent.y * FX.gpu.getHeight()));
		}
	}

	public void setDimens(float width,float height){
		setWidth(width);
		setHeight(height);
		updateFBO = true;
	}

	public void begin(){
		fbo.begin();
	}

	public void end(){
		fbo.end();
	}

	@Override
	public void onDraw(Drawer drawer) {
		if(updateFBO){
			fbo.delete();
			fbo.create((int)(extent.x * FX.gpu.getWidth()),(int)(extent.y * FX.gpu.getHeight()));
			updateFBO = false;
		}
		drawer.setScale(extent.x,extent.y);
		drawer.freeRender(vbo,local,mix_color,fbo.getTexture());
		FX.gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
		drawer.renderLineQuad(local,edge_color);
	}

	public void setUseEdge(boolean z){
		use_edges = z;
	}
	
	public void setOnRenderListener(onRenderListener listener){
		this.listener = listener;
	}

	@Override
	public void onDestroy() {
		setVisibility(INVISIBLE);
		FX.gl.glDeleteBuffer(vbo);
		mix_color = null;
		edge_color = null;
		listener = null;
		fbo.delete();
		fbo = null;
	}

	boolean first = true;

	@Override
	public void onTouch(float x, float y, byte type) {
		if(listener != null){
			if(first){
					ox = x;
					oy = y;
					first = false;
				}
				listener.touch(this,x,y,ox,oy,type);
				ox = x;
				oy = y;
		}
	}

	@Override
	public boolean testTouch(float x,float y){
		return isVisible() && GameUtils.testRect(x,y,local,getExtentWidth(),getExtentHeight());
	}

	public static interface onRenderListener{
		void touch(RenderView v, float x,float y,float ox,float oy,byte type);
	}
}
