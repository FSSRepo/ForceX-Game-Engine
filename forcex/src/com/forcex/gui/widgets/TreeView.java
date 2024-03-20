package com.forcex.gui.widgets;
import com.forcex.gui.*;
import com.forcex.utils.*;

public class TreeView extends View {
	TreeAdapter adapter;
	Color background_color,interlined;
	OnTreeListener listener;
	
	public TreeView(float width,float height,TreeAdapter adapter){
		setWidth(width);
		setHeight(height);
		this.adapter = adapter;
		adapter.treeview = this;
		background_color = new Color(Color.WHITE);
		interlined = new Color(Color.GREY);
	}
	
	public void setOnTreeListener(OnTreeListener listener){
		this.listener = listener;
	}
	
	@Override
	public void onDraw(Drawer drawer) {
		drawer.setScale(extent.x,extent.y);
		drawer.renderQuad(local,background_color,-1);
		drawer.scissorArea(local.x,local.y,extent.x,extent.y);
		adapter.render(drawer);
		drawer.finishScissor();
	}

	@Override
	public void onCreate(Drawer drawer) {
		adapter.create();
	}

	@Override
	public void onTouch(float x, float y, byte type) {
		adapter.onTouch(x,y,type);
	}

	@Override
	public void onDestroy() {
		setVisibility(INVISIBLE);
		background_color = null;
		interlined = null;
		listener = null;
		adapter.destroy();
		adapter = null;
	}

	@Override
	protected void notifyTouchOutside(float x,float y,byte type) {
		
	}
	
	@Override
	protected boolean testTouch(float x, float y) {
		return isVisible() && GameUtils.testRect(x,y,local,extent.x,extent.y);
	}
	
	public static interface OnTreeListener{
		void onClick(TreeView tree,TreeNode node,boolean longclick);
	}
}
