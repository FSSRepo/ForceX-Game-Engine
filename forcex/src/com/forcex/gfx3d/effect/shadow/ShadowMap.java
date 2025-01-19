package com.forcex.gfx3d.effect.shadow;
import com.forcex.math.*;
import com.forcex.core.*;
import com.forcex.gfx3d.*;
import com.forcex.*;
import com.forcex.core.gpu.*;
import com.forcex.gfx3d.effect.*;

public class ShadowMap {
    private int fbo = -1;
    private GL gl = FX.gl;
    private final Light light;
    private final ShadowShader shader;
    private int texture = -1;
	private final boolean skin, filtering;
	public static int size = 128;

	Matrix4f temp;
	
    public ShadowMap(Light light, boolean useSkinning, boolean filterAlpha) {
        this.light = light;
		light.useInShadow();
		shader = new ShadowShader(useSkinning, filterAlpha);
		skin = useSkinning;
		filtering = filterAlpha;
		temp = new Matrix4f();
    }
	
	public void begin() {
		if(fbo == -1) {
			create();
		}
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo);
        gl.glViewport(size, size);
        gl.glCullFace(GL.GL_FRONT);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
        shader.start();
		light.updateLookAt();
	}
	
	public void render(ModelObject obj) {
		if (obj.isShadowMapCullfaceEnabled()) {
			gl.glEnable(GL.GL_CULL_FACE);
		} else {
			gl.glDisable(GL.GL_CULL_FACE);
		}
		shader.setMatrix4f(shader.u_MVPMatrix, light.getProjView().mult(temp, obj.getTransform()));
		Mesh mesh = obj.getMesh();
		VertexBuffer vbo = mesh.getVertexBuffer();
		vbo.bind();
		vbo.enableVertexAttrib(shader.attrib_position, 3, 0);
		if(filtering) {
			vbo.enableVertexAttrib(shader.attrib_texcoord,2,mesh.getVertexInfo().tex_coord_offset);
		}
		if(skin) {
			if(obj.hasAnimator()) {
				vbo.enableVertexAttrib(shader.attrib_bone_wights,4,mesh.getVertexInfo().bone_weights_offset);
				vbo.enableVertexAttrib(shader.attrib_bone_indices,4,mesh.getVertexInfo().bone_indices_offset);
				obj.getAnimator().update();
				shader.setMatrix4fArray(shader.u_BoneMatrices,obj.getAnimator().getBoneMatrices());
				shader.setInt(shader.u_UseSkeleton,1);
			} else {
				shader.setInt(shader.u_UseSkeleton,0);
			}
		}
		for (MeshPart p : mesh.getParts().list) {
			if (p.material.color.a >= 100) {
				if(filtering) {
					Texture.bind(GL.GL_TEXTURE0, p.material.diffuseTexture);
				}
				p.draw(mesh.getPrimitiveType(),false);
			}
		}
	}
	
    public void end() {
        shader.stop();
        gl.glViewport(FX.gpu.getWidth(), FX.gpu.getHeight());
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
        gl.glDisable(GL.GL_CULL_FACE);
    }
	
    public void create() {
		if(!FX.gpu.hasOGLExtension("GL_OES_depth_texture")) {
			FX.device.showInfo(
				"ForceX: \n"+
				"Shadow Map:\n"+
				"Estate: CRASHED\n"+
				"Extension 'GL_OES_depth_texture' not founded."+
				"Error can't create the depth texture.",true);
			FX.device.stopRender();
		}
		Texture.remove(texture);
		texture = -1;

		if(fbo != -1) {
			FX.gl.glDeleteFrameBuffer(fbo);
		}
        texture = gl.glGenTexture();
        gl.glBindTexture(GL.GL_TEXTURE_2D, texture);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);

        fbo = gl.glGenFramebuffer();
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo);
        gl.glEmptyTexture(GL.GL_TEXTURE_2D, size, size, GL.GL_DEPTH_COMPONENT, GL.GL_DEPTH_COMPONENT, GL.GL_UNSIGNED_INT);
        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_TEXTURE_2D, texture);
        if(gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER) != GL.GL_FRAMEBUFFER_COMPLETE) {
			FX.device.showInfo(
				"ForceX: \n"+
				"FrameBuffer:\n"+
				"Estate: CRASHED\n"+
				"Error can't create the framebuffer.",true);
			FX.device.stopRender();
		}
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
    }

    public int getShadowTexture() {
        return texture;
    }
}
