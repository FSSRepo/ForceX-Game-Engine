package com.forcex.gfx3d;

import com.forcex.*;
import com.forcex.anim.*;
import com.forcex.core.*;
import com.forcex.core.gpu.*;
import com.forcex.gfx3d.shader.*;
import java.util.*;
import com.forcex.utils.*;

public class Mesh {
    VertexBuffer vbo;
    MeshPart parts = new MeshPart();
    int primitiveType;
	public float lineSize = 2.0f,pointSize = 4.0f;
	public int reflectionMap = -1;
    boolean isClone;
	boolean started;
	public boolean useGlobalColor = false;
	public Color global_color;
	public boolean useCustomPartType = false;
	
    public Mesh(boolean isStatic) {
       	vbo = new VertexBuffer(new VertexData(),new VertexInfo(), isStatic);
        primitiveType = GL.GL_TRIANGLES;
		global_color = new Color(Color.WHITE);
    }
	
	public Mesh(boolean isStatic,VertexData vertexs) {
        vbo = new VertexBuffer(vertexs,new VertexInfo(), isStatic);
        primitiveType = GL.GL_TRIANGLES;
		global_color = new Color(Color.WHITE);
    }
	
	public Mesh clone(){
		if(!isClone && started){
			Mesh mesh = new Mesh(false);
			mesh.vbo = vbo.clone();
			mesh.primitiveType = primitiveType;
			mesh.isClone = true;
			for(MeshPart p : getParts()){
				mesh.addPart(p.clone());
			}
			mesh.global_color = global_color;
			return mesh;
		}else{
			FX.device.showInfo("Error clonning the mesh: it maybe this mesh is a clon or this isn't initialized",true);
			FX.device.stopRender();
		}
		return null;
	}
	
	public void setPrimitiveType(int primitiveType){
		this.primitiveType = primitiveType;
	}

	public int getPrimitiveType(){
		return primitiveType;
	}

    public void setVertices(float[] data) {
        getVertexInfo().vertexCount = data.length / 3;
        getVertexData().vertices = data;
        vbo.reset();
    }

    public void setTextureCoords(float[] data) {
		getVertexInfo().addFlag(VertexInfo.HAS_TEXTURECOORDS);
        getVertexData().texcoords = data;
        vbo.reset();
    }

    public void setNormals(float[] data) {
        getVertexInfo().addFlag(VertexInfo.HAS_NORMALS);
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
        getVertexData().bitangents = data;
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
        getVertexData().boneindices = data;
        vbo.reset();
    }
	
	public void setBoneWeights(float[] data) {
        getVertexInfo().addFlag(VertexInfo.HAS_BONES);
        getVertexData().boneweights = data;
        vbo.reset();
    }
	
    public void addPart(MeshPart part) {
        parts.add(part);
    }
	
	public VertexBuffer getVertexBuffer(){
		return vbo;
	}
	
    public MeshPart getPart(int idx) {
		if(idx >= parts.size()){
			return parts.get(0);
		}
        return parts.get(idx);
    }
	
	public ArrayList<MeshPart> getParts(){
        return parts.list;
    }
	
    public VertexInfo getVertexInfo() {
        return vbo.info;
    }
	
	public VertexData getVertexData(){
		return vbo.vdata;
	}
	
	protected void update(){
		vbo.update();
		parts.update(getVertexInfo().hasTangents());
	}
	
	public void initialize(){
		if(!started){
			vbo.update();
			parts.update(getVertexInfo().hasTangents());
			started = true;
		}
	}
	
	public void preRender(){}
	public void postRender(){}
	
	/* without anim */
    protected void render(DefaultShader shader,boolean lighting) {
		preRender();
		vbo.bind();
		vbo.EnableVertexAttrib(shader.attrib_position,3,0);
		VertexInfo v = getVertexInfo();
		if(v.hasTextureCoords()){
			vbo.EnableVertexAttrib(shader.attrib_texcoord,2,v.texcoord_ofs);
		}
		if((v.hasNormals() && shader.flag(DefaultShader.LIGHTING_FLAG)) && lighting){
			vbo.EnableVertexAttrib(shader.attrib_normal,3,v.normal_ofs);
		}
		if((v.hasTangents() && shader.flag(DefaultShader.NORMAL_MAP_FLAG)) && lighting){
			vbo.EnableVertexAttrib(shader.attrib_tangent,3,v.tangent_ofs);
			vbo.EnableVertexAttrib(shader.attrib_bitangent,3,v.bitangent_ofs);
		}
		if((v.hasColors() && shader.flag(DefaultShader.COLOR_FLAG))){
			vbo.EnableVertexAttrib(shader.attrib_color,4,v.color_ofs);
		}
		shader.setPointSize(pointSize);
		shader.setUseSkeleton(false);
		drawParts(shader);
		postRender();
    }
	
	/* with animation */
	protected void render(DefaultShader shader,boolean lighting,Animator animator) {
		preRender();
		vbo.bind();
		vbo.EnableVertexAttrib(shader.attrib_position,3,0);
		VertexInfo v = getVertexInfo();
		if(v.hasTextureCoords()){
			vbo.EnableVertexAttrib(shader.attrib_texcoord,2,v.texcoord_ofs);
		}
		if((v.hasNormals() && shader.flag(DefaultShader.LIGHTING_FLAG)) && lighting){
			vbo.EnableVertexAttrib(shader.attrib_normal,3,v.normal_ofs);
		}
		if((v.hasTangents() && shader.flag(DefaultShader.NORMAL_MAP_FLAG)) && lighting){
			vbo.EnableVertexAttrib(shader.attrib_tangent,3,v.tangent_ofs);
			vbo.EnableVertexAttrib(shader.attrib_bitangent,3,v.bitangent_ofs);
		}
		if((v.hasBones() && shader.flag(DefaultShader.ANIMATION_FLAG))){
			vbo.EnableVertexAttrib(shader.attrib_bonew,4,v.bone_w_ofs);
			vbo.EnableVertexAttrib(shader.attrib_bonei,4,v.bone_i_ofs);
		}
		shader.setPointSize(pointSize);
		shader.setBoneMatrices(animator.getBoneMatrices());
		shader.setUseSkeleton(true);
		drawParts(shader);
		postRender();
    }
	
	private void drawParts(DefaultShader shader){
		FX.gl.glLineWidth(lineSize);
		for(int i = 0;i < parts.size();i++){
			MeshPart p = parts.get(i);
			if(p.visible){
				setupMaterial(shader,p.material);
				p.draw(primitiveType,useCustomPartType);
			}
		}
	}
	
	private void setupMaterial(DefaultShader shader, Material mat){
		shader.setMaterial(mat.ambient,mat.diffuse,mat.specular,mat.reflection,useGlobalColor ? global_color : mat.color);
		Texture.bind(GL.GL_TEXTURE0, mat.diffuseTexture);
        if (getVertexInfo().hasTangents()) {
			shader.setUseNormalMap(mat.isNormalMap);
			if(mat.isNormalMap){
         	   Texture.bind(GL.GL_TEXTURE1, mat.normalTexture);
			}
        }
        if (reflectionMap != -1 && shader.flag(DefaultShader.REFLECTION_MAP)) {
            Texture.bind(shader.flag(DefaultShader.NORMAL_MAP_FLAG) ? GL.GL_TEXTURE2 : GL.GL_TEXTURE1, GL.GL_TEXTURE_CUBE_MAP, reflectionMap);
       }
	}
	
    public void delete() {
		if(!isClone){
        	vbo.delete();
		}
        vbo = null;
		if(!isClone){
      	  parts.delete();
		}
        parts = null;
    }
}
