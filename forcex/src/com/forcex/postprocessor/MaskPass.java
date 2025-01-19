package com.forcex.postprocessor;

import com.forcex.FX;
import com.forcex.gfx3d.Camera;
import com.forcex.gfx3d.ModelObject;
import com.forcex.gfx3d.ModelRenderer;
import com.forcex.utils.Color;

import java.util.ArrayList;
import java.util.Collections;

public class MaskPass {
    boolean fill;
    ArrayList<ModelObject> masking;
    ModelRenderer render;
    FrameBuffer fbo;
    boolean alpha_background = false, normal_color = false;

    public MaskPass(ModelRenderer render) {
        this(render, 1, 1);
    }

    public MaskPass(ModelRenderer render, float width, float height) {
        this.render = render;
        masking = new ArrayList<>();
        fbo = new FrameBuffer((int) (width * FX.gpu.getWidth()), (int) (height * FX.gpu.getHeight()));
    }

    public void addMaskObject(ModelObject... objs) {
		Collections.addAll(masking, objs);
    }

    public void setTransparentBackground(boolean z) {
        alpha_background = z;
    }

    public void setNormalColor(boolean z) {
        normal_color = z;
    }

    public void reset() {
        fill = false;
    }

    public void render(Camera cam) {
        if (fill) {
            fbo.release();
            return;
        }
        fbo.begin();
        FX.gl.glClearColor(0, 0, 0, alpha_background ? 0 : 1);
        fbo.clear();
        render.begin(cam);
        for (ModelObject o : masking) {
            if (!normal_color) {
                boolean tmp = o.getMesh().useGlobalColor;
                Color temp = o.getMesh().global_color;
                o.getMesh().useGlobalColor = true;
                render.render(o);
                o.getMesh().useGlobalColor = tmp;
                o.getMesh().global_color = temp;
            } else {
                render.render(o);
            }
        }
        render.end();
        fbo.end();
        fill = true;
    }

    public int getTextureMask() {
        return fbo.getTexture();
    }
}
