package com.forcex.gui.widgets;
import java.util.*;
import com.forcex.gui.*;
import com.forcex.utils.*;
import com.forcex.math.*;
import com.forcex.app.*;
import com.forcex.*;

public class TabView extends View {
	ArrayList<String> tabs = new ArrayList<>();
	int selected = 0;
	TextView textview;
	float constraint_width = 0;
	Color tab_color = new Color(230,230,230);
	Color tab_selected = new Color(23,180,240);
 	byte max = 0;
	OnTabListener listener;
	
	public TabView(float constraint_width,float width,float height){
		textview = new TextView(UIContext.default_font);
		textview.setTextSize(height*0.9f);
		textview.setAnimationScroll(true);
		textview.setConstraintWidth(constraint_width);
		setWidth(width);
		setHeight(height);
		this.constraint_width = constraint_width;
		max = (byte)(width / constraint_width);
	}
	
	public void addTab(String tab){
		tabs.add(tab);
	}
	
	public void removeAll(){
		tabs.clear();
	}
	
	public void removeIndex(String tab){
		tabs.remove(tab);
	}
	
	public void removeTab(String tab){
		tabs.remove(tab);
	}
	
	public void setSelect(int index){
		selected = index;
	}
	
	public void setOnTabListener(OnTabListener listener){
		this.listener = listener;
	}
	
	@Override
	public void onCreate(Drawer drawer) {
		super.onCreate(drawer);
	}
	
	long time = 0;
	
	@Override
	public void onTouch(float x, float y, byte type) {
		if(type == EventType.TOUCH_PRESSED){
			time = System.currentTimeMillis();
		}
		if(type == EventType.TOUCH_DROPPED){
			float dx = local.x - extent.x;
			for(byte i = 0;i < tabs.size();i++){
				if(GameUtils.testRect(x,y,separator.set(dx + constraint_width,local.y),constraint_width,extent.y)){
					if(listener != null){
						listener.onTabClick(tabs.get(i),i,(System.currentTimeMillis() - time) > 400);
					}
					selected = i;
				}
				dx += constraint_width * 2f;
			}
			time = 0;
		}
	}

	@Override
	protected boolean testTouch(float x, float y) {
		return GameUtils.testRect(x,y,local,extent.x,extent.y);
	}
	
	@Override
	protected void notifyTouchOutside(float x,float y,byte type) { time = -1;}
	
	Vector2f separator = new Vector2f();
	
	@Override
	public void onDraw(Drawer drawer) {
		drawer.setScale(extent.x,extent.y);
		drawer.renderQuad(local,tab_color,-1);
		float x = local.x - extent.x;
		for(byte i = 0;i < tabs.size();i++){
			if(i >= max){
				break;
			}
			textview.setText(tabs.get(i));
			textview.local.set(
				x + (textview.isConstraintUse() ? constraint_width : textview.text_anim_width),local.y);
			textview.onDraw(drawer);
			if(selected == i){
				float test = extent.y*0.08f;
				drawer.setScale(constraint_width,test);
				drawer.renderQuad(separator.set(x+constraint_width,(local.y - extent.y) + test),tab_selected,-1);
			}
			x += constraint_width * 2f;
			if(i < tabs.size()){
				drawer.setTransform(90,0,extent.y);
				drawer.renderLine(separator.set(x,local.y),tab_selected);
			}
		}
	}
	
	public static interface OnTabListener{
		void onTabClick(String text,byte position,boolean longclick);
	}
}
