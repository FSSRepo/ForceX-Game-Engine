package com.forcex.utils;

public class Color {

    public short r,g,b,a;

    public Color() {
        r = 255;
        g = 255;
        b = 255;
        a = 255;
    }

    public Color(int r, int g, int b,int a) {
        set(r,g,b,a);
    }

    public Color(int r, int g, int b) {
        set(r,g,b,255);
    }

    public Color(Color o) {
        this.r = o.r;
        this.g = o.g;
        this.b = o.b;
        this.a = o.a;
    }

    public Color(int color) {
        set(color);
    }

    public void set(int color) {
        r = red(color);
        g = green(color);
        b = blue(color);
        a = alpha(color);
    }
	
	public Color set(int r,int g,int b,int a){
		this.r = (short)r;
		this.g = (short)g;
		this.b = (short)b;
		this.a = (short)a;
		return this;
	}
	
	public Color set(int r,int g,int b){
		this.r = (short)r;
		this.g = (short)g;
		this.b = (short)b;
		return this;
	}
	
	public Color setColor(Color other){
		this.r = other.r;
		this.g = other.g;
		this.b = other.b;
		return this;
	}
	
	public Color setAlpha(float factor){
		this.a = (short)(factor * 255.0f);
		return this;
	}
	
	public Color set(Color other){
		this.r = other.r;
		this.g = other.g;
		this.b = other.b;
		this.a = other.a;
		return this;
	}
	
    public static int rgb(int red, int green, int blue) {
        return (0xFF << 24) | (red << 16) | (green << 8) | blue;
    }

    public static int argb(int alpha, int red, int green, int blue) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
	
    //Basic
    public static final int WHITE = (0XFFFFFFFF);
    public static final int RED = (0XFFFF0000);
    public static final int GREEN = (0XFF00FF00);
    public static final int BLUE = (0XFF0000FF);
    public static final int BLACK = (0xFF000000);
    public static final int VIOLET = (0xFFFF00FF);
    public static final int YELLOW = (0XFFFFFF00);
    public static final int ORANGE = (0XFFFFA600);
    public static final int GREY = (0xFF888888);
    public static final int TRANSPARENT = (0x00000000);

    public static short red(int color) {
        return (short)((color >> 16) & 0xFF);
    }

    public static short green(int color) {
        return (short)((color >> 8) & 0xFF);
    }

    public static short blue(int color) {
        return (short)(color & 0xFF);
    }

    public static short alpha(int color) {
        return (short)(color >>> 24);
    }

    public int toRGBA() {
        return argb(a, r, g, b);
    }

    public byte[] getData() {
        return new byte[]{(byte) r, (byte) g, (byte) b, (byte) a};
    }

    @Override
    public boolean equals(Object o) {
        Color c = (Color) o;
        return (r == c.r && g == c.g && b == c.b);
    }

    @Override
    public String toString() {
        return "Red: " + r + " Green: " + g + " Blue: " + b+" Alpha: "+a;
    }

    public String toHex() {
        return Integer.toHexString(toRGBA());
    }
}
