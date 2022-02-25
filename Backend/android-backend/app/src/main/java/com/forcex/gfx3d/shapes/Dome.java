package com.forcex.gfx3d.shapes;
import com.forcex.gfx3d.*;
import com.forcex.math.*;

public class Dome extends Mesh
{
	public Dome(boolean insideView,float radius,Vector3f center,int planes,int radialSamples){
		super(true);
        int vertCount = ((planes - 1) * (radialSamples + 1)) + 1;
        float[] vertices = new float[vertCount*3];
		float[] normals = new float[vertCount*3];
		float[] texcoords = new float[vertCount*2];
        float fInvRS = 1.0f / radialSamples;
        float fYFactor = 1.0f / (planes - 1);
        float[] afSin = new float[(radialSamples)];
        float[] afCos = new float[(radialSamples)];
        for (int iR = 0; iR < radialSamples; iR++) {
            float fAngle = Maths.PI_2 * fInvRS * iR;
            afCos[iR] = Maths.cos(fAngle);
            afSin[iR] = Maths.sin(fAngle);
        }
        int i = 0;
		for (int iY = 0; iY < (planes - 1); iY++, i++) {
            float fYFraction = fYFactor * iY; 
            float fY = radius * fYFraction;
            Vector3f kSliceCenter = new Vector3f().set(center);
            kSliceCenter.y += fY;
            float fSliceRadius = Maths.sqrt(Maths.abs(radius * radius - fY * fY));
			int iSave = i;
			for (int iR = 0; iR < radialSamples; iR++, i++) {
                float fRadialFraction = iR * fInvRS; // in [0,1)
                Vector3f kRadial = new Vector3f(afCos[iR], 0, afSin[iR]);
              	Vector3f result = kRadial.mult(fSliceRadius);
                vertices[i*3] = kSliceCenter.x + result.x;
				vertices[i*3+1] = kSliceCenter.y + result.y;
				vertices[i*3+2] = kSliceCenter.z + result.z;
				result.x = vertices[i*3];
				result.y = vertices[i*3+1];
				result.z = vertices[i*3+2];
				Vector3f nor = result.sub(center).normalize();
                if (!insideView) {
                    normals[i*3] = nor.x;
					normals[i*3+1] = nor.y;
					normals[i*3+2] = nor.z;
                } else {
                    normals[i*3] = -nor.x;
					normals[i*3+1] = -nor.y;
					normals[i*3+2] = -nor.z;
                }
                texcoords[i*2] = fRadialFraction;
				texcoords[i*2+1] = fYFraction;
			}
			vertices[i*3] =   vertices[iSave*3];
			vertices[i*3+1] = vertices[iSave*3+1];
			vertices[i*3+2] = vertices[iSave*3+2];
			normals[i*3] = 	 normals[iSave*3];
			normals[i*3+1] = normals[iSave*3+1];
			normals[i*3+2] = normals[iSave*3+2];
            texcoords[i*2] = 1.0f;
			texcoords[i*2+1] = fYFraction;
        }
		vertices[i*3] = center.x;
		vertices[i*3+1] = center.y + radius;
		vertices[i*3+2] = center.z;
		normals[i*3] = 0;
		normals[i*3+1] = insideView ? -1 : 1;
		normals[i*3+2] = 0;
		texcoords[i*2] = 0.5f;
		texcoords[i*2+1] = 1.0f;
        int triCount = (planes - 2) * radialSamples * 2 + radialSamples;
        short[] indices = new short[triCount*3];
        int idxo = 0;
        int index = 0;
        for (int plane = 1; plane < (planes - 1); plane++) {
            int bottomPlaneStart = ((plane - 1) * (radialSamples + 1));
            int topPlaneStart = (plane * (radialSamples + 1));
            for (int sample = 0; sample < radialSamples; sample++, index += 6) {
                if (insideView){
                    indices[idxo] = ((short) (bottomPlaneStart + sample));
                    indices[idxo+1] = ((short) (bottomPlaneStart + sample + 1));
                    indices[idxo+2] = ((short) (topPlaneStart + sample));
					indices[idxo+3] = ((short) (bottomPlaneStart + sample + 1));
                    indices[idxo+4] = ((short) (topPlaneStart + sample + 1));
                    indices[idxo+5] = ((short) (topPlaneStart + sample));
                }else{
                    indices[idxo] = ((short) (bottomPlaneStart + sample));
                    indices[idxo+1] = ((short) (topPlaneStart + sample));
                    indices[idxo+2] = ((short) (bottomPlaneStart + sample + 1));
                    indices[idxo+3] = ((short) (bottomPlaneStart + sample + 1));
                    indices[idxo+4] = ((short) (topPlaneStart + sample));
                    indices[idxo+5] = ((short) (topPlaneStart + sample + 1));
                }
				idxo += 6;
            }
        }
        int bottomPlaneStart = (planes - 2) * (radialSamples + 1);
        for (int samples = 0; samples < radialSamples; samples++, index += 3) {
            if (insideView){
                indices[idxo] = ((short) (bottomPlaneStart + samples));
                indices[idxo+1] = ((short) (bottomPlaneStart + samples + 1));
                indices[idxo+2] = ((short) (vertCount - 1));
            }else{
                indices[idxo] = ((short) (bottomPlaneStart + samples));
                indices[idxo+1] = ((short) (vertCount - 1));
                indices[idxo+2] = ((short) (bottomPlaneStart + samples + 1));
            }
			idxo += 3;
        }

		setVertices(vertices);
		setTextureCoords(texcoords);
		setNormals(normals);
		addPart(new MeshPart(indices));
	}
}
