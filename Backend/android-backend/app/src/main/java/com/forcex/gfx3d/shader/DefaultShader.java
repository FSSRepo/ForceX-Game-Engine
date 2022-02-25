package com.forcex.gfx3d.shader;
import com.forcex.*;
import com.forcex.gfx3d.*;
import com.forcex.gfx3d.effect.*;
import com.forcex.io.*;
import com.forcex.math.*;
import com.forcex.utils.*;
import java.util.*;

public class DefaultShader extends ShaderProgram {
    public static final int ALPHA_TEST_FLAG =			0x001;
	public static final int LIGHTING_FLAG = 			0x002;
	public static final int ANIMATION_FLAG = 			0x004;
	public static final int CLIP_PLANE_FLAG = 			0x008;
	public static final int NORMAL_MAP_FLAG =			0x010;
	public static final int SHADOW_MAP_FLAG = 			0x020;
	public static final int REFLECTION_MAP = 		 	0x040;
	public static final int SHADOW_PCF = 			 	0x080;
	public static final int FOG_FLAG = 					0x100;
	public static final int COLOR_FLAG = 				0x200;
	public static final int GAMMA_CORRECTION_FLAG = 	0x400;
	private int flags = -1;
	
	// space
	private int 
	u_ModelMatrix = -1,
	u_ProjViewMatrix = -1,
	u_normalMatrix = -1,
	u_cameraPos = -1,
	
	// animation
	u_boneMatrices = -1,
	u_useSkeleton = -1,
	
	// material
	u_materialSpecular = -1,
	u_materialDiffuse = -1,
	u_materialAmbient = -1,
	u_materialColor = -1,
	u_materialReflection = -1,
	
	// normal map
	u_isNormalMap = -1,
	// light
	u_lightPosition = -1,
	u_lightColor = -1,
	u_lightAmbient = -1,
	
	// clip space
	u_clipPlane = -1,
	// fog
	u_fogParams = -1,
	u_fogColor = -1,
	// shadow
	u_shadowMatrix = -1,
	u_shadowFactor = -1,
	u_shadowSize = -1;
	
	// extras
	int u_PointSize = -1;
	
	Matrix3f tmp = new Matrix3f();
	
    public DefaultShader(int i) {
        update(i);
    }
	public void setShadowParams(Matrix4f sm,float factor) {
		setMatrix4f(u_shadowMatrix, sm);
		setFloat(u_shadowFactor,factor);
    }
	public void setShadowParams(Matrix4f sm,float factor,float size) {
		setMatrix4f(u_shadowMatrix, sm);
		setFloat(u_shadowFactor,factor);
		setVector2(u_shadowSize,new Vector2f(1.0f / size));
    }
	
    public void setModelMatrix(Matrix4f modelmatrix) {
        setMatrix3f(u_normalMatrix, modelmatrix.getUpperLeft(tmp).invert().transpose());
        setMatrix4f(u_ModelMatrix, modelmatrix);
    }

    public void setMaterial(float ambient,float diffuse,float specular,float reflection,Color color) {
		if(flag(LIGHTING_FLAG)){
        	setFloat(u_materialSpecular, specular);
			setFloat(u_materialDiffuse, diffuse);
			setFloat(u_materialAmbient, ambient);
		}
		if(flag(REFLECTION_MAP)){
			setFloat(u_materialReflection, reflection);
		}
		setColor4(u_materialColor, color);
    }
	
    public void setClipPlane(Vector4f clip) {
        setVector4(u_clipPlane, clip);
    }

    public void setFogParams(float start, float end, Color color) {
		setVector2(u_fogParams, new Vector2f(end,start - end));
		setColor3(u_fogColor, color);
    }

    public void setLight(Light light) {
		setVector3(u_lightPosition, light.getPosition());
		setColor3(u_lightColor, light.getColor());
		setColor3(u_lightAmbient, light.getAmbient());
    }

    public void setBoneMatrices(Matrix4f[] array) {
        setMatrix4fArray(u_boneMatrices, array);
    }

    public void setPointSize(float f) {
        setFloat(u_PointSize, f);
    }

	public void setUseSkeleton(boolean z) {
		setInt(u_useSkeleton, z ? 1 : 0);
    }
	
	public void setUseNormalMap(boolean z) {
		setInt(u_isNormalMap, z ? 1 : 0);
    }
	
	public void setCamera(Camera camera) {
        setMatrix4f(u_ProjViewMatrix, camera.getProjViewMatrix());
        setVector3(u_cameraPos, camera.position);
    }
	
    public boolean flag(int i) {
        return (flags & i) != 0;
    }
	
    private String genPrefix() {
        String prefix = "";
        if (flag(LIGHTING_FLAG)) {
            prefix += "#define lightingFlag\n";
        }
        if (flag(FOG_FLAG)) {
            prefix += "#define fogFlag\n";
        }
        if (flag(ALPHA_TEST_FLAG)) {
           	prefix += "#define alphaTestFlag\n";
        }
        if (flag(ANIMATION_FLAG)) {
            prefix += "#define animFlag\n";
        }
        if (flag(COLOR_FLAG)) {
            prefix += "#define colorFlag\n";
        }
        if (flag(NORMAL_MAP_FLAG)) {
            prefix += "#define normalMapFlag\n";
        }
        if (flag(REFLECTION_MAP)) {
            prefix += "#define reflectionCubeMapFlag\n";
        }
        if (flag(SHADOW_MAP_FLAG)) {
            prefix += "#define shadowMapFlag\n";
			if(flag(SHADOW_PCF)){
				prefix += "#define shadowPCF\n";
			}
        }
		if (flag(CLIP_PLANE_FLAG)) {
            prefix += "#define clipPlane\n";
        }
		if(flag(GAMMA_CORRECTION_FLAG)){
			prefix += "#define gammaCorrection\n";
		}
        return prefix;
    }
	
    public static DefaultShader build(int i) {
        return new DefaultShader(i);
    }

    public void update(int flags) {
		if (this.flags != flags) {
            this.flags = flags;
			String genPrefix = genPrefix();
            if (program != -1) {
                cleanUp();
            }
			createProgram(genPrefix + FileUtils.readStringText(FX.homeDirectory+"shaders/default.vs"), genPrefix + FileUtils.readStringText(FX.homeDirectory+"shaders/default.fs"));
			bindAttributes();
			bindUniforms();
        }
    }
	
	private void bindAttributes(){
		attrib_position = getAttribLocation("aPosition");
		attrib_texcoord = getAttribLocation("aTexCoord");
		if (flag(LIGHTING_FLAG) || flag(REFLECTION_MAP)) {
			attrib_normal = getAttribLocation("aNormal");
			if (flag(NORMAL_MAP_FLAG)) {
				attrib_tangent = getAttribLocation("aTangent");
				attrib_bitangent = getAttribLocation("aBitangent");
			}
		}
		if (flag(COLOR_FLAG)) {
			attrib_color = getAttribLocation("aColor");
		}
		if (flag(ANIMATION_FLAG)) {
			attrib_bonew = getAttribLocation("aBoneWeights");
			attrib_bonei = getAttribLocation("aBoneIndices");
		}
	}
	
	private void bindUniforms(){
		u_ProjViewMatrix = getUniformLocation("uProjViewMatrix");
		u_ModelMatrix = getUniformLocation("uModelMatrix");
		u_normalMatrix = getUniformLocation("uNormalMatrix");
		u_PointSize = getUniformLocation("uPointSize");
		u_materialColor = getUniformLocation("mat_color");
		u_cameraPos = getUniformLocation("uCameraPosition");
		if (flag(CLIP_PLANE_FLAG)) {
			u_clipPlane = getUniformLocation("uClipPlane");
		}
		if (flag(ANIMATION_FLAG)) {
			u_useSkeleton = getUniformLocation("useSkinning");
			u_boneMatrices = getUniformLocation("u_boneMatrices");
		}
		if (flag(LIGHTING_FLAG)) {
			u_lightPosition = getUniformLocation("lightPosition");
			u_lightColor = getUniformLocation("lightColor");
			u_lightAmbient = getUniformLocation("lightAmbient");
			u_materialDiffuse = getUniformLocation("mat_diffuse");
			u_materialSpecular = getUniformLocation("mat_specular");
			u_materialAmbient = getUniformLocation("mat_ambient");
			if (flag(NORMAL_MAP_FLAG)) {
				u_isNormalMap = getUniformLocation("mat_useNormalMap");
                setTextureChannel("uNormalTexture",1);
            }
		}
		if (flag(FOG_FLAG)) {
			u_fogParams = getUniformLocation("ufogParams");
			u_fogColor = getUniformLocation("uFogColor");
		}
		if (flag(REFLECTION_MAP)) {
			u_materialReflection = getUniformLocation("mat_reflection");
			setTextureChannel("uReflectTexture",flag(NORMAL_MAP_FLAG) ? 2 : 1);
		}
		if (flag(SHADOW_MAP_FLAG)) {
			u_shadowMatrix = getUniformLocation("uShadowMatrix");
			u_shadowFactor = getUniformLocation("shadowFactor");
			if(flag(SHADOW_PCF)){
				u_shadowSize = getUniformLocation("ShadowSize");
			}
			setTextureChannel("uShadowTexture",flag(NORMAL_MAP_FLAG) && flag(REFLECTION_MAP) ? 3 : (flag(NORMAL_MAP_FLAG) || flag(REFLECTION_MAP) ? 2 : 1));
		}
	}
}
