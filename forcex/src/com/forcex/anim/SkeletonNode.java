package com.forcex.anim;

import com.forcex.math.Matrix4f;

import java.util.ArrayList;

/**
 * Represents a node in a skeleton hierarchy.
 */
public class SkeletonNode {
    public Matrix4f InverseBoneMatrix; // The inverse bone matrix.
    public Matrix4f modelMatrix; // The model matrix of the node.
    public int boneID = -1; // Identifier for the bone.
    public int boneNum; // Bone number.
    public String name; // Name of the node.
    public ArrayList<SkeletonNode> children = new ArrayList<>(); // List of child nodes.
    public SkeletonNode parent; // Parent node.

    /**
     * Default constructor.
     */
    public SkeletonNode() {
        name = "";
        boneID = -1;
    }

    /**
     * Adds a child node to the current node.
     *
     * @param frame The child node to be added.
     */
    public void addChild(SkeletonNode frame) {
        frame.parent = this;
        children.add(frame);
    }

    /**
     * Retrieves the local model matrix of the node.
     *
     * @return The local model matrix.
     */
    public Matrix4f getLocalModelMatrix() {
        Matrix4f ltm = new Matrix4f();
        if (parent != null) {
            parent.getLocalModelMatrix().mult(ltm, modelMatrix);
        } else {
            ltm = modelMatrix;
        }
        return ltm;
    }

    /**
     * Finds a node in the hierarchy based on the bone number.
     *
     * @param num The bone number to search for.
     * @return The found node or null if not found.
     */
    public SkeletonNode findByBoneNum(int num) {
        if (boneNum == num) {
            return this;
        }
        for (SkeletonNode n : children) {
            SkeletonNode r = n.findByBoneNum(num);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    /**
     * Finds a node in the hierarchy based on the bone ID.
     *
     * @param id The bone ID to search for.
     * @return The found node or null if not found.
     */
    public SkeletonNode findByBoneId(int id) {
        if (boneID == id) {
            return this;
        }
        for (SkeletonNode n : children) {
            SkeletonNode r = n.findByBoneId(id);
            if (r != null) {
                return r;
            }
        }
        return null;
    }
}
