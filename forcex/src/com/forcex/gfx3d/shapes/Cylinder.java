package com.forcex.gfx3d.shapes;
import com.forcex.gfx3d.*;
import com.forcex.math.*;

public class Cylinder extends Mesh
{
	public Cylinder(int axisSamples, int radialSamples,
					float topRadius, float bottomRadius,
					float height,boolean closed){
		super(true);
		height *= 2f;
		int numVertices = (axisSamples * (radialSamples+1));
		int numTriangles = axisSamples * radialSamples * 2;
		if( closed ) {
            // If there are caps, add two additional rims and two summits.
            numVertices += 2 + 2 * (radialSamples + 1);
            // Add one triangle per radial sample, twice, to form the caps.
            numTriangles += 2 * radialSamples ;
        }
		float[] vertices = new float[numVertices*3];
		float[] normals = new float[numVertices*3];
		float[] texcoords = new float[numVertices*2];
		short[] indices = new short[numTriangles*3];
		float[][] circlePoints = new float[radialSamples+1][2];
        for (int circlePoint = 0; circlePoint < radialSamples; circlePoint++) {
            float angle = Maths.PI_2 / radialSamples * circlePoint;
            circlePoints[circlePoint][0] = Maths.cos(angle);
            circlePoints[circlePoint][1] = Maths.sin(angle);
        }
        // Add an additional point for closing the texture around the side of the cylinder.
        circlePoints[radialSamples][0] = circlePoints[0][0];
        circlePoints[radialSamples][1] = circlePoints[0][1];

        Vector3f[] circleNormals = new Vector3f[radialSamples+1];
        for (int circlePoint = 0; circlePoint < radialSamples+1; circlePoint++) {
            // The normal is the orthogonal to the side, which can be got without trigonometry.
            // The edge direction is oriented so that it goes up by Height, and out by the radius difference; let's use
            // those values in reverse order.
            Vector3f normal = new Vector3f(height * circlePoints[circlePoint][0],bottomRadius - topRadius,  height * circlePoints[circlePoint][1]);
			normal.normalize();
            circleNormals[circlePoint] = normal;
        }
        int currentIndex = 0;

        // Add a circle of points for each axis sample.
        for(int axisSample = 0; axisSample < axisSamples; axisSample++ ) {
            float currentHeight = -(height*0.5f) + height * axisSample / (axisSamples-1);
            float currentRadius = bottomRadius + (topRadius - bottomRadius) * axisSample / (axisSamples-1);
            for (int circlePoint = 0; circlePoint < radialSamples + 1; circlePoint++) {
                // Position, by multipliying the position on a unit circle with the current radius.
                vertices[currentIndex*3] = circlePoints[circlePoint][0] * currentRadius;
                vertices[currentIndex*3 +1] = currentHeight  ;
                vertices[currentIndex*3 +2] = circlePoints[circlePoint][1] * currentRadius ;

                // Normal
                Vector3f currentNormal = circleNormals[circlePoint];
                normals[currentIndex*3] = currentNormal.x;
                normals[currentIndex*3+1] = currentNormal.y;
                normals[currentIndex*3+2] = currentNormal.z;

                // Texture
                // The X is the angular position of the point.
                texcoords[currentIndex *2] = (float) circlePoint / radialSamples;
				if (closed)
                    texcoords[currentIndex *2 +1] = (bottomRadius + (height*0.5f) + currentHeight) / (bottomRadius + height + topRadius);
                else
                    texcoords[currentIndex *2 +1] = (height * 0.5f) + currentHeight;
                currentIndex++;
            }
        }
		if (closed) {
            // Bottom
            for (int circlePoint = 0; circlePoint < radialSamples + 1; circlePoint++) {
                vertices[currentIndex*3] = circlePoints[circlePoint][0] * bottomRadius;
                vertices[currentIndex*3 +1] = -height*0.5f;
                vertices[currentIndex*3 +2] = circlePoints[circlePoint][1] * bottomRadius;

                normals[currentIndex*3] = 0;
                normals[currentIndex*3+1] = -1;
                normals[currentIndex*3+2] = 0;

                texcoords[currentIndex *2] = (float) circlePoint / radialSamples;
                texcoords[currentIndex *2 +1] = bottomRadius / (bottomRadius + height + topRadius);

                currentIndex++;
            }
            // Top
            for (int circlePoint = 0; circlePoint < radialSamples + 1; circlePoint++) {
                vertices[currentIndex*3] = circlePoints[circlePoint][0] * topRadius;
                vertices[currentIndex*3 +1] = height*0.5f;
                vertices[currentIndex*3 +2] = circlePoints[circlePoint][1] * topRadius;

                normals[currentIndex*3] = 0;
                normals[currentIndex*3+1] = 1;
                normals[currentIndex*3+2] = 0;

                texcoords[currentIndex *2] = (float) circlePoint / radialSamples;
                texcoords[currentIndex *2 +1] = (bottomRadius + height) / (bottomRadius + height + topRadius);

                currentIndex++;
            }

            // Add the centers of the caps.
            vertices[currentIndex*3] = 0;
            vertices[currentIndex*3 +1] = -height*0.5f;
            vertices[currentIndex*3 +2] = 0;

            normals[currentIndex*3] = 0;
            normals[currentIndex*3+1] = -1;
            normals[currentIndex*3+2] = 0;

            texcoords[currentIndex *2] = 0.5f;
            texcoords[currentIndex *2+1] = 0f;

            currentIndex++;

            vertices[currentIndex*3] = 0;
            vertices[currentIndex*3 +1] = height*0.5f;
            vertices[currentIndex*3 +2] = 0;

            normals[currentIndex*3] = 0;
            normals[currentIndex*3+1] = 1;
            normals[currentIndex*3+2] = 0;

            texcoords[currentIndex *2] = 0.5f;
            texcoords[currentIndex *2+1] = 1f;
        }
        currentIndex = 0;
        for (short axisSample = 0; axisSample < axisSamples - 1; axisSample++) {
            for (int circlePoint = 0; circlePoint < radialSamples; circlePoint++) {
                indices[currentIndex++] = (short) (axisSample * (radialSamples + 1) + circlePoint);
                indices[currentIndex++] =  (short) (axisSample * (radialSamples + 1) + circlePoint + 1);
                indices[currentIndex++] =  (short) ((axisSample + 1) * (radialSamples + 1) + circlePoint);

                indices[currentIndex++] =  (short) ((axisSample + 1) * (radialSamples + 1) + circlePoint);
                indices[currentIndex++] =  (short) (axisSample * (radialSamples + 1) + circlePoint + 1);
                indices[currentIndex++] =  (short) ((axisSample + 1) * (radialSamples + 1) + circlePoint + 1);
            }
        }
		if(closed) {
            short bottomCapIndex = (short) (numVertices - 2);
            short topCapIndex = (short) (numVertices - 1);

            int bottomRowOffset = (axisSamples) * (radialSamples +1 );
            int topRowOffset = (axisSamples+1) * (radialSamples +1 );

            for (int circlePoint = 0; circlePoint < radialSamples; circlePoint++) {
                indices[currentIndex++] =  (short) (bottomRowOffset + circlePoint +1);
                indices[currentIndex++] = (short) (bottomRowOffset + circlePoint);
                indices[currentIndex++] =  bottomCapIndex;


                indices[currentIndex++] = (short) (topRowOffset + circlePoint);
                indices[currentIndex++] =  (short) (topRowOffset + circlePoint +1);
                indices[currentIndex++] =  topCapIndex;
            }
        }
		setVertices(vertices);
		setTextureCoords(texcoords);
		setNormals(normals);
		addPart(new MeshPart(indices));
	}
}
