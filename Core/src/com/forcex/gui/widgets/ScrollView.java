package com.forcex.gui.widgets;
import com.forcex.gui.*;
import com.forcex.utils.*;
import com.forcex.app.*;

public class ScrollView extends View {
	Layout layout;
	boolean first = true;
	float oy,cursorY,cursorYOld;
	
	public ScrollView(Layout layout,float height){
		this.layout = layout;
		layout.setToWrapContent();
		setHeight(height);
	}

	@Override
	public void updateExtent() {
		layout.settingLayout();
		setWidth(layout.getExtentWidth());
	}
	
	@Override
	public void onDraw(Drawer drawer) {
		if(cursorY < 0.0f){
			cursorY = 0;
		}
		drawer.setScale(extent.x,extent.y);
		drawer.renderLineQuad(local,null);
		layout.local.set(local.x,local.y + extent.y - layout.getExtentHeight() + cursorY);
		drawer.scissorArea(local.x,local.y,extent.x,extent.y);
		layout.onDraw(drawer);
		drawer.finishScissor();
	}
	
	public void onTouch(float x,float y,byte type){
		if(first){
			oy = y;
			first = false;
		}
		if(type == EventType.TOUCH_PRESSED){
			cursorYOld = cursorY;
		}else if(type == EventType.TOUCH_DRAGGING){
			float delta = (y - oy) * 1.25f;
			if(cursorY + delta < ((layout.getExtentHeight() * 2.0f) - (extent.y*2.0f))){
				cursorY += delta;
			}
			layout.onTouch(x,y,type);
		}else if(type == EventType.TOUCH_DROPPED && (Math.abs(cursorY - cursorYOld) < 0.04f)){
			layout.onTouch(x,y,EventType.TOUCH_PRESSED);
		}
		oy = y;
	}
	
	@Override
	protected boolean testTouch(float x, float y) {
		return isVisible() && 
		GameUtils.testRect(x,y,local,layout.getExtentWidth(),layout.getExtentHeight()) && 
		layout.testIsTouching(x,y);
	}

	@Override
	public void onDestroy() {
		layout.onDestroy();
		layout = null;
	}
	
}
