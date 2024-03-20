package com.forcex.postprocessor;
import java.util.*;
import com.forcex.gfx3d.*;
import com.forcex.utils.*;
import com.forcex.*;

public class MaskPass {
	boolean fill;
	ArrayList<ModelObject> masking;
	ModelRenderer render;
	FrameBuffer fbo;
	boolean alpha_background = false,normal_color = false;
	
	public MaskPass(ModelRenderer render){
		this(render,1,1);
	}
	
	public MaskPass(ModelRenderer render,float width,float height){
		this.render = render;
		masking = new ArrayList<>();
		fbo = new FrameBuffer((int)(width*FX.gpu.getWidth()),(int)(height*FX.gpu.getHeight()));
	}
	
	public void addMaskObject(ModelObject... objs){
		for(ModelObject o : objs){
			masking.add(o);
		}
	}
	
	public void setTransparentBackground(boolean z){
		alpha_background = z;
	}
	
	public void setNormalColor(boolean z){
		normal_color = z;
	}
	
	public void reset(){
		fill = false;
	}
	
	public void render(Camera cam){
		if(fill){
			fbo.deleteFBO();
			return;
		}
		fbo.begin();
		FX.gl.glClearColor(0,0,0,alpha_background ? 0 : 1);
		fbo.clear();
		render.begin(cam);
		for(ModelObject o : masking){
			if(!normal_color){
				boolean tmp = o.getMesh().useGlobalColor;
				Color temp = o.getMesh().global_color;
				o.getMesh().useGlobalColor = true;
				render.render(o);
				o.getMesh().useGlobalColor = tmp;
				o.getMesh().global_color = temp;
			}else{
				render.render(o);
			}
		}
		render.end();
		fbo.end();
		fill = true;
	}
	
	public int getTextureMask(){
		return fbo.getTexture();
	}
}
