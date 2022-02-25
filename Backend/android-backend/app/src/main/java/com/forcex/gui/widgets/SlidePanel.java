package com.forcex.gui.widgets;
import com.forcex.gui.*;
import com.forcex.utils.*;
import com.forcex.core.*;
import com.forcex.*;
import com.forcex.core.gpu.*;
import com.forcex.math.*;
import com.forcex.app.*;

public class SlidePanel {
	private float duration = 0.25f;
	private byte status = 0;
	private Color color;
	private int texture = -1;
	private Layout layout;
	private Layout queue;
	private float anim;
	private Vector2f position;
	float height,y_position;
	boolean show_again = false;
	
	public SlidePanel(Layout layout){
		color = new Color(Color.WHITE);
		position = new Vector2f();
		this.layout = layout;
		layout.beforeSetting = true;
		status = 0;
		height = 1;
	}
	
	public void setContent(Layout layout){
		this.layout = layout;
		this.layout.beforeSetting = true;
	}
	
	public void setYPosition(float y){
		y_position = y;
	}
	
	public void setHeight(float height){
		this.height = height;
	}

	public boolean isShowing(){
		return (status == 3 || status == 1);
	}
	
	public void showWithContent(Layout content){
		if(isShowing()){
			dimiss();
			queue = content;
			show_again = true;
		}else{
			setContent(content);
			show();
		}
	}
	

	public void show(){
		if(status == 0){
			status = 1;
			timer = 0.0f;
		}
	}

	public void dimiss(){
		if(status == 3){
			status = 2;
			timer = 0f;
		}
	}

	public void setBackGroundTexture(String path){
		Texture.remove(texture);
		texture = Texture.load(path,false);
	}

	public void setBackGoundColor(int colorHex){
		color.set(colorHex);
	}
	
	public Layout getContent(){
		return layout;
	}
	
	float timer = 0;
	Vector2f relative = new Vector2f();

	public void render(Drawer drawer) {
		if(status != 0){
			layout.settingLayout();
			position.set(-1 + layout.getExtentWidth(),(y_position + height) - layout.getExtentHeight());
			anim = -1 - layout.getExtentWidth();
			processAnimation();
			drawer.setScale(layout.getExtentWidth(),height);
			position.set(layout.local.x,y_position);
			drawer.renderQuad(position,color,texture);
			if(layout != null){
				layout.onDraw(drawer);
			}
		}else if(show_again){
			setContent(queue);
			queue = null;
			status = 1;
			timer = 0.0f;
			show_again = false;
		}
	}

	private void processAnimation(){
		float porcent = timer / duration;
		switch(status){
			case 1: // init show
				if(porcent > 1.0f){
					status = 3;
					layout.local.set(position);
					break;
				}
				layout.local.set(anim + (porcent * (position.x - anim)),position.y);
				timer += FX.gpu.getDeltaTime();
				break;
			case 2: // dimiss
				if(porcent > 1.0f){
					status = 0;
					layout.local.set(anim,position.y);
					break;
				}
				layout.local.set(position.x + (porcent * (anim - position.x)),position.y);
				timer += FX.gpu.getDeltaTime();
				break;
			case 3:
				break;
		}
	}
	
	public boolean testTouch(float x,float y) {
		return status == 3 && GameUtils.testRect(x,y,position,layout.getExtentWidth(),height);
	}
	
	public void onTouch(float x, float y, byte type) {
		layout.onTouch(x,y,type);
	}
}
