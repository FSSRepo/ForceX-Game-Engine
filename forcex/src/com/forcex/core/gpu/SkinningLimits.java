package com.forcex.core.gpu;

import com.forcex.FX;
import com.forcex.core.GL;
import com.forcex.utils.Logger;

/**
 * Calculates the number of bone matrices that fit in the vertex uniform
 * budget of the current GPU. The value is queried lazily (the GL context must
 * be current) and cached until {@link #reset()} is called.
 */
public final class SkinningLimits {
    public static final int MIN_BONES = 32;
    public static final int MAX_BONES_CAP = 128;
    /** vec4 reserved for world, light, fog, shadow and clip plane uniforms. */
    public static final int RESERVED_UNIFORM_VECTORS = 48;

    private static int maxBones = -1;
    private static int uniformVectors = -1;

    private SkinningLimits() {
    }

    public static int getMaxBones() {
        if (maxBones == -1) {
            resolve();
        }
        return maxBones;
    }

    public static int getVertexUniformVectors() {
        if (uniformVectors == -1) {
            resolve();
        }
        return uniformVectors;
    }

    /** Call when the GL context is recreated so the capacity is queried again. */
    public static void reset() {
        maxBones = -1;
        uniformVectors = -1;
    }

    private static void resolve() {
        int vectors = 0;
        try {
            if (FX.gl != null) {
                vectors = FX.gl.glGetInteger(GL.GL_MAX_VERTEX_UNIFORM_VECTORS);
            }
        } catch (RuntimeException e) {
            Logger.log("SkinningLimits: can't query GL_MAX_VERTEX_UNIFORM_VECTORS (" + e + ")");
            vectors = 0;
        }
        uniformVectors = vectors;
        if (vectors <= 0) {
            maxBones = MIN_BONES;
            Logger.log("SkinningLimits: uniform capacity unknown, MAX_BONES=" + maxBones);
            return;
        }
        int available = (vectors - RESERVED_UNIFORM_VECTORS) / 4;
        if (available < MIN_BONES) {
            maxBones = MIN_BONES;
            Logger.log("SkinningLimits: GPU has only " + vectors + " vertex uniform vectors, "
                    + "keeping MAX_BONES=" + maxBones + " (shader may exceed the guaranteed budget)");
        } else {
            maxBones = Math.min(available, MAX_BONES_CAP);
        }
        String gpu = (FX.gpu != null ? FX.gpu.getGPUModel() : "unknown GPU");
        Logger.log("SkinningLimits: GPU=" + gpu + ", GL_MAX_VERTEX_UNIFORM_VECTORS=" + vectors
                + ", MAX_BONES=" + maxBones);
    }
}
