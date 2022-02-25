package com.forcex.gui.widgets;
import com.forcex.gui.*;
import com.forcex.utils.*;
import java.util.*;
import com.forcex.math.*;
import com.forcex.core.gpu.*;
import com.forcex.core.*;
import com.forcex.*;

public class TextView extends View {
	public static final byte TEXT_ALIGNMENT_CENTER = 0x3;
	public static final byte TEXT_ALIGNMENT_CENTER_RIGHT = 0x4;
	public static final byte TEXT_ALIGNMENT_CENTER_LEFT = 0x5;
	private Font font;
	private byte text_alignment;
	private short num_lines;
	private String text = "";
	private int text_vbo = -1,text_ibo = -1,index_lenght;
	private float constaint_width = 0.0f, max_line, text_size;
	float text_anim_width;
	protected Color text_color;
	private boolean animationScroll;
	
	public TextView(Font font){
		this.font = font;
		text_size = 0.035f;
		text_alignment = TEXT_ALIGNMENT_CENTER;
		setText("");
		text_color = new Color(Color.BLACK);
		setDebugColor(30,199,0);
	}

	public void setFont(Font font){
		this.font = font;
		setText(text);
	}
	
	Vector2f anim_step = new Vector2f();
	float deltax = 0.0f;
	
	@Override
	public void onDraw(Drawer drawer) {
		drawer.setScale(text_size,text_size);
		if(!animationScroll || animationScroll && (text_anim_width < constaint_width)){
			drawer.setupBuffer(text_vbo,local);
			drawer.shader.setSpriteColor(text_color);
			Texture.bind(GL.GL_TEXTURE0,font.font_texture);
			drawer.gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,text_ibo);
			drawer.gl.glDrawElements(GL.GL_TRIANGLES,index_lenght);
		}else{
			anim_step.set((local.x - constaint_width) + text_anim_width + deltax,local.y);
			drawer.scissorArea(local.x,local.y,constaint_width,height);
			drawer.setupBuffer(text_vbo,anim_step);
			drawer.shader.setSpriteColor(text_color);
			Texture.bind(GL.GL_TEXTURE0,font.font_texture);
			drawer.gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,text_ibo);
			drawer.gl.glDrawElements(GL.GL_TRIANGLES,index_lenght);
			drawer.finishScissor();
			deltax -= text_anim_width * 0.1f * FX.gpu.getDeltaTime();
			if(deltax < -text_anim_width * 2.0f){
				deltax = constaint_width * 2.0f;
			}
		}
		if(debug){
			drawer.setScale(getWidth(),getHeight());
			drawer.renderLineQuad(local,this.debug_color);
		}
	}
	
	public void setConstraintWidth(float constaint){
		constaint_width = constaint;
	}
	
	public void setAnimationScroll(boolean enable){
		animationScroll = enable;
	}
	
	public void setTextAlignment(byte alignment){
		this.text_alignment = alignment;
	}
	
	public void setTextColor(int r,int g,int b){
		text_color.set(r,g,b);
	}
	
	public Color getTextColor(){
		return text_color;
	}
	
	public void setTextSize(float size){
		text_size = size;
		if(constaint_width > 0.0f){
			updateText();
		}else{
			setWidth(max_line * 0.5f * text_size);
			setHeight(num_lines * text_size);
		}
	}
	
	public void setText(String text){
		this.text = text;
		updateText();
	}
	
	private void updateText(){
		String process = "";
		if(constaint_width > 0.0f && !animationScroll){
			float width = 0.0f;
			String[] lines = text.split("\n");
			for(String line : lines){
				String[] words = line.split(" ");
				if(words.length > 1){
					for(int i = 0;i < words.length;i++){
						float w = getTextWidthReal(words[i]);
						if((width + w) > constaint_width){
							process += "\n" + words[i] + (i < words.length - 1 ? " ":"");
							width = w + (i < words.length - 1 ? getTextWidthReal(" ") : 0);
						}else{
							process += words[i] + (i < words.length - 1 ? " " : "");
							width += w + (i < words.length - 1 ? getTextWidthReal(" ") : 0);
						}
					}
				}else if(words.length == 1){
					for(byte i = 0;i < words[0].length();i++){
						String sampler = words[0].charAt(i)+"";
						float w = getTextWidthReal(sampler);
						if((width + w) > constaint_width){
							process += "\n" + sampler + (i < words.length - 1 ? " ":"");
							width = w + (i < words.length - 1 ? getTextWidthReal(" ") : 0);
						}else{
							process += sampler + (i < words.length - 1 ? " " : "");
							width += w + (i < words.length - 1 ? getTextWidthReal(" ") : 0);
						}
					}
				}
				if(lines.length > 1){
					process += "\n";
					width = 0.0f;
				}
			}
		}else{
			process = text;
		}
		String[] lines = process.split("\n");
		max_line = getLineMaxWidth(lines);
		num_lines = (short) lines.length;
		int text_lenght = getDrawTextLenght(process);
		float[] vertex = new float[text_lenght * 16];
		short[] indices = new short[text_lenght * 6];
		index_lenght = indices.length;
		float cursorX = 0,cursorY = (num_lines - 1);
		int offset = 0,index_offset = 0,vertex_offset = 0;
		for(String line : lines){
			switch(text_alignment){
				case TEXT_ALIGNMENT_CENTER:
					cursorX = -getTextWidth(line) * 0.5f;
					break;
				case TEXT_ALIGNMENT_CENTER_RIGHT:
					cursorX = (max_line * 0.5f) - getTextWidth(line);
					break;
				case TEXT_ALIGNMENT_CENTER_LEFT:
					cursorX = max_line * -0.5f;
					break;
			}
			for(char c : line.toCharArray()){
				if(c != ' '){
					indices[index_offset] = (short)(vertex_offset);
					indices[index_offset+1] = (short)(vertex_offset + 1);
					indices[index_offset+2] = (short)(vertex_offset + 3);
					indices[index_offset+3] = (short)(vertex_offset);
					indices[index_offset+4] = (short)(vertex_offset + 2);
					indices[index_offset+5] = (short)(vertex_offset + 3);
					vertex_offset += 4;
					index_offset += 6;
					byte row = (byte)((c - font.startChar) / font.rowPitch);
					byte col = (byte)((c - font.startChar) - (font.rowPitch * row));
					float u_start = col * font.columnFactor;
					float v_start = row * font.rowFactor;
					float u_end = u_start + font.columnFactor;
					float v_end = v_start + font.rowFactor;
					vertex[offset] = cursorX;
					vertex[offset+1] = cursorY + 1;
					vertex[offset+2] = u_start;
					vertex[offset+3] = v_start;
					offset += 4;
					vertex[offset] = cursorX;
					vertex[offset+1] = cursorY - 1;
					vertex[offset+2] = u_start;
					vertex[offset+3] = v_end;
					offset += 4;
					vertex[offset] = cursorX + 1;
					vertex[offset+1] = cursorY + 1;
					vertex[offset+2] = u_end;
					vertex[offset+3] = v_start;
					offset += 4;
					vertex[offset] = cursorX + 1;
					vertex[offset+1] = cursorY - 1;
					vertex[offset+2] = u_end;
					vertex[offset+3] = v_end;
					offset += 4;
				}
				cursorX += font.charWidths[(byte)c & 0xff];
			}
			cursorY -= 2;
		}
		if(text_vbo != -1){
			FX.gl.glDeleteBuffer(text_vbo);
			FX.gl.glDeleteBuffer(text_ibo);
		}
		text_vbo = Drawer.genBuffer(vertex,false,false);
		text_ibo = Drawer.genBuffer(indices,true,false);
		vertex = null;
		indices = null;
		if(!animationScroll){
			setWidth(max_line * 0.5f * text_size);
		}else{
			setWidth(constaint_width);
			text_anim_width = max_line * 0.5f * text_size;
		}
		setHeight(num_lines * text_size);
	}
	
	public boolean isConstraintUse(){
		return animationScroll && (text_anim_width > constaint_width);
	}
	
	public float getTextWidth(String text){
		float text_width = 0.0f;
		for(short i = 0;i < text.length();i++){
			text_width += font.charWidths[(byte)text.charAt(i) & 0xff];
		}
		return text_width;
	}
	
	public float getTextWidthReal(String text){
		float text_width = 0.0f;
		for(short i = 0;i < text.length();i++){
			text_width += font.charWidths[(byte)text.charAt(i) & 0xff];
		}
		return text_width * 0.5f * text_size;
	}
	
	public float getTextSize(){
		return text_size;
	}
	
	public float getLineMaxWidth(String[] lines){
		float max_width = 0.0f;
		for(short i = 0;i < lines.length;i++){
			float x = getTextWidth(lines[i]);
			if(max_width < x){
				max_width = x;
			}
		}
		return max_width;
	}
	
	private int getDrawTextLenght(String process){
		int i = 0;
		for(char c : process.toCharArray()){
			if(c != '\n' && c != ' '){
				i++;
			}
		}
		return i;
	}
	
	public Font getFont(){
		return font;
	}

	@Override
	public void onDestroy() {
		setVisibility(INVISIBLE);
		font = null;
		text_color = null;
		FX.gl.glDeleteBuffer(text_vbo);
		FX.gl.glDeleteBuffer(text_ibo);
		text = null;
	}
	
	private class TextSizeSpan{
		public short start_index;
		public short end_index;
		public float size;
	}
	
	private class TextColorSpan{
		public short start_index;
		public short end_index;
		public Color color;
	}
}
