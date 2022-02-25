package com.forcex.gui;
import java.util.*;
import com.forcex.utils.*;
import com.forcex.math.*;

public class Layout extends View {
	public static final byte VERTICAL 				= 0x1;
	public static final byte HORIZONTAL 			= 0x2;
	public static final byte FILL_PARENT 			= 0x1;
	public static final byte MATCH_PARENT 			= 0x2;
	public static final byte WRAP_CONTENT			= 0x3;
	public static final byte CENTER 				= 0xA;
	public static final byte RIGHT 					= 0xB;
	public static final byte LEFT 					= 0xC;

	ArrayList<View> children = new ArrayList<>();
	private boolean isRelative = false;
	protected byte orientation;
	public boolean beforeSetting = false,widthCustom = false;
	
	public Layout(UIContext context){
		orientation = VERTICAL;
		width_type = FILL_PARENT;
		height_type = FILL_PARENT;
		this.context = context;
		setDebugColor(73,138,19);
	}

	public void add(View view){
		if(view.parent == null){
			view.parent = this;
			view.context = context;
			children.add(view);
		}
	}
	
	public void add(View view,int index){
		if(view.parent == null){
			view.parent = this;
			view.context = context;
			children.add(index,view);
		}
	}
	
	public int indexOf(View view){
		return children.indexOf(view);
	}
	
	public View getView(int index){
		return children.get(index);
	}
	
	public void remove(View view){
		if(view.parent == this){
			children.remove(view);
			view.parent = null;
			view.previus = null;
			view.next = null;
		}
	}

	public void removeAll(boolean destroy){
		for(View view : children){
			if(destroy){
				view.onDestroy();
			}
			view.parent = null;
		}
		children.clear();
	}

	public void setIsRelative(boolean z){
		isRelative = z;
		if(z){
			width_type = WRAP_CONTENT;
			height_type = WRAP_CONTENT;
		}else{
			setToWrapContent();
		}
	}
	
	public void setUseWidthCustom(boolean z){
		widthCustom = z;
	}

	public void setOrientation(byte orientation){
		this.orientation = orientation;
	}

	public void setToWrapContent(){
		width_type = WRAP_CONTENT;
		height_type = WRAP_CONTENT;
	}

	@Override
	void draw(Drawer drawer) {
		if(!created){
			onCreate(drawer);
			created = true;
		}
		onDraw(drawer);
	}

	@Override
	public void onDraw(Drawer drawer) {
		if(!beforeSetting){
			settingLayout();
		}
		for(short i = 0;i < children.size();i++){
			View view = children.get(i);
			if(view.isVisible()){
				view.draw(drawer);
				view.debug = debug;
			}
		}
		if(debug){
			drawer.setScale(extent.x,extent.y);
			drawer.renderLineQuad(local,debug_color);
		}
	}
	
	public boolean testIsTouching(float x,float y){
		for(short i = 0;i < children.size();i++){
			if(children.get(i).isVisible() && !children.get(i).ignoreTouch){
				if(children.get(i).testTouch(x,y)){
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void onTouch(float x, float y, byte type) {
		for(short i = 0;i < children.size();i++){
			if(children.get(i).isVisible() && !children.get(i).ignoreTouch){
				if(children.get(i).testTouch(x,y)){
					children.get(i).onTouch(x,y,type);
				}else{
					children.get(i).notifyTouchOutside(x,y,type);
				}
			}
		}
	}

	@Override
	public void onDestroy() {
		for(View v : children){
			v.onDestroy();
		}
		children.clear();
	}

	public boolean isRoot(){
		return !hasParent();
	}

	@Override
	protected void notifyTouchOutside(float x,float y,byte type) {
		for(short i = 0;i < children.size();i++){
			if(children.get(i).isVisible() && !children.get(i).ignoreTouch){
				children.get(i).notifyTouchOutside(x,y,type);
			}
		}
	}
	
	public void settingLayout(){
		settingExtentView();
		predictLayoutDimens();
		sortViews();
	}

	public void predictLayoutDimens(){
		if(isRoot()){
			if(width_type == FILL_PARENT){
				extent.x = 1f;
			}else{
				if(!widthCustom){
					extent.x = getPredictWidth();
				}else{
					extent.x = width;
				}
			}
			if(height_type == FILL_PARENT){
				extent.y = 1f;
			}else{
				extent.y = getPredictHeight();
			}
		}else{
			// only layouts the parent is a layout
			if(width_type != WRAP_CONTENT){
				extent.x = parent.extent.x;
			}else{
				if(!widthCustom){
					extent.x = getPredictWidth();
				}else{
					extent.x = width;
				}
			}
			if(height_type != WRAP_CONTENT){
				extent.y = parent.extent.y;
			}else{
				extent.y = getPredictHeight();
			}
		}
	}

	public void settingExtentView(){
		for(short i = 0;i < children.size();i++){
			View view = children.get(i);
			if(i-1 >= 0){
				view.previus = children.get(i - 1);
			}
			if(i+1 < children.size()){
				view.next = children.get(i + 1);
			}
			if(view.isLayout()){
				((Layout)view).settingExtentView();
				((Layout)view).predictLayoutDimens();
				continue;
			}
			view.updateExtent();
			if(view.width_type != WRAP_CONTENT){
				view.extent.x = extent.x;
			}else{
				view.extent.x = view.width;
			}
			if(view.height_type != WRAP_CONTENT){
				view.extent.y = extent.y;
			}else{
				view.extent.y = view.height * (view.applyAspectRatio ? context.getAspectRatio() : 1.0f);
			}
		}
	}

	float cursorX = 0.0f,cursorY = 0.0f;

	public void sortViews(){
		if(children.size() == 0)return;
		cursorX = local.x - extent.x;
		cursorY = local.y + extent.y;
		for(short i = 0;i < children.size();i++){
			View view = children.get(i);
			if(view.getVisibility() == View.GONE)continue;
			if(!isRelative){
				if(view.isLayout()){
					((Layout)view).predictLayoutDimens();
				}
				if(orientation == VERTICAL){
					switch(view.alignment){
						case CENTER:
							view.local.x = cursorX + getExtentWidth();
							break;
						case RIGHT:
							view.local.x = (local.x + extent.x) - view.getExtentWidth() - view.margin_right;
							break;
						case LEFT:
							view.local.x = cursorX + view.getExtentWidth() + view.margin_left;
							break;
					}
					if(!view.noApplyYConstaint){
						if(view.hasPrevius()){
							View prev = view.previus;
							if(prev.noApplyYConstaint || prev.getVisibility() == View.GONE){
								prev = getPrevius(i);
								if(prev != null){
									cursorY -= prev.margin_buttom + prev.getExtentHeight();
								}
							}else{
								cursorY -= prev.margin_buttom + prev.getExtentHeight();
							}
						}
						cursorY -= (view.margin_top + view.getExtentHeight());
					}
					view.local.y = cursorY;
				}else{
					if(view.hasPrevius()){
						View prev = view.previus;
						if(prev.getVisibility() == View.GONE){
							prev = getPrevius(i);
							if(prev != null){
								cursorX += prev.margin_right + prev.getExtentWidth();
							}
						}else{
							cursorX += prev.margin_right + prev.getExtentWidth();
						}
					}
					cursorX += view.margin_left + view.getExtentWidth();
					view.local.x = cursorX;
					view.local.y = cursorY - view.margin_top - view.getExtentHeight();
				}
			}else{
				view.local.x = local.x + view.relative.x;
				view.local.y = local.y + view.relative.y;
			}
		}
	}
	
	private View getPrevius(short index){
		for(short i = (short)(index - 1);i >= 0;i--){
			View v = children.get(i);
			if(v.getVisibility() != View.GONE && !v.noApplyYConstaint){
				return v;
			}
		}
		return null;
	}
	
	public float getPredictWidth(){
		float prect_width = 0.0f;
		for(View view : children){
			if(view.getVisibility() == View.GONE)continue;
			if(!isRelative){
				if(orientation == VERTICAL){
					if(prect_width < view.extent.x){
						prect_width = view.extent.x + view.margin_right*0.5f + view.margin_left*0.5f;
					}
				}else{
					if(view.hasPrevius() && view.previus.getVisibility() != View.GONE){
						View prev = view.previus;
						prect_width += prev.margin_right * 0.5f;
					}
					prect_width += view.margin_left * 0.5f + view.getExtentWidth();
				}
			}else{
				float test = Maths.abs(view.relative.x + view.getExtentWidth());
				if(prect_width < test){
					prect_width = test;
				}
			}
		}
		return prect_width;
	}
	
	public float getPredictHeight(){
		float prect_height = 0.0f;
		for(short i = 0;i < children.size();i++){
			View view = children.get(i);
			if(
				view.getVisibility() == View.GONE || view.noApplyYConstaint)continue;
			if(!isRelative){
				if(orientation == VERTICAL){
					if(!view.noApplyYConstaint){
						if(view.hasPrevius()){
							View prev = view.previus;
							if(prev.noApplyYConstaint || prev.getVisibility() == View.GONE){
								prev = getPrevius(i);
								if(prev != null){
									prect_height += prev.margin_buttom * 0.5f;
								}
							}else{
								prect_height += prev.margin_buttom * 0.5f;
							}
						}
						prect_height += view.margin_top * 0.5f + view.getExtentHeight();
					}
				}else{
					float test = view.margin_top * 0.5f + view.getExtentHeight() + view.margin_buttom * 0.5f;
					if(prect_height < test){
						prect_height = test;
					}
				}
			}else{
				float test = Maths.abs(view.relative.y + view.getExtentHeight());
				if(prect_height < test){
					prect_height = test;
				}
			}
		}
		return prect_height;
	}
	
	public View findViewByID(int id){
		for(View v :children){
			if(v.getId() == id){
				return v;
			}else if(v.isLayout()){
				View result = ((Layout)v).findViewByID(id);
				if(result != null){
					return result;
				}
			}
		}
		return null;
	}
}
