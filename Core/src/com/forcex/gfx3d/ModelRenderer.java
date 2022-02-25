package com.forcex.gfx3d;
import com.forcex.gfx3d.shader.*;
import com.forcex.core.*;
import com.forcex.*;
import com.forcex.utils.*;
import com.forcex.math.*;
import com.forcex.core.gpu.*;
import com.forcex.gfx3d.effect.shadow.*;

public class ModelRenderer
{
	Vector4f clipPlane;
    Environment env;
    DefaultShader shader;
    private boolean 
	useAlphaTest,
	useAnimation,
	useVertexColor,
	useNormalMap,
	useReflectionMap,
	useShadowMap,
	useShadowPCF,
	useGammaCorrection,
	useClipPlane,
	flags_need_update;
	public int shadowMap = -1;
	GL gl = FX.gl;
	
	public ModelRenderer() {
        this(false);
    }

    public ModelRenderer(boolean anim) {
        this(new Environment(), anim);
    }

    public ModelRenderer(Environment environment) {
        this(environment, false);
    }

    public ModelRenderer(Environment environment, boolean anim) {
        env = environment;
        useAnimation = anim;
    }

    public void setClipPlane(float x,float y,float z,float val) {
        clipPlane.set(x,y,z,val);
    }
	
    public void useAlphaTest(boolean z) {
        useAlphaTest = z;
    }

    public void clearBuffers() {
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
    }

    public void useVertexColor(boolean z) {
        useVertexColor = z;
    }

    public void useNormalMap(boolean z) {
        useNormalMap = z;
    }

    public void useReflectionMap(boolean z) {
        useReflectionMap = z;
    }

    public void useShadowMap(boolean z,boolean usePCF) {
        useShadowMap = z;
		useShadowPCF = usePCF;
    }
	
	public void useGammaCorrection(boolean z){
		useGammaCorrection = z;
	}
	
    public void useClipPlane(boolean z) {
        useClipPlane = z;
		if(z){
     	   clipPlane = new Vector4f(0, -1, 0, 100000);
		}
    }

	public void notifyFlagUpdate(){
		flags_need_update = true;
	}
	
    public void begin(Camera camera) {
        if (shader == null) {
            shader = DefaultShader.build(updateFlags());
			flags_need_update = false;
        } else if(flags_need_update) {
            shader.update(updateFlags());
			flags_need_update = false;
        }
        shader.start();
        shader.setCamera(camera);
        if (useClipPlane) {
            shader.setClipPlane(clipPlane);
        }
        if (env.light != null) {
            shader.setLight(env.light);
			if (useShadowMap) {
				if(useShadowPCF){
					shader.setShadowParams(env.light.getProjView(),0.5f,ShadowMap.size);
				}else{
					shader.setShadowParams(env.light.getProjView(),0.5f);
					
				}
				Texture.bind((useNormalMap && useReflectionMap) ? GL.GL_TEXTURE3 : ((useNormalMap || useReflectionMap) ? GL.GL_TEXTURE2 : GL.GL_TEXTURE1),shadowMap);
			}
        }
		if(env.fogColor != null){
			shader.setFogParams(env.fogParams,env.fogColor);
		}
    }

    public Environment getEnvironment() {
        return env;
    }
	
	private int updateFlags(){
		int flag = 0;
		if(env.light != null){
			useVertexColor = false;
			flag |= DefaultShader.LIGHTING_FLAG;
		}
		if(env.fogParams != null){
			flag |= DefaultShader.FOG_FLAG;
		}
		if(useAnimation){
			flag |= DefaultShader.ANIMATION_FLAG;
		}
		if(useAlphaTest){
			flag |= DefaultShader.ALPHA_TEST_FLAG;
		}
		if(useVertexColor){
			flag |= DefaultShader.COLOR_FLAG;
		}
		if(useNormalMap){
			flag |= DefaultShader.NORMAL_MAP_FLAG;
		}
		if(useReflectionMap){
			flag |= DefaultShader.REFLECTION_MAP;
		}
		if(useShadowMap){
			flag |= DefaultShader.SHADOW_MAP_FLAG;
			if(useShadowPCF){
				flag |= DefaultShader.SHADOW_PCF;
			}
		}
		if(useClipPlane){
			flag |= DefaultShader.CLIP_PLANE_FLAG;
		}
		if(useGammaCorrection){
			flag |= DefaultShader.GAMMA_CORRECTION_FLAG;
		}
		return flag;
	}
	
	public void render(ModelObject obj){
		obj.update();
		obj.render(shader);
	}
	
	public void render(DrawDynamicTriangle drawer){
		drawer.render(shader);
	}
	
	public void end(){
		gl.glDisableVertexAttribArray(shader.attrib_position);
		gl.glDisableVertexAttribArray(shader.attrib_texcoord);
		if(shader.attrib_color != -1){
			gl.glDisableVertexAttribArray(shader.attrib_color);
		}
		if(shader.attrib_normal != -1){
			gl.glDisableVertexAttribArray(shader.attrib_normal);
		}
		if(shader.attrib_tangent != -1){
			gl.glDisableVertexAttribArray(shader.attrib_bitangent);
			gl.glDisableVertexAttribArray(shader.attrib_tangent);
		}
		if(shader.attrib_bonew != -1){
			gl.glDisableVertexAttribArray(shader.attrib_bonew);
			gl.glDisableVertexAttribArray(shader.attrib_bonei);
		}
		shader.stop();
	}
	
	public void delete(){
		env = null;
		shader.cleanUp();
		shader = null;
	}
}
