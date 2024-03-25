package com.forcex.gui.widgets;
import com.forcex.gui.*;
import com.forcex.utils.*;
import com.forcex.app.*;

public class Spinner extends View implements ListView.OnItemClickListener {
	private Color background,edge_color;
	private TextView textview;
	private ListView listview;
	private ListAdapter adapter;
	private int position_pointer = 0;
	private boolean choose_option = false;
	private OnSpinnerListener listener;
	
	public Spinner(float width,float height,ListAdapter adapter){
		setWidth(width);
		setHeight(height);
		listview = new ListView(width,height * 2f,adapter);
		this.adapter = adapter;
		background = new Color(235,235,235);
		edge_color = new Color(8,150,250,255);
	}

	public ListView getSpinnerList(){
		return listview;
	}

	public int getItemPosition(){
		return position_pointer;
	}

	public void setItemPosition(int idx){
		position_pointer = idx;
		textview.setText(getItem(idx).text);
	}

	public void setBackGroundColor(int r,int g,int b){
		background.set(r,g,b);
	}

	public void setTextColor(int r,int g,int b){
		textview.setTextColor(r,g,b);
	}

	public void setOnSpinnerListener(OnSpinnerListener listener){
		this.listener = listener;
	}
	
	public SpinnerItem getItem(int index){
		return (SpinnerItem)adapter.getItem((short)index);
	}

	@Override
	public void onCreate(Drawer drawer) {
		textview = new TextView(context.default_font);
		textview.setTextSize(extent.y * 0.8f);
		textview.setText(getItem(0).text);
		listview.setHeight(extent.y * 4f);
		listview.updateExt();
		listview.onCreate(drawer);
		listview.setOnItemClickListener(this);
	}
	
	@Override
	public void onDraw(Drawer drawer) {
		drawer.setScale(extent.x,extent.y);
		drawer.renderQuad(local,background,-1);
		textview.local.set(local);
		textview.onDraw(drawer);
		drawer.setScale(extent.x,extent.y);
		drawer.renderLineQuad(local,edge_color);
	}
	
	@Override
	public void onTouch(float x, float y,byte type){
		if(type == EventType.TOUCH_DROPPED){
			if(!choose_option){
				listview.setRelativePosition(local.x,local.y - extent.y - listview.getExtentHeight());
				choose_option = context.addSlotPriority(listview);
				context.addUntouchableView(listview);
			}else if(GameUtils.testRect(x,y,local,extent.x,extent.y)){
				choose_option = false;
				context.removeSlotPriority(listview.getId());
				context.removeUntouchableView(listview);
			}
		}
	}

	@Override
	public boolean testTouch(float x,float y){
		if(isVisible() && (GameUtils.testRect(x,y,local,extent.x,extent.y) || choose_option && listview.testTouch(x,y))){
			return true;
		}
		choose_option = false;
		context.removeSlotPriority(listview.getId());
		context.removeUntouchableView(listview);
		return false;
	}

	@Override
	public void onItemClick(ListView view, Object item, short position, boolean longclick) {
		context.removeSlotPriority(view.getId());
		context.removeUntouchableView(view);
		choose_option = false;
		textview.setText(((SpinnerItem)item).text);
		position_pointer = position;
		if(listener != null){
			listener.onItemClick(this,((SpinnerItem)item).text,position);
		}
	}

	
	@Override
	public void onDestroy() {
		setVisibility(INVISIBLE);
		background = null;
		edge_color = null;
		textview.onDestroy();
		textview = null;
		listview.onDestroy();
		listview = null;
		listener = null;
	}
	
	
	public static interface OnSpinnerListener{
		void onItemClick(Spinner sp,String item,int position);
	}
	
	public static class SpinnerItem{
		public String text;
		
		public SpinnerItem(String text){
			this.text = text;
		}
	}
}
