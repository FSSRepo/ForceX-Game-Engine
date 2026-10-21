package com.forcex.android;
import com.forcex.utils.*;
import android.view.*;
import android.view.View.*;
import com.forcex.app.*;

public class AndroidInput implements OnTouchListener {
	private static final int PRESSED = 0, DRAGGING = 1, DROPPED = 2;
	PoolArray<TouchEvent> events;
	ForceXApp app;
	
	public AndroidInput(ForceXApp app) {
		events = new PoolArray<>(120);
		this.app = app;
	}

	public boolean onTouch(View view, MotionEvent event) {
		if(app.inputs.size() == 0) return true;
		int index = event.getActionIndex();
		switch(event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				push(event, index, PRESSED);
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				push(event, index, DROPPED);
				break;
			case MotionEvent.ACTION_CANCEL:
				for(int i = 0; i < event.getPointerCount(); i++) {
					push(event, i, DROPPED);
				}
				break;
			case MotionEvent.ACTION_MOVE:
				// Cada dedo se reporta con su propia posición.
				for(int i = 0; i < event.getPointerCount(); i++) {
					push(event, i, DRAGGING);
				}
				break;
		}
		return true;
	}

	// El primer dedo (id 0) usa los eventos TOUCH_*, el resto los TOUCH_*_2; pointer lleva el id del dedo.
	private void push(MotionEvent event, int index, int state) {
		TouchEvent evt = new TouchEvent();
		evt.x = event.getX(index);
		evt.y = event.getY(index);
		int id = event.getPointerId(index);
		boolean primary = id == 0;
		switch(state) {
			case PRESSED:
				evt.type = primary ? EventType.TOUCH_PRESSED : EventType.TOUCH_PRESSED_2;
				break;
			case DRAGGING:
				evt.type = primary ? EventType.TOUCH_DRAGGING : EventType.TOUCH_DRAGGING_2;
				break;
			default:
				evt.type = primary ? EventType.TOUCH_DROPPED : EventType.TOUCH_DROPPED_2;
				break;
		}
		evt.pointer = (byte) id;
		events.push(evt);
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
