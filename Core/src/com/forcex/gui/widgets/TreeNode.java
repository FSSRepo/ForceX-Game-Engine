package com.forcex.gui.widgets;
import java.util.*;
import com.forcex.math.*;

public class TreeNode {
	ArrayList<TreeNode> children = new ArrayList<>();
	TreeNode parent;
	boolean expand = false,showing = true;
	byte index = -1;
	
	public void addChild(TreeNode node){
		node.parent = this;
		children.add(node);
	}
	
	public boolean isRoot(){
		return parent == null;
	}
	
	public boolean hasChildren(){
		return children.size() > 0;
	}
	
	public TreeNode getParent(){
		return parent;
	}
	
	public ArrayList<TreeNode> getChildren(){
		return children;
	}
	
	public boolean isExpand(){
		return expand;
	}
	
	public void setExpand(boolean z){
		expand = z;
	}
}
