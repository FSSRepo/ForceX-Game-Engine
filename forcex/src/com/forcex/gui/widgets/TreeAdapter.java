package com.forcex.gui.widgets;
import com.forcex.gui.*;
import com.forcex.math.*;
import com.forcex.app.*;
import com.forcex.utils.*;
import com.forcex.*;

public abstract class TreeAdapter {
	private Layout layout;
	private UIContext ctx;
	private TreeNode root;
	private float curX,curY,curXO,curYO,ox,oy,item_height,item_height2,space_left,limitY;
	protected TreeView treeview;
	private boolean first = true;
	private short num_items = 0,old_num = 0;
	protected float width_total,excedent;
	
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
		curX = 0;
		curY = 0;
	}

	protected abstract void createView(Layout container);
	protected abstract void updateView(TreeNode node,Layout container);

	void create(){
		createView(layout);
		layout.settingExtentView();
		layout.predictLayoutDimens();
		item_height = layout.getPredictHeight();
		item_height2 = item_height * 2.0f;
		excedent = treeview.getExtentHeight() / item_height;
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
		if(curY < 0.0f || num_items < excedent){
			curY = 0.0f;
		}
		if(num_items > excedent && curY > limitY){
			curY = ((num_items - excedent) * item_height2);
		}
		delta.set(
			treeview.local.x - treeview.getExtentWidth() - curX,
			treeview.local.y + treeview.getExtentHeight() - item_height + curY);
		num_items = 0;
		width_total = 0.0f;
		renderNode(drawer,root,0);
		if(num_items != old_num){
			old_num = num_items;
			limitY = ((num_items - excedent) * item_height2);
		}
	}
	
	Color emphasize_color = new Color(20,220,240,90);
	float timer_anim = 0;
	boolean inverse = false;
	
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
		if(node.emphasize > 0) {
			drawer.setScale(treeview.getExtentWidth(),item_height);
			drawer.renderQuad(layout.relative.set(treeview.local.x,layout.local.y),emphasize_color,-1);
			if(timer_anim > 0.3f) {
				inverse = !inverse;
				timer_anim = 0;
				node.emphasize--;
			}
			emphasize_color.a = (short)(inverse ? (1f - (timer_anim / 0.3f)) * 90f : (timer_anim / 0.3f) * 90f);
			timer_anim += FX.gpu.getDeltaTime();
		}
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
		if(root == null){
			return false;
		}
		if(GameUtils.testRect(x,y,layout.local.set(treeview.local.x,delta.y),treeview.getExtentWidth(),item_height)){
			root.emphasize = 0;
			treeview.listener.onClick(treeview,root,(System.currentTimeMillis() - click) > 300);
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
	
	long click = 0;
	
	void onTouch(float x,float y,byte type){
		if(first){
			ox = x;
			oy = y;
			first = false;
		}
		if(type == EventType.TOUCH_PRESSED){
			curXO = curX;
			curYO = curY;
			click = System.currentTimeMillis();
		}else if(type == EventType.TOUCH_DRAGGING){
			float deltaY = (y - oy) * 1.25f;
			float deltaX = (ox - x) * 1.25f;
			if(curX + deltaX < (width_total - treeview.getExtentWidth())){
				curX += deltaX;
			}else if(width_total > treeview.getExtentWidth()){
				curXO += 0.04f;
			}
			if(curY + deltaY < limitY){
				curY += deltaY;
			}else if(num_items > excedent){
				curYO += 0.04f;
			}
		}else if(
			treeview.listener != null && 
			type == EventType.TOUCH_DROPPED && 
			(Math.abs(curY - curYO) < 0.04f && 
			 Math.abs(curX - curXO) < 0.04f)){
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
