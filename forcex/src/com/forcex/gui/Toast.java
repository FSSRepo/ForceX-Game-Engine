package com.forcex.gui;
import com.forcex.gui.widgets.*;
import java.util.*;
import com.forcex.math.*;
import com.forcex.utils.*;
import com.forcex.*;

public class Toast {
	private float width,height,text_size;
	private Color background;
	private ArrayList<QueueNotification> queue = new ArrayList<>();
	private TextView textview;
	private static final byte NOTIFICATION_INFO =		 0;
	private static final byte NOTIFICATION_ERROR =		 1;
	private static final byte NOTIFICATION_DEBUG =		 2;
	private static final byte NOTIFICATION_WARNING = 	 3;
	protected static Toast instance;
	QueueNotification toast0;
	QueueNotification toast1;
	float pos_y_down, pos_y_up;
	
	private Toast(UIContext context){
		background = new Color(0,0,0,180);
		textview = new TextView(UIContext.default_font);
		textview.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER_LEFT);
		textview.setTextColor(0,0,0);
		text_size = 0.04f;
		textview.setTextSize(text_size);
		width = 0.4f;
		height = 0.1f * context.getAspectRatio();
		textview.setConstraintWidth(width - 0.04f);
		pos_y_down = -1 + 0.03f + height;
		pos_y_up = (0.03f + height) * 2f;
	}
	
	protected void addQueue(QueueNotification n){
		n.position.set(0,pos_y_down);
		queue.add(n);
	}

	protected static void create(UIContext context){
		if(instance == null){
			instance = new Toast(context);
		}
	}
	
	public static void setCancellable(boolean z){
		instance.queue.get(instance.queue.size() - 1).cancellable = z;
	}
	
	public static void info(String text,float duration){
		QueueNotification q = new QueueNotification();
		q.duration = duration;
		q.text = text;
		q.notification_type = NOTIFICATION_INFO;
		instance.addQueue(q);
	}

	public static void error(String text,float duration){
		QueueNotification q = new QueueNotification();
		q.duration = duration;
		q.text = text;
		q.notification_type = NOTIFICATION_ERROR;
		if(instance == null){
			q = null;
			return;
		}
		instance.addQueue(q);
	}

	public static void debug(String text,float duration){
		QueueNotification q = new QueueNotification();
		q.duration = duration;
		q.text = text;
		q.notification_type = NOTIFICATION_DEBUG;
		instance.addQueue(q);
	}

	public static void warning(String text,float duration){
		QueueNotification q = new QueueNotification();
		q.duration = duration;
		q.text = text;
		q.notification_type = NOTIFICATION_WARNING;
		instance.addQueue(q);
	}

	boolean click_t0 = true;
	protected boolean testTouch(float x,float y){
		if(queue.isEmpty() || toast0 == null){
			return false;
		}
		if(GameUtils.testRect(x,y,toast0.position,width,height)){
			click_t0 = true;
			return true;
		}
		if(toast1 != null && GameUtils.testRect(x,y,toast1.position,width,height)){
			click_t0 = false;
			return true;
		}
		return false;
	}

	protected void onTouch(boolean press){
		if(press) {
			if(click_t0 && toast0.cancellable){
				if(toast0.state != 2){
					toast0.lifetime = toast0.duration * 0.9f;
					toast0.state = 2;
				}
			}else if(toast1 != null && toast1.state != 2 && toast1.cancellable){
				toast1.lifetime = toast1.duration * 0.9f;
				toast1.state = 2;
			}
		}
	}
	
	protected void render(Drawer drawer){
		if(!queue.isEmpty()){
			toast0 = queue.get(0);
			if(queue.size() > 1){
				toast1 = queue.get(1);
				if(!toast0.down && toast0.state == 3){
					toast0.state = 0;
					toast0.position.y = (pos_y_down + pos_y_up);
				}
				if(toast0.down && toast0.state == 0){
					toast0.state = 1;
				}
			}else{
				toast1 = null;
			}
			float porcent1 = toast0.getPorcent();
			float porcent2 = toast1 == null ? -1.0f : toast1.getPorcent();
			setupToast(toast0);
			drawToast(drawer,toast0,porcent1);
			if(porcent2 != -1){
				setupToast(toast1);
				drawToast(drawer,toast1,porcent2);
			}
		}
	}
	
	float porcent_seek = -1;
	
	private void drawToast(Drawer drawer,QueueNotification current,float porcent){
		if(porcent < 0.1f){
			float x = (porcent / 0.1f);
			if(current.state != 2){
				current.position.x = (0.95f - (width * x));
			}
			background.a = (short)(180.0f * x);
			textview.getTextColor().a = (short)(255.0f * x);
		}else if(porcent > 0.1f && porcent < 0.9f){
			current.position.x = 0.95f - width;
			textview.default_color.a = 255;
			background.a = (short)(180);
			textview.setTextSize(text_size);
		}
		if(porcent > 0.9f){
			float x = (porcent - 0.9f) / 0.1f;
			background.a = (short)(180.0f * (1f - x));
			if(current.state != 2){
				current.position.x = ((0.98f - width) + (width * x));
			}else{
				float sampleWidth = width * (1 - x);
				float sampleHeight = height * (1 - x);
				textview.local.set(
					current.position.x - sampleWidth + (0.03f + textview.getWidth()),
					current.position.y + sampleHeight - (textview.getHeight() + 0.03f));
				drawer.setScale(sampleWidth,sampleHeight);
				drawer.renderQuad(current.position,background,-1);
				textview.setTextSize(text_size * (1 - x));
			}
			textview.getTextColor().a = (short)(255.0f * (1f - x));
		}
		if(current.state == 1 || current.state == 3){
			if(porcent_seek == -1){
				porcent_seek = porcent;
			}
			float anim = (porcent - porcent_seek) / 0.1f;
			current.position.y = (current.state == 1) ? (pos_y_down + pos_y_up * anim):((pos_y_down + pos_y_up * (1 - anim)));
			if(anim > 1f){
				current.position.y = (current.state == 1) ? (pos_y_down + pos_y_up) : (pos_y_down);
				current.down = current.state == 3;
				current.state = 0;
				porcent_seek = -1;
				
			}
		}
		if(porcent > 1.0f){
			queue.remove(current);
			textview.setTextSize(text_size);
			porcent_seek = -1;
			if(current == toast1 && toast0 != null){
				if(!toast0.down && toast0.state == 0){
					toast0.state = 3;
					toast1 = null;
					return;
				}
			}
			toast0 = null;
			return;
		}
		if(current.state != 2){
			drawer.setScale(width,height);
			drawer.renderQuad(current.position,background,-1);
			textview.local.set(
				current.position.x - width + (0.03f + textview.getWidth()),
				current.position.y + height - (textview.getHeight() + 0.03f));
		}
		textview.onDraw(drawer);
		current.lifetime += FX.gpu.getDeltaTime();
	}
	
	private void setupToast(QueueNotification not){
		textview.setText(not.text);
		switch(not.notification_type){
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
	}

	protected static void delete(){
		instance = null;
	}

	private static class QueueNotification {
		float lifetime;
		float duration;
		String text;
		byte notification_type;
		Vector2f position = new Vector2f();
		int state = 0; // up = 1, down =3, clicked = 2,showing = 0
		boolean down = true;
		boolean cancellable = true;
		
		public float getPorcent() {
			return lifetime / duration;
		}
	}
}
