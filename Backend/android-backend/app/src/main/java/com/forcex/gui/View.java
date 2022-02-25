package com.forcex.gui;
import com.forcex.math.*;
import com.forcex.app.*;
import com.forcex.utils.*;

public class View {
	public static final byte VISIBLE 					= 0xA;
	public static final byte INVISIBLE 					= 0xB;
	public static final byte GONE 						= 0xC;
	
	private byte visibility;
	public Vector2f local,relative;
	protected Vector2f extent;
	private int id;
	protected OnClickListener listener;
	protected UIContext context;
	View parent,previus,next;
	protected boolean created = false,applyAspectRatio = false,ignoreTouch = false,noApplyYConstaint = false;
	protected float margin_right = 0.0f,
					margin_left = 0.0f,
					margin_top = 0.0f,
					margin_buttom = 0.0f,
					// Only Wrap Content
					width = 0.0f, 
					height = 0.0f;

	protected byte alignment,width_type,height_type;
	static int id_gen = 0;
	
	public View(){
		local = new Vector2f();
		extent = new Vector2f();
		relative = new Vector2f();
		visibility = VISIBLE;
		width_type = Layout.WRAP_CONTENT;
		height_type = Layout.WRAP_CONTENT;
		id = id_gen;
		id_gen++;
		alignment = Layout.ALIGNMENT_NONE;
	}
	
	public void setRelativePosition(float x,float y){
		relative.set(x,y);
	}
	
	public void setId(int id){
		this.id = id;
	}
	
	public void setAlignment(byte alignment){
		this.alignment = alignment;
	}
	
	public int getId(){
		return id;
	}
	
	public boolean isVisible(){
		return visibility == VISIBLE;
	}
	
	public byte getVisibility(){
		return visibility;
	}
	
	public void setVisibility(byte visibility){
		this.visibility = visibility;
	}
	
	public void onCreate(Drawer drawer){}
	
	public void onDraw(Drawer drawer){}
	
	public void onDestroy(){}
	
	public void onTouch(float x,float y,byte type){}
	
	public void updateExtent(){}
	
	void draw(Drawer drawer){
		if(!hasParent()){
			extent.set(width,height * (applyAspectRatio ? context.getAspectRatio() : 1.0f));
			local.set(relative);
		}
		if(!created){
			onCreate(drawer);
			created = true;
		}
		onDraw(drawer);
	}
	
	protected void dispatchTouch(float x,float y,byte type){
		if(type == EventType.TOUCH_DROPPED && listener != null){
			listener.OnClick(this);
		}
		onTouch(x,y,type);
	}
	
	public boolean isLayout(){
		return this instanceof Layout;
	}
	
	public boolean hasParent(){
		return parent != null;
	}
	
	public boolean hasPrevius(){
		return previus != null;
	}
	
	public boolean hasNext(){
		return next != null;
	}
	
	public View getParent(){
		return parent;
	}
	
	public View getPrevius(){
		return previus;
	}
	
	public View getNext(){
		return next;
	}
	
	public void setOnClickListener(OnClickListener listener){
		this.listener = listener;
	}
	
	public void setIgnoreTouch(boolean z){
		this.ignoreTouch = z;
	}
	
	public void setApplyAspectRatio(boolean z){
		applyAspectRatio = z;
	}
	
	public float getExtentWidth(){
		return extent.x;
	}
	
	public float getExtentHeight(){
		return extent.y;
	}
	
	public void setWidthType(byte type){
		width_type = type;
	}

	public void setHeightType(byte type){
		height_type = type;
	}
	
	public void setMarginTop(float margin){
		margin_top = margin;
	}
	
	public void setMarginRight(float margin){
		margin_right = margin;
	}
	
	public void setMarginLeft(float margin){
		margin_left = margin;
	}
	
	public void setMarginBottom(float margin){
		margin_buttom = margin;
	}
	
	public void setNoApplyConstraintY(boolean z){
		noApplyYConstaint = z;
	}
	
	public void setWidth(float width){
		this.width = width;
		width_type = Layout.WRAP_CONTENT;
	}
	
	public void setHeight(float height){
		this.height = height;
		height_type = Layout.WRAP_CONTENT;
	}
	
	public float getWidth(){
		return width;
	}
	
	public float getHeight(){
		return height;
	}
	
	protected boolean testTouch(float x,float y){
		return 
			x >= (local.x - extent.x) && 
			x <= (local.x + extent.x) &&    
			y >= (local.y - extent.y) &&     
			y <= (local.y + extent.y);
	}
	
	public static interface OnClickListener{
		void OnClick(View view);
	}
}
