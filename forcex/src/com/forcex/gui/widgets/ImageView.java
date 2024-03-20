package com.forcex.gui.widgets;
import com.forcex.gui.*;
import com.forcex.math.*;
import com.forcex.*;
import com.forcex.utils.*;
import com.forcex.core.*;

public class ImageView extends View {
	private int texture = -1,vbo_circle,ibo_circle,vbo_fb;
	private Color color;
	private boolean circle,framebuffer_texture;
	private int num_triangles;
	private float rotation;
	
	public ImageView(int texture,float width,float height){
		setWidth(width);
		setHeight(height);
		this.texture = texture;
		color = new Color(Color.WHITE);
		setDebugColor(57,5,179);
	}
	
	public ImageView(int texture,float radius){
		setWidth(radius);
		setHeight(radius);
		this.texture = texture;
		color = new Color(Color.WHITE);
		circle = true;
		setDebugColor(57,5,179);
	}
	
	public ImageView(int texture){
		this(texture,0.2f,0.2f);
	}
	
	public void setMixColor(int red,int green,int blue){
		color.set(red,green,blue);
	}
	
	public void setMixColor(Color color){
		this.color.set(color);
	}
	
	public void setTexture(int texture){
		this.texture = texture;
	}
	
	public void setRotation(float rotation){
		this.rotation = rotation;
	}
	
	public void setFrameBufferTexture(boolean z){
		framebuffer_texture = z;
	}

	@Override
	public void onCreate(Drawer drawer) {
		if(circle){
			float[] vertexs = new float[92*7];
			int offset = 0;
			vertexs[offset] = 0;
			vertexs[offset+1] = 0;
			vertexs[offset+2] = 0.5f;
			vertexs[offset+3] = 0.5f;
			vertexs[offset+4] = 1f;
			vertexs[offset+5] = 1f;
			vertexs[offset+6] = 1f;
			offset += 7;
			for(short i = 0;i <= 90;i++){
				float c = Maths.cos((i / 90.0f) * Maths.PI_2);
				float s = Maths.sin((i / 90.0f) * Maths.PI_2);
				vertexs[offset] = s;
				vertexs[offset+1] = c;
				vertexs[offset+2] = (s * 0.5f) + 0.5f;
				vertexs[offset+3] = (c * -0.5f) + 0.5f;
				vertexs[offset+4] = 1f;
				vertexs[offset+5] = 1f;
				vertexs[offset+6] = 1f;
				offset += 7;
			}
			short[] indices = new short[91*3];
			offset = 0;
			for(short i = 1;i <= 91;i++){
				indices[offset] = i;
				indices[offset+1] = 0;
				indices[offset+2] = (short)(i+1);
				offset += 3;
			}
			num_triangles = indices.length;
			vbo_circle = drawer.createBuffer(vertexs,false,false);
			ibo_circle = drawer.createBuffer(indices,true,false);
		}
		vbo_fb = drawer.createBuffer(new float[]{
								   -1,1,0,1,1,1,1,
								   -1,-1,0,0,1,1,1,
								   1,1,1,1,1,1,1,
								   1,-1,1,0,1,1,1
							   },false,false);
	}
	
	@Override
	public void onDraw(Drawer drawer) {
		if(framebuffer_texture){
			drawer.setScale(extent.x,extent.y);
			drawer.freeRender(vbo_fb,local,color,texture);
			FX.gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
		}else{
			if(!circle){
				drawer.setTransform(rotation,extent.x,extent.y);
				drawer.renderQuad(local,color,texture);
			}else{
				drawer.setScale(extent.x,extent.y);
				drawer.freeRender(vbo_circle,local,color,texture);
				FX.gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,ibo_circle);
				FX.gl.glDrawElements(GL.GL_TRIANGLES,num_triangles);
			}
		}
		if(debug){
			drawer.setScale(extent.x,extent.y);
			drawer.renderLineQuad(local,this.debug_color);
		}
	}

	@Override
	public void onDestroy() {
		setVisibility(INVISIBLE);
		if(texture != -1){
			FX.gl.glDeleteTexture(texture);
		}
		if(circle){
			FX.gl.glDeleteBuffer(vbo_circle);
			FX.gl.glDeleteBuffer(ibo_circle);
		}
		color = null;
	}

	@Override
	public void updateExtent()
	{
		extent.set(width,height * (applyAspectRatio ? context.getAspectRatio() : 1.0f));
		local.set(relative);
	}
	
}
