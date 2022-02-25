package com.forcex.gui.widgets;
import com.forcex.gui.*;
import com.forcex.utils.*;
import com.forcex.app.*;
import com.forcex.*;

public class Button extends View {
	private int texture = -1;
	private Color background,select,edge;
	public float click = 0.0f;
	private TextView textview;
	private boolean adaptDimensions = false;
	private float icon_dimen;
	private boolean useEdge;
	
	public Button(float width,float height){
		setWidth(width);
		setHeight(height);
		background = new Color(225,225,225);
		select = new Color(10,110,240);
		setDebugColor(5,98,179);
	}
	
	public Button(String text,Font font,float width,float height){
		this(width,height);
		textview = new TextView(font);
		textview.setTextSize(height);
		textview.setText(text);
		textview.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
		textview.setConstraintWidth(width);
	}
	
	public Button(String text,Font font){
		this(0.1f,0.1f);
		textview = new TextView(font);
		textview.setText(text);
		textview.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
		adaptDimensions = true;
	}
	
	public void setText(String text){
		if(textview == null)return;
		textview.setText(text);
	}
	
	public void setTextColor(int r,int g,int b){
		if(textview == null)return;
		textview.setTextColor(r,g,b);
	}
	
	public void setTextSize(float size){
		if(textview == null)return;
		textview.setTextSize(size);
	}
	
	public void setIconTexture(int icon){
		this.texture = icon;
	}
	
	public void setBackgroundColor(int red,int green,int blue){
		background.set(red,green,blue);
	}
	
	public void setBackgroundColor(int red,int green,int blue,int alpha){
		background.set(red,green,blue,alpha);
	}
	
	public void setEnable(boolean z){
		if(z){
			background.set(225,225,225,255);
			textview.setTextColor(0,0,0);
			ignoreTouch = false;
		}else{
			background.set(190,190,190,255);
			textview.setTextColor(140,140,140);
			ignoreTouch = true;
		}
	}
	
	public void setUseEdge(Color color){
		edge = color;
		useEdge = true;
	}

	@Override
	public void onCreate(Drawer drawer) {
		if(textview != null && !adaptDimensions){
			if(texture != 1){
				icon_dimen = height * 0.9f;
				width += icon_dimen * 0.5f;
			}
		}
	}

	@Override
	public void updateExtent() {
		if(textview != null){
			if(adaptDimensions){
				width = textview.getWidth();
				height = textview.getHeight();
				if(texture != -1){
					icon_dimen = height * 0.9f;
					width += icon_dimen;
				}
			}
		}
	}
	
	@Override
	public void onDraw(Drawer drawer) {
		drawer.setScale(extent.x,extent.y);
		drawer.renderQuad(local,background,textview != null ? -1 : texture);
		if(click > 0.0f){
			drawer.renderQuad(local,select,-1);
			click -= FX.gpu.getDeltaTime();
		}
		if(textview != null){
			if(texture != -1){
				drawer.setScale(icon_dimen,icon_dimen * (applyAspectRatio ? context.getAspectRatio() : 1.0f));
				drawer.renderQuad(local.sub(extent.x - icon_dimen,0),null,texture);
				textview.local.set(local.x  - (extent.x - icon_dimen * 2f - textview.getWidth()), local.y);
			}else{
				textview.local.set(local);
			}
			textview.onDraw(drawer);
			textview.setDebugMode(debug);
		}
		if(useEdge && !debug){
			drawer.setScale(extent.x,extent.y);
			drawer.renderLineQuad(local,edge);
		}else if(debug){
			drawer.setScale(extent.x,extent.y);
			drawer.renderLineQuad(local,this.debug_color);
		}
	}

	@Override
	public void onTouch(float x, float y, byte type)
	{
		switch(type){
			case EventType.TOUCH_PRESSED:
				click = 0.1f;
				if(listener != null){
					listener.OnClick(this);
				}
				break;
		}
	}

	@Override
	protected boolean testTouch(float x, float y){
		return isVisible() && GameUtils.testRect(x,y,local,extent.x,extent.y);
	}
	
	@Override
	public void onDestroy() {
		setVisibility(INVISIBLE);
		if(texture != -1){
			FX.gl.glDeleteTexture(texture);
		}
		background = null;
		select = null;
		edge = null;
		if(textview != null){
			textview.onDestroy();
			textview = null;
		}
	}
}
