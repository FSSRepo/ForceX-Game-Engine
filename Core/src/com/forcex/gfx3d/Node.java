package com.forcex.gfx3d;
import java.util.*;
import com.forcex.math.*;

public class Node
{
	// nodes
	ArrayList<Node> children = new ArrayList<Node>();
	Node parent;
	String name = "";
	short id = -1;
	// space operations
	public Vector3f position;
	public Quaternion rotation;
	public Matrix3f rot_matrix;
	Matrix4f ltm; // temporaly matrix local transformation matrix
	// model attached
	ModelObject attach;
	boolean notifyUpdate = true;
	
	public Node(String name){
		this.name = name;
		rotation = new Quaternion();
		position = new Vector3f();
		ltm = new Matrix4f();
	}
	
	public Node(int id){
		this.name = "";
		this.id = (short)id;
		rotation = new Quaternion();
		position = new Vector3f();
	}
	
	public void setupMatrixRot(){
		rotation = null;
		rot_matrix = new Matrix3f();
	}
	
	public void update(){
		if(notifyUpdate){
			if(attach != null){
				attach.setTransform(getLocalModelMatrix());
			}
		}
		for(Node child : children){
			child.update();
		}
	}
	
	public void addChild(Node node){
		node.parent = this;
		children.add(node);
	}
	
	public String getName(){
		return name;
	}
	
	public void setID(short id){
		this.id = id;
	}
	
	public Matrix4f getLocalModelMatrix(){
		if(notifyUpdate){
			if(parent != null){
				parent.getLocalModelMatrix().mult(ltm,getModelMatrix());
			}else{
				ltm.set(getModelMatrix());
			}
			notifyUpdate = false;
		}
		return ltm;
	}
	
	public Matrix4f getModelMatrix(){
		if(rotation == null){
			return new Matrix4f(rot_matrix).setLocation(position);
		}
		return Matrix4f.fromTransform(rotation,position);
	}
	
	public boolean isRoot(){
		return parent == null;
	}
	
	public void notifyUpdate(){
		notifyUpdate = true;
		for(Node child : children){
			child.notifyUpdate();
		}
	}
	
	public void attach(ModelObject obj){
		attach = obj;
	}
	
	public Node getNode(int id){
		if(this.id == id){
			return this;
		}
		for(Node n : children){
			Node r = n.getNode(name);
			if(r != null){
				return r;
			}
		}
		return null;
	}
	
	public Node getNode(String name){
		if(this.name.equals(name)){
			return this;
		}
		for(Node n : children){
			Node r = n.getNode(name);
			if(r != null){
				return r;
			}
		}
		return null;
	}
}
