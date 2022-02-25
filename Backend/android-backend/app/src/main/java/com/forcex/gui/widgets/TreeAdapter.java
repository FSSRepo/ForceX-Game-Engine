package com.forcex.gui.widgets;
import com.forcex.gui.*;
import com.forcex.math.*;
import com.forcex.app.*;
import com.forcex.utils.*;

public abstract class TreeAdapter {
	private Layout layout;
	private UIContext ctx;
	private TreeNode root;
	private float curX,curY,curXO,curYO,ox,oy,
	item_height,item_height2,space_left;
	TreeView treeview;
	boolean first = true;
	byte num_items = 0,click_log = -1;
	float width_total;

	public TreeAdapter(UIContext ctx,TreeNode root){
		layout = new Layout(ctx);
		layout.setToWrapContent();
		layout.setOrientation(Layout.VERTICAL);
		layout.beforeSetting = true;
		this.ctx = ctx;
		this.root = root;
		delta = new Vector2f();
	}
	
	public void setTreeNode(TreeNode root){
		this.root = root;
	}

	protected abstract void createView(Layout container);
	protected abstract void updateView(TreeNode node,Layout container);

	void create(){
		createView(layout);
		layout.settingExtentView();
		layout.predictLayoutDimens();
		item_height = layout.getPredictHeight();
		item_height2 = item_height * 2.0f;
		space_left = treeview.getExtentWidth() * 0.2f;
	}
	Vector2f delta;

	void render(Drawer drawer){
		if(root == null){
			return;
		}
		if(curX < 0.0f){
			curX = 0;
		}
		if(curY < 0){
			curY = 0;
		}
		delta.set(
			treeview.local.x - treeview.getExtentWidth() - curX,
			treeview.local.y + treeview.getExtentHeight() - item_height + curY);
		num_items = 0;
		width_total = 0.0f;
		renderNode(drawer,root,0);
	}

	private void renderNode(Drawer drawer, TreeNode node,int level){
		updateView(node,layout);
		layout.settingExtentView();
		float item_width = (level * space_left) + layout.getPredictWidth();
		layout.local.set(delta.x + item_width,delta.y);
		if(width_total < item_width){
			width_total = item_width;
		}
		layout.predictLayoutDimens();
		layout.sortViews();
		layout.onDraw(drawer);
		node.index = num_items;
		drawer.setScale(treeview.getExtentWidth(),0);
		if(num_items == 0){
			drawer.renderLine(layout.relative.set(treeview.local.x,layout.local.y - item_height),treeview.interlined);
		}else{
			drawer.renderLine(layout.relative.set(treeview.local.x,layout.local.y + item_height),treeview.interlined);
		}
		num_items++;
		delta.y -= item_height2;
		if(node.expand){
			for(TreeNode tree : node.children){
				renderNode(drawer,tree,level+1);
			}
		}
	}
	
	private boolean testTouch(float x,float y,TreeNode root){
		if(GameUtils.testRect(x,y,layout.local.set(treeview.local.x,delta.y),treeview.getExtentWidth(),item_height)){
			boolean second = click_log == root.index;
			treeview.listener.onClick(treeview,root,second);
			click_log = second ?  -1 : root.index;
			return true;
		}
		delta.y -= item_height2;
		if(root.expand){
			for(TreeNode n : root.children){
				if(testTouch(x,y,n)){
					return true;
				}
			}
		}
		return false;
	}

	void onTouch(float x,float y,byte type){
		if(first){
			ox = x;
			oy = y;
			first = false;
		}
		if(type == EventType.TOUCH_PRESSED){
			curXO = curX;
			curYO = curY;
		}else if(type == EventType.TOUCH_DRAGGING){
			float dY = (y - oy) * 1.25f;
			float dX = (ox - x) * 1.25f;
			if(curX + dX < (width_total * 2f) - treeview.getExtentWidth() * 2.0f){
				curX += dX;
			}
			if(curY + dY < (num_items * item_height2) - treeview.getExtentHeight() * 2.0f){
				curY += dY;
			}
		}else if(treeview.listener != null && type == EventType.TOUCH_DROPPED && (Math.abs(curY - curYO) < 0.05f)){
			delta.y = (treeview.local.y + treeview.getExtentHeight()) - item_height + curY;
			testTouch(x,y,root);
		}
		ox = x;
		oy = y;
	}
	
	void destroy(){
		layout.onDestroy();
		layout = null;
		treeview = null;
		root = null;
	}

	public UIContext getContext(){
		return ctx;
	}

	public Layout getLayout(){
		return layout;
	}
}
