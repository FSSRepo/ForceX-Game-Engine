package com.forcex.gfx3d;

import com.forcex.math.Matrix3f;
import com.forcex.math.Matrix4f;
import com.forcex.math.Quaternion;
import com.forcex.math.Vector3f;

import java.util.ArrayList;

public class Node {
    // nodes
    private final ArrayList<Node> children = new ArrayList<Node>();
    private Node parent;
    private String name = "";
	private short id = -1;

    // space operations
    public Vector3f position;
    public Quaternion rotation;
    public Matrix3f rotation_matrix;
	private Matrix4f ltm; // temporally matrix local transformation matrix

    // model attached
	private ModelObject attach;
	private boolean notifyUpdate = true;

    public Node(String name) {
        this.name = name;
        rotation = new Quaternion();
        position = new Vector3f();
        ltm = new Matrix4f();
    }

    public Node(int id) {
        this.name = "";
        this.id = (short) id;
        rotation = new Quaternion();
        position = new Vector3f();
    }

    public void setupRotationMatrix() {
        rotation = null;
        rotation_matrix = new Matrix3f();
    }

    public void update() {
        if (notifyUpdate) {
            if (attach != null) {
                attach.setTransform(getLocalModelMatrix());
            }
        }
        for (Node child : children) {
            child.update();
        }
    }

    public void addChild(Node node) {
        node.parent = this;
        children.add(node);
    }

    public ArrayList<Node> getChildren() {
        return children;
    }

    public String getName() {
        return name;
    }

    public int getID() { return id; }

    public void setID(short id) {
        this.id = id;
    }

    public Matrix4f getLocalModelMatrix() {
        if (notifyUpdate) {
            if (parent != null) {
                parent.getLocalModelMatrix().mult(ltm, getModelMatrix());
            } else {
                ltm.set(getModelMatrix());
            }
            notifyUpdate = false;
        }
        return ltm;
    }

    public Matrix4f getModelMatrix() {
        if (rotation == null) {
            return new Matrix4f(rotation_matrix).setLocation(position);
        }
        return Matrix4f.fromTransform(rotation, position);
    }

    public Node getParent() {
        return parent;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public void notifyUpdate() {
        notifyUpdate = true;
        for (Node child : children) {
            child.notifyUpdate();
        }
    }

    public void attach(ModelObject obj) {
        attach = obj;
    }

    public Node getNode(int id) {
        if (this.id == id) {
            return this;
        }
        for (Node n : children) {
            Node r = n.getNode(name);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    public Node getNode(String name) {
        if (this.name.equals(name)) {
            return this;
        }
        for (Node n : children) {
            Node r = n.getNode(name);
            if (r != null) {
                return r;
            }
        }
        return null;
    }
}
