package com.forcex.gfx3d;

import com.forcex.core.gpu.*;
import java.util.*;
import com.forcex.core.*;

public class MeshPart {
	public ArrayList<MeshPart> list;
	
    public MeshPart(short[] idx) {
        index = idx;
		if(index != null){
			indxSize = idx.length;
		}
		material = new Material();
    }
	
	protected MeshPart() {
        list = new ArrayList<>();
    }
	
    public short[] index = null;
	public int indxSize = 0;
    public Material material = null;
    public IndexBuffer buffer = null;
    public boolean visible = true;
	public int type = GL.GL_TRIANGLES;
	
	void add(MeshPart part){
		list.add(part);
	}
	
	MeshPart get(int idx){
		return list.get(idx);
	}
	
    void update(boolean normalMap) {
        for(MeshPart item : list){
			item.updateInternal(normalMap);
		}
    }
	
	public void delete(){
		for(MeshPart item : list){
			item.del();
		}
		list.clear();
	}
	
	int size(){
		return list.size();
	}
	
	private void updateInternal(boolean normalMap) {
		if (buffer == null) {
			buffer = new IndexBuffer();
		}
		buffer.update(index);
		material.update(normalMap);
	}
	
    private void del() {
        index = null;
        material.delete();
        material = null;
		if(buffer != null){
			buffer.delete();
			buffer = null;
		}
    }
	
	public MeshPart clone(){
		MeshPart p = new MeshPart(null);
		p.buffer = buffer.clone();
		p.material = material.clone();
		p.indxSize = index.length;
		p.visible = visible;
		return p;
	}
	
	public void draw(int primitive,boolean hook){
		if(!hook){
			buffer.bind(primitive,indxSize);
		}else{
			buffer.bind(type,indxSize);
		}
	}
}
