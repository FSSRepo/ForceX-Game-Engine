package com.forcex.gfx3d.effect.shadow;
import com.forcex.gfx3d.shader.*;
import com.forcex.io.*;
import com.forcex.*;

public class ShadowShader extends ShaderProgram {
    public int u_MVPMatrix,u_BoneMatrices,u_UseSkeleton;
	
    public ShadowShader(boolean useSkinning,boolean filterAplha) {
		String prefix = "";
		if(useSkinning){
			prefix += "#define useSkinning\n";
		}
		if(filterAplha){
			prefix += "#define filterAlpha\n";
		}
		createProgram(
			prefix + FileUtils.readStringText(FX.homeDirectory+"shaders/shadow.vs"),
			prefix + FileUtils.readStringText(FX.homeDirectory+"shaders/shadow.fs"));
		u_MVPMatrix = getUniformLocation("uMVPMatrix");
		attrib_position = getAttribLocation("aPosition");
		if(useSkinning){
			attrib_bonew = getAttribLocation("aBoneWeights");
			attrib_bonei = getAttribLocation("aBoneIndices");
			u_UseSkeleton = getUniformLocation("isAnimation");
			u_BoneMatrices = getUniformLocation("uBoneMatrices");
		}
		if(filterAplha){
			attrib_texcoord = getAttribLocation("aTexCoords");
		}
    }
}
