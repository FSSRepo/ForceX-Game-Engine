package com.forcex.gui.widgets;
import com.forcex.*;
import com.forcex.app.*;
import com.forcex.gui.*;
import com.forcex.math.*;
import com.forcex.utils.*;
import java.util.*;

public abstract class ListAdapter <T extends Object> {
	private ArrayList<ListItem> items;
	private Layout layout;
	ListView listview;
	private boolean first = true;
	private float cursorY = 0.0f,cursorYOld = 0.0f,
	item_height = 0.0f,
	item_height2 = 0.0f,
	dy,oy,excedent,
	rectcursorx,
	rectcursory,
	cursor_transition,
	extent_y2;
	private UIContext ctx;
	private byte simpleViewVisible;
	private short index_start = -1;
	Vector2f rectCursor;
	float clickTime = -1;
	boolean debug;
	
	public ListAdapter(UIContext ctx){
		items = new ArrayList<>();
		layout = new Layout(ctx);
		layout.setUseWidthCustom(true);
		layout.setToWrapContent();
		layout.setOrientation(Layout.VERTICAL);
		layout.beforeSetting = true;
		rectCursor = new Vector2f();
		this.ctx = ctx;
	}
	
	public void add(T item){
		items.add(new ListItem(item));
		if(index_start != -1){
			if(items.size() > 0){
				rectcursory = listview.getExtentHeight() * ((float)simpleViewVisible / items.size());
			}else{
				rectcursory = listview.getExtentHeight();
			}
		}
		cursor_transition = 2.0f;
	}
	
	public void remove(int index){
		if(items.size() == 0 || index >= items.size()){
			return;
		}
		items.remove(index);
		if(index_start + simpleViewVisible > items.size()){
			setToFinalList();
		}
		if(index_start != -1 && items.size() > 0){
			rectcursory = listview.getExtentHeight() * ((float)simpleViewVisible / items.size());
		}else{
			rectcursory = listview.getExtentHeight();
		}
		cursor_transition = 2.0f;
	}
	
	public void removeLast(){
		if(items.size() == 0){
			return;
		}
		items.remove(items.size() - 1);
		if(index_start + simpleViewVisible > items.size()){
			setToFinalList();
		}
		if(index_start != -1 && items.size() > 0){
			rectcursory = listview.getExtentHeight() * ((float)simpleViewVisible / items.size());
		}else{
			rectcursory = listview.getExtentHeight();
		}
		cursor_transition = 3.0f;
	}
	
	public void removeAll(){
		if(items.size() == 0){
			cursor_transition = 0.0f;
			return;
		}
		items.clear();
		cursorY = 0;
		rectcursory = listview.getExtentHeight();
		cursor_transition = 3.0f;
	}
	
	
	public UIContext getContext(){
		return ctx;
	}
	
	public T getItem(int index){
		return items.get(index).item;
	}
	
	public int getNumItem(){
		return items.size();
	}
	
	protected abstract void createView(Layout container);
	
	protected abstract void updateView(T item,short position,Layout container);
	
	public void destroyView(){}
	
	boolean touchCursor = false;
	
	void onTouch(float x,float y,byte type){
		if(first){
			oy = y;
			first = false;
		}
		if(type == EventType.TOUCH_PRESSED){
			cursorYOld = cursorY;
			clickTime = 0.0f;
			if(GameUtils.testRect(x,y,rectCursor,rectcursorx,listview.getHeight())){
				touchCursor = true;
			}
		}else if(items.size() > simpleViewVisible && type == EventType.TOUCH_DRAGGING){
			if(!touchCursor){
				float delta = (y - oy) * 1.25f;
				if((index_start + simpleViewVisible >= items.size())){
					if(cursorY + delta > ((items.size() - excedent) * item_height2)){
						cursorYOld += 0.05f;
					}else{
						cursorY += delta;
					}
				}else{
					cursorY += delta;
				}
			}else{
				setBeginIndex((short)((listview.local.y + listview.getExtentHeight() - rectcursory - y) * items.size() / extent_y2));	
			}
		}else if(
			listview.listener != null && type == EventType.TOUCH_DROPPED && Math.abs(cursorY - cursorYOld) < 0.04f){
			dy = (listview.local.y + listview.getExtentHeight()) - item_height + cursorY;
			for(short i = 0;i < items.size();i++){
				if(items.get(i).show && GameUtils.testRect(x,y,layout.local.set(listview.local.x,dy),listview.getExtentWidth(),item_height)){
					listview.listener.onItemClick(listview,getItem(i),i,clickTime > 0.5f);
					break;
				}
				dy -= item_height2;
			}
		}
		if(type == EventType.TOUCH_DROPPED){
			cursor_transition = 3.0f;
			clickTime = -1.0f;
			touchCursor = false;
		}
		oy = y;
	}
	
	void create(){
		createView(layout);
		layout.settingExtentView();
		layout.predictLayoutDimens();
		item_height = layout.getPredictHeight();
		item_height2 = item_height * 2.0f;
		rectcursorx = 0.03f;
		excedent = listview.getExtentHeight() / item_height;
		simpleViewVisible = (byte)Math.ceil(excedent);
		extent_y2 = listview.getExtentHeight() * 2.0f;
		if(items.size() > 0){
			rectcursory = listview.getExtentHeight() * ((float)simpleViewVisible / items.size());
		}else{
			rectcursory = listview.getExtentHeight();
		}
		layout.setWidth(listview.getExtentWidth());
	}
	
	void render(Drawer drawer){
		if(items.size() == 0){
			cursorY = 0.0f;
			index_start = 0;
			return;
		}
		if(cursorY < 0.0f || cursorY > 0.0f && items.size() < simpleViewVisible){
			cursorY = 0.0f;
			cursorYOld += 0.05f;
		}
		if(items.size() > simpleViewVisible && cursorY > ((items.size() - excedent) * item_height2)){
			cursorY = ((items.size() - excedent) * item_height2);
		}
		testCulling();
		dy = (listview.local.y + listview.getExtentHeight()) - item_height + cursorY;
		for(short i = 0;i < items.size();i++){
			if(items.get(i).show){
				updateView(getItem(i),i,layout);
				layout.settingExtentView();
				layout.local.set(listview.local.x,dy);
				layout.predictLayoutDimens();
				layout.sortViews();
				layout.setDebugMode(debug);
				layout.onDraw(drawer);
				drawer.setScale(listview.getExtentWidth(),0);
				if(i == 0){
					drawer.renderLine(layout.relative.set(listview.local.x,layout.local.y - item_height),listview.interline_color.setAlpha(1.0f));
				}else{
					drawer.renderLine(layout.relative.set(listview.local.x,layout.local.y + item_height),listview.interline_color.setAlpha(1.0f));
				}
				if(items.get(i).select){
					drawer.setScale(listview.getExtentWidth(),item_height);
					drawer.renderQuad(layout.local,listview.select_color,-1);
				}
			}
			dy -= item_height2;
		}
		if(cursor_transition > 0.0f){
			drawer.setScale(rectcursorx,rectcursory);
			drawer.renderQuad(rectCursor.set(listview.local.x + listview.getExtentWidth() - rectcursorx,listview.local.y + listview.getExtentHeight() - (rectcursory + ((float)index_start / items.size()) * extent_y2)),
							  listview.interline_color.setAlpha(cursor_transition * 0.5f),-1);
		}
		if(clickTime != -1.0f){
			clickTime += FX.gpu.getDeltaTime();
		}else{
			if(cursor_transition > 0.0f){
			 	cursor_transition -= FX.gpu.getDeltaTime();
		 	}
		}
	}
	
	void destroy(){
		destroyView();
		items.clear();
		items = null;
		layout.onDestroy();
		rectCursor = null;
		layout = null;
	}
	
	private void testCulling(){
		index_start = (short)(cursorY / item_height2);
		for(short i = 0;i < items.size();i++){
			items.get(i).show = i >= index_start && (i - index_start <= simpleViewVisible);
		}
	}
	
	public void setBeginIndex(short index){
		cursorY = (index * item_height2);
		float test = (items.size() - excedent) * item_height2;
		if(cursorY > test){
			cursorY = test;
		}
	}
	
	public void setToFinalList(){
		cursorY = (items.size() - excedent) * item_height2;
	}
	
	public void setSelectItem(short index,boolean select){
		items.get(index).select = select;
	}
	
	public ListItem getActualSelectedItem(){
		for(ListItem it :items){
			if(it.select){
				return it;
			}
		}
		return null;
	}
	
	private class ListItem{
		T item;
		boolean show = true;
		public boolean select = false;
		
		public ListItem(T itm){
			item = itm;
		}
	}
}
