package com.forcex.gfx3d;

import com.forcex.FX;
import com.forcex.anim.Animator;
import com.forcex.core.GL;
import com.forcex.core.gpu.Texture;
import com.forcex.core.gpu.VertexBuffer;
import com.forcex.core.gpu.VertexData;
import com.forcex.core.gpu.VertexInfo;
import com.forcex.gfx3d.shader.DefaultShader;
import com.forcex.math.Matrix4f;
import com.forcex.utils.Color;
import com.forcex.utils.Logger;

import java.util.ArrayList;
import java.util.Arrays;

public class Mesh {
    VertexBuffer vbo;
    MeshPart parts = new MeshPart();
    int primitiveType;
    public float lineSize = 2.0f, pointSize = 4.0f;
    public int reflectionMap = -1;
    boolean isClone;
    boolean started;
    public boolean useGlobalColor = false;
    public Color global_color;
    public boolean useCustomPartType = false;
    private final ArrayList<BoneBatch> boneBatches = new ArrayList<>();
    private int usedBoneCount = 0;

    public Mesh(boolean isStatic) {
        vbo = new VertexBuffer(new VertexData(), new VertexInfo(), isStatic);
        primitiveType = GL.GL_TRIANGLES;
        global_color = new Color(Color.WHITE);
    }

    public Mesh(boolean isStatic, VertexData vertexs) {
        vbo = new VertexBuffer(vertexs, new VertexInfo(), isStatic);
        primitiveType = GL.GL_TRIANGLES;
        global_color = new Color(Color.WHITE);
    }

    public Mesh clone() {
        if (!isClone && started) {
            if (!boneBatches.isEmpty()) {
                Logger.log("Mesh: cloning a batched skinned mesh does not copy its bone batches.");
            }
            Mesh mesh = new Mesh(false);
            mesh.vbo = vbo.clone();
            mesh.primitiveType = primitiveType;
            mesh.isClone = true;
            for (MeshPart p : parts.list) {
                mesh.addPart(p.clone());
            }
            mesh.global_color = global_color;
            return mesh;
        } else {
            FX.device.showInfo("Error cloning the mesh: it maybe this mesh is a clone or this isn't initialized", true);
            FX.device.stopRender();
        }
        return null;
    }

    public void setPrimitiveType(int primitiveType) {
        this.primitiveType = primitiveType;
    }

    public int getPrimitiveType() {
        return primitiveType;
    }

    public void setVertices(float[] data) {
        getVertexInfo().vertexCount = data.length / 3;
        if (getVertexData().vertices != null) {
            getVertexData().vertices = null;
        }
        getVertexData().vertices = data;
        vbo.reset();
    }

    public void setTextureCoords(float[] data) {
        getVertexInfo().addFlag(VertexInfo.HAS_TEXTURE_COORDINATES);
        if (getVertexData().uvs != null) {
            getVertexData().uvs = null;
        }
        getVertexData().uvs = data;
        vbo.reset();
    }

    public void setNormals(float[] data) {
        getVertexInfo().addFlag(VertexInfo.HAS_NORMALS);
        if (getVertexData().normals != null) {
            getVertexData().normals = null;
        }
        getVertexData().normals = data;
        vbo.reset();
    }

    public void setTangents(float[] data) {
        getVertexInfo().addFlag(VertexInfo.HAS_TANGENTS);
        getVertexData().tangents = data;
        vbo.reset();
    }

    public void setBiTangents(float[] data) {
        getVertexInfo().addFlag(VertexInfo.HAS_TANGENTS);
        getVertexData().bi_tangents = data;
        vbo.reset();
    }

    public void setVertexColor(byte[] data) {
        getVertexInfo().removeFlag(VertexInfo.HAS_NORMALS);
        getVertexInfo().addFlag(VertexInfo.HAS_COLORS);
        getVertexData().colors = data;
        vbo.reset();
    }

    public void setBoneIndices(byte[] data) {
        getVertexInfo().addFlag(VertexInfo.HAS_BONES);
        getVertexData().bone_indices = data;
        vbo.reset();
    }

    public void setBoneWeights(float[] data) {
        getVertexInfo().addFlag(VertexInfo.HAS_BONES);
        getVertexData().bone_weights = data;
        vbo.reset();
    }

    public void addPart(MeshPart part) {
        parts.add(part);
    }

    public VertexBuffer getVertexBuffer() {
        return vbo;
    }

    public MeshPart getPart(int idx) {
        if (idx >= parts.size()) {
            return parts.get(0);
        }
        return parts.get(idx);
    }

    public MeshPart getParts() {
        return parts;
    }

    public VertexInfo getVertexInfo() {
        return vbo.vertex_info;
    }

    public VertexData getVertexData() {
        return vbo.vertex_data;
    }

    public boolean hasBoneBatches() {
        return !boneBatches.isEmpty();
    }

    public ArrayList<BoneBatch> getBoneBatches() {
        return boneBatches;
    }

    /** Number of distinct bones used by the vertices (batching cost metric). */
    public int getUsedBoneCount() {
        return usedBoneCount;
    }

    /**
     * Splits the skinned mesh into batches whose local bone palette fits in
     * {@code maxBones}. Vertices are duplicated per batch and their bone
     * indices are remapped to the batch palette. Returns true when batches
     * were created (the mesh exceeds the GPU budget).
     */
    public boolean buildBoneBatches(int maxBones) {
        if (!boneBatches.isEmpty()) {
            return true;
        }
        VertexInfo info = getVertexInfo();
        VertexData data = getVertexData();
        if (!info.hasBones() || data.bone_indices == null || data.bone_weights == null) {
            return false;
        }
        if (primitiveType != GL.GL_TRIANGLES) {
            Logger.log("Mesh: bone batching requires GL_TRIANGLES (primitive=" + primitiveType + ").");
            return false;
        }
        int limit = Math.max(maxBones, 12);
        int boneCount = 0;
        for (int i = 0; i < data.bone_indices.length; i++) {
            boneCount = Math.max(boneCount, data.bone_indices[i] & 0xFF);
        }
        boneCount++;
        boolean[] usedBones = new boolean[boneCount];
        int used = 0;
        for (int v = 0; v < info.vertexCount; v++) {
            for (int k = 0; k < 4; k++) {
                int bone = data.bone_indices[v * 4 + k] & 0xFF;
                if (data.bone_weights[v * 4 + k] > 0f && bone < boneCount && !usedBones[bone]) {
                    usedBones[bone] = true;
                    used++;
                }
            }
        }
        usedBoneCount = used;
        if (used <= limit) {
            return false;
        }
        ArrayList<BatchBuilder> builders = new ArrayList<>();
        BatchBuilder current = null;
        int[] needed = new int[12];
        for (int p = 0; p < parts.size(); p++) {
            short[] indices = parts.get(p).index;
            if (indices == null) {
                continue;
            }
            for (int t = 0; t + 2 < indices.length; t += 3) {
                int neededCount = collectTriangleBones(indices, t, data, info.vertexCount, boneCount, needed);
                if (neededCount > limit) {
                    Logger.log("Mesh: a triangle uses " + neededCount + " bones, over the palette limit " + limit + ".");
                    return false;
                }
                if (current == null) {
                    current = new BatchBuilder(boneCount);
                }
                int added = 0;
                for (int i = 0; i < neededCount; i++) {
                    if (!current.inBatch[needed[i]]) {
                        added++;
                    }
                }
                if (current.bones.size() + added > limit) {
                    builders.add(current);
                    current = new BatchBuilder(boneCount);
                }
                for (int i = 0; i < neededCount; i++) {
                    if (!current.inBatch[needed[i]]) {
                        current.inBatch[needed[i]] = true;
                        current.bones.add(needed[i]);
                    }
                }
                current.triangles.add(new int[]{p, indices[t], indices[t + 1], indices[t + 2]});
            }
        }
        if (current != null) {
            builders.add(current);
        }
        for (BatchBuilder builder : builders) {
            BoneBatch batch = buildBatch(builder, info, data, boneCount);
            if (batch != null) {
                boneBatches.add(batch);
            }
        }
        return !boneBatches.isEmpty();
    }

    private static int collectTriangleBones(short[] indices, int offset, VertexData data, int vertexCount, int boneCount, int[] out) {
        int count = 0;
        for (int k = 0; k < 3; k++) {
            int vertex = indices[offset + k];
            if (vertex < 0 || vertex >= vertexCount) {
                continue;
            }
            for (int j = 0; j < 4; j++) {
                int bone = data.bone_indices[vertex * 4 + j] & 0xFF;
                if (data.bone_weights[vertex * 4 + j] <= 0f || bone >= boneCount) {
                    continue;
                }
                boolean duplicate = false;
                for (int m = 0; m < count; m++) {
                    if (out[m] == bone) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate && count < out.length) {
                    out[count++] = bone;
                }
            }
        }
        return count;
    }

    private BoneBatch buildBatch(BatchBuilder builder, VertexInfo srcInfo, VertexData srcData, int boneCount) {
        int[] vertexMap = new int[srcInfo.vertexCount];
        Arrays.fill(vertexMap, -1);
        int localCount = 0;
        for (int[] triangle : builder.triangles) {
            for (int k = 1; k <= 3; k++) {
                int vertex = triangle[k];
                if (vertex >= 0 && vertex < vertexMap.length && vertexMap[vertex] == -1) {
                    vertexMap[vertex] = localCount++;
                }
            }
        }
        if (localCount == 0) {
            return null;
        }
        int[] localBone = new int[boneCount];
        Arrays.fill(localBone, -1);
        int[] palette = new int[builder.bones.size()];
        for (int i = 0; i < palette.length; i++) {
            palette[i] = builder.bones.get(i);
            localBone[palette[i]] = i;
        }
        VertexData sub = new VertexData();
        sub.vertices = new float[localCount * 3];
        boolean hasUvs = srcInfo.hasTextureCoords() && srcData.uvs != null;
        if (hasUvs) {
            sub.uvs = new float[localCount * 2];
        }
        boolean hasNormals = srcInfo.hasNormals() && srcData.normals != null;
        if (hasNormals) {
            sub.normals = new float[localCount * 3];
        }
        boolean hasColors = srcInfo.hasColors() && srcData.colors != null;
        if (hasColors) {
            sub.colors = new byte[localCount * 4];
        }
        if (srcInfo.hasBones()) {
            sub.bone_weights = new float[localCount * 4];
            sub.bone_indices = new byte[localCount * 4];
        }
        boolean hasTangents = srcInfo.hasTangents() && srcData.tangents != null && srcData.bi_tangents != null;
        if (hasTangents) {
            sub.tangents = new float[localCount * 3];
            sub.bi_tangents = new float[localCount * 3];
        }
        for (int v = 0; v < srcInfo.vertexCount; v++) {
            int local = vertexMap[v];
            if (local == -1) {
                continue;
            }
            System.arraycopy(srcData.vertices, v * 3, sub.vertices, local * 3, 3);
            if (hasUvs) {
                System.arraycopy(srcData.uvs, v * 2, sub.uvs, local * 2, 2);
            }
            if (hasNormals) {
                System.arraycopy(srcData.normals, v * 3, sub.normals, local * 3, 3);
            }
            if (hasColors) {
                System.arraycopy(srcData.colors, v * 4, sub.colors, local * 4, 4);
            }
            if (hasTangents) {
                System.arraycopy(srcData.tangents, v * 3, sub.tangents, local * 3, 3);
                System.arraycopy(srcData.bi_tangents, v * 3, sub.bi_tangents, local * 3, 3);
            }
            if (sub.bone_weights != null) {
                System.arraycopy(srcData.bone_weights, v * 4, sub.bone_weights, local * 4, 4);
                for (int j = 0; j < 4; j++) {
                    int bone = srcData.bone_indices[v * 4 + j] & 0xFF;
                    float weight = srcData.bone_weights[v * 4 + j];
                    int localBoneIndex = (weight > 0f && bone < boneCount) ? localBone[bone] : -1;
                    sub.bone_indices[local * 4 + j] = (byte) Math.max(localBoneIndex, 0);
                }
            }
        }
        VertexInfo subInfo = srcInfo.clone();
        subInfo.setVertexCount(localCount);
        BoneBatch batch = new BoneBatch(sub, subInfo, palette);
        int i = 0;
        while (i < builder.triangles.size()) {
            int partIndex = builder.triangles.get(i)[0];
            int start = i;
            while (i < builder.triangles.size() && builder.triangles.get(i)[0] == partIndex) {
                i++;
            }
            short[] localIndices = new short[(i - start) * 3];
            for (int t = start; t < i; t++) {
                int[] triangle = builder.triangles.get(t);
                for (int k = 1; k <= 3; k++) {
                    localIndices[(t - start) * 3 + k - 1] = (short) vertexMap[triangle[k]];
                }
            }
            MeshPart part = new MeshPart(localIndices);
            part.material = parts.get(partIndex).material;
            batch.addPart(part);
        }
        return batch;
    }

    private static final class BatchBuilder {
        final ArrayList<Integer> bones = new ArrayList<>();
        final boolean[] inBatch;
        final ArrayList<int[]> triangles = new ArrayList<>();

        BatchBuilder(int boneCount) {
            inBatch = new boolean[boneCount];
        }
    }

    protected void update() {
        vbo.update();
        parts.update(getVertexInfo().hasTangents());
        for (int i = 0; i < boneBatches.size(); i++) {
            boneBatches.get(i).initialize();
        }
    }

    public void initialize() {
        if (!started) {
            vbo.update();
            parts.update(getVertexInfo().hasTangents());
            for (int i = 0; i < boneBatches.size(); i++) {
                boneBatches.get(i).initialize();
            }
            started = true;
        }
    }

    public void initializeIfNeeded() {
        if (!isClone && !started) {
            initialize();
        }
    }

    public void preRender() {
    }

    public void postRender() {
    }

    /* without anim */
    protected void render(DefaultShader shader, boolean lighting) {
        preRender();
        vbo.bind();
        vbo.enableVertexAttrib(shader.attrib_position, 3, 0);
        VertexInfo v_info = getVertexInfo();
        if (v_info.hasTextureCoords()) {
            vbo.enableVertexAttrib(shader.attrib_texcoord, 2, v_info.tex_coord_offset);
        }
        if ((v_info.hasNormals() && shader.flag(DefaultShader.LIGHTING_FLAG)) && lighting) {
            vbo.enableVertexAttrib(shader.attrib_normal, 3, v_info.normal_offset);
        }
        if ((v_info.hasTangents() && shader.flag(DefaultShader.NORMAL_MAP_FLAG)) && lighting) {
            vbo.enableVertexAttrib(shader.attrib_tangent, 3, v_info.tangent_ofs);
            vbo.enableVertexAttrib(shader.attrib_bitangent, 3, v_info.bitangent_ofs);
        }
        if ((v_info.hasColors() && shader.flag(DefaultShader.COLOR_FLAG))) {
            vbo.enableVertexAttrib(shader.attrib_color, 4, v_info.color_offset);
        }
        shader.setPointSize(pointSize);
        shader.setUseSkeleton(false);
        drawParts(shader);
        postRender();
    }

    /* with animation */
    protected void render(DefaultShader shader, boolean lighting, Animator animator) {
        if (!boneBatches.isEmpty()) {
            renderBoneBatches(shader, lighting, animator.getBoneMatrices());
            return;
        }
        preRender();
        vbo.bind();
        vbo.enableVertexAttrib(shader.attrib_position, 3, 0);
        VertexInfo v_info = getVertexInfo();
        if (v_info.hasTextureCoords()) {
            vbo.enableVertexAttrib(shader.attrib_texcoord, 2, v_info.tex_coord_offset);
        }
        if ((v_info.hasNormals() && shader.flag(DefaultShader.LIGHTING_FLAG)) && lighting) {
            vbo.enableVertexAttrib(shader.attrib_normal, 3, v_info.normal_offset);
        }
        if ((v_info.hasTangents() && shader.flag(DefaultShader.NORMAL_MAP_FLAG)) && lighting) {
            vbo.enableVertexAttrib(shader.attrib_tangent, 3, v_info.tangent_ofs);
            vbo.enableVertexAttrib(shader.attrib_bitangent, 3, v_info.bitangent_ofs);
        }
        if ((v_info.hasColors() && shader.flag(DefaultShader.COLOR_FLAG))) {
            vbo.enableVertexAttrib(shader.attrib_color, 4, v_info.color_offset);
        }
        if ((v_info.hasBones() && shader.flag(DefaultShader.ANIMATION_FLAG))) {
            vbo.enableVertexAttrib(shader.attrib_bone_wights, 4, v_info.bone_weights_offset);
            vbo.enableVertexAttrib(shader.attrib_bone_indices, 4, v_info.bone_indices_offset);
        }
        shader.setPointSize(pointSize);
        shader.setBoneMatrices(animator.getBoneMatrices());
        shader.setUseSkeleton(true);
        drawParts(shader);
        postRender();
    }

    private void renderBoneBatches(DefaultShader shader, boolean lighting, Matrix4f[] boneMatrices) {
        preRender();
        shader.setPointSize(pointSize);
        shader.setUseSkeleton(true);
        boolean animation = shader.flag(DefaultShader.ANIMATION_FLAG);
        for (int i = 0; i < boneBatches.size(); i++) {
            BoneBatch batch = boneBatches.get(i);
            batch.initialize();
            batch.bind();
            VertexInfo v_info = batch.getVertexInfo();
            batch.enableVertexAttrib(shader.attrib_position, 3, 0);
            if (v_info.hasTextureCoords()) {
                batch.enableVertexAttrib(shader.attrib_texcoord, 2, v_info.tex_coord_offset);
            }
            if ((v_info.hasNormals() && shader.flag(DefaultShader.LIGHTING_FLAG)) && lighting) {
                batch.enableVertexAttrib(shader.attrib_normal, 3, v_info.normal_offset);
            }
            if ((v_info.hasTangents() && shader.flag(DefaultShader.NORMAL_MAP_FLAG)) && lighting) {
                batch.enableVertexAttrib(shader.attrib_tangent, 3, v_info.tangent_ofs);
                batch.enableVertexAttrib(shader.attrib_bitangent, 3, v_info.bitangent_ofs);
            }
            if (v_info.hasColors() && shader.flag(DefaultShader.COLOR_FLAG)) {
                batch.enableVertexAttrib(shader.attrib_color, 4, v_info.color_offset);
            }
            if (v_info.hasBones() && animation) {
                batch.enableVertexAttrib(shader.attrib_bone_wights, 4, v_info.bone_weights_offset);
                batch.enableVertexAttrib(shader.attrib_bone_indices, 4, v_info.bone_indices_offset);
            }
            shader.setBoneMatrices(batch.getPalette(boneMatrices));
            for (int p = 0; p < batch.getParts().size(); p++) {
                MeshPart part = batch.getParts().get(p);
                if (part.visible) {
                    setupMaterial(shader, part.material);
                    part.draw(primitiveType, useCustomPartType);
                }
            }
        }
        postRender();
    }

    private void drawParts(DefaultShader shader) {
        FX.gl.glLineWidth(lineSize);
        for (int i = 0; i < parts.size(); i++) {
            MeshPart p = parts.get(i);
            if (p.visible) {
                setupMaterial(shader, p.material);
                p.draw(primitiveType, useCustomPartType);
            }
        }
    }

    private void setupMaterial(DefaultShader shader, Material mat) {
        shader.setMaterial(mat.ambient, mat.diffuse, mat.specular, mat.reflection, useGlobalColor ? global_color : mat.color);
        Texture.bind(GL.GL_TEXTURE0, mat.diffuseTexture);
        if (getVertexInfo().hasTangents()) {
            shader.setUseNormalMap(mat.isNormalMap);
            if (mat.isNormalMap) {
                Texture.bind(GL.GL_TEXTURE1, mat.normalTexture);
            }
        }
        if (reflectionMap != -1 && shader.flag(DefaultShader.REFLECTION_MAP)) {
            Texture.bind(shader.flag(DefaultShader.NORMAL_MAP_FLAG) ? GL.GL_TEXTURE2 : GL.GL_TEXTURE1, GL.GL_TEXTURE_CUBE_MAP, reflectionMap);
        }
    }

    public void delete() {
        if (!isClone) {
            vbo.delete();
        }
        vbo = null;
        if (!isClone) {
            for (int i = 0; i < boneBatches.size(); i++) {
                boneBatches.get(i).delete();
            }
            boneBatches.clear();
            parts.delete();
        }
        parts = null;
    }
}
