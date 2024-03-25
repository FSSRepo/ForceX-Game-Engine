package com.forcex.utils;

import com.forcex.math.Vector4f;

public class TextureAtlas {
    public static Vector4f getOffset(int numRows, int texIdx) {
        int col = texIdx % numRows;
        int row = texIdx / numRows;
        float u = col / (float) numRows;
        float v = row / (float) numRows;
        float factor = 1.0f / numRows;
        return new Vector4f(u, v, factor + u, factor + v);
    }
}
