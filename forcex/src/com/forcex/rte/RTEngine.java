package com.forcex.rte;
import com.forcex.utils.*;
import com.forcex.math.*;
import com.forcex.rte.utils.*;
import com.forcex.gui.*;
import com.forcex.rte.objects.*;

public class RTEngine {
	RTScene scene;
	int width,height;
	RTFrameBuffer framebuffer;
	int tile_size = 128;
	int numThreads = 8;
	int tileX = 0,tileY = 0;
	int numTileX,numTileY,threadRunning;
	OnRenderingListener listener;
	RTCore[] cores;
	int processedTiles = 0;
	
	public RTEngine(RTScene scene,int width,int height){
		this.scene = scene;
		this.width = width;
		this.height = height;
		framebuffer = new RTFrameBuffer(width,height);
	}
	
	public void setTileSize(int size) {
		tile_size = size;
	}
	
	public void setMultiThread(int threads){
		numThreads = threads;
	}
	
	public void setRenderingListener(OnRenderingListener listener){
		this.listener = listener;
	}
	
	public void updateTiles() {
		numTileX = (width / tile_size) + ((width % tile_size) != 0 ? 1 : 0);
		numTileY = (height / tile_size) + ((height % tile_size) != 0 ? 1 : 0);
		processedTiles = 0;
	}
	
	public void execute() {
		updateTiles();
		if(numThreads == 0) {
			for(int x = 0;x < width;x ++) {
				for(int y = 0;y < height;y ++) {
					framebuffer.fill(getPixelData(x,y),x,y);
				}
			}
		}else{
			cores = new RTCore[numThreads];
			for(int i = 0;i < numThreads;i++) {
				cores[i] = new RTCore();
				new Thread(cores[i]).start();
			}
			threadRunning = numThreads;
		}
	}
	
	public RTPixel getPixelData(float x,float y) {
		if(x >= width || y >= height){
			return null;
		}
		float u = (2 * (x /  width) - 1);
		float v = -(2 * (y /  height) - 1);
		Ray ray = scene.camera.getPickRay(u,v);
		RTHit hit = scene.raycast(ray);
		if(hit == null) {
			RTPixel pixel = new RTPixel();
			if(scene.skybox == null) {
				pixel.color.set(scene.sky_color);
			}else{
				pixel.color.set(scene.skybox.getColor(ray.direction));
			}
			pixel.emission = pixel.color.getLuminance() * scene.sky_intensity;
			return pixel;
		} else {
			return processHit(hit,4);
		}
	}
	
	public int getWidth(){
		return width;
	}
	
	public int getHeight(){
		return height;
	}
	
	public int getNumTilesX(){
		return numTileX;
	}
	
	public int getNumTilesY(){
		return numTileY;
	}
	
	public int getAllNumTiles(){
		return numTileX * numTileY;
	}
	
	public int getActualProgress(){
		return processedTiles;
	}
	
	public RTPixel processHit(RTHit hit,int recursionLimit) {
		// processing lighting
		float diffuse = getDiffuseLighting(hit);
		float specular = getSpecularLighting(hit);
		// processing reflections
		Ray reflect_ray = new Ray();
		reflect_ray.direction = hit.ray.direction.sub(hit.normal.mult(2 * hit.ray.direction.dot(hit.normal)));
		reflect_ray.origin = hit.position.add(reflect_ray.direction.mult(0.001f)); 
		RTHit reflectionHit = recursionLimit > 0 ? scene.raycast(reflect_ray) : null;
		RTPixel reflection = null;
		if (reflectionHit != null) {
			reflection = processHit(reflectionHit, recursionLimit - 1);
		} else {
			reflection = new RTPixel();
			if(scene.skybox != null) {
				reflection.color = scene.skybox.getColor(reflect_ray.direction);
				reflection.emission = reflection.color.getLuminance() * scene.sky_intensity;
			}else{
				reflection.color = scene.sky_color;
				reflection.emission = reflection.color.getLuminance() * scene.sky_intensity;
			}
		}
		RTPixel pixel = new RTPixel();
		if(hit.object.arePrimitives) {
			RTMaterial mat = hit.object.materials.get(0);
			specular *= mat.specular * mat.roughness;
			pixel.color = mat.getTextureColor(hit.position.sub(hit.object.position)).lerp(reflection.color,mat.roughness)
				.mult(diffuse)
				.add(specular)
				.add(hit.object.materials.get(0).color.mult(mat.emission))
				.add(reflection.color.mult(reflection.emission * mat.roughness));
			pixel.emission = Math.min(1, mat.emission + reflection.emission * mat.roughness + specular);
		}else{
			RTMesh obj = (RTMesh)hit.object;
			RTMaterial mat = obj.hit_material;
			specular *= mat.specular * mat.roughness;
			pixel.color = mat.getTextureColor(hit.position.sub(hit.object.position)).lerp(reflection.color,mat.roughness)
				.mult(diffuse)
				.add(specular)
				.add(hit.object.materials.get(0).color.mult(mat.emission))
				.add(reflection.color.mult(reflection.emission * mat.roughness));
			pixel.emission = Math.min(1, mat.emission + reflection.emission * mat.roughness + specular);
		}
		return pixel;
	}
	
	public float getDiffuseLighting(RTHit hit) {
		Ray light_ray = new Ray();
		light_ray.origin = scene.light.getPosition();
		light_ray.direction = hit.position.sub(scene.light.getPosition()).normalize();
		RTHit lightObstacle = scene.raycast(light_ray);
		if (lightObstacle != null && lightObstacle.object != hit.object) {
			return scene.global_illumination;
		} else {
			return Maths.max(scene.global_illumination, Maths.min(1,2 * hit.normal.dot(scene.light.getPosition().sub(hit.position).normalize())));
		}
	}
	
	public float getSpecularLighting(RTHit hit){
		// Specular Light
		Vector3f light_dir = hit.position.sub(scene.light.getPosition()).normalize();
		Vector3f cameraDirection = scene.camera.position.sub(hit.position).normalize();
        Vector3f lightReflectionVector = light_dir.sub(hit.normal.mult(2 * light_dir.dot(hit.normal)));
        float specularFactor = Math.max(0, Math.min(1, lightReflectionVector.dot(cameraDirection)));
        return (float) Math.pow(specularFactor, 2);
	}
	
	public RTFrameBuffer getFrameBuffer(){
		return framebuffer;
	}
	
	public static interface OnRenderingListener {
		void updateFrameBuffer(RTEngine engine);
		void finish(RTEngine engine);
	}
	
	public int getNumCores(){
		if(cores == null){
			return 0;
		}
		return cores.length;
	}
	
	public RTCore getCore(int index){
		return cores[index];
	}
	
	boolean first = true;
	boolean usingThread = false;
	
	public boolean hasNext(RTCore core) {
		while(usingThread){
			try {
				Thread.sleep(100);
			}catch (InterruptedException e){}
		}
		usingThread = true;
		if(tileX >= numTileX || tileY >= numTileY) {
			usingThread = false;
			return false;
		}
		if(first){
			core.currentX = 0;
			core.currentY = 0;
			processedTiles++;
			first = false;
			usingThread = false;
			return true;
		}
		for(RTCore c : cores) {
			if(c.currentX == tileX && c.currentY == tileY) {
				tileX++;
				if(tileX == numTileX) {
					tileX = 0;
					tileY++;
				}
				processedTiles++;
				core.currentX = tileX;
				core.currentY = tileY;
				break;
			}
		}
		usingThread = false;
		return true;
	}
	
	public class RTCore implements Runnable {
		public int currentX = 0,currentY = 0;
		
		@Override
		public void run() {
			RTPixel[] buffer = new RTPixel[tile_size * tile_size];
			while(hasNext(this)) {
				int offsetX = currentX * tile_size;
				int offsetY = currentY * tile_size;
				for(int x = 0;x < tile_size;x ++) {
					for(int y = 0;y < tile_size;y ++) {
						
						buffer[y * tile_size + x] = getPixelData(offsetX + x,offsetY + y);
					}
				}
				framebuffer.fill(buffer,offsetX,offsetY,tile_size);
				if(listener != null){
					listener.updateFrameBuffer(RTEngine.this);
				}
			}
			buffer = null;
			threadRunning--;
			if(threadRunning == 0){
				if(listener != null){
					listener.finish(RTEngine.this);
				}
				cores = null;
			}
		}
	}
}
