package com.forcex.gtasdk;

import com.forcex.core.GL;
import com.forcex.core.gpu.VertexData;
import com.forcex.gfx3d.Mesh;
import com.forcex.gfx3d.MeshPart;

import java.util.ArrayList;

public class DFFGeometry {
    public static final int GEOMETRY_FLAG_TRISTRIP = 0x01;
    public static final int GEOMETRY_FLAG_POSITIONS = 0x02;
    public static final int GEOMETRY_FLAG_TEXCOORDS = 0x04;
    public static final int GEOMETRY_FLAG_COLORS = 0x08;
    public static final int GEOMETRY_FLAG_NORMALS = 0x10;
    public static final int GEOMETRY_FLAG_DYNAMIC_LIGHTING = 0x20;
    public static final int GEOMETRY_FLAG_MODULATEMATCOLOR = 0x40;
    public static final int GEOMETRY_FLAG_MULTIPLEUVSETS = 0x80;
    public ArrayList<DFFIndices> splits = new ArrayList<>();
    public ArrayList<DFFMaterial> materials = new ArrayList<>();
    public float[] vertices;
    public float[] uvs;
    public float[] normals;
    public byte[] colors;
    public byte[] nightColors;
    public int vertexCount;
    public int frameIdx = -1;
    public short model_id = -1;
    public String name = "";
    public int flags;
    public int uvsets;
    public float diffuse, specular, ambient;
    public boolean isTriangleStrip, hasMeshExtension;
    public DFFSkin skin;

    public boolean hasNormals() {
        return normals != null;
    }

    public int getNumTrangles() {
        if (isTriangleStrip) {
            return 0;
        }
        int total = 0;
        for (DFFIndices idx : splits) {
            total += (idx.index.length / 3);
        }
        return total;
    }

    public byte[] getRGB(int type, int idx) {
        switch (type) {
            case 0:
                return new byte[]{
                        colors[idx], colors[idx + 1], colors[idx + 2], colors[idx + 3]
                };
            case 1:
                return new byte[]{
                        nightColors[idx], nightColors[idx + 1], nightColors[idx + 2], nightColors[idx + 3]
                };
        }
        return null;
    }

    public void changeUVChannel() {
        if (uvsets == 2) {
            uvsets = 1;
            float[] uvs = new float[vertexCount * 2];
			if (vertexCount * 2 >= 0) System.arraycopy(this.uvs, 0, uvs, 0, vertexCount * 2);
            flags -= GEOMETRY_FLAG_MULTIPLEUVSETS;
            this.uvs = uvs;
        } else {
            uvsets = 2;
            float[] uvs = new float[vertexCount * 4];
            int i;
            for (i = 0; i < vertexCount * 2; i++) {
                uvs[i] = this.uvs[i];
            }
            for (; i < uvs.length; i++) {
                uvs[i] = 0.0f;
            }
            flags |= GEOMETRY_FLAG_MULTIPLEUVSETS;
            this.uvs = uvs;
        }
    }

    public boolean isModulateMaterial() {
        return (flags & GEOMETRY_FLAG_MODULATEMATCOLOR) != 0;
    }

    public void setModulateMaterial() {
        if (isModulateMaterial()) {
            flags -= GEOMETRY_FLAG_MODULATEMATCOLOR;
        } else {
            flags |= GEOMETRY_FLAG_MODULATEMATCOLOR;
        }
    }

    public void fromMesh(Mesh mesh) {
        flags |= GEOMETRY_FLAG_POSITIONS;
        VertexData data = mesh.getVertexData();
        vertices = mesh.getVertexData().vertices;
        vertexCount = vertices.length / 3;
        if (data.normals != null) {
            flags |= GEOMETRY_FLAG_NORMALS;
            normals = data.normals;
        }
        if (data.uvs != null) {
            flags |= GEOMETRY_FLAG_TEXCOORDS;
            uvsets = 1;
            if (data.uvs.length > vertexCount * 2) {
                flags |= GEOMETRY_FLAG_MULTIPLEUVSETS;
                uvsets = 2;
            }
            uvs = data.uvs;
        }
        if (data.colors != null) {
            flags |= GEOMETRY_FLAG_COLORS;
            colors = data.colors;
        }
        int i = 0;
        for (MeshPart p : mesh.getParts().list) {
            DFFIndices indx = new DFFIndices();
            DFFMaterial mat = new DFFMaterial();
            isTriangleStrip = mesh.getPrimitiveType() == GL.GL_TRIANGLE_STRIP;
            indx.index = p.index;
            indx.material = i;
            mat.color = p.material.color;
            mat.texture = p.material.textureName;
            materials.add(mat);
            splits.add(indx);
            i++;
        }
    }

    public void replace(DFFGeometry src, Mesh mesh_dest) {
        flags = src.flags;
        vertices = src.vertices;
        normals = src.normals;
        colors = src.colors;
        uvsets = src.uvsets;
        uvs = src.uvs;
        materials.clear();
		materials.addAll(src.materials);
        splits.clear();
		splits.addAll(src.splits);
        isTriangleStrip = src.isTriangleStrip;
        vertexCount = src.vertexCount;
        hasMeshExtension = src.hasMeshExtension;
        if ((src.flags & DFFGeometry.GEOMETRY_FLAG_TRISTRIP) == 0) {
            flags |= DFFGeometry.GEOMETRY_FLAG_TRISTRIP;
        }
        if (mesh_dest != null) {
            mesh_dest.getParts().delete();
            mesh_dest.setVertices(vertices);
            if ((flags & DFFGeometry.GEOMETRY_FLAG_TEXCOORDS) != 0 || (flags & DFFGeometry.GEOMETRY_FLAG_MULTIPLEUVSETS) != 0) {
                mesh_dest.setTextureCoords(uvs);
            }
            for (int j = 0; j < splits.size(); j++) {
                DFFIndices indx = splits.get(j);
                DFFMaterial mat = materials.get(indx.material);
                MeshPart part = new MeshPart(indx.index);
                part.material.color.set(mat.color);
                part.material.textureName = mat.texture;
                mesh_dest.addPart(part);
            }
            mesh_dest.setPrimitiveType(isTriangleStrip ? GL.GL_TRIANGLE_STRIP : GL.GL_TRIANGLES);
            if (normals != null) {
                mesh_dest.setNormals(normals);
            }
        }
    }

    public void clear() {
        splits.clear();
        materials.clear();
        vertices = null;
        normals = null;
        uvs = null;
        colors = null;
    }
}
