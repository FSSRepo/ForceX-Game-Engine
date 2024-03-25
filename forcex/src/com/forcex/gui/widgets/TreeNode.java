package com.forcex.gui.widgets;

import java.util.ArrayList;

public class TreeNode {
    public TreeNode parent;
    protected ArrayList<TreeNode> children = new ArrayList<>();
	protected boolean expand = false;
	protected byte emphasize = 0;
	protected short index = -1;

    public void addChild(TreeNode node) {
        node.parent = this;
        children.add(node);
    }

    public boolean isRoot() {
        return parent == null;
    }

    public boolean hasChildren() {
        return children.size() > 0;
    }

    public ArrayList<TreeNode> getChildren() {
        return children;
    }

    public boolean isExpand() {
        return expand;
    }

    public void setExpand(boolean z) {
        expand = z;
    }

    public void emphasize() {
        emphasize = 100;
    }

    public void expandBack() {
        if (parent != null) {
            parent.expand = true;
            parent.expandBack();
        }
    }

    public void collapseAll() {
        expand = false;
        for (TreeNode n : children) {
            n.collapseAll();
        }
    }
}
