package com.forcex.gui;
import com.forcex.gui.widgets.*;
import java.util.*;
import com.forcex.math.*;
import com.forcex.utils.*;
import com.forcex.*;

public class Toast {
	private float time,width,text_size,height;
	private Color background;
	private ArrayList<QueueNotification> queue = new ArrayList<>();
	private TextView textview;
	private Vector2f position;
	private static final byte NOTIFICATION_INFO =		 0;
	private static final byte NOTIFICATION_ERROR =		 1;
	private static final byte NOTIFICATION_DEBUG =		 2;
	private static final byte NOTIFICATION_WARNING = 	 3;
	protected static Toast instance;
	boolean clicked = false;

	private Toast(UIContext context){
		background = new Color(0,0,0,180);
		textview = new TextView(context.default_font);
		textview.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER_LEFT);
		textview.setTextColor(255,255,255);
		text_size = 0.04f;
		textview.setTextSize(text_size);
		time = 0.0f;
		width = 0.4f;
		height = 0.1f * context.getAspectRatio();
		position = new Vector2f(0,-1 + 0.03f + height);
		textview.setConstraintWidth(width - 0.04f);
	}

	protected static void create(UIContext context){
		if(instance == null){
			instance = new Toast(context);
		}
	}
	static boolean first = true;
	static QueueNotification profiling;
	
	public static void profile(String text,float duration){
		if(first){
			if(profiling == null){
				info("Profiled -> "+text,duration);
				profiling = instance.queue.get(instance.queue.size() - 1);
			} else {
				profiling.text = text;
			}
		}
	}

	public static void info(String text,float duration){
		QueueNotification q = new QueueNotification();
		q.duration = duration;
		q.text = text;
		q.notification_type = NOTIFICATION_INFO;
		instance.queue.add(q);
	}

	public static void error(String text,float duration){
		QueueNotification q = new QueueNotification();
		q.duration = duration;
		q.text = text;
		q.notification_type = NOTIFICATION_ERROR;
		instance.queue.add(q);
	}

	public static void debug(String text,float duration){
		QueueNotification q = new QueueNotification();
		q.duration = duration;
		q.text = text;
		q.notification_type = NOTIFICATION_DEBUG;
		instance.queue.add(q);
	}

	public static void warning(String text,float duration){
		QueueNotification q = new QueueNotification();
		q.duration = duration;
		q.text = text;
		q.notification_type = NOTIFICATION_WARNING;
		instance.queue.add(q);
	}

	protected boolean testTouch(float x,float y){
		return !queue.isEmpty() && GameUtils.testRect(x,y,position,width,height);
	}

	protected void onTouch(boolean press){
		if(press){
			time = queue.get(0).duration * 0.9f;
			clicked = true;
		}
	}

	protected void render(Drawer drawer){
		if(!queue.isEmpty()){
			QueueNotification it = queue.get(0);
			float porcent = time / it.duration;
			if(profiling != null && it == profiling){
				textview.setText(it.text);
			}
			if(porcent < 0.1f){
				textview.setTextColor(0,0,0);
				if(profiling == null || it != profiling){
					textview.setText(it.text);
				}
				switch(it.notification_type){
					case NOTIFICATION_INFO:
						background.set(37,110,255);
						break;
					case NOTIFICATION_ERROR:
						background.set(255,38,57);
						break;
					case NOTIFICATION_DEBUG:
						background.set(29,185,82);
						break;
					case NOTIFICATION_WARNING:
						background.set(240,193,0);
						break;
				}
				
				float x = (porcent / 0.1f);
				if(!clicked){
					position.x = (0.95f - (width * x));
				}
				background.a = (short)(180.0f * x);
				textview.getTextColor().a = (short)(255.0f * x);
			}else if(porcent > 0.1f && porcent < 0.9f){
				position.x = 0.95f - width;
			}
			if(porcent > 0.9f){
				float x = (porcent - 0.9f) / 0.1f;
				background.a = (short)(180.0f * (1f - x));
				if(!clicked){
					position.x = ((0.98f - width) + (width * x));
				}else{
					float sampleWidth = width * (1 - x);
					float sampleHeight = height * (1 - x);
					textview.local.set(position.x - sampleWidth + (0.03f + textview.getWidth()),position.y + sampleHeight - (textview.getHeight() + 0.03f));
					drawer.setScale(sampleWidth,sampleHeight);
					drawer.renderQuad(position,background,-1);
					textview.setTextSize(text_size *(1-x));
				}
				textview.getTextColor().a = (short)(255.0f * (1f - x));
			}
			
			if(porcent > 1.0f){
				time = 0.0f;
				queue.remove(it);
				if(profiling != null && profiling == it){
					profiling = null;
					first = false;
				} 
				clicked = false;
				textview.setTextSize(text_size);
			}
			if(!clicked){
				drawer.setScale(width,height);
				drawer.renderQuad(position,background,-1);
				textview.local.set(position.x - width + (0.03f + textview.getWidth()),position.y + height - (textview.getHeight() + 0.03f));
			}
			textview.onDraw(drawer);
			time += FX.gpu.getDeltaTime();
		}
	}

	protected static void delete(){
		instance = null;
	}

	private static class QueueNotification{
		float duration;
		String text;
		byte notification_type;
	}
}
