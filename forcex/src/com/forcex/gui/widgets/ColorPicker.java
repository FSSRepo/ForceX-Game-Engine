package com.forcex.gui.widgets;
import com.forcex.gui.*;
import com.forcex.utils.*;
import com.forcex.core.gpu.*;

public class ColorPicker extends View implements ProgressBar.onSeekListener {
	public float width,height;
	private int texture_id = -1;
	private ProgressBar[] colors_bar;
	private TextView colors_tv;
	private OnColorPickListener listener;
	private Color sampler;

	public ColorPicker(UIContext context){
		this(context,0.1f,0.1f);
	}

	public ColorPicker(UIContext context,float width,float height){
		setWidth(width);
		setHeight(height);
		colors_bar = new ProgressBar[4];
		colors_tv = new TextView(context.default_font);
		colors_tv.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
		for(byte i = 0;i < 4;i++){
			colors_bar[i] = new ProgressBar(width, (height - 0.05f) / 4f);
			colors_bar[i].setId(i);
			if(i == 3){
				colors_bar[3].use2ndBackgroundTexture = true;
			}
			colors_bar[i].setProgress(100);
			colors_bar[i].useSeekBar(true);
			colors_bar[i].updateExt();
			colors_bar[i].setOnSeekListener(this);
			switch(i){
				case 0: colors_bar[i].setColor(0xffcccccc,0xffff0000); break;
				case 1: colors_bar[i].setColor(0xffcccccc,0xff00ff00); break;
				case 2: colors_bar[i].setColor(0xffcccccc,0xff0000ff); break;
				case 3: colors_bar[i].setColor(0x00ffffff,0xffffffff); break;
			}
		}
		colors_tv.setTextSize(colors_bar[0].getHeight() * 0.92f);
		sampler = new Color();
	}
	
	public void setColor(Color color){
		colors_bar[0].setProgress((color.r / 255f) * 100.0f);
		colors_bar[1].setProgress((color.g / 255f) * 100.0f);
		colors_bar[2].setProgress((color.b / 255f) * 100.0f);
		colors_bar[3].setProgress((color.a / 255f) * 100.0f);
		colors_bar[3].setColor(0xff000000,color.toRGBA());
		sampler.set(color);
	}

	@Override
	public void seek(int id,float progress){
		switch(id){
			case 0:
				colors_bar[3].colors[1].r = (short)(255.0f * (progress / 100.0f));
				break;
			case 1:
				colors_bar[3].colors[1].g = (short)(255.0f * (progress / 100.0f));
				break;
			case 2:
				colors_bar[3].colors[1].b = (short)(255.0f * (progress / 100.0f));
				break;
			case 3:
				sampler.a = (short)(255.0f * (progress / 100.0f));
				break;
		}
		sampler.setColor(colors_bar[3].colors[1]);
		if(listener != null){
			listener.pick(sampler.toRGBA());
		}
	}

	public void setOnColorPickListener(OnColorPickListener listener){
		this.listener = listener;
	}

	@Override
	public void finish(float final_progress){

	}

	@Override
	public void onTouch(float x, float y, byte type) {
		for(ProgressBar b : colors_bar){
			if(b.testTouch(x,y)){
				b.onTouch(x,y,type);
			}
		}
	}

	@Override
	public void onCreate(Drawer drawer) {
		short w = 128;
		short h = 32;
		byte[] img = new byte[(w*h)*4];
		for(short x = 0;x < w;x++){
			for(short y = 0;y < h;y++){
				int offset = (x + (w * y)) * 4;
				img[offset] = (byte)255;
				img[offset+1] = (byte)255;
				img[offset+2] = (byte)255;
				img[offset+3] = (byte)(255.0f * ((float)x / w));
			}
		}
		texture_id = Texture.load(w,h,BufferUtils.createByteBuffer(img),false);
		colors_bar[3].texture_background_2 = texture_id;
	}

	@Override
	public void onDraw(Drawer drawer)
	{
		float dy = (local.y + extent.y) - 0.02f - colors_bar[0].getHeight();
		for(byte i = 0;i < 4;i++){
			if(i == 3 && 
			   colors_bar[3].colors[1].r < 120 && 
			   colors_bar[3].colors[1].g < 120 &&
			   colors_bar[3].colors[1].b < 120){
				colors_tv.setTextColor(255,255,255);
			}else{
				colors_tv.setTextColor(0,0,0);
			}
			colors_bar[i].local.set(local.x,dy);
			colors_bar[i].onDraw(drawer);
			colors_tv.setText(getColor(i)+":"+(int)(255.0f * (colors_bar[i].getProgress() / 100.0f)));
			colors_tv.local.set(local.x,colors_bar[i].local.y);
			colors_tv.onDraw(drawer);
			dy -= (colors_bar[i].getHeight() * 2f) + 0.02f;
		}
	}
	
	public String getColor(byte i){
		switch(i){
			case 0:
				return "Red";
			case 1:
				return "Green";
			case 2:
				return "Blue";
			case 3:
				return "Alpha";
		}
		return "";
	}
	
	@Override
	public void onDestroy() {
		for(ProgressBar bar : colors_bar){
			bar.onDestroy();
		}
		colors_bar = null;
		colors_tv.onDestroy();
		colors_tv = null;
		listener = null;
		sampler = null;
	}
	

	@Override
	protected boolean testTouch(float x, float y){
		return isVisible() && GameUtils.testRect(x,y,local,extent.x,extent.y);
	}

	public static interface OnColorPickListener{
		void pick(int color);
	}
}
