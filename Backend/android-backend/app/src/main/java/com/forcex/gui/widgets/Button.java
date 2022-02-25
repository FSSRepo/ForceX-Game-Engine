package com.forcex.gui.widgets;
import com.forcex.gui.*;
import com.forcex.utils.*;
import com.forcex.app.*;
import com.forcex.*;

public class Button extends View {
	private int texture = -1;
	private Color background,select,edge;
	public boolean click = false;
	private TextView textview;
	private boolean adaptDimensions = false;
	private float icon_dimen;
	private boolean useEdge;
	
	public Button(float width,float height){
		setWidth(width);
		setHeight(height);
		background = new Color(225,225,225);
		select = new Color(10,110,240);
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
		if(click){
			drawer.renderQuad(local,select,-1);
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
		}
		if(useEdge){
			drawer.setScale(extent.x,extent.y);
			drawer.renderLineQuad(local,edge);
		}
	}
	
	@Override
	protected void dispatchTouch(float x, float y, byte type) {
		switch(type){
			case EventType.TOUCH_PRESSED:
				if(listener != null){
					listener.OnClick(this);
				}
				click = true;
				break;
			case EventType.TOUCH_DROPPED:
				click = false;
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
