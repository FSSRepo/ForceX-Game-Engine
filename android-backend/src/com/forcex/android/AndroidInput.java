package com.forcex.android;
import com.forcex.utils.*;
import android.view.*;
import android.view.View.*;
import com.forcex.app.*;

public class AndroidInput implements OnTouchListener {
	PoolArray<TouchEvent> events;
	ForceXApp app;
	
	public AndroidInput(ForceXApp app) {
		events = new PoolArray<>(120);
		this.app = app;
	}

	public boolean onTouch(View view, MotionEvent event) {
		if(app.inputs.size() == 0) return true;
		TouchEvent evt = new TouchEvent();
		evt.x = event.getX();
		evt.y = event.getY();
		switch(event.getAction() & 0xff) {
			case MotionEvent.ACTION_DOWN:
				evt.type = EventType.TOUCH_PRESSED;
				evt.pointer = 0;
				break;
			case MotionEvent.ACTION_UP:
				evt.type = EventType.TOUCH_DROPPED;
				evt.pointer = 0;
				break;
			case MotionEvent.ACTION_MOVE:
				evt.type = EventType.TOUCH_DRAGGING;
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				evt.type = EventType.TOUCH_PRESSED_2;
				evt.pointer = 1;
				break;
			case MotionEvent.ACTION_POINTER_UP:
				evt.type = EventType.TOUCH_DROPPED_2;
				evt.pointer = 1;
				break;
		}
		if(evt.type == EventType.TOUCH_DRAGGING && event.getPointerCount() > 1) {
			evt.type = EventType.TOUCH_DRAGGING_2;
		}
		events.push(evt);
		return true;
	}

	void processEvent() {
		TouchEvent evt;
		while((evt = events.pop()) != null){
			for(InputListener input : app.inputs){
				input.onTouch(evt.x, evt.y, evt.type, evt.pointer);
			}
		}
	}

	class TouchEvent {
		float x;
		float y;
		byte type;
		byte pointer;
	}
}
