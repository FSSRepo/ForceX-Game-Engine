package com.forcex.rte.utils;
import com.forcex.utils.*;

public class RTColor {
	public float r,g,b,a;
	
	public RTColor(Color color){
		r = color.r / 255f;
		g = color.g / 255f;
		b = color.b / 255f;
		a = color.a / 255f;
	}
	
	public RTColor(float red,float green,float blue){
		this(red,green,blue,1f);
	}
	
	public RTColor(float red,float green,float blue,float alpha){
		r = red;
		g = green;
		b = blue;
		a = alpha;
		normalize();
	}
	
	public float getLuminance() {
        return r*0.2126F + g*0.7152F + b*0.0722F;
    }
	
	public RTColor mult(float amount){
		return new RTColor(r * amount,g * amount,b * amount, a);
	}
	
	public RTColor add(float amount){
		return new RTColor(r + amount,g + amount,b + amount, a);
	}
	
	public RTColor add(RTColor color){
		return new RTColor(r + color.r,g + color.g,b + color.b, a);
	}
	
	public RTColor sub(RTColor color){
		return new RTColor(r - color.r,g - color.g,b - color.b, a);
	}
	
	public RTColor set(Color color){
		r = color.r / 255f;
		g = color.g / 255f;
		b = color.b / 255f;
		a = color.a / 255f;
		return this;
	}
	
	public RTColor lerp(RTColor b,float porcent){
		return new RTColor(	
			r + porcent * (b.r - r),
			g + porcent * (b.g - g),
			this.b + porcent * (b.b - this.b),
			a
		);
	}
	
	public RTColor set(RTColor color){
		r = color.r;
		g = color.g;
		b = color.b;
		a = color.a;
		normalize();
		return this;
	}
	
	public void normalize(){
		r = r > 1 ? 1 : r;
		g = g > 1 ? 1 : g;
		b = b > 1 ? 1 : b;
	}
}
