package com.forcex.gui.widgets;
import com.forcex.gui.*;
import com.forcex.math.*;
import java.util.*;
import com.forcex.utils.*;
import com.forcex.app.*;
import com.forcex.core.gpu.*;
import com.forcex.*;

public class KeyBoard {
	public static KeyBoard instance = null;
	private ArrayList<KeyInfo> keys,numbers;
	private ArrayList<KeyButton> buttons,buttons_number;
	private float 
	width,height,
	key_width,key_height;
	private TextView textview;
	private Color backgound,key_click,toggle_btn;
	private byte offset = 0;
	private onKeyBoardListener listener;
	private boolean UseMayus;
	public boolean showKeyBoard = false,keyboardNumber = false;
	private Vector2f position;
	float aspectRatio = 0.0f;
	EditText ref;
	boolean force_close = false;
	public static boolean physic_keyboard = false;

	public static void create(float width,UIContext ctx){
		if(instance != null){
			return;
		}
		instance = new KeyBoard(width);
		instance.position = new Vector2f(0,-1.0f + (width / 1.5f));
		instance.create(ctx);
		instance.aspectRatio = ctx.getAspectRatio();
	}
	
	private KeyBoard(){}
	
	private KeyBoard(float width){
		float h = width / 1.5f;
		keys = new ArrayList<>();
		buttons = new ArrayList<>();
		numbers = new ArrayList<>();
		buttons_number = new ArrayList<>();
		key_width = width / 10;
		key_height = h / 5;
		this.width = width;
		this.height = h;
		backgound = new Color(210,210,210,255);
		key_click = new Color(180,180,180,180);
		toggle_btn = new Color(12,220,250);
	}
	
	public void create(UIContext context) {
		if(physic_keyboard)return;
		float dx = (position.x - width) + key_width;
		float dy = (position.y + height) - key_height;
		// KeyBoard Normal
		for(byte i = 0;i < 10;i++){
			KeyInfo key = new KeyInfo((char)(48 + i));
			key.key_pos.set(dx + (i * (key_width * 2.1f)),dy);
			keys.add(key);
		}
		dy -= key_height * 2f;
		fill('q','w','e','r','t','y','u','i','o','p');
		for(byte i = 0;i < 10;i++){
			keys.get(10 + i).key_pos.set(dx + (i * (key_width * 2.1f)),dy);
		}
		dy -= key_height * 2f;
		fill('a','s','d','f','g','h','j','k','l');
		for(byte i = 0;i < 9;i++){
			keys.get(20 + i).key_pos.set(dx + (key_width*0.5f) + (i * (key_width * 2.1f)),dy);
		}
		dy -= key_height * 2f;
		fill('z','x','c','v','b','n','m','.');
		for(byte i = 0;i < 8;i++){
			keys.get(29 + i).key_pos.set(dx + ((i+1) * (key_width * 2.1f)),dy);
		}
		KeyButton mayus = new KeyButton(0,dx,dy,key_width);
		mayus.icon = Texture.load(FX.homeDirectory + "gui/shift.png");
		buttons.add(mayus);
		KeyButton delete = new KeyButton(1,keys.get(9).key_pos.x,dy,key_width);
		delete.icon = Texture.load(FX.homeDirectory + "gui/backspace.png");
		buttons.add(delete);
		dy -= key_height * 2f;
		KeyButton symbols = new KeyButton(2,dx,dy,key_width);
		symbols.text = "!%@";
		buttons.add(symbols);
		KeyButton space = new KeyButton(3,position.x,dy,key_width * 6f);
		space.text = "Space";
		buttons.add(space);
		KeyButton enter = new KeyButton(4,keys.get(9).key_pos.x,dy,key_width);
		enter.icon = Texture.load(FX.homeDirectory + "gui/enter.png");
		buttons.add(enter);
		// signs
		dy = (position.y + height) - key_height;
		fill('+','-','/','*','=','|','$','@','%','¡');
		for(byte i = 0;i < 10;i++){
			keys.get(37 + i).key_pos.set(dx + (i * (key_width * 2.1f)),dy);
		}
		dy -= key_height * 2f;
		fill('#','[',']','(',')','<','>','{','}','\\');
		for(byte i = 0;i < 10;i++){
			keys.get(47 + i).key_pos.set(dx + (i * (key_width * 2.1f)),dy);
		}
		dy -= key_height * 2f;
		fill('~',':',',',';','"','_','°');
		for(byte i = 0;i < 7;i++){
			keys.get(57 + i).key_pos.set(dx + ((i+1) * (key_width * 2.1f)),dy);
		}
		dy -= key_height * 2f;
		// key number
		dy = (position.y + height) - key_height;
		dx += key_width * 1.5f;
		fillNumbers('1','2','3');
		for(byte i = 0;i < 3;i++){
			numbers.get(i).key_pos.set(dx + (i * (key_width * 5f)),dy);
		}
		KeyButton del_n = new KeyButton(0,dx + key_width * 15.0f,dy,key_width * 2.5f);
		del_n.icon = delete.icon;
		buttons_number.add(del_n);
		dy -= key_height * 2f;
		fillNumbers('4','5','6');
		for(byte i = 0;i < 3;i++){
			numbers.get(3 + i).key_pos.set(dx + (i * (key_width * 5f)),dy);
		}
		KeyButton minus_n = new KeyButton(3,dx + key_width * 15.0f,dy,key_width * 2.5f);
		minus_n.text = "-";
		buttons_number.add(minus_n);
		dy -= key_height * 2f;
		fillNumbers('7','8','9');
		for(byte i = 0;i < 3;i++){
			numbers.get(6+i).key_pos.set(dx + (i * (key_width * 5f)),dy);
		}
		KeyButton ent_n = new KeyButton(1,dx + key_width * 15.0f,dy,key_width * 2.5f);
		ent_n.icon = enter.icon;
		buttons_number.add(ent_n);
		dy -= key_height * 2f;
		KeyButton zero_n = new KeyButton(2,dx + key_width * 5f,dy,key_width * 5f);
		zero_n.text = "0";
		buttons_number.add(zero_n);
		KeyButton point_n = new KeyButton(4,dx + key_width * 15.0f,dy,key_width * 2.5f);
		point_n.text = ".";
		buttons_number.add(point_n);
		textview = new TextView(context.default_font);
		textview.setTextColor(0,0,0);
	}
	
	private void fill(char... chs){
		for(char c : chs){
			keys.add(new KeyInfo(c));
		}
	}
	
	private void fillNumbers(char... chs){
		for(char c : chs){
			numbers.add(new KeyInfo(c));
		}
	}
	
	public void render(Drawer drawer) {
		if(!showKeyBoard || physic_keyboard){
			for(KeyInfo k : keys){
				k.isClick = false;
			}
			return;
		}
		if(index != -1){
			drawer.setScale(1,ref.getExtentHeight() * 1.2f);
			ref.local.set(position.x,position.y + height + (ref.getExtentHeight() * 1.2f));
			drawer.renderQuad(ref.local,backgound,-1);
			ref.onDraw(drawer);
		}
		drawer.setScale(1,height);
		drawer.renderQuad(position,backgound,-1);
		if(!keyboardNumber){
			for(byte i = offset;i < (offset == 0 ? 37 : keys.size());i++){
				KeyInfo key = keys.get(i);
				if(key.isClick){
					drawer.setScale(key_width,key_height);
					drawer.renderQuad(key.key_pos,key_click,-1);
				}
				textview.local.set(key.key_pos.x,key.key_pos.y);
				textview.setTextSize(key_height);
				textview.setText(key.character+"");
				textview.onDraw(drawer);
			}
			for(KeyButton btn : buttons){
				if(btn.click || btn.type == 0 && UseMayus || btn.type == 2 && offset != 0){
					drawer.setScale(btn.btn_width,key_height);
					drawer.renderQuad(btn.btn_pos,toggle_btn,-1);
				}
				if(btn.type == 3){
					drawer.setScale(btn.btn_width,key_height*0.9f);
					drawer.renderLineQuad(btn.btn_pos,key_click);
				}
				if(btn.icon != -1){
					drawer.setScale(key_height / aspectRatio,key_height);
					drawer.renderQuad(btn.btn_pos,null,btn.icon);
				}
				if(btn.text.length() > 0){
					textview.local.set(btn.btn_pos.x,btn.btn_pos.y);
					textview.setTextSize(key_height*0.5f);
					textview.setText(btn.text);
					textview.onDraw(drawer);
				}
			}
		}else{
			for(byte i = 0;i < numbers.size();i++){
				KeyInfo key = numbers.get(i);
				if(key.isClick){
					drawer.setScale(key_width * 2.5f,key_height);
					drawer.renderQuad(key.key_pos,key_click,-1);
				}
				textview.local.set(key.key_pos.x,key.key_pos.y);
				textview.setTextSize(key_height);
				textview.setText(key.character+"");
				textview.onDraw(drawer);
			}
			for(KeyButton btn : buttons_number){
				if(btn.click){
					drawer.setScale(btn.btn_width,key_height);
					drawer.renderQuad(btn.btn_pos,toggle_btn,-1);
				}
				if(btn.icon != -1){
					drawer.setScale(key_height / aspectRatio,key_height);
					drawer.renderQuad(btn.btn_pos,null,btn.icon);
				}
				if(btn.text.length() > 0){
					textview.local.set(btn.btn_pos.x,btn.btn_pos.y);
					if(btn.type < 2){
						textview.setTextSize(key_height*0.5f);
					}
					textview.setText(btn.text);
					textview.onDraw(drawer);
				}
			}
		}
		if(force_close){
			if(ref == null){
				solveHideView();
				showKeyBoard = false;
				setOnKeyBoardListener(null);
			}else{
				ref.detachKeyBoard();
			}
			force_close = false;
		}
	}
	
	public void setCharacterType(){
		for(byte k = 10;k < 36;k++){
			KeyInfo key = keys.get(k);
			key.character = (char)(UseMayus ? (65 + (key.character - 97)) : (97 + (key.character - 65)));
		}
	}
	
	public void setOnKeyBoardListener(onKeyBoardListener listener){
		this.listener = listener;
	}
	
	boolean shift_mayus = false;
	
	public void onKeyEvent(byte key,boolean pressed){
                if(pressed && key == Key.KEY_SHIFT){
                    shift_mayus = true;
                  }
		if(!pressed && listener != null){
			if(!keyboardNumber && (key >= Key.A_KEY && key <= Key.Z_KEY)){
				listener.onKeyDown(Key.toKeyChar(key,UseMayus || shift_mayus));
			}
			if(key >= Key.KEY_0 && key <= Key.KEY_9){
				listener.onKeyDown(Key.toKeyChar(key,UseMayus));
			}
			switch(key){
				case Key.KEY_CAPITAL:
					UseMayus = !UseMayus;
					break;
				case Key.KEY_SPACE:
					if(listener != null){
						listener.onKeyDown(' ');
					}
					break;
				case Key.KEY_DOT:
					if(listener != null){
						listener.onKeyDown('.');
					}
					break;
				case Key.KEY_MINUS:
					if(listener != null){
						listener.onKeyDown('-');
					}
					break;
                                 case Key.KEY_SHIFT:
					shift_mayus = false;
					break;
				case Key.KEY_ENTER:
					if(listener != null){
						listener.onEnter();
					}
					break;
				case Key.KEY_DEL:
					if(listener != null){
						listener.onBackspace();
					}
					break;
			}
		}
	}
	
	public void onTouch(float x, float y,byte type) {
		if(!keyboardNumber){
			for(KeyButton btn :buttons){
				if(GameUtils.testRect(x,y,btn.btn_pos,btn.btn_width,key_height)){
					if(type == EventType.TOUCH_PRESSED){
						switch(btn.type){
							case 0:
								UseMayus = !UseMayus;
								setCharacterType();
								break;
							case 1:
								if(listener != null){
									listener.onBackspace();
								}
								break;
							case 2:
								offset = (byte)(offset == 0 ? 37 : 0);
								break;
							case 3:
								if(listener != null){
									listener.onKeyDown(' ');
								}
								break;
							case 4:
								if(listener != null){
									listener.onEnter();
								}
								break;
						}
						btn.click = true;
					}else if(type == EventType.TOUCH_DROPPED){
						btn.click = false;
					}
				}else{
					btn.click = false;
				}
			}
			for(byte i = offset;i < (offset == 0 ? 37 : keys.size());i++){
				KeyInfo k = keys.get(i);
				if(GameUtils.testRect(x,y,k.key_pos,key_width,key_height)){
					if(type == EventType.TOUCH_PRESSED){
						k.isClick = true;
						if(listener != null){
							listener.onKeyDown(k.character);
						}
						break;
					}else if(type == EventType.TOUCH_DROPPED){
						k.isClick = false;
					}
				}else{
					k.isClick = false;
				}
			}
		}else{
			for(KeyButton btn : buttons_number){
				if(GameUtils.testRect(x,y,btn.btn_pos,btn.btn_width,key_height)){
					if(type == EventType.TOUCH_PRESSED){
						switch(btn.type){
							case 0:
								if(listener != null){
									listener.onBackspace();
								}
								break;
							case 1:
								if(listener != null){
									listener.onEnter();
								}
								break;
							case 2:
								if(listener != null){
									listener.onKeyDown('0');
								}
								break;
							case 3:
								if(listener != null){
									listener.onKeyDown('-');
								}
								break;
							case 4:
								if(listener != null){
									listener.onKeyDown('.');
								}
								break;
						}
						btn.click = true;
					}else if(type == EventType.TOUCH_DROPPED){
						btn.click = false;
					}
				}else{
					btn.click = false;
				}
			}
			for(byte i = 0;i < numbers.size();i++){
				KeyInfo k = numbers.get(i);
				if(GameUtils.testRect(x,y,k.key_pos,key_width * 2.5f,key_height)){
					if(type == EventType.TOUCH_PRESSED){
						k.isClick = true;
						if(listener != null){
							listener.onKeyDown(k.character);
						}
						break;
					}else if(type == EventType.TOUCH_DROPPED){
						k.isClick = false;
					}
				}else{
					k.isClick = false;
				}
			}
		}
	}

	public boolean testTouch(float x,float y) {
		return !physic_keyboard && showKeyBoard && GameUtils.testRect(x,y,position,1.0f,height);
	}

	public static interface onKeyBoardListener{
		void onBackspace();
		void onEnter();
		void onKeyDown(char key);
	}

	public void deleteInmediate(){
		showKeyBoard = false;
		keys.clear();
		if(textview != null){
			textview.onDestroy();
		}
		backgound = null;
		textview = null;
		key_click = null;
		listener = null;
	}
	
	public void forceClose(){
		force_close = showKeyBoard;
	}
	
	public class KeyInfo{
		public char character;
		public KeyInfo(char c){
			character = c;
		}
		public boolean isClick = false;
		Vector2f key_pos = new Vector2f();
	}
	
	private class KeyButton{
		byte type = 0;
		boolean click;
		Vector2f btn_pos;
		float btn_width;
		int icon = -1;
		String text = "";
		
		public KeyButton(int type,float x,float y,float width){
			this.type = (byte) type;
			btn_pos = new Vector2f(x,y);
			btn_width = width;
		}
	}
	
	int index = -1;
	Layout container_temp;
	
	boolean solveShowView(){
		if(index != -1){
			return false;
		}
		if(!physic_keyboard){
			float x1 = ref.local.x - ref.getExtentWidth();
			float x2 = ref.local.x + ref.getExtentWidth();
			float y = ref.local.y - ref.getExtentHeight();
			if(GameUtils.testRect(x1,y,position,1f,height) || GameUtils.testRect(x2,y,position,1f,height)){
				if(ref.hasParent()){
					container_temp = ((Layout)ref.getParent());
					index = container_temp.indexOf(ref);
					container_temp.remove(ref);
				}
			}
		}
		return true;
	}
	
	void solveHideView(){
		if(index == -1 || container_temp == null || physic_keyboard){
			return;
		}
		container_temp.add(ref,index);
		container_temp = null;
		index = -1;
	}
}
