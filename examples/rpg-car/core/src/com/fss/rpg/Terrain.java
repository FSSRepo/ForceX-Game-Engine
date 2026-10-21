package com.fss.rpg;

import com.forcex.gfx3d.Mesh;
import com.forcex.gfx3d.MeshPart;
import com.forcex.math.Matrix4f;

/**
 * Terreno procedural: malla de alturas generada con ruido de valor (fBm) y su forma de colision.
 * El area alrededor del punto de aparicion se aplana para que jugador y autos no nazcan sobre una pendiente.
 */
public class Terrain {
    // mitad del lado del terreno (mismo tamano que el piso plano anterior) y altura del terreno plano
    public static final float HALF_SIZE = 200f;
    public static final float BASE_HEIGHT = 1f;

    // vertices por lado; se limita para que los indices (short) no superen 32767
    private static final int GRID = 181;
    // metros por vuelta de textura
    private static final float UV_METERS = 16f;

    // ruido: longitud de onda de la primera octava (m), amplitud (m), octavas
    private static final float NOISE_SCALE = 90f;
    private static final float AMPLITUDE = 9f;
    private static final int OCTAVES = 4;

    // zona plana alrededor del punto de aparicion
    private static final float FLAT_CENTER_X = 28f, FLAT_CENTER_Z = 15f;
    private static final float FLAT_RADIUS = 22f, BLEND_RADIUS = 30f;

    private final int seed;
    private float[] vertices;
    private short[] indices;
    private Mesh mesh;

    public Terrain(int seed) {
        this.seed = seed;
        build();
    }

    public Mesh getMesh() {
        return mesh;
    }

    public float[] getVertices() {
        return vertices;
    }

    public short[] getIndices() {
        return indices;
    }

    public PhysicObject createPhysicObject() {
        return new PhysicObject(CollisionShape.createMeshShape(vertices, indices), 0.0f, new Matrix4f(), mesh);
    }

    public float heightAt(float x, float z) {
        float noise = 0.0f, amplitude = 1.0f, total = 0.0f, frequency = 1.0f / NOISE_SCALE;
        for (int i = 0; i < OCTAVES; i++) {
            noise += valueNoise(x * frequency, z * frequency, seed + i * 101) * amplitude;
            total += amplitude;
            amplitude *= 0.5f;
            frequency *= 2.0f;
        }
        // de [0, 1] a [-1, 1]
        noise = (noise / total) * 2.0f - 1.0f;

        float dx = x - FLAT_CENTER_X;
        float dz = z - FLAT_CENTER_Z;
        float distance = (float) Math.sqrt(dx * dx + dz * dz);
        float blend = smoothstep((distance - FLAT_RADIUS) / BLEND_RADIUS);

        return BASE_HEIGHT + noise * AMPLITUDE * blend;
    }

    private void build() {
        int cells = GRID - 1;
        float step = HALF_SIZE * 2.0f / cells;

        vertices = new float[GRID * GRID * 3];
        float[] normals = new float[GRID * GRID * 3];
        float[] uvs = new float[GRID * GRID * 2];
        float[] heights = new float[GRID * GRID];

        for (int j = 0; j < GRID; j++) {
            for (int i = 0; i < GRID; i++) {
                float x = -HALF_SIZE + i * step;
                float z = -HALF_SIZE + j * step;
                int v = j * GRID + i;
                heights[v] = heightAt(x, z);
                vertices[v * 3] = x;
                vertices[v * 3 + 1] = heights[v];
                vertices[v * 3 + 2] = z;
                uvs[v * 2] = x / UV_METERS;
                uvs[v * 2 + 1] = z / UV_METERS;
            }
        }

        // normales por diferencias centrales de la altura
        for (int j = 0; j < GRID; j++) {
            for (int i = 0; i < GRID; i++) {
                float left = heights[j * GRID + Math.max(i - 1, 0)];
                float right = heights[j * GRID + Math.min(i + 1, GRID - 1)];
                float down = heights[Math.max(j - 1, 0) * GRID + i];
                float up = heights[Math.min(j + 1, GRID - 1) * GRID + i];
                float nx = (left - right) / (2.0f * step);
                float nz = (down - up) / (2.0f * step);
                float len = (float) Math.sqrt(nx * nx + 1.0f + nz * nz);
                int v = j * GRID + i;
                normals[v * 3] = nx / len;
                normals[v * 3 + 1] = 1.0f / len;
                normals[v * 3 + 2] = nz / len;
            }
        }

        // mismo orden de vertices que la cara superior de Box para que mire hacia arriba
        indices = new short[cells * cells * 6];
        int index = 0;
        for (int j = 0; j < cells; j++) {
            for (int i = 0; i < cells; i++) {
                short v00 = (short) (j * GRID + i);
                short v10 = (short) (j * GRID + i + 1);
                short v01 = (short) ((j + 1) * GRID + i);
                short v11 = (short) ((j + 1) * GRID + i + 1);
                indices[index++] = v00;
                indices[index++] = v11;
                indices[index++] = v10;
                indices[index++] = v00;
                indices[index++] = v01;
                indices[index++] = v11;
            }
        }

        mesh = new Mesh(true);
        mesh.setVertices(vertices);
        mesh.setNormals(normals);
        mesh.setTextureCoords(uvs);
        mesh.addPart(new MeshPart(indices));
    }

    // ruido de valor en [0, 1] con interpolacion suave entre los valores de la rejilla entera
    private static float valueNoise(float x, float z, int seed) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        float fx = smoothstep(x - x0);
        float fz = smoothstep(z - z0);
        float a = hash(x0, z0, seed);
        float b = hash(x0 + 1, z0, seed);
        float c = hash(x0, z0 + 1, seed);
        float d = hash(x0 + 1, z0 + 1, seed);
        float top = a + (b - a) * fx;
        float bottom = c + (d - c) * fx;
        return top + (bottom - top) * fz;
    }

    private static float hash(int x, int z, int seed) {
        int h = x * 374761393 + z * 668265263 + seed * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    private static float smoothstep(float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        return t * t * (3.0f - 2.0f * t);
    }
}
