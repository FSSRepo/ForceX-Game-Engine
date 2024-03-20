package com.forcex.gui.widgets;
import com.forcex.gui.*;
import com.forcex.math.*;
import com.forcex.utils.*;
import com.forcex.*;
import java.util.*;
import com.forcex.app.*;
import com.forcex.core.*;

public class HelpTip {
	View focus;
	TextView text;
	public boolean showing;
	Vector2f position;
	Color color,btn_color,arrow_color,drag_color;
	UIContext ctx;
	Vector2f local, btn_extents,arrow_from,arrow_to,drag_from,drag_to;
	boolean request_show = false,request_dimiss = false,left = false,inverse_arrow = false;
	float timer;
	LanguageString lang;
	int tvbo,drag_image = -1;
	QueueTip current;
	Vector2f img_location;
	
	class QueueTip{
		String title;
		String description;
		View view;
		boolean drag_x;
		boolean drag_y;
		int reference_img = -1;
		float img_width,img_height;
	}
	
	ArrayList<QueueTip> tips;
	
	public HelpTip(UIContext ctx){
		position = new Vector2f();
		btn_extents = new Vector2f();
		arrow_from = new Vector2f();
		arrow_to = new Vector2f();
		drag_from = new Vector2f();
		drag_to = new Vector2f();
		color = new Color(0,0,0,180);
		btn_color = new Color(225,225,225);
		arrow_color = new Color(0,244,0);
		this.ctx = ctx;
		tips = new ArrayList<QueueTip>();
		text = new TextView(ctx.default_font);
		text.setConstraintWidth(0.4f);
		text.setTextColor(255,255,255);
		tvbo = ctx.getDrawer().createBuffer(new float[]{
							 0,0.5f,0,0,1,1,1,
							 -1.5f,0,0,0,1,1,1,
							 0,-0.5f,0,0,1,1,1
						 },false,false);
		drag_color = new Color(0xffffffff);
		img_location = new Vector2f();
	}
	
	public void setLanguage(LanguageString lang){
		this.lang = lang;
	}
	
	public void add(String title,String description,View view){
		QueueTip tip = new QueueTip();
		tip.title = title;
		tip.description = description;
		tip.view = view;
		tips.add(tip);
		request_show = true;
		showing = true;
		timer = 0.0f;
	}
	
	public void setDragIcon(int texture){
		drag_image = texture;
	}
	
	public void addAndDragX(String title,String description,View view){
		QueueTip tip = new QueueTip();
		tip.title = title;
		tip.description = description;
		tip.view = view;
		tip.drag_x = true;
		tips.add(tip);
		request_show = true;
		showing = true;
		timer = 0.0f;
	}
	
	public void addAndDragY(String title,String description,View view){
		QueueTip tip = new QueueTip();
		tip.title = title;
		tip.description = description;
		tip.view = view;
		tip.drag_y = true;
		tips.add(tip);
		request_show = true;
		showing = true;
		timer = 0.0f;
	}
	
	public void setReferenceImage(int image,float width,float height,boolean aspectRatio){
		if(tips.size() > 0){
			QueueTip t = tips.get(tips.size() - 1);
			t.reference_img = image;
			t.img_height = (aspectRatio ? height * ctx.getAspectRatio():height);
			t.img_width = width;
		}
	}
	
	private void show(View view){
		this.focus = view;
		local = view.local;
		focus.setVisibility(View.INVISIBLE);
		focus.updateExtent();
	}
	
	public void onTouch(float x,float y,byte type){
		if(focus != null && type == EventType.TOUCH_DROPPED && GameUtils.testRect(x,y,text.local.set(0.8f,-0.8f),btn_extents.x,btn_extents.y)){
			focus.setVisibility(View.VISIBLE);
			tips.remove(current);
			current = null;
			if(!focus.hasParent()){
				focus.onDestroy();
			}
			focus = null;
			if(tips.size() == 0){
				request_dimiss = true;
			}
		}
	}
	
	public void drawTriangle(Drawer drawer,float rot,float porcent){
		drawer.setTransform(rot,0.07f,0.07f * ctx.getAspectRatio());
		drawer.freeRender(tvbo,arrow_from.lerp(arrow_to,porcent),arrow_color,-1);
		FX.gl.glDrawArrays(GL.GL_TRIANGLES,0,3);
	}
	
	public void drawDragIcon(Drawer drawer,float porcent){
		if(inverse_arrow){
			drag_color.a = (short)(255f * (1 - porcent));
		}else{
			drag_color.a = (short)(255f * porcent);
		}
		drawer.setScale(0.07f,0.07f*ctx.getAspectRatio());
		drawer.renderQuad(!inverse_arrow ? drag_from.lerp(drag_to,porcent) : drag_to,drag_color,drag_image);
	}
	
	boolean dragging = false;
	
	public void render(Drawer drawer){
		if(request_show){
			color.a = (short)(170f * (timer / 0.5f));
			if(timer > 0.5f){
				request_show = false;
				timer = 0.0f;
			}
			timer += FX.gpu.getDeltaTime();
		}
		if(request_dimiss){
			color.a = (short)(170f * (1 - (timer / 0.5f)));
			if(timer > 0.5f){
				request_dimiss = false;
				showing = false;
				timer = 0.0f;
			}
			timer += FX.gpu.getDeltaTime();
		}
		drawer.setScale(1f,1f);
		drawer.renderQuad(position,color,-1);
		if(!request_show && tips.size() > 0){
			if(current == null){
				current = tips.get(0);
				if(current.view == null || processViewSlide()){
					current = null;
					return;
				}
				show(current.view);
				left = local.x <= 0;
				arrow_from.set(local.x + (focus.getExtentWidth() + 0.1f) * (left ? 1 : -1.0f),local.y);
				arrow_to.set(local.x + (focus.getExtentWidth() + 0.2f) * (left ? 1 : -1.0f),local.y);
				dragging = false;
				if(current.drag_x){
					drag_from.x = local.x - focus.getExtentWidth();
					drag_to.x = local.x + focus.getExtentWidth();
					drag_to.y = drag_from.y = local.y;
					dragging = true;
				}else if(current.drag_y){
					drag_from.y = local.y - focus.getExtentHeight();
					drag_to.y = local.y + focus.getExtentHeight();
					drag_to.x = drag_from.x = local.x;
					dragging = true;
				}
			}
			drawTriangle(drawer,left ? 0 : 180f,inverse_arrow ? (1 - (timer / 0.8f)) : (timer/0.8f));
			if(timer > 0.8f){
				inverse_arrow = !inverse_arrow;
				timer = 0.0f;
			}
			timer += FX.gpu.getDeltaTime();
			focus.local.set(local);
			focus.onDraw(drawer);
			if(dragging){
				drawDragIcon(drawer,timer / 0.8f);
			}
			text.setTextColor(0,0,0);
			renderButton(drawer);
			text.setTextColor(255,255,255);
			renderTitle(drawer);
			renderDescription(drawer);
			if(current.reference_img != -1){
				img_location.x = left ? (0.95f - current.img_width) : (-0.95f + current.img_width);
				img_location.y = text.local.y - (0.015f + text.getHeight() + current.img_height);
				drawer.setScale(current.img_width,current.img_height);
				drawer.renderQuad(img_location,null,current.reference_img);
			}
		}
	}
	
	private boolean processViewSlide(){
		View v = current.view;
		if(v.hasParent() && v.getParent().isLayout()) {
			return ((Layout)v.getParent()).usingInSlidePanel && !((Layout)v.getParent()).finishedAnim;
		}
		return false;
	}
	
	private void renderButton(Drawer drawer){
		text.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
		text.setTextSize(0.07f);
		text.setText(lang != null ? lang.get("next") : "Next");
		text.local.set(0.8f,-0.8f);
		btn_extents.x = text.getWidth()*1.5f;
		btn_extents.y = text.getHeight()*1.2f;
		drawer.setScale(btn_extents.x,btn_extents.y);
		drawer.renderQuad(text.local,btn_color,-1);
		text.onDraw(drawer);
	}
	
	private void renderTitle(Drawer drawer){
		text.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
		text.setTextSize(0.07f);
		text.setText(current.title);
		text.local.x = -0.95f + text.getWidth();
		boolean invasion = text.local.x > focus.local.x - focus.getExtentWidth() && text.local.x < focus.local.x + focus.getExtentWidth();
		if(invasion){
			float margin_right = (local.x + focus.getExtentWidth() + 0.05f) + text.getWidth();
			float margin_left = (local.x - focus.getExtentWidth() - 0.05f) - text.getWidth();
			if(text.local.x > focus.local.x){ // check right
				if(margin_left < 1){
					text.local.x = margin_right;
				}else{
					text.local.x = margin_left;
				}
			}else{ // is left
				if(margin_left > -1){
					text.local.x = margin_left;
				}else{
					text.local.x = margin_right;
				}
			}
		}
		text.local.y = (local.y + focus.getExtentHeight()) < 0.8f ? 0.98f - text.getHeight() : ((local.y - focus.getExtentHeight()) - 0.1f - text.getHeight());
		if(text.local.y < -1.0f){
			text.local.y = 0.98f - text.getHeight();
		}else if(text.local.y < -0.5f){
			text.local.y = (local.y + focus.getExtentHeight() - text.getHeight());
		}
		text.onDraw(drawer);
	}
	
	private void renderDescription(Drawer drawer){
		float x = text.local.x - text.getWidth();
		float y = text.local.y - text.getHeight();
		text.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER_LEFT);
		text.setTextSize(0.05f);
		text.setText(current.description);
		text.local.x = x + text.getWidth();
		text.local.y = y - 0.05f - text.getHeight();
		text.onDraw(drawer);
	}
}
