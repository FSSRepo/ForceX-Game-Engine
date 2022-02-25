package com.forcex.gui.widgets;
import com.forcex.gui.*;
import java.util.*;
import com.forcex.app.*;
import com.forcex.utils.*;
import com.forcex.math.*;
import com.forcex.*;

public abstract class GridAdapter {
	private ArrayList<GridItem> items;
	GridView gridview;
	private Layout layout;
	private float 
	curY = 0.0f,curYO = 0.0f,oy,dx,dy,
	item_height = 0.0f,
	item_height2 = 0.0f,
	rectcursorx,
	rectcursory,
	cursor_transition = 2.0f,
	extent_y2,
	excedent;
	byte simpleViewVisible = 0;
	private UIContext ctx;
	private boolean first = true,dispatchDrag = false;
	short num_rows = 0,row_begin= -1;
	Vector2f rectCursor;
	float clickTime = -1;
	boolean touchCursor = false;
	
	public GridAdapter(UIContext context){
		items = new ArrayList<>();
		rectCursor = new Vector2f();
		layout = new Layout(context);
		layout.setUseWidthCustom(true);
		layout.setToWrapContent();
		layout.setOrientation(Layout.VERTICAL);
		layout.beforeSetting = true;
		this.ctx = context;
	}
	
	public void add(Object item){
		items.add(new GridItem(item));
		if(row_begin != -1){
			num_rows = (short)Math.ceil((float)items.size() / gridview.limitX);
			if(items.size() > 0){
				rectcursory = gridview.getExtentHeight() * ((float)simpleViewVisible / num_rows);
			}else{
				rectcursory = gridview.getExtentHeight();
			}
		}
		cursor_transition = 2.0f;
	}
	
	public void remove(short index){
		if(items.size() == 0 || index >= items.size()){
			return;
		}
		items.remove(index);
		if(row_begin + simpleViewVisible > num_rows){
			setToFinalList();
		}
		if(row_begin != -1 && items.size() > 0){
			num_rows = (short)Math.ceil((float)items.size() / gridview.limitX);
			rectcursory = gridview.getExtentHeight() * ((float)simpleViewVisible / num_rows);
		}else{
			rectcursory = gridview.getExtentHeight();
		}
		cursor_transition = 2.0f;
	}

	public void removeLast(){
		if(items.size() == 0){
			return;
		}
		items.remove(items.size() - 1);
		if(row_begin + simpleViewVisible > num_rows){
			setToFinalList();
		}
		if(row_begin != -1 && items.size() > 0){
			num_rows = (short)Math.ceil((float)items.size() / gridview.limitX);
			rectcursory = gridview.getExtentHeight() * ((float)simpleViewVisible / num_rows);
		}else{
			rectcursory = gridview.getExtentHeight();
		}
		cursor_transition = 2.0f;
	}
	
	public void removeAll(){
		items.clear();
		if(num_rows != -1){
			item_height = 0.0f;
			curY = 0.0f;
			num_rows = 0;
			rectcursory = gridview.getExtentHeight();
			cursor_transition = 2.0f;
		}
	}
	
	public UIContext getContext(){
		return ctx;
	}

	public Object getItem(short index){
		return items.get(index).item;
	}

	protected abstract void createView(Layout container);

	protected abstract void updateView(Object item,short position,Layout container);
	
	void onTouch(float x,float y,byte type){
		if(first){
			oy = y;
			first = false;
		}
		if(type == EventType.TOUCH_PRESSED){
			curYO = curY;
			clickTime = 0.0f;
			cursor_transition = 2.0f;
			if(GameUtils.testRect(x,y,rectCursor,rectcursorx,rectcursory)){
				touchCursor = true;
			}
		}else if(type == EventType.TOUCH_DRAGGING){
			if(!touchCursor){
				float delta = (y - oy) * 1.25f;
				if(curY + delta <= ((num_rows - excedent) * item_height2)){
					curY += delta;
					dispatchDrag = false;
				}else{
					dispatchDrag = true;
				}
			}else{
				setBeginRow((short)((gridview.local.y + gridview.getExtentHeight() - rectcursory - y) * num_rows / extent_y2));
			}
		}else if(gridview.listener != null && type == EventType.TOUCH_DROPPED && (Math.abs(curY - curYO) < 0.04f || dispatchDrag && clickTime < 0.4f)){
			dx = (gridview.local.x - gridview.getExtentWidth()) + gridview.max_item_width;
			dy = (gridview.local.y + gridview.getExtentHeight()) - item_height + curY;
			byte curx = 0;
			for(short i = 0;i < items.size();i++){
				if(items.get(i).show && GameUtils.testRect(x,y,layout.local.set(dx + (curx * gridview.max_item_width * 2f),dy),gridview.max_item_width,item_height)){
					gridview.listener.onItemClick(getItem(i),i,clickTime > 0.5f);
					break;
				}
				curx++;
				if(curx == gridview.limitX){
					dy -= item_height2;
					curx = 0;
				}
			}
			clickTime = -1.0f;
			touchCursor = false;
		}else if(type == EventType.TOUCH_DROPPED){
			clickTime = -1.0f;
			touchCursor = false;
		}
		oy = y;
	}
	
	void create(){
		layout.setWidth(gridview.max_item_width);
		createView(layout);
		layout.settingExtentView();
		layout.predictLayoutDimens();
		item_height = layout.getPredictHeight();
		item_height2 = item_height * 2.0f;
		excedent = gridview.getExtentHeight() / item_height;
		simpleViewVisible = (byte)Math.ceil(excedent);
		num_rows = (short)Math.ceil((float)items.size() / gridview.limitX);
		extent_y2 = gridview.getExtentHeight() * 2.0f;
		rectcursorx = gridview.getExtentWidth() * 0.1f;
		if(items.size() > 0){
			rectcursory = gridview.getExtentHeight() * ((float)simpleViewVisible / num_rows);
		}else{
			rectcursory = gridview.getExtentHeight();
		}
	}
	
	void render(Drawer drawer){
		if(items.isEmpty()){
			curY = 0;
			return;
		}
		if(curY < 0.0f || items.size() < simpleViewVisible){
			curY = 0.0f;
		}
		
		dx = (gridview.local.x - gridview.getExtentWidth()) + gridview.max_item_width;
		dy = (gridview.local.y + gridview.getExtentHeight()) + curY;
		byte curx = 0;
		testCulling();
		for(short i = 0;i < items.size();i++){
			if(items.get(i).show){
				updateView(getItem(i),i,layout);
				layout.settingExtentView();
				layout.predictLayoutDimens();
				layout.local.set(dx + (curx * gridview.max_item_width * 2f),dy - layout.getExtentHeight());
				layout.sortViews();
				layout.onDraw(drawer);
				if(item_height < layout.getExtentHeight()){
					item_height = layout.getExtentHeight();
					item_height2 = item_height * 2.0f;
					excedent = gridview.getExtentHeight() / item_height;
					simpleViewVisible = (byte)Math.ceil(excedent);
					num_rows = (short)Math.ceil((float)items.size() / gridview.limitX);
					if(items.size() > 0){
						rectcursory = gridview.getExtentHeight() * ((float)simpleViewVisible / num_rows);
					}else{
						rectcursory = gridview.getExtentHeight();
					}
				}
				if(items.get(i).select){
					drawer.setScale(gridview.max_item_width,item_height);
					drawer.renderQuad(rectCursor.set(layout.local.x,dy - item_height),gridview.select_color,-1);
				}
			}
			curx++;
			if(curx == gridview.limitX){
				dy -= item_height2;
				curx = 0;
			}
		}
		if(cursor_transition > 0.0f){
			drawer.setScale(rectcursorx,rectcursory);
			drawer.renderQuad(rectCursor.set(gridview.local.x + gridview.getExtentWidth() - rectcursorx,gridview.local.y + gridview.getExtentHeight() - (rectcursory + ((float)row_begin / num_rows) * extent_y2)),
							  gridview.interlined.setAlpha(cursor_transition * 0.5f),-1);
		}
		if(clickTime != -1.0f){
			clickTime += FX.gpu.getDeltaTime();
		}else{
			if(cursor_transition > 0.0f){
			 	cursor_transition -= FX.gpu.getDeltaTime();
		 	}
		}
	}
	
	private void testCulling(){
		row_begin = (short)(curY / item_height2);
		for(short r = 0;r < num_rows;r++){
			for(short c = 0;c < gridview.limitX;c++){
				int idx = r * gridview.limitX + c;
				if(idx < items.size()){
					items.get(idx).show = (r >= row_begin && (r - row_begin <= simpleViewVisible));
				}
			}
		}
	}
	
	public void setBeginRow(short row){
		curY = (row * item_height2);
	}
	
	public void setToFinalList(){
		curY = (num_rows - excedent) * item_height2;
	}
	
	public void setSelect(short position,boolean z){
		items.get(position).select = z;
	}
	
	void destroy(){
		items.clear();
		gridview = null;
		layout.onDestroy();
		layout = null;
		rectCursor = null;
		ctx = null;
	}
	
	public class GridItem{
		Object item;
		boolean show = true;
		public boolean select = false;

		public GridItem(Object itm){
			item = itm;
		}
	}
}
