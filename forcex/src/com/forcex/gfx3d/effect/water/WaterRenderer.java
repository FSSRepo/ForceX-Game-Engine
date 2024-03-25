package com.forcex.gfx3d.effect.water;
import com.forcex.gfx3d.*;
import com.forcex.*;
import com.forcex.core.*;
import com.forcex.utils.*;
import com.forcex.gfx3d.effect.*;
import com.forcex.math.*;
import java.util.*;

public class WaterRenderer
{
	WaterShader shader;
	float deltaMove = 0,moveSpeed = 0.1f;
	int vbo_rect, vbo_waves, ibo_waves, diffuseTex, dudvTex, normTex;
	GL gl = FX.gl;
	boolean useLighting, useReflex;
	ArrayList<WaterTile> tiles = new ArrayList<>();
	public ReflectionBuffer fbo;
	Camera reflectcam;
	int idxcount;
	public float waterHeight = 2;
	Plane plane;
	Matrix4f model_matrix = new Matrix4f();
	Matrix4f temp_matrix = new Matrix4f();
	Vector3f st = new Vector3f();
	HeightMap hmap;
	boolean first = true;
	
	
	public void addTile(WaterTile tile){
		tiles.add(tile);
	}
	
	public WaterRenderer(String heightmap, boolean useLight, boolean useReflections) {
		useLighting = useLight;
		useReflex = useReflections;
		if(heightmap.length() > 0) {
			hmap = new HeightMap(heightmap,2,1,false,false);
			idxcount = hmap.indices.length;
		}
	}
	
	public void setTextureDiffuse(int id){
		diffuseTex = id;
	}
	public void setTextureDudvMap(int id){
		dudvTex = id;
	}
	public void setTextureNormalMap(int id){
		normTex = id;
	}
	
	public Camera getCamera(Camera camera){
		// setup plane
		waterHeight = tiles.get(0).position.y;
		plane.distance = waterHeight;
		// setup camera
		reflectcam.position.set(plane.reflect(camera.position));
		
		st.set(camera.position).addLocal(camera.direction);
		reflectcam.direction.set(plane.reflect(st));
		reflectcam.direction.subLocal(reflectcam.position);
		
		st.set(camera.position).subLocal(camera.up);
		reflectcam.up.set(plane.reflect(st));
		reflectcam.up.subLocal(reflectcam.position);
		// finish
		reflectcam.update();
		return reflectcam;
	}
	
	public void beginReflection(){
		if(useReflex && fbo == null){
			fbo = new ReflectionBuffer();
			reflectcam = new Camera(1);
			plane = new Plane(new Vector3f(0,1,0),0);
		}else return;
		fbo.begin();
	}
	
	public void endReflection(){
		if(fbo != null)fbo.end();
	}
	
	public void render(Camera cam,Light light) {
		if(first){
			shader = new WaterShader(useLighting,useReflex);
			float[] quad_vertices = {
				-1,0,1,
				-1,0,-1,
				1,0,1,
				1,0,-1
			};
			vbo_rect = gl.glGenBuffer();
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER,vbo_rect);
			gl.glBufferData(GL.GL_ARRAY_BUFFER,quad_vertices.length * 4,BufferUtils.createFloatBuffer(quad_vertices),GL.GL_STATIC_DRAW);
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER,0);
			if(hmap != null) {
				vbo_waves = gl.glGenBuffer();
				gl.glBindBuffer(GL.GL_ARRAY_BUFFER,vbo_waves);
				gl.glBufferData(GL.GL_ARRAY_BUFFER,hmap.vertices.length * 4,BufferUtils.createFloatBuffer(hmap.vertices),GL.GL_STATIC_DRAW);
				gl.glBindBuffer(GL.GL_ARRAY_BUFFER,0);
				ibo_waves = gl.glGenBuffer();
				gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,ibo_waves);
				gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER,hmap.indices.length * 2,BufferUtils.createShortBuffer(hmap.indices),GL.GL_STATIC_DRAW);
				gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,0);
				hmap = null;
			}
			first = false;
		}
		shader.start();
		shader.setMoveFactor((deltaMove * moveSpeed) % 2);
		shader.setTime(deltaMove);
		if(useLighting){
			shader.setCamera(cam);
			shader.setLight(light);
		}
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA,GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glActiveTexture(GL.GL_TEXTURE0);
		gl.glBindTexture(GL.GL_TEXTURE_2D,diffuseTex);
		gl.glActiveTexture(GL.GL_TEXTURE1);
		gl.glBindTexture(GL.GL_TEXTURE_2D,dudvTex);
		if(useLighting){
			gl.glActiveTexture(GL.GL_TEXTURE2);
			gl.glBindTexture(GL.GL_TEXTURE_2D,normTex);
			if(useReflex){
				gl.glActiveTexture(GL.GL_TEXTURE3);
				gl.glBindTexture(GL.GL_TEXTURE_2D,fbo.getReflectionTexture());
			}
			for(WaterTile tile : tiles){
				if(tile.isWaterPool){
					gl.glBindBuffer(GL.GL_ARRAY_BUFFER,vbo_rect);
				}else{
					gl.glBindBuffer(GL.GL_ARRAY_BUFFER,vbo_waves);
				}
				gl.glVertexAttribPointer(shader.attrib_position,3,GL.GL_FLOAT,false,0,0);
				gl.glEnableVertexAttribArray(shader.attrib_position);
				shader.setIsPicine(tile.isWaterPool);
				renderWaterTileLight(tile);
			}
		}else{
			for(WaterTile tile : tiles){
				if(tile.isWaterPool){
					gl.glBindBuffer(GL.GL_ARRAY_BUFFER,vbo_rect);
				}else{
					gl.glBindBuffer(GL.GL_ARRAY_BUFFER,vbo_waves);
				}
				gl.glVertexAttribPointer(shader.attrib_position,3,GL.GL_FLOAT,false,0,0);
				gl.glEnableVertexAttribArray(shader.attrib_position);
				shader.setIsPicine(tile.isWaterPool);
				renderWaterTile(tile,cam);
			}
		}
		gl.glDisable(GL.GL_BLEND);
		shader.stop();
		gl.glBindTexture(GL.GL_TEXTURE_2D,0);
		gl.glDisableVertexAttribArray(shader.attrib_position);
		deltaMove += FX.gpu.getDeltaTime();
	}
	
	private void renderWaterTileLight(WaterTile tile) {
		model_matrix.setScale(tile.size,1,tile.size).setLocation(tile.position);
		shader.setModelMatrix(model_matrix);
		if(tile.isWaterPool) {
			gl.glDrawArrays(GL.GL_TRIANGLE_STRIP,0,4);
		} else {
			gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ibo_waves);
			gl.glDrawElements(GL.GL_TRIANGLES,idxcount);
		}
	}
	
	private void renderWaterTile(WaterTile tile, Camera cam) {
			model_matrix.setScale(tile.size,1,tile.size).setLocation(tile.position);
			shader.setMVPMatrix(cam.getProjViewMatrix().mult(temp_matrix, model_matrix));
			if(tile.isWaterPool) {
				gl.glDrawArrays(GL.GL_TRIANGLE_STRIP,0,4);
			}else{
				gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ibo_waves);
				gl.glDrawElements(GL.GL_TRIANGLES, idxcount);
			}
	}
} 
