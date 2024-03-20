package com.forcex.gtasdk;
import java.util.*;
import com.forcex.gfx3d.*;
import com.forcex.utils.*;
import com.forcex.math.*;
import com.forcex.anim.*;
import com.forcex.core.*;
import com.forcex.gui.*;

public class DFFSDK
{
	public ArrayList<DFFGeometry> geom = new ArrayList<>();
	public ArrayList<DFFFrame> fms = new ArrayList<>();
	public ArrayList<DFFAtomic> atomics = new ArrayList<>();
	public ArrayList<DFFHanim.Bone> bones = new ArrayList<>();
	public int atomicCount;
	public int frameCount;
	public int geometryCount;
	public int game;
	public boolean hasCollision;
	public String col_id = "";
	protected boolean hasExtensionDff;
	public boolean errorNames = false;
	private DFFFrame root;
	public String name = "";
	
	public DFFFrame findFrame(String name){
		for(int i = 0;i < fms.size();i++){
			if(fms.get(i).name.equals(name)){
				return fms.get(i);
			}
		}
		return null;
	}
	
	public DFFGeometry findGeometry(String name){
		for(int i = 0;i < geom.size();i++){
			if(geom.get(i).name.equals(name)){
				return geom.get(i);
			}
		}
		return null;
	}
	
	public DFFGeometry findGeometry(short model_id){
		for(int i = 0;i < geom.size();i++){
			if(geom.get(i).model_id == model_id){
				return geom.get(i);
			}
		}
		return null;
	}
	
	public int indexOfFrame(String name){
		for(int i = 0;i < fms.size();i++){
			if(fms.get(i).name.equals(name)){
				return i;
			}
		}
		return -1;
	}
	
	public int indexOfGeometry(String name){
		for(int i = 0;i < geometryCount;i++){
			if(geom.get(i).name.equals(name)){
				return i;
			}
		}
		return -1;
	}

	public DFFFrame getFrame(int idx){
		if(idx == -1){
			return null;
		}
		return fms.get(idx);
	}
	
	public DFFFrame getFrameRoot(){
		if(root == null){
			root = fms.get(isSkin() ? 1 : 0);
			for(short i = (short)(isSkin() ? 2 : 1);i < fms.size();i++){
				DFFFrame frame = fms.get(i);
				if(frame.parentIdx != -1){
					frame.parent = fms.get(frame.parentIdx);
					frame.parent.childs.add(frame);
				}
			}
		}
		return root;
	}
	
	public void addGeometry(DFFGeometry geometry){
		geom.add(geometry);
		geometryCount++;
	}
	
	public void addFrame(DFFFrame frame){
		fms.add(frame);
		frameCount++;
	}
	
	public void addAtomic(DFFAtomic atomic){
		atomics.add(atomic);
		atomicCount++;
	}
	
	public DFFAtomic findAtomicByGeometry(int gidx){
		for(int i = 0;i < atomicCount;i++){
			if(atomics.get(i).geoIdx == gidx){
				return atomics.get(i);
			}
		}
		return null;
	}
	
	public DFFAtomic findAtomicByFrame(int fidx){
		for(int i = 0;i < atomicCount;i++){
			if(atomics.get(i).frameIdx == fidx){
				return atomics.get(i);
			}
		}
		return null;
	}
	
	public boolean hasTexture(){
		for(DFFGeometry geo : geom){
			for(DFFMaterial mat : geo.materials){
				if(mat.hasTexture()){
					return true;
				}
			}
		}
		return false;
	}
	
	public void updateParents(DFFFrame parent){
		for(DFFFrame child : parent.childs){
			child.parentIdx = indexOfFrame(parent.name);
			updateParents(child);
		}
	}
	
	public boolean isSkin(){
		if(geom.size() == 0){
			return false;
		}
		return geom.get(0).skin != null;
	}
	
	public void checkMaterialEffect(){
		for(DFFGeometry geo : geom){
			DFFAtomic atm = findAtomicByFrame(geo.frameIdx);
			if(atm != null){
				atm.unknow1 = 5;
				for(DFFMaterial m : geo.materials){
					if(!(m.hasReflectionMat && m.hasSpecularMat)){
						atm.hasMaterialEffect = false;
						break;
					}else{
						atm.hasMaterialEffect = true;
						atm.materialFxType = 1;
					}
				}
			}
		}
	}
	
	public boolean checkIsOnlyDFF() {
        boolean onlydff = true;
        for (DFFGeometry geo : geom) {
            if(!geo.isModulateMaterial() || geo.uvsets == 1){
				onlydff = false;
				break;
			}
        }
        return onlydff;
    }
	
	public void convertOnlyDFF() {
		for (DFFGeometry geo : geom) {
			if (!geo.isModulateMaterial()) {
                geo.setModulateMaterial();
            }
            if (geo.uvsets == 1) {
                geo.changeUVChannel();
            }
        }
        checkMaterialEffect();
    }
	
	public ModelObject getObject(Object obj,int i,boolean forceNormalMap){
		if(!(obj instanceof ModelObject)){
			return null;
		}
		getFrameRoot();
		ModelObject o = (ModelObject)obj;
		DFFGeometry geo = geom.get(i);
		if(isSkin()) {
			o.setName(name);
		}else if(geo.frameIdx != -1) {
			DFFFrame fm = fms.get(geo.frameIdx);
			o.setName(fm.name);
			o.setTransform(fm.getLocalModelMatrix());
		}else{
			return null;
		}
		Mesh mesh = new Mesh(true);
		mesh.setVertices(geo.vertices);
		if((geo.flags & DFFGeometry.GEOMETRY_FLAG_TEXCOORDS) != 0 || (geo.flags & DFFGeometry.GEOMETRY_FLAG_MULTIPLEUVSETS) != 0){
			mesh.setTextureCoords(geo.texcoords);
		}
		for(int j = 0;j < geo.splits.size();j++){
			DFFIndices indx = geo.splits.get(j);
			DFFMaterial mat = geo.materials.get(indx.material);
			MeshPart part = new MeshPart(indx.index);
			part.material.color.set(mat.color);
			part.material.textureName = mat.texture;
			mesh.addPart(part);
		}
		if(geo.isTriangleStrip && forceNormalMap){
			return null;
		}
		mesh.setPrimitiveType(geo.isTriangleStrip ? GL.GL_TRIANGLE_STRIP : GL.GL_TRIANGLES);
		if(geo.normals != null){
			mesh.setNormals(geo.normals);
		}
		if(geo.normals == null && forceNormalMap){
			mesh.setNormals(MathGeom.calculateNormals(geo.vertices,mesh.getParts().list,true));
		}
		if(forceNormalMap){
			MathGeom.setNormalMapProps(mesh,true);
		}
		if(isSkin()){
			mesh.setBoneWeights(geo.skin.boneWeigts);
			mesh.setBoneIndices(geo.skin.boneIndices);
		}
		o.setMesh(mesh);
		return o;
	}
	
	public void deleteGeometry(DFFFrame fm){
		if(fm.geoAttach == -1){
			return;
		}
		geometryCount--;
		geom.remove(geom.get(fm.geoAttach));
		fm.geoAttach = -1;
		fm.model_id = -1;
		updateAtomics();
	}
	
	protected void updateAtomics() {
		atomics.clear();
		for(int i = 0;i < fms.size();i++){
			if(getFrame(i).geoAttach != -1){
				DFFFrame fm = getFrame(i);
				DFFGeometry geo = findGeometry(fm.model_id);
				DFFAtomic atm = new DFFAtomic();
				atm.hasRenderToRight = true;
				atm.RTRval1 = 0x120;
				atm.RTRval2 = 0;
				atm.unknow1 = 5;
				fm.geoAttach = (short)geom.indexOf(geo);
				geo.frameIdx = i;
				atm.frameIdx = geo.frameIdx;
				atm.geoIdx = fm.geoAttach;
				addAtomic(atm);
			}
		}
		atomicCount = atomics.size();
	}
	
	public SkeletonNode getSkeleton(DFFFrame fm){
		SkeletonNode root = new SkeletonNode();
		root.name = fm.name;
		root.boneID = (short)fm.hanim.boneID;
		root.boneNum = (short)getBone(fm.hanim.boneID).num;
		root.InverseBoneMatrix = geom.get(0).skin.boneMatrices[root.boneNum];
		root.InverseBoneMatrix.data[Matrix4f.M33] = 1.0f;
		root.modelMatrix = fm.getModelMatrix();
		for(DFFFrame fmc : fm.childs){
			root.addChild(getSkeleton(fmc));
		}
		return root;
	}
	
	private DFFHanim.Bone getBone(int id){
		for(DFFHanim.Bone b : bones){
			if(b.id == id){
				return b;
			}
		}
		return null;
	}
	
	public void assignModelID() {
		int m = 90;
		for(DFFGeometry g : geom){
			getFrame(g.frameIdx).model_id = (short)m;
			g.model_id = (short)m;
			m++;
		}
	}
}
