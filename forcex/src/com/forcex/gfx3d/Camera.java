package com.forcex.gfx3d;

import com.forcex.math.*;
import com.forcex.*;
import com.forcex.utils.*;
import com.forcex.app.*;
import com.forcex.gui.*;

public class Camera {
    Matrix4f viewMatrix = new Matrix4f();
	Matrix4f ProjViewMatrix = new Matrix4f();
    Matrix4f projectMatrix = new Matrix4f();
    public Vector3f position = 	new Vector3f(0, 0, 4);
    public Vector3f direction = new Vector3f(0, 0,-1);
    public Vector3f up = 		new Vector3f(0, 1, 0);
	float fov = 50,aspectRatio = 0.0f,near = 0.1f,far = 1000f;
	boolean update = true;
	Vector4f plane_ext;
	boolean smooth_delta = false;
	float deltaX = 0,deltaY = 0,touch_time = 0,timelapse = 0;
	long start_touch = 0;
	boolean delta_first = true;
	float resistance = 1f;
	boolean use_up_z;
	
	public static enum ProjectionType{
		ORTHOGRAPHIC,
		PERSPECTIVE
	}

	ProjectionType type;

	public Camera(float aspectRatio){
		this.aspectRatio = aspectRatio;
		type = ProjectionType.PERSPECTIVE;
	}

	public Camera(){
		this((float)FX.gpu.getWidth() / FX.gpu.getHeight());
	}

	public void setProjectionType(ProjectionType type){
		this.type = type;
		if(type == ProjectionType.ORTHOGRAPHIC){
			plane_ext = new Vector4f(-1,1,-1,1);
		}
		update = true;
	}

    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

	public Matrix4f getProjViewMatrix() {
        return ProjViewMatrix;
    }

    public Matrix4f getProjectionMatrix() {
        return projectMatrix;
    }

    public Vector3f getPosition() {
        return position;
    }
	
	public void setSmoothMovement(boolean z){
		smooth_delta = z;
	}

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
    }

	public void setOrthoExtent(float left, float right, float bottom,float top) {
        plane_ext.set(left,right,bottom,top);
		update = true;
    }

	public void setNearFar(float near, float far) {
        this.near = near;
		this.far = far;
		update = true;
    }

	public void setAspectRatio(float aspectRatio){
		this.aspectRatio = aspectRatio;
		update = true;
	}

	public void setFieldOfView(float fov){
		this.fov = fov;
		update = true;
	}

    public void move(float x, float y, float z) {
		Vector3f right = up.cross(direction).normalize();
		position.addLocal(right.mult(x));
		position.addLocal(direction.mult(y));
		position.addLocal(up.mult(z));
    }

    public void rotate(float x, float y) {
		direction.rotY(y);
		up.rotY(y);
		Vector3f side = up.cross(direction).normalize();
		direction.multLocal(Matrix4f.setRotation(new Matrix4f(),x,side));
		up.set(direction).crossLocal(side);
		direction.normalize();
		up.normalize();
    }
	
    public void zoom(float dist) {
		position.subLocal(direction.mult(dist));
    }

    public void update() {
		if(update){
			if(type == ProjectionType.PERSPECTIVE){
				projectMatrix.setPerspective(fov,aspectRatio, near, far);
			}else{
				projectMatrix.setOrthogonal(plane_ext.x,plane_ext.y,plane_ext.z,plane_ext.w,near,far);
			}
			update = false;
		}
		if(smooth_delta && deltaX != 0 && deltaY != 0 && touch_time > 0) {
			if(delta_first){
				touch_time = touch_time > 1.4f ? 1.4f : touch_time;
				timelapse = Maths.abs(1.4f - touch_time);
				deltaX = Maths.clamp(deltaX,-30,30);
				deltaY = Maths.clamp(deltaY,-30,30);
				deltaX = deltaX * touch_time * resistance;
				deltaY = deltaY * touch_time * resistance;
				delta_first = false;
			}
			float nx = deltaX * timelapse;
			float ny = deltaY * timelapse;
			orbit(ny,nx);
			if(timelapse > 0){
				timelapse -= FX.gpu.getDeltaTime();
			}else{
				deltaX = 0;
				deltaY = 0;
			}
			
		}
		projectMatrix.mult(ProjViewMatrix,viewMatrix.setlookAt(position, direction, up));
    }

	public void lookAt(float x,float y,float z){
		lookAt(direction.set(x,y,z));
	}

	public void lookAt(Vector3f point){
		direction.set(point).subLocal(position).normalize();
		Vector3f right = Vector3f.getUpFromDirection(direction,use_up_z).cross(direction).normalize();
		up.set(direction).crossLocal(right).normalize();
	}
	
	public Vector3f right(){
		return up.cross(direction).normalize();
	}

	public void set(Vector3f position,Quaternion rotation){
		this.position.set(position);
		direction.set(rotation.getDirection());
		up.set(rotation.getUp());
	}

	public void setOrientation(Quaternion rot){
		direction.set(rot.getDirection());
		up.set(rot.getUp());
	}

    public Ray getPickRay(float x,float y) {
        Ray ray = new Ray();
		Matrix4f pvi = ProjViewMatrix.invert();
		ray.origin = unproject(pvi,new Vector3f(x, y, 0));
        Vector3f raydir = unproject(pvi,new Vector3f(x, y, 1));
        ray.direction = raydir.subLocal(ray.origin).normalize();
        return ray;
    }

    private Vector3f unproject(Matrix4f pvi,Vector3f v) {
        v.z = 2 * v.z - 1;
        return v.project(pvi);
	}
	
	public void setResistance(float r){
		resistance = r;
	}
	
	public void setInputType(byte type) {
		if(!smooth_delta){
			return;
		}
		if(type == EventType.TOUCH_PRESSED){
			start_touch = System.currentTimeMillis();
			deltaX = 0;
			deltaY = 0;
			touch_time = 0;
		}else if(type == EventType.TOUCH_DROPPED){
			touch_time = (System.currentTimeMillis() - start_touch) / 1000.0f;
			delta_first = true;
		}
	}
	
	public void updateDelta(float x,float y){
		if(!smooth_delta){
			return;
		}
		deltaX += x;
		deltaY += y;
	}
	
	public void setUseZUp(boolean z){
		this.use_up_z = z;
	}
	
	public void orbit(Vector3f point,float x, float y){
		Vector3f around = point.sub(position);
		position.addLocal(around);
		if(!use_up_z){
			around.rotY(y);
		}else{
			around.rotZ(y);
		}
		Vector3f side = up.cross(direction).normalize();
		around.multLocal(Matrix4f.setRotation(new Matrix4f(),x,side));
		position.subLocal(around);
		lookAt(point);
	}

	public void orbit(float x, float y){
		if(!use_up_z){
			position.rotY(y);
		}else{
			position.rotZ(y);
		}
		Vector3f side = up.cross(direction).normalize();
		position.multLocal(Matrix4f.setRotation(new Matrix4f(),x,side));
		lookAt(0,0,0);
	}

    public void delete() {
        direction = null;
        up =  null;
        position = null;
        projectMatrix =  null;
        viewMatrix =  null;
        ProjViewMatrix =  null;
    }

	public void setDirection(int dir) {
        switch (dir) {
            case DIRECTION_RIGHT:
				direction.set(1,0,0);
                break;
            case DIRECTION_LEFT:
                direction.set(-1,0,0);
				 break;
            case DIRECTION_FRONT:
				if(!use_up_z){
              	  direction.set(0,0,1);
				}else{
				  direction.set(0,1,0);
				}
				 break;
            case DIRECTION_BACK:
                if(!use_up_z){
					direction.set(0,0,-1);
				}else{
					direction.set(0,-1,0);
				}
				break;
        }
		if(!use_up_z){
			if(dir == DIRECTION_TOP){
				direction.set(0, 1,0);
				up.set(0,0,-1);
			}else if(dir == DIRECTION_BUTTOM){
				direction.set(0,-1,0);
				up.set(0,0,1);
			}else{
				up.set(0,1,0);
			}
		}else{
			if(dir == DIRECTION_TOP){
				direction.set(0,0,1);
				up.set(0,-1,0);
			}else if(dir == DIRECTION_BUTTOM){
				direction.set(0,0,-1);
				up.set(0,1,0);
			}else{
				up.set(0,0,1);
			}
		}
    }
	
	public static final byte DIRECTION_RIGHT =  0;
	public static final byte DIRECTION_LEFT =   1;
	public static final byte DIRECTION_TOP = 	2;
	public static final byte DIRECTION_BUTTOM = 3;
	public static final byte DIRECTION_FRONT =  4;
	public static final byte DIRECTION_BACK =   5;
}
