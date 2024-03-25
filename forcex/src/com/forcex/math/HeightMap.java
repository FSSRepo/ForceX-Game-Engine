package com.forcex.math;

import com.forcex.FX;
import com.forcex.utils.Image;

public class HeightMap {
    public float[] vertices;
    public float[] uvs;
    public float[] normals;
    public short[] indices;

	public HeightMap(String path, float height, float extent, boolean uvs, boolean nor) {
		Image height_map_image = new Image(path);
        if (height_map_image.width != height_map_image.height) {
            FX.device.showInfo("Error the heightmap '" + path + "' isn't cuadrade", true);
            FX.device.stopRender();
        }
        if (height_map_image.width > 128) {
            FX.device.showInfo("Error the heightmap '" + path + "' must be 128 maximum width image", true);
            FX.device.stopRender();
        }
        short vertexCount = (short) (height_map_image.height * height_map_image.height);
        vertices = new float[vertexCount * 3];
        int offset = 0;
        short x, y;
        for (x = 0; x < height_map_image.width; x++) {
            for (y = 0; y < height_map_image.width; y++) {
                vertices[offset] = -(2f * (x / ((float) height_map_image.width - 1)) - 1f) * extent;
                vertices[offset + 1] = getHeight(height_map_image, x, y) * height;
                vertices[offset + 2] = (2f * (y / ((float) height_map_image.width - 1)) - 1f) * extent;
                offset += 3;
            }
        }
        if (uvs) {
            offset = 0;
            this.uvs = new float[vertexCount * 2];
            for (x = 0; x < height_map_image.width; x++) {
                for (y = 0; y < height_map_image.width; y++) {
                    this.uvs[offset] = (x / (float) height_map_image.width - 1f);
                    this.uvs[offset + 1] = (y / (float) height_map_image.width - 1f);
                    offset += 2;
                }
            }
        }
        if (nor) {
            offset = 0;
            normals = new float[vertexCount * 3];
            Vector3f normal = new Vector3f();
            for (x = 0; x < height_map_image.width; x++) {
                for (y = 0; y < height_map_image.width; y++) {
                    float heightL = getHeight(height_map_image, x - 1, y);
                    float heightR = getHeight(height_map_image, x + 1, y);
                    float heightD = getHeight(height_map_image, x, y - 1);
                    float heightU = getHeight(height_map_image, x, y + 1);
                    normal.set(heightL - heightR, 2f, heightD - heightU);
                    normal.normalize();
                    normals[offset] = normal.x;
                    normals[offset + 1] = normal.y;
                    normals[offset + 2] = normal.z;
                    offset += 3;
                }
            }
            normal = null;
        }
        indices = new short[6 * ((height_map_image.width - 1) * height_map_image.width)];
        int pointer = 0;
        for (short gz = 0; gz < height_map_image.width - 1; gz++) {
            for (short gx = 0; gx < height_map_image.width - 1; gx++) {
                int topLeft = (gz * height_map_image.width) + gx;
                int topRight = topLeft + 1;
                int bottomLeft = ((gz + 1) * height_map_image.width) + gx;
                int bottomRight = bottomLeft + 1;
                indices[pointer++] = (short) topLeft;
                indices[pointer++] = (short) bottomLeft;
                indices[pointer++] = (short) topRight;
                indices[pointer++] = (short) topRight;
                indices[pointer++] = (short) bottomLeft;
                indices[pointer++] = (short) bottomRight;
            }
        }
        height_map_image.clear();
    }

    private float getHeight(Image img, int x, int y) {
        if (x < 0 || x >= img.height || y < 0 || y >= img.height) {
            return 0;
        }
        return
                (img.getRGBA(x, y).r & 0xff) * 0.003921f;
    }
}
