package com.forcex.gui;
import java.util.*;
import com.forcex.gfx3d.shader.*;
import com.forcex.*;
import com.forcex.app.threading.*;
import com.forcex.gui.widgets.*;
import com.forcex.app.*;

public class UIContext {
	private View content;
	private Drawer drawer;
	private float aspectRatio;
	public Font default_font;
	private SlidePanel slide_panel;
	public View[] view_priority;
	private Dialog[] dialogs;
	ArrayList<View> ghostviews;
	
	public UIContext(){
		drawer = new Drawer(new SpriteShader(true,false,true));
		aspectRatio = (float)FX.gpu.getWidth() / FX.gpu.getHeight();
		default_font = new Font(FX.homeDirectory+"/fonts/windows.fft");
		Toast.create(this);
		view_priority = new View[3];
		dialogs = new Dialog[5];
		ghostviews = new ArrayList<View>();
	}
	
	public void createKeyBoard(float width){
		KeyBoard.create(width,this);
	}
	
	public void setSlidePanel(SlidePanel panel){
		slide_panel = panel;
	}
	
	public void setContentView(View view){
		if(content != null){
			return;
		}
		content = view;
		if(!content.isLayout()){
			content.context = this;
		}
	}
	
	public View findViewByID(int id){
		if(content == null){
			return null;
		}
		for(View v : view_priority){
			if(v != null && v.getId() == id){
				return v;
			}
		}
		if(content.getId() == id){
			return content;
		}
		if(slide_panel != null){
			View v = slide_panel.getContent().findViewByID(id);
			if(v != null){
				return v;
			}
		}
		if(content.isLayout()){
			return ((Layout)content).findViewByID(id);
		}
		return null;
	}
	
	public float getAspectRatio(){
		return aspectRatio;
	}
	
	public void draw(){
		if(content == null) return;
		drawer.start();
		if(content.isVisible()){
			content.draw(drawer);
		}
		for(View v : ghostviews){
			if(v.isVisible()){
				v.draw(drawer);
			}
		}
		if(slide_panel != null){
			slide_panel.render(drawer);
		}
		Dialog prio = getPriority();
		for(byte i = 0;i < dialogs.length;i++){
			if(dialogs[i] != null && dialogs[i] != prio){
				dialogs[i].render(drawer);
			}
		}
		if(prio != null){
			prio.render(drawer);
		}
		KeyBoard.instance.render(drawer);
		Toast.instance.render(drawer);
		drawer.end();
	}
	
	public Dialog getPriority(){
		for(Dialog d : dialogs){
			if(d != null && d.this_is_priority){
				return d;
			}
		}
		return null;
	}
	
	void rewindPriority(){
		for(Dialog d : dialogs){
			if(d != null && d.this_is_priority){
				d.this_is_priority = false;
			}
		}
	}
	
	public boolean isDialogEmpyt(){
		for(Dialog d : dialogs){
			if(d != null){
				return false;
			}
		}
		return true;
	}
	
	public boolean testTouch(float x,float y){
		if(KeyBoard.instance != null && KeyBoard.instance.testTouch(x,y))return true;
		Dialog prio = getPriority();
		if(prio != null && prio.testTouch(x,y)){
			return true;
		}
		for(Dialog d : dialogs){
			if(d != null && d.testTouch(x,y)){
				 return true;
			}
		}
		for(byte i = 0;i < view_priority.length;i++){
			if(view_priority[i] != null && view_priority[i].testTouch(x,y)){
				return true;
			}
		}
		if(content == null)return false;
		if(Toast.instance.testTouch(x,y))return true;
		if(slide_panel != null && slide_panel.testTouch(x,y))return true;
		if(content.testTouch(x,y)){
			if(content.isLayout()){
				return ((Layout)content).testIsTouching(x,y);
			}else{
				return true;
			}
		}
		return false;
	}
	
	public void onTouch(final float x,final float y,final byte type){
		if(content == null)return;
		FX.gpu.queueTask(new Task(){
				@Override
				public boolean execute() {
					if(KeyBoard.instance != null && KeyBoard.instance.testTouch(x,y)){
						KeyBoard.instance.onTouch(x,y,type);
						return true;
					}
					Dialog prio = getPriority();
					if(prio != null && prio.testTouch(x,y)){
						prio.onTouch(x,y,type);
						return true;
					}
					for(Dialog d : dialogs){
						if(d != null && d.testTouch(x,y)){
							if(prio != null) prio.this_is_priority = false;
							d.this_is_priority = true;
							d.onTouch(x,y,type);
							return true;
						}
					}
					for(byte i = 0;i < view_priority.length;i++){
						if(view_priority[i] != null && view_priority[i].testTouch(x,y)){
							view_priority[i].onTouch(x,y,type);
							return true;
						}
					}
					if(Toast.instance.testTouch(x,y)){
						Toast.instance.onTouch(type == EventType.TOUCH_PRESSED);
					}
					if(slide_panel != null && slide_panel.testTouch(x,y)){
						slide_panel.onTouch(x,y,type);
						return true;
					}
					if(content.testTouch(x,y)){
						content.dispatchTouch(x,y,type);
					}
					return true;
				}
		});
	}
	
	public void removeSlotPriority(int id){
		for(byte i = 0;i < 3;i++){
			if(view_priority[i] != null && view_priority[i].getId() == id){
				view_priority[i] = null;
				return;
			}
		}
	}
	
	public boolean addSlotPriority(View view){
		for(byte i = 0;i < 3;i++){
			if(view_priority[i] ==null){
				view_priority[i] = view;
				return true;
			}
		}
		return false;
	}
	
	void removeSlotDialog(short id){
		for(byte i = 0;i < 5;i++){
			if(dialogs[i] != null && dialogs[i].id == id){
				dialogs[i] = null;
				return;
			}
		}
	}

	boolean addSlotDialog(Dialog dialog){
		for(byte i = 0;i < 5;i++){
			if(dialogs[i] == null){
				dialogs[i] = dialog;
				return true;
			}
		}
		return false;
	}
	
	public void removeGhostView(View ghost){
		ghostviews.remove(ghost);
	}
	
	public boolean isGhostView(View ghost){
		for(View v : ghostviews){
			if(v == ghost){
				return true;
			}
		}
		return false;
	}

	public void addGhostView(View ghost){
		ghost.context = this;
		ghostviews.add(ghost);
	}
	
	public void destroy(){
		default_font.delete();
		default_font = null;
		Toast.delete();
	}
}
