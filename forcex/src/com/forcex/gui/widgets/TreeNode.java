package com.forcex.gui.widgets;
import java.util.*;
import com.forcex.math.*;

public class TreeNode {
	ArrayList<TreeNode> children = new ArrayList<>();
	public TreeNode parent;
	boolean expand = false;
	byte emphasize = 0;
	short index = -1;
	
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
	
	public ArrayList<TreeNode> getChildren(){
		return children;
	}
	
	public boolean isExpand(){
		return expand;
	}
	
	public void setExpand(boolean z){
		expand = z;
	}
	
	public void emphasize(){
		emphasize = 100;
	}
	
	public void expandBack() {
		if(parent != null) {
			parent.expand = true;
			parent.expandBack();
		}
	}
	
	public void collapseAll()  {
		expand = false;
		for(TreeNode n : children){
			n.collapseAll();
		}
	}
}
