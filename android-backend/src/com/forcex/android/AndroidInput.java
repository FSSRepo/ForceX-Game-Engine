package com.forcex.android;
import com.forcex.app.*;
import com.forcex.utils.*;

import android.view.*;
import android.view.View.*;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Traduce touch, teclas físicas y texto del diálogo nativo a eventos de {@link InputListener}.
 * <p>
 * Los eventos se encolan en el hilo de UI y se entregan en {@link #processEvent()},
 * que el renderer ejecuta en el hilo de render.
 */
public class AndroidInput implements OnTouchListener, OnKeyListener {
	private static final int PRESSED = 0, DRAGGING = 1, DROPPED = 2;
	PoolArray<TouchEvent> events;
	ForceXApp app;

	private final ConcurrentLinkedQueue<KeyEventData> key_events = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<String> text_events = new ConcurrentLinkedQueue<>();

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

	/**
	 * Teclas físicas del {@link View} del juego. Incluye las teclas de caracteres,
	 * que permiten escribir sin teclado virtual.
	 */
	public boolean onKey(View view, int keyCode, KeyEvent event) {
		if(app.inputs.size() == 0) return false;
		int action = event.getAction();
		if(action != KeyEvent.ACTION_DOWN && action != KeyEvent.ACTION_UP) return false;

		byte key = toKey(keyCode);
		if(key == -1) return false;

		key_events.offer(new KeyEventData(key, action == KeyEvent.ACTION_DOWN));
		return true;
	}

	/**
	 * Encola el texto completo aceptado en el diálogo nativo. Se admite la cadena
	 * vacía para poder borrar el contenido del campo.
	 */
	public void queueText(String text) {
		if(text != null) {
			text_events.offer(text);
		}
	}

	private static byte toKey(int keyCode) {
		if(keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
			return (byte) (Key.A_KEY + (keyCode - KeyEvent.KEYCODE_A));
		}
		if(keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
			return (byte) (Key.KEY_0 + (keyCode - KeyEvent.KEYCODE_0));
		}
		switch(keyCode) {
			case KeyEvent.KEYCODE_SPACE: return Key.KEY_SPACE;
			case KeyEvent.KEYCODE_ENTER:
			case KeyEvent.KEYCODE_NUMPAD_ENTER: return Key.KEY_ENTER;
			case KeyEvent.KEYCODE_DEL: return Key.KEY_DEL;
			case KeyEvent.KEYCODE_TAB: return Key.KEY_TAB;
			case KeyEvent.KEYCODE_ESCAPE: return Key.KEY_ESC;
			case KeyEvent.KEYCODE_CAPS_LOCK: return Key.KEY_CAPITAL;
			case KeyEvent.KEYCODE_DPAD_UP: return Key.KEY_UP;
			case KeyEvent.KEYCODE_DPAD_DOWN: return Key.KEY_DOWN;
			case KeyEvent.KEYCODE_DPAD_LEFT: return Key.KEY_LEFT;
			case KeyEvent.KEYCODE_DPAD_RIGHT: return Key.KEY_RIGHT;
			case KeyEvent.KEYCODE_SHIFT_LEFT: return Key.KEY_LEFT_SHIFT;
			case KeyEvent.KEYCODE_SHIFT_RIGHT: return Key.KEY_RIGHT_SHIFT;
			case KeyEvent.KEYCODE_PERIOD: return Key.KEY_DOT;
			case KeyEvent.KEYCODE_MINUS: return Key.KEY_MINUS;
			default: return -1;
		}
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
		KeyEventData key;
		while((key = key_events.poll()) != null) {
			for(InputListener input : app.inputs) {
				input.onKeyEvent(key.key, key.down);
			}
		}
		String text;
		while((text = text_events.poll()) != null) {
			for(InputListener input : app.inputs) {
				input.onTextInput(text);
			}
		}
	}

	class TouchEvent {
		float x;
		float y;
		byte type;
		byte pointer;
	}

	private static class KeyEventData {
		final byte key;
		final boolean down;

		KeyEventData(byte key, boolean down) {
			this.key = key;
			this.down = down;
		}
	}
}
