package com.forcex.gfx3d.effect.shadow;

import com.forcex.gfx3d.shader.ShaderProgram;

public class ShadowShader extends ShaderProgram {
    public int u_MVPMatrix, u_BoneMatrices, u_UseSkeleton;

    public ShadowShader(boolean useSkinning, boolean filterAplha) {
        String prefix = "";
        if (useSkinning) {
            prefix += "#define useSkinning\n";
        }
        if (filterAplha) {
            prefix += "#define filterAlpha\n";
        }
        createProgram("shaders/shadow.vs", "shaders/shadow.fs", prefix);
        u_MVPMatrix = getUniformLocation("uMVPMatrix");
        attrib_position = getAttribLocation("aPosition");
        if (useSkinning) {
            attrib_bone_wights = getAttribLocation("aBoneWeights");
            attrib_bone_indices = getAttribLocation("aBoneIndices");
            u_UseSkeleton = getUniformLocation("isAnimation");
            u_BoneMatrices = getUniformLocation("uBoneMatrices");
        }
        if (filterAplha) {
            attrib_texcoord = getAttribLocation("aTexCoords");
        }
    }
}
