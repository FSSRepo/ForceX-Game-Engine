package com.forcex.gui;
import com.forcex.*;
import com.forcex.utils.*;
import com.forcex.math.*;
import com.forcex.gui.widgets.*;
import com.forcex.core.gpu.*;
import com.forcex.app.*;

public class Dialog {
	private Layout layout;
	protected boolean this_is_priority,moving = false;
	private byte state = -1; // -1: no show, 1: show,2: hide,3: dimiss
	private float label_width, label_height,close_width, padding = 0.01f, icon_dimens;
	private Color background;
	private Vector2f position;
	short id = 0;
	private static short id_gen = -32000;
	private int close_texture,icon_texture = -1;
	private TextView textview;
	private OnDimissListener listener;
	private boolean useLabel = true, useCloseButton = true;
	
	public Dialog(Layout layout){
		this.layout = layout;
		layout.setToWrapContent();
		label_height = 0.06f;
		close_width = 0.07f;
		background = new Color();
		position = new Vector2f();
		id = id_gen;
		id_gen++;
		this_is_priority = layout.context.isDialogEmpty();
		close_texture = Texture.load(FX.homeDirectory + "gui/close.png");
		if(!layout.context.addSlotDialog(this)){
			Toast.error("Dialog Error:\nThere aren't slots availables.",4f);
		}
	}
	
	public void setIcon(int texture){
		icon_texture = texture;
	}
	
	public void show(){
		state = 1;
	}
	
	public void setOnDimissListener(OnDimissListener listener){
		this.listener = listener;
	}
	
	public void setTitle(String text){
		if(textview == null){
			textview = new TextView(layout.context.default_font);
			textview.setTextSize(label_height * 0.7f);
			textview.setTextColor(0,0,0);
		}
		textview.setText(text);
	}
	
	public void hide(){
		state = 2;
		this_is_priority = false;
	}
	
	void render(Drawer drawer){
		if(state == 1 || state == 3){
			if(state == 3){
				// destroy
				layout.context.removeSlotDialog(id);
				layout.onDestroy();
				layout = null;
				background = null;
				position = null;
				listener = null;
				FX.gl.glDeleteTexture(close_texture);
				return;
			}
			settingDialog();
			drawer.setScale(layout.extent.x + padding,layout.extent.y + padding);
			drawer.renderQuad(position,background,-1);
			layout.onDraw(drawer);
			if(useLabel){
				float dx = layout.extent.x + padding;
				float dy = layout.extent.y + padding + label_height;
				drawer.setScale(label_width,label_height);
				drawer.renderQuad(position.add(-dx + label_width,dy),null,-1);
				if(useCloseButton){
					drawer.setScale(close_width,label_height);
					drawer.renderQuad(position.add(dx - close_width,dy),null,close_texture);
				}
				if(icon_texture != -1){
					drawer.setScale(icon_dimens,icon_dimens * 1.2f);
					drawer.renderQuad(position.add(-dx + icon_dimens,dy),null,icon_texture);
				}
				if(textview != null){
					textview.local.set(position.x + (-dx + (icon_texture != -1 ? icon_dimens * 2.1f : 0.01f) + textview.getWidth()),dy+position.y);
					textview.onDraw(drawer);
				}
			}
		}
	}
	
	private void settingDialog(){
		layout.local.set(position);
		layout.settingExtentView();
		layout.predictLayoutDimens();
		layout.sortViews();
		if(useLabel){
			label_width = (layout.extent.x + padding) - (useCloseButton ? close_width : 0.0f);
			if(label_width <= 0.07f){
				label_width = 0.2f;
			}
			if(icon_texture != -1){
				icon_dimens = label_height * 0.5f;
			}
		}
	}
	
	public void setUseLabel(boolean z){
		useLabel = z;
	}
	
	public void setUseCloseButton(boolean z){
		useCloseButton = z;
	}
	
	public void setUsePadding(boolean z){
		padding = z ? 0.01f : 0.0f;
	}
	
	Vector2f dist = new Vector2f();
	
	void onTouch(float x,float y,byte type){
		if(useLabel){
			if(type == EventType.TOUCH_PRESSED && GameUtils.testRect(x,y,position.add(-(layout.extent.x + padding) + label_width,layout.extent.y + padding + label_height),label_width,label_height)){
				moving = true;
				dist.set(position.x - x,position.y - y);
				return;
			}else if(moving && type == EventType.TOUCH_DRAGGING){
				position.set(x + dist.x,y + dist.y);
				return;
			}else if(moving && type == EventType.TOUCH_DROPPED){
				moving = false;
				return;
			}
			if(useCloseButton && type == EventType.TOUCH_DROPPED && GameUtils.testRect(x,y,position.add((layout.extent.x + padding) - close_width,(layout.extent.y + padding + label_height)),close_width,label_height)){
				if(listener != null){
					if(listener.dimiss()){
						dimiss();
					}
				}else{
					dimiss();
				}
				return;
			}
		}
		layout.onTouch(x,y,type);
	}
	
	public void setThisPriority(){
		layout.context.resetPriorities();
		this_is_priority = true;
	}
	
	public void dimiss(){
		state = 3;
	}
	
	public boolean isVisible(){
		return state == 1;
	}
	
	void notifyTouchOutside(float x,float y,byte type){
		layout.notifyTouchOutside(x,y,type);
	}
	
	boolean testTouch(float x,float y){
		return GameUtils.testRect(x,y,position.add(0,(useLabel ? label_height : 0.0f) + padding),layout.getExtentWidth() + padding,layout.getExtentHeight() + padding + (useLabel ? label_height : 0.0f));
	}
	
	public static interface OnDimissListener{
		 boolean dimiss();
	}
}
