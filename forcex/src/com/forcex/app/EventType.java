package com.forcex.app;

public class EventType {
    public static final byte TOUCH_DRAGGING = 	 0x34;
    public static final byte TOUCH_DROPPED =	 0x35;
    public static final byte TOUCH_PRESSED = 	 0x36;
	public static final byte TOUCH_DROPPED_2 =	 0x37;
    public static final byte TOUCH_PRESSED_2 = 	 0x38;
	public static final byte TOUCH_DRAGGING_2 =  0x39;
    public static final byte MOUSE_SCROLL =      0x40;

    // continuous mouse position request
    public static final byte MOUSE_HOVER =       0x41;
	
    public static final byte BACK_BUTTON = 		 0x12;
    public static final byte PAUSE_EVENT = 		 0x13;
	public static final byte REQUEST_EXIT = 	 0x14;
	public static final byte NOTHING = 		 	 0x15;
	public static final byte TOUCH_POINTER = 	 0x3A;
}
