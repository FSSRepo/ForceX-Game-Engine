package com.forcex.gtasdk;

import com.forcex.math.Matrix3f;
import com.forcex.math.Matrix4f;
import com.forcex.math.Vector3f;

import java.util.ArrayList;

public class DFFFrame {
    public ArrayList<DFFFrame> children = new ArrayList<>();
    public DFFHanim hanim;
    public String name = "";
    public int parentIdx;
    public int flags;
    public short geoAttach = -1;
    public Matrix3f rotation = new Matrix3f();
    public Vector3f position = new Vector3f(0, 0, 0);
    public DFFFrame parent;
    public Matrix4f ltm;
    public boolean updateLTM = true;
    public short model_id = -1;

    public void invalidateLTM() {
        updateLTM = true;
        for (DFFFrame child : children) {
            child.invalidateLTM();
        }
    }

    public boolean hasHanim() {
        return hanim != null;
    }

    public Matrix4f getModelMatrix() {
        return new Matrix4f(rotation).setLocation(position);
    }

    public Matrix4f getLocalModelMatrix() {
        if (updateLTM) {
            if (ltm == null) {
                ltm = new Matrix4f();
            }
            if (parent != null) {
                parent.getLocalModelMatrix().mult(ltm, getModelMatrix());
            } else {
                ltm = getModelMatrix();
            }
            updateLTM = false;
            return ltm;
        }
        return ltm;
    }

    public void delete(DFFSDK dff) {
        if (parent != null) {
            parent.children.remove(this);
        }
        ArrayList<DFFFrame> frame_del = new ArrayList<>();
        ArrayList<DFFGeometry> geom_del = new ArrayList<>();
        deleteFrame(frame_del, geom_del, this, dff);
        for (DFFFrame f : frame_del) {
            dff.fms.remove(f);
            dff.frameCount--;
        }
        for (DFFGeometry geo : geom_del) {
            dff.geom.remove(geo);
            dff.geometryCount--;
            geo.clear();
        }
        dff.updateAtomics();
        frame_del.clear();
        geom_del.clear();
        dff.updateParents(dff.getFrameRoot());
        dff.checkMaterialEffect();
    }

    private void deleteFrame(ArrayList<DFFFrame> fm, ArrayList<DFFGeometry> gm, DFFFrame root, DFFSDK sdk) {
        fm.add(root);
        if (root.geoAttach != -1) {
            gm.add(sdk.geom.get(root.geoAttach));
        }
        for (DFFFrame c : root.children) {
            deleteFrame(fm, gm, c, sdk);
        }
        root.children.clear();
        root.children = null;
    }

    public int getLevel() {
        int level = 0;
        DFFFrame current_parent = parent;
        while (current_parent != null) {
            level++;
			current_parent = current_parent.parent;
        }
        return level;
    }

    public void print() {
        int level = getLevel();
        for (int i = 0; i < level; i++) {
            System.out.print("	");
        }
        System.out.println(name + " [" + (geoAttach == -1 ? "FRAME" : "GEOMETRY") + "]");
        for (DFFFrame c : children) {
            c.print();
        }
    }
}
