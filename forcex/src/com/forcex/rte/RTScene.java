package com.forcex.rte;
import com.forcex.gfx3d.*;
import com.forcex.gfx3d.effect.*;
import com.forcex.math.*;
import com.forcex.rte.utils.*;
import java.util.*;
import com.forcex.rte.objects.*;

public class RTScene {
	protected RTSky skybox;
	public float sky_intensity = 0.4f;
	protected RTColor sky_color;
	protected Camera camera;
	protected Light light;
	protected ArrayList<RTObject> objects = new ArrayList<RTObject>();
	public float global_illumination = 1f;
	
	public RTScene(Light light,Camera camera){
		this.light = light;
		this.camera = camera;
		sky_color = new RTColor(0,0,0);
	}
	
	public void add(RTObject object){
		objects.add(object);
	}
	
	public void loadSky(String hdri) {
		if(skybox == null){
			skybox = new RTSky(hdri);
		}
	}
	public void setColorSky(float red,float green,float blue) {
		sky_color.r = red;
		sky_color.g = green;
		sky_color.b = blue;
	}
	
	public RTHit raycast(Ray ray){
		RTHit closestHit = null;
        for (RTObject object : objects) {
            Vector3f hitPos = object.intersect(ray);
            if (hitPos != null && (closestHit == null || closestHit.position.distance(ray.origin) > hitPos.distance(ray.origin))) {
                closestHit = new RTHit();
				closestHit.object = object;
				closestHit.position = hitPos;
				closestHit.normal = object.arePrimitives? object.getNormal(hitPos):((RTMesh)object).hit_normal;
				closestHit.ray = ray;
            }
        }
        return closestHit;
	}
}
