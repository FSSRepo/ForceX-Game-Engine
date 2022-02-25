package com.forcex.gfx3d.shapes;
import com.forcex.gfx3d.*;
import com.forcex.gui.*;
import com.forcex.core.*;
import com.forcex.*;

public class Text extends Mesh {
	Font font;
	
	public Text(String text,Font font,float scale){
		super(false);
		this.font = font;
		setText(text,scale);
	}
	
	private void setText(String text,float scale){
		int text_lenght = getDrawTextLenght(text);
		float[] vertex = new float[text_lenght * 12];
		float[] texcoord  = new float[text_lenght * 8];
		short[] indices = new short[text_lenght * 6];
		float cursorX = -getTextWidth(text) * 0.5f;
		int offset = 0,index_offset = 0,vertex_offset = 0;
		for(char c : text.toCharArray()){
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
				vertex[offset*3] = cursorX;
				vertex[offset*3+1] = 1;
				vertex[offset*3+2] = 0;
				texcoord[offset*2] = u_start;
				texcoord[offset*2+1] = v_start;
				offset ++;
				vertex[offset*3] = cursorX;
				vertex[offset*3+1] = -1;
				vertex[offset*3+2] = 0;
				texcoord[offset*2] = u_start;
				texcoord[offset*2+1] = v_end;
				offset ++;
				vertex[offset*3] = cursorX + 1;
				vertex[offset*3+1] = 1;
				vertex[offset*3+2] = 0;
				texcoord[offset*2] = u_end;
				texcoord[offset*2+1] = v_start;
				offset ++;
				vertex[offset*3] = cursorX + 1;
				vertex[offset*3+1] = -1;
				vertex[offset*3+2] = 0;
				texcoord[offset*2] = u_end;
				texcoord[offset*2+1] = v_end;
				offset ++;
			}
			cursorX += font.charWidths[(byte)c & 0xff];
		}
		for(offset = 0;offset < vertex.length;offset++){
			vertex[offset] *= scale;
		}
		setVertices(vertex);
		setTextureCoords(texcoord);
		MeshPart p = new MeshPart(indices);
		p.material.diffuseTexture = font.font_texture;
		addPart(p);
		setPrimitiveType(GL.GL_TRIANGLES);
		vertex = null;
		texcoord = null;
		indices = null;
	}

	@Override
	public void preRender() {
		FX.gl.glDisable(GL.GL_DEPTH_TEST);
	}

	@Override
	public void postRender(){
		FX.gl.glEnable(GL.GL_DEPTH_TEST);
	}

	@Override
	public void delete() {
		getPart(0).material.diffuseTexture = -1;
		font = null;
		super.delete();
	}
	
	private int getDrawTextLenght(String process){
		int i = 0;
		for(char c : process.toCharArray()){
			if(c != ' '){
				i++;
			}
		}
		return i;
	}
	
	private float getTextWidth(String text){
		float text_width = 0.0f;
		for(short i = 0;i < text.length();i++){
			text_width += font.charWidths[(byte)text.charAt(i) & 0xff];
		}
		return text_width;
	}
}
