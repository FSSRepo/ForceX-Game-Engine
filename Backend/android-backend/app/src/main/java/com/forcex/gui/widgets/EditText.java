package com.forcex.gui.widgets;
import com.forcex.utils.*;
import com.forcex.gui.*;
import com.forcex.math.*;
import com.forcex.app.*;

public class EditText extends View
{
	private boolean using = false,
	only_numbers = false,
	password = false,
	only_one_line = false;
	private TextView textview;
	private KeyBoard keyboard;
	private Color edge_color;
	private String text;
	private String hint;
	private onEditTextListener lt;
	private short[] multicolor;
	private short num_lines,lines_max;
	private String[] lines;
	
	public EditText(UIContext ctx){
		this(ctx,0.1f,0.05f,0.05f);
	}

	public EditText(UIContext context,float width,float height,float text_size){
		setWidth(width);
		setHeight(height);
		text = "";
		hint = "";
		edge_color = new Color();
		textview = new TextView(context.default_font);
		textview.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER_LEFT);
		textview.setTextSize(text_size);
		only_one_line = (height == text_size);
		lines_max = (short)(height / text_size);
	}

	private void usingThisEditText(boolean z){
		if(multicolor == null){
			if(!z){
				edge_color.set(220,220,220);
			}else{
				edge_color.set(7,128,255);
			}
		}else{
			if(!z){
				edge_color.set(multicolor[3],multicolor[4],multicolor[5]);
			}else{
				edge_color.set(multicolor[0],multicolor[1],multicolor[2]);
			}
		}
	}
	
	public void setEdgeMultiColor(int r1,int g1,int b1,int r2,int g2,int b2){
		if(multicolor == null){
			multicolor = new short[6];
		}
		multicolor[0] = (short)r1;
		multicolor[1] = (short)g1;
		multicolor[2] = (short)b1;
		multicolor[3] = (short)r2;
		multicolor[4] = (short)g2;
		multicolor[5] = (short)b2;
	}
	
	public void setPasswordMode(boolean z){
		password = z;
	}

	public void setNumbersMode(boolean z){
		only_numbers = z;
	}

	public void setOnEditTextListener(onEditTextListener lt){
		this.lt = lt;
	}

	public void detachKeyBoard(){
		if(keyboard != null){
			keyboard.showKeyBoard = false;
			keyboard.setOnKeyBoardListener(null);
			keyboard = null;
		}
	}

	public void setText(String text){
		this.text = text;
		updateTextView();
	}

	public void setHint(String hint){
		this.hint = hint;
	}

	public String getText(){
		return text;
	}

	public boolean isEmpty(){
		return text.length() == 0;
	}
	
	byte frame = 0;
	Vector2f cursor = new Vector2f();
	Color cursor_color = new Color(0, 0, 0);
	
	@Override
	public void onDraw(Drawer drawer) {
		usingThisEditText(using);
		drawer.setScale(extent.x,extent.y);
		drawer.renderQuad(local,null,-1);
		if(hint.length() > 0 && text.length() == 0){
			textview.setText(hint);
			textview.local.set((local.x - extent.x) + textview.getWidth(),local.y + extent.y - textview.getTextSize());
			textview.setTextColor(128,128,128);
			textview.onDraw(drawer);
		}else{
			textview.local.set(
				local.x - (extent.x - textview.getWidth()),
				(local.y + extent.y - textview.getTextSize()) - (num_lines - 1) * textview.getTextSize());
			textview.setTextColor(0,0,0);
			textview.onDraw(drawer);
			if(frame < 30 && using){
				cursor.set((local.x - extent.x) + textview.getTextWidthReal(lines[num_lines - 1]) * 2f,textview.local.y - ((num_lines - 1) * textview.getTextSize()));
				drawer.setTransform(90.0f,1,textview.getTextSize());
				drawer.renderLine(cursor,cursor_color);
			}else if(frame > 60){
				frame = 0;
			}
			frame++;
		}
		drawer.setScale(extent.x,extent.y); 
		drawer.renderLineQuad(local,edge_color);
	}
	
	private void updateTextView(){
		// update limits
		if(text.length() == 0){
			textview.setText("");
			return;
		}
		if(only_one_line && textview.getTextWidthReal(text) > extent.x){
			text = text.substring(0,text.length() - 1);
		}else if(!only_one_line){
			String process = "";
			float width = 0.0f;
			byte lo = 1;
			for(short i = 0;i < text.length();i++){
				char test = text.charAt(i);
				if(test != '\n'){
					width += textview.getFont().charWidths[test & 0xff] * 0.5f * textview.getTextSize();
					if(width < extent.x){
						process += test;
					}else if(lo < lines_max){
						process += "\n"+test;
						width = 0.0f;
						lo++;
					}
				}else if(lo < lines_max){
					process += "\n";
					width = 0.0f;
					lo++;
				}
				text = process;
			}
		}
		if(password){
			String gen = "";
			for(int i = 0;i < text.length();i++){
				gen +="*";
			}
			textview.setText(gen);
		}else{
			textview.setText(text);
		}
		lines = null;
		lines = text.split("\n");
		num_lines = (short)lines.length;
	}

	@Override
	public void onTouch(float x, float y, byte type) {
		if(type == EventType.TOUCH_PRESSED){
			using = true;
			callKeyBoard();
		}
	}

	@Override
	protected boolean testTouch(float x, float y) {
		if(isVisible() && GameUtils.testRect(x,y,local,extent.x,extent.y) || (keyboard != null && keyboard.testTouch(x,y))){
			return true;
		}
		using = false;
		detachKeyBoard();
		return false;
	}
	
	public static interface onEditTextListener {
		void onChange(View view, String text,boolean deleting);
		void onEnter(View view, String text);
	}

	@Override
	public void onDestroy() {
		detachKeyBoard();
		textview.onDestroy();
		textview = null;
		lt = null;
		text = null;
		multicolor = null;
		edge_color = null;
	}

	private void callKeyBoard(){
		if(keyboard == null){
			keyboard = KeyBoard.instance;
			if(keyboard.ref != this && keyboard.ref != null){
				keyboard.ref.detachKeyBoard();
				keyboard.ref.using = false;
			}
			keyboard.ref = this;
			keyboard.showKeyBoard = true;
			keyboard.keyboardNumber = only_numbers;
			keyboard.setOnKeyBoardListener(new KeyBoard.onKeyBoardListener(){
					@Override
					public void onBackspace() {
						if(text.length() > 0){
							text = text.substring(0,text.length() - 1);
							updateTextView();
							processChange(true);
						}
					}

					@Override
					public void onEnter() {
						if(!only_one_line){
							text += "\n";
							updateTextView();
							processChange(false);
						}else{
							processEnter();
						}
					}
					
					@Override
					public void onKeyDown(char key){
						text += key;
						updateTextView();
						processChange(false);
					}
					
				});
		}
	}
	
	private void processChange(boolean del){
		if(lt != null){
			lt.onChange(this,text,del);
		}
	}
	
	private void processEnter(){
		if(lt != null){
			lt.onEnter(this,text);
		}
		using = false;
		detachKeyBoard();
	}
}
