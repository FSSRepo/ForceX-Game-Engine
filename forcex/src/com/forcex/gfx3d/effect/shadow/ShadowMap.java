package com.forcex.gfx3d.effect.shadow;
import com.forcex.math.*;
import com.forcex.core.*;
import com.forcex.gfx3d.*;
import com.forcex.*;
import com.forcex.core.gpu.*;
import com.forcex.gfx3d.effect.*;

public class ShadowMap {
    public static final int DEFAULT_SIZE = 128;

    private int fbo = -1;
    private GL gl = FX.gl;
    private final Light light;
    private final ShadowShader shader;
    private int texture = -1;
	private final boolean skin, filtering;
	private final int size;
	private boolean polygon_offset;
	private float polygon_offset_factor = 1.5f, polygon_offset_units = 2.0f;

	Matrix4f temp;

	private boolean prev_depth_test, prev_depth_mask, prev_cull_face, prev_polygon_offset;
	private int prev_cull_face_mode, prev_depth_func;
	private final int[] prev_viewport = new int[4];
	private final float[] prev_polygon_offset_values = new float[2];
	
    public ShadowMap(Light light, boolean useSkinning, boolean filterAlpha) {
		this(light, useSkinning, filterAlpha, DEFAULT_SIZE);
    }

	public ShadowMap(Light light, boolean useSkinning, boolean filterAlpha, int size) {
        this.light = light;
		light.useInShadow();
		shader = new ShadowShader(useSkinning, filterAlpha);
		skin = useSkinning;
		filtering = filterAlpha;
		this.size = size;
		temp = new Matrix4f();
    }

	/**
	 * Enables polygon offset during the shadow pass. It is an alternative to
	 * front-face culling for open geometry that still produces acne.
	 */
	public void setPolygonOffset(boolean enabled, float factor, float units) {
		polygon_offset = enabled;
		polygon_offset_factor = factor;
		polygon_offset_units = units;
	}

	public int getSize() {
		return size;
	}
	
	public void begin() {
		prev_depth_test = gl.glIsEnabled(GL.GL_DEPTH_TEST);
		prev_cull_face = gl.glIsEnabled(GL.GL_CULL_FACE);
		prev_depth_mask = gl.glGetInteger(GL.GL_DEPTH_WRITEMASK) != 0;
		prev_cull_face_mode = gl.glGetInteger(GL.GL_CULL_FACE_MODE);
		prev_depth_func = gl.glGetInteger(GL.GL_DEPTH_FUNC);
		gl.glGetInteger(GL.GL_VIEWPORT, prev_viewport, 0);
		prev_polygon_offset = gl.glIsEnabled(GL.GL_POLYGON_OFFSET_FILL);
		if (polygon_offset) {
			gl.glGetFloat(GL.GL_POLYGON_OFFSET_FACTOR, prev_polygon_offset_values, 0);
			gl.glGetFloat(GL.GL_POLYGON_OFFSET_UNITS, prev_polygon_offset_values, 1);
		}

		if(fbo == -1) {
			create();
		}
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo);
        gl.glViewport(size, size);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LESS);
        gl.glDepthMask(true);
        gl.glCullFace(GL.GL_FRONT);
        if (polygon_offset) {
            gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset(polygon_offset_factor, polygon_offset_units);
        }
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
        shader.start();
		light.updateLookAt();
	}
	
	public void render(ModelObject obj) {
		if (obj == null || !obj.isVisible()) {
			return;
		}
		Mesh mesh = obj.getMesh();
		if (mesh == null) {
			return;
		}
		mesh.initializeIfNeeded();
		if (obj.isShadowMapCullfaceEnabled()) {
			gl.glEnable(GL.GL_CULL_FACE);
		} else {
			gl.glDisable(GL.GL_CULL_FACE);
		}
		shader.setMatrix4f(shader.u_MVPMatrix, light.getProjView().mult(temp, obj.getTransform()));
		if (skin && obj.hasAnimator() && mesh.hasBoneBatches()) {
			Matrix4f[] boneBatches = obj.getAnimator().getBoneMatrices();
			if (boneBatches != null) {
				shader.setInt(shader.u_UseSkeleton, 1);
				renderBatches(mesh, boneBatches);
				return;
			}
		}
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
				Matrix4f[] bones = obj.getAnimator().getBoneMatrices();
				if(bones != null) {
					shader.setBoneMatrices(shader.u_BoneMatrices,bones);
					shader.setInt(shader.u_UseSkeleton,1);
				} else {
					shader.setInt(shader.u_UseSkeleton,0);
				}
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
	
	private void renderBatches(Mesh mesh, Matrix4f[] boneMatrices) {
		for (BoneBatch batch : mesh.getBoneBatches()) {
			batch.initialize();
			batch.bind();
			VertexInfo info = batch.getVertexInfo();
			batch.enableVertexAttrib(shader.attrib_position, 3, 0);
			if (filtering) {
				batch.enableVertexAttrib(shader.attrib_texcoord, 2, info.tex_coord_offset);
			}
			batch.enableVertexAttrib(shader.attrib_bone_wights, 4, info.bone_weights_offset);
			batch.enableVertexAttrib(shader.attrib_bone_indices, 4, info.bone_indices_offset);
			shader.setBoneMatrices(shader.u_BoneMatrices, batch.getPalette(boneMatrices));
			for (MeshPart p : batch.getParts().list) {
				if (p.material.color.a >= 100) {
					if (filtering) {
						Texture.bind(GL.GL_TEXTURE0, p.material.diffuseTexture);
					}
					p.draw(mesh.getPrimitiveType(), false);
				}
			}
		}
	}

    public void end() {
        shader.stop();
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
        gl.glViewport(prev_viewport[2], prev_viewport[3]);
        gl.glDepthFunc(prev_depth_func);
        gl.glDepthMask(prev_depth_mask);
        gl.glCullFace(prev_cull_face_mode);
        if(polygon_offset) {
            gl.glPolygonOffset(prev_polygon_offset_values[0], prev_polygon_offset_values[1]);
            if(prev_polygon_offset) {
                gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
            } else {
                gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);
            }
        }
        if(prev_depth_test) {
            gl.glEnable(GL.GL_DEPTH_TEST);
        } else {
            gl.glDisable(GL.GL_DEPTH_TEST);
        }
        if(prev_cull_face) {
            gl.glEnable(GL.GL_CULL_FACE);
        } else {
            gl.glDisable(GL.GL_CULL_FACE);
        }
    }

	/**
	 * Invalidates the GL resources of the shadow map (texture and FBO) so they
	 * are recreated on the next begin(). Call it when the OpenGL context is
	 * recreated (for instance when the Android surface is resumed), because the
	 * previous identifiers are no longer valid.
	 */
	public void invalidate() {
		fbo = -1;
		texture = -1;
	}
	
    public void create() {
		if(FX.gpu.isOpenGLES() && !FX.gpu.hasOGLExtension("GL_OES_depth_texture")) {
			FX.device.showInfo(
				"ForceX: \n"+
				"Shadow Map:\n"+
				"Estate: CRASHED\n"+
				"Extension 'GL_OES_depth_texture' not founded."+
				"Error can't create the depth texture.",true);
			FX.device.stopRender();
		}
		// On desktop the extension does not exist; depth textures are core and
		// the format is validated below with glCheckFramebufferStatus.
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
