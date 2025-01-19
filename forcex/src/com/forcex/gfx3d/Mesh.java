package com.forcex.gfx3d;

import com.forcex.FX;
import com.forcex.anim.Animator;
import com.forcex.core.GL;
import com.forcex.core.gpu.Texture;
import com.forcex.core.gpu.VertexBuffer;
import com.forcex.core.gpu.VertexData;
import com.forcex.core.gpu.VertexInfo;
import com.forcex.gfx3d.shader.DefaultShader;
import com.forcex.utils.Color;

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

    protected void update() {
        vbo.update();
        parts.update(getVertexInfo().hasTangents());
    }

    public void initialize() {
        if (!started) {
            vbo.update();
            parts.update(getVertexInfo().hasTangents());
            started = true;
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
            parts.delete();
        }
        parts = null;
    }
}
