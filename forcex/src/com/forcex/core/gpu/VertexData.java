package com.forcex.core.gpu;

import com.forcex.utils.BufferUtils;

import java.nio.FloatBuffer;

public class VertexData {
    public float[]
            vertices, uvs, normals,
            tangents, bi_tangents,
            bone_weights;
    public byte[] colors, bone_indices;

    public FloatBuffer convert(VertexInfo info) {
        FloatBuffer fb = BufferUtils.createByteBuffer(info.getDataSize()).asFloatBuffer();
        if (bi_tangents == null && info.hasTangents()) {
            info.removeFlag(VertexInfo.HAS_TANGENTS);
        }
        for (int i = 0; i < info.vertexCount; i++) {
            fb.put(vertices[i * 3]);
            fb.put(vertices[i * 3 + 1]);
            fb.put(vertices[i * 3 + 2]);
            if (info.hasTextureCoords()) {
                fb.put(uvs[i * 2]);
                fb.put(uvs[i * 2 + 1]);
            }
            if (info.hasNormals()) {
                fb.put(normals[i * 3]);
                fb.put(normals[i * 3 + 1]);
                fb.put(normals[i * 3 + 2]);
            }
            if (info.hasColors()) {
                fb.put((colors[i * 4] & 0xff) * 0.003921f);
                fb.put((colors[i * 4 + 1] & 0xff) * 0.003921f);
                fb.put((colors[i * 4 + 2] & 0xff) * 0.003921f);
                fb.put((colors[i * 4 + 3] & 0xff) * 0.003921f);
            }
            if (info.hasBones()) {
                fb.put(bone_weights[i * 4]);
                fb.put(bone_weights[i * 4 + 1]);
                fb.put(bone_weights[i * 4 + 2]);
                fb.put(bone_weights[i * 4 + 3]);
                fb.put(bone_indices[i * 4] & 0xff);
                fb.put(bone_indices[i * 4 + 1] & 0xff);
                fb.put(bone_indices[i * 4 + 2] & 0xff);
                fb.put(bone_indices[i * 4 + 3] & 0xff);
            }
            if (info.hasTangents()) {
                fb.put(tangents[i * 3]);
                fb.put(tangents[i * 3 + 1]);
                fb.put(tangents[i * 3 + 2]);
            }
            if (info.hasTangents()) {
                fb.put(bi_tangents[i * 3]);
                fb.put(bi_tangents[i * 3 + 1]);
                fb.put(bi_tangents[i * 3 + 2]);
            }
        }
        fb.position(0);
        return fb;
    }

    public void delete() {
        vertices = null;
        uvs = null;
        bone_weights = null;
        bone_indices = null;
        colors = null;
        normals = null;
        tangents = null;
        bi_tangents = null;
    }
}
