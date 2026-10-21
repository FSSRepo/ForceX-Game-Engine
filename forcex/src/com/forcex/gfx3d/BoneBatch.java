package com.forcex.gfx3d;

import com.forcex.core.gpu.IndexBuffer;
import com.forcex.core.gpu.VertexBuffer;
import com.forcex.core.gpu.VertexData;
import com.forcex.core.gpu.VertexInfo;
import com.forcex.math.Matrix4f;

/**
 * A subset of a skinned mesh whose vertices only use a bone palette that fits
 * in the GPU uniform budget. It owns its vertex buffer (with remapped bone
 * indices) and its index buffers, while the materials are shared with the
 * original mesh parts.
 */
public class BoneBatch {
    private final VertexBuffer vbo;
    private final MeshPart parts = new MeshPart();
    private final int[] boneMap;
    private final Matrix4f[] palette;
    private final Matrix4f identity = new Matrix4f();

    BoneBatch(VertexData data, VertexInfo info, int[] boneMap) {
        this.boneMap = boneMap;
        this.palette = new Matrix4f[boneMap.length];
        this.vbo = new VertexBuffer(data, info, true);
    }

    void addPart(MeshPart part) {
        parts.add(part);
    }

    public void initialize() {
        vbo.update();
        for (MeshPart part : parts.list) {
            if (part.buffer == null) {
                part.buffer = new IndexBuffer();
                part.buffer.update(part.index);
            }
        }
    }

    public void bind() {
        vbo.bind();
    }

    public void enableVertexAttrib(int attrib_idx, int dimen, int offset) {
        vbo.enableVertexAttrib(attrib_idx, dimen, offset);
    }

    public VertexBuffer getVertexBuffer() {
        return vbo;
    }

    public VertexInfo getVertexInfo() {
        return vbo.vertex_info;
    }

    public MeshPart getParts() {
        return parts;
    }

    public int getBoneCount() {
        return boneMap.length;
    }

    public int getVertexCount() {
        return vbo.vertex_info.vertexCount;
    }

    /**
     * Collects the global bone matrices used by this batch into its local
     * palette. Missing bones fall back to identity so the shader never reads
     * an invalid entry.
     */
    public Matrix4f[] getPalette(Matrix4f[] globalBones) {
        for (int i = 0; i < boneMap.length; i++) {
            int bone = boneMap[i];
            palette[i] = (globalBones != null && bone >= 0 && bone < globalBones.length && globalBones[bone] != null)
                    ? globalBones[bone] : identity;
        }
        return palette;
    }

    public void delete() {
        vbo.delete();
        for (MeshPart part : parts.list) {
            if (part.buffer != null) {
                part.buffer.delete();
                part.buffer = null;
            }
            part.index = null;
        }
        parts.list.clear();
    }
}
