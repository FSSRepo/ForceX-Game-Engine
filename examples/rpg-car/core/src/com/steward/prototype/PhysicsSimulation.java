package com.steward.prototype;
import com.forcex.*;
import com.forcex.core.*;
import com.forcex.gfx3d.*;
import com.forcex.gfx3d.shapes.*;
import com.forcex.gfx3d.effect.*;
import com.forcex.utils.*;

import java.util.*;
import com.forcex.math.*;
import com.forcex.core.gpu.*;
import com.forcex.io.*;
import com.forcex.postprocessor.*;
import com.forcex.gui.*;
import com.openpl.plContactCallBack;

import static com.openpl.PL10.*;

public class PhysicsSimulation {
	ModelRenderer batch;
	Camera camera;
	ArrayList<PhysicModel> objects;
	PostProcessing post,pause_post;
	FrameBuffer fbo;
	Light light;
	String[] iter;
	int window_width,window_height;
	SkyBox sky;
	boolean show_menu;
	int bb_tex,bbn_tex,fb_tex,fbn_tex,trr_tex,trrn_tex;
	PrototypeUNI proto;
	
	public PhysicsSimulation(PrototypeUNI proto){
		this.proto = proto;
	}

	public void create(){
		sky = new SkyBox(200);
		window_width = FX.gpu.getWidth();
		window_height = FX.gpu.getHeight();
		batch = new ModelRenderer();
		camera = new Camera();
		objects = new ArrayList<PhysicModel>();
		light = new Light();

		camera.setPosition(20,5,25f);
		camera.lookAt(0,0,0);
		light.setPosition(100,100,200);
		batch.getEnvironment().setLight(light);
		batch.useNormalMap(true);
		batch.useGammaCorrection(true);
		startPhysicsEngine();

		fbo = new FrameBuffer(window_width,window_height);
		post = new PostProcessing();
		post.addPass(new BloomPass(
						 new BrightnessPass(window_width/3,window_height/3),
						 new BlurPass(BlurPass.HORIZONTAL,false,window_width/6,window_height/6),
						 new BlurPass(BlurPass.VERTICAL,false,window_width/6,window_height/6)));
		post.getPass(0).renderfbo = true;

		post.addPass(new NormalPass(new Color(Color.WHITE)));
		pause_post = new PostProcessing();
		pause_post.addPass(new BlurPass(BlurPass.HORIZONTAL, true, FX.gpu.getWidth() / 3, FX.gpu.getHeight() / 3));
		pause_post.getPass(0).renderfbo = true;
		pause_post.addPass(new BlurPass(BlurPass.VERTICAL, true, FX.gpu.getWidth() / 3, FX.gpu.getHeight() / 3));
		pause_post.getPass(1).renderfbo = true;
		pause_post.addPass(new NormalPass(new Color(180,140,180)));
	}

	
	float rx = 0;
	float timer = 0;

	public void render(float delta,UIContext ctx){
		fbo.begin();
		FX.gl.glClearColor(0.08f,0.08f,0.08f,1);
		FX.gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
	
		camera.update();
		plStepSimulation(!show_menu ? delta : delta * 0.04f,16,1/60.0f);
		batch.begin(camera);
	
		for(PhysicModel obj : objects){
			obj.updatePhysics();
			batch.render(obj);
		}
	
		batch.end();
		sky.render(camera);
		fbo.end();
		FX.gl.glClearColor(0.08f,0.08f,0.08f,1);
		FX.gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		
		if(!show_menu){
			post.doProcessing(fbo.getTexture());
		}else{
			pause_post.doProcessing(fbo.getTexture());
		}
		ctx.draw();
		
		setRotation(90,rx);rx += 0.05f * delta;
		float seg = timer % 60.0f;
		proto.tvTimer.setText((int)(timer/60f)+":" + (seg >= 0 && seg < 10 ? "0":"") + (int)seg + " " + objects.size());
		timer += delta;
	}

	private void startPhysicsEngine(){
		// Creando el contexto. Es necesario para 
		// el funcionamiento del motor grafico
		if(plCreateContext()){
			System.out.println("Physics Library Context has been created.\nVersion: "+plGetString(PL_VERSION));
			createDynamicWorld();
		}
	}
	
	public void destroy() {
		camera.delete();
		batch.delete();
		post.delete();
		fbo.delete();
		plDestroyContext();
	}

	private void createDynamicWorld(){
		plDynamicWorld3f(PL_GRAVITY,0,-9.81f,0);
		plDynamicWorldi(PL_CONTACT_TEST,PL_TRUE);
		plSetContactCallBack(new ContactListener());
	
		// agregar objetos a la escena
		fb_tex = Texture.load(FX.homeDirectory+"proto/football.png");
		fbn_tex = Texture.load(FX.homeDirectory+"proto/football_n.png");
		bb_tex = Texture.load(FX.homeDirectory+"proto/basket.png");
		bbn_tex = Texture.load(FX.homeDirectory+"proto/basket_n.png");
		trr_tex = Texture.load(FX.homeDirectory+"proto/terrain.png");
		trrn_tex = Texture.load(FX.homeDirectory+"proto/terrain_n.png");
		
		for(int i = 0;i < 6;i++){
			addFootball(i);
		}
		
		for(int i = 0;i < 6;i++){
			addBasketBall(i);
		}
		
		Mesh terrain = addBox(new Vector3f(30f,0.08f,30f),new Vector3f(),new Quaternion(),0f,90);
		terrain.getPart(0).material.diffuseTexture = trr_tex;
		terrain.getPart(0).material.normalTexture = trrn_tex;
		objects.get(objects.size()-1).setName("terreno");
		
		// cargar posiciones
		iter = FileUtils.readStringText(FX.homeDirectory + "proto/placement.dat").split("\n");
		reset();
	}
	
	public void addMore(){
		for(int i = 2;i <= 4;i++){
			addFootball(i);
		}
		for(int i = 2;i <= 4;i++){
			addCaja(i);
		}
		reset();
	}
	
	
	private void addFootball(int i){
		Mesh football = addSphere(1f,new Vector3f(),2f,20+i);
		football.getPart(0).material.diffuseTexture = fb_tex;
		football.getPart(0).material.normalTexture = fbn_tex;
		football.getPart(0).material.specular = 0.3f;
		football.getPart(0).material.diffuse = 0.2f;
		objects.get(objects.size()-1).body.setCallbackFilter(0x8);
		objects.get(objects.size()-1).body.setCallbackFlag(0x8);
		objects.get(objects.size()-1).setName("football");
	}
	
	private void addBasketBall(int i){
		Mesh basket = addSphere(1f,new Vector3f(),1.8f,30+i);
		basket.getPart(0).material.diffuseTexture = bb_tex;
		basket.getPart(0).material.normalTexture = bbn_tex;
		basket.getPart(0).material.specular = 0.1f;
		basket.getPart(0).material.diffuse = 0.2f;
		objects.get(objects.size()-1).body.setCallbackFilter(0x4);
		objects.get(objects.size()-1).body.setCallbackFlag(0x4);
		objects.get(objects.size()-1).setName("basket");
	}
	
	private void addCaja(int i){
		Mesh box = addBox(new Vector3f(1f,1f,1f),new Vector3f(),new Quaternion(),1.8f,30+i);
		box.getPart(0).material.diffuseTexture = trr_tex;
		box.getPart(0).material.normalTexture = trrn_tex;
		box.getPart(0).material.specular = 0.1f;
		box.getPart(0).material.diffuse = 0.2f;
		objects.get(objects.size()-1).body.setCallbackFilter(0x2);
		objects.get(objects.size()-1).body.setCallbackFlag(0x2);
		objects.get(objects.size()-1).setName("caja");
	}

	public void reset(){
		for(String it : iter){
			if(it.length() == 0) {
				continue;
			}
			String[] attribs = it.split(",");
			int id = Integer.parseInt(attribs[0]);
			for(PhysicModel m : objects){
				if(m.getID() == id){
					m.body.setLocation(
						Float.parseFloat(attribs[1]),
						Float.parseFloat(attribs[2]),
						Float.parseFloat(attribs[3]));
				}else if(m.getID() == 90){
					m.body.setTransform(new Vector3f(),new Quaternion());
				}
			}
		}
		timer = 0;
	}

	public void setRotation(int id,float x){
		Quaternion quat = Quaternion.fromEulerAngles(0,0,x);
		for(PhysicModel m : objects){
			if(m.getID() == id){
				Vector3f loc = m.getPosition();
				m.body.setTransform(loc,quat);
			}
			m.body.activate();
		}
	}

	private Mesh addBox(Vector3f extent,Vector3f position,Quaternion rotation,float mass,int id){
		Box box = new Box(extent.x,extent.y,extent.z);
		Matrix4f transform = new Matrix4f();
		transform.setTransform(rotation,position);
		RigidBody body = new RigidBody(mass,CollisionShape.createBox(extent),transform);
		PhysicModel m = new PhysicModel(box,body);
		m.setID(id);
		body.setUserPointer(body.id);
		body.addCollisionFlag(PL_CF_CUSTOM_MATERIAL_CALLBACK);
		objects.add(m);
		plDynamicWorldi(PL_ADD_RIGID_BODY,body.id);
		return box;
	}

	private Mesh addSphere(float radius,Vector3f position,float mass,int id){
		Sphere sph = new Sphere(radius,25,25);
		Matrix4f transform = new Matrix4f();
		transform.setLocation(position);
		RigidBody body = new RigidBody(mass,CollisionShape.createSphere(radius),transform);
		PhysicModel m = new PhysicModel(sph,body);
		m.setID(id);
		body.setUserPointer(body.id);
		body.addCollisionFlag(PL_CF_CUSTOM_MATERIAL_CALLBACK);
		objects.add(m);
		plDynamicWorldi(PL_ADD_RIGID_BODY,body.id);
		return sph;
	}
	
	

	private class PhysicModel extends ModelObject {
		RigidBody body;

		public PhysicModel(Mesh mesh,RigidBody body){
			super(mesh);
			this.body = body;
		}

		public void updatePhysics()
		{
			if(!body.isStatic()){
				plGetRigidBodyfv(
					body.id,PL_MOTION_STATE_TRANSFORM,
					getTransform().data,16);
			}else{
				setTransform(body.transform);
			}
		}
	}

	public class RigidBody
	{
		public int id;
		boolean userPointer;
		Matrix4f transform;
		float mass;

		public RigidBody(float mass,int shape,Matrix4f transform){
			id = plGenBody();
			this.mass = mass;
			this.transform = transform;
			bind();
			plRigidBodyi(PL_COLLISION_SHAPE,shape);
			if(mass > 0){
				plRigidBodyf(PL_MASS,mass);
			}
			plRigidBodyfv(PL_MOTION_STATE_TRANSFORM,transform.data,16);
			plCreate(PL_RIGID_BODY);
			unbind();
		}

		public void setUserPointer(int ptr){
			bind();
			plRigidBodyi(PL_USER_POINTER,ptr);
			unbind();
			userPointer = true;
		}
		
		public void setLocation(float x,float y,float z){
			bind();
			plRigidBody3f(PL_POSITION,x,y,z);
			unbind();
		}
		
		public void setTransform(Vector3f loc,Quaternion rot){
			transform.setTransform(rot,loc);
			bind();
			plRigidBodyfv(PL_TRANSFORM,transform.data,16);
			unbind();
		}

		public void setCallbackFlag(int flag){
			if(!userPointer){
				return;
			}
			bind();
			plRigidBodyi(PL_COLLISION_CALLBACK_FLAG,flag);
			unbind();
		}

		public void setCallbackFilter(int filter){
			if(!userPointer){
				return;
			}
			bind();
			plRigidBodyi(PL_COLLISION_CALLBACK_FILTER,filter);
			unbind();
		}

		public void activate(){
			bind();
			plRigidBodyi(PL_ACTIVATION_STATE,PL_ACTIVATE);
			unbind();
		}

		public void setMass(float mass){
			plRigidBodyf(PL_MASS,mass);
		}

		public boolean isStatic(){
			return mass == 0.0f;
		}

		public void applyImpulse(Vector3f impulse){
			plRigidBody3f(PL_APPLY_CENTRAL_IMPULSE,impulse.x,impulse.y,impulse.z);
		}

		public void addCollisionFlag(int flag){
			bind();
			plRigidBodyi(PL_ADD_COLLISION_FLAG,flag);
			unbind();
		}

		public void bind(){
			plBindBody(id);
		}

		public void unbind(){
			plBindBody(0);
		}
		
		public void delete(){
			plDeleteBody(id);
		}
	}

	public static class CollisionShape {
		public static int createBox(Vector3f extents){
			int shape = plGenShape();
			plBindShape(shape);
			plShape3f(PL_EXTENT,extents.x,extents.y,extents.z);
			plCreate(PL_BOX_SHAPE);
			plBindShape(0);
			return shape;
		}

		public static int createSphere(float radius){
			int shape = plGenShape();
			plBindShape(shape);
			plShapef(PL_RADIUS,radius);
			plCreate(PL_SPHERE_SHAPE);
			plBindShape(0);
			return shape; 
		}

		public static int createCapCylCon(float height,float radius,int type){
			int shape = plGenShape();
			plBindShape(shape);
			plShapef(PL_HEIGHT,height);
			plShapef(PL_RADIUS,radius);
			plCreate(type);
			plBindShape(0);
			return shape; 
		}

		public static int createStaticPlane(Vector3f normal,float constant){
			int shape = plGenShape();
			plBindShape(shape);
			plShapef(PL_PLANE_CONSTANT,constant);
			plShape3f(PL_PLANE_NORMAL,normal.x,normal.y,normal.z);
			plCreate(PL_STATIC_PLANE_SHAPE);
			plBindShape(0);
			return shape; 
		}
	}
	
	public PhysicModel getObject(int id){
		for(PhysicModel o : objects){
			if(o.body.id == id){
				return o;
			}
		}
		return null;
	}
	public class ContactListener extends plContactCallBack {
		@Override
		public void onContact(int user_ptr1, boolean match1, int user_ptr2, boolean match2) {
			PhysicModel from = getObject(user_ptr1);
			PhysicModel to = getObject(user_ptr2);
			if(match1){
				proto.tvCollisionInfo.setText("By match 1 ["+from.getName()+" -> "+to.getName()+"]");
				proto.tvCollisionInfo.setTextColor(230,20,20);
			}
			if(match2){
				proto.tvCollisionInfo.setText("By match 2 ["+to.getName()+" -> "+from.getName()+"]");
				proto.tvCollisionInfo.setTextColor(230,20,20);
			}
		}
		
	}
}
