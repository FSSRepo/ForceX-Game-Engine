package com.forcex.gui.widgets;
import com.forcex.gui.*;
import com.forcex.utils.*;

public class ListView extends View {
	ListAdapter adapter;
	private Color background_color;
	Color interline_color,select_color;
	onItemClickListener listener;
	
	public ListView(float width,float height,ListAdapter adapter){
		setWidth(width);
		setHeight(height);
		this.adapter = adapter;
		adapter.listview = this;
		background_color = new Color(Color.WHITE);
		interline_color = new Color(Color.GREY);
		select_color = new Color(12,190,240,130);
		setDebugColor(179,5,153);
	}

	@Override
	public void onCreate(Drawer drawer) {
		adapter.create();
	}
	
	public void setOnItemClickListener(onItemClickListener listener){
		  this.listener = listener;
	}
	
	public void setBackgroundColor(int red,int green,int blue,int alpha){
		background_color.set(red,green,blue,alpha);
	}
	
	public void setInterlinedColor(int red,int green,int blue,int alpha){
		interline_color.set(red,green,blue,alpha);
	}
	
	@Override
	public void onDraw(Drawer drawer) {
		adapter.debug = debug;
		drawer.setScale(extent.x,extent.y);
		drawer.renderQuad(local,background_color,-1);
		drawer.scissorArea(local.x,local.y,extent.x,extent.y);
		adapter.render(drawer);
		drawer.finishScissor();
		if(debug){
			drawer.setScale(extent.x,extent.y);
			drawer.renderLineQuad(local,this.debug_color);
		}
	}

	@Override
	public void onTouch(float x, float y, byte type) {
		adapter.onTouch(x,y,type);
	}

	@Override
	protected void notifyTouchOutside(float x,float y,byte type) {
		adapter.clickTime = -1.0f;
		adapter.touchCursor = false;
	}
	
	@Override
	protected boolean testTouch(float x, float y) {
		return isVisible() && GameUtils.testRect(x,y,local,extent.x,extent.y);
	}

	@Override
	public void onDestroy() {
		setVisibility(INVISIBLE);
		adapter.destroy();
		adapter = null;
		background_color = null;
		interline_color = null;
		listener = null;
	}
	
	public static interface onItemClickListener{
		void onItemClick(ListView view,Object item,short position,boolean longclick);
	}
	
	void updateExt(){
		extent.set(width,height);
	}
}
