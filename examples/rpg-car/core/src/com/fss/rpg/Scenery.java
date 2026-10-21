package com.fss.rpg;

import com.forcex.gfx3d.Mesh;
import com.forcex.gfx3d.ModelObject;
import com.forcex.gfx3d.particle.ParticleRenderer;
import com.forcex.gfx3d.particle.ParticleSystem;
import com.forcex.gfx3d.shapes.Box;
import com.forcex.gfx3d.shapes.Cylinder;
import com.forcex.math.Matrix4f;
import com.forcex.math.Quaternion;
import com.forcex.math.Vector3f;
import com.forcex.utils.Color;

import java.util.ArrayList;
import java.util.Random;

/**
 * Arboles y chimeneas repartidos sobre el terreno. Los troncos y las chimeneas tienen colision;
 * el follaje es solo visual. Cada chimenea emite humo con un sistema de particulas.
 */
public class Scenery {
    private static final int TREE_COUNT = 140;
    private static final int CHIMNEY_COUNT = 10;

    // area libre alrededor del punto de aparicion y margen con el borde del terreno
    private static final float SPAWN_X = 28f, SPAWN_Z = 15f, SPAWN_CLEAR_RADIUS = 34f;
    private static final float EDGE_MARGIN = 12f;
    private static final float MAX_SLOPE = 0.45f;
    private static final float TREE_SPACING = 5f;

    // el humo solo se simula cerca de la camara
    private static final float SMOKE_RANGE = 140f;

    private static final float TRUNK_HALF_HEIGHT = 1.5f, TRUNK_RADIUS = 0.32f;
    private static final float CHIMNEY_HALF_WIDTH = 0.9f, CHIMNEY_HALF_HEIGHT = 3.5f;

    // con colision y sombra: se agregan a la lista de objetos fisicos del juego
    public final ArrayList<PhysicObject> solids = new ArrayList<>();
    // solo visuales (follaje, remates)
    public final ArrayList<ModelObject> decorations = new ArrayList<>();

    private static class Chimney {
        final Vector3f smoke_origin;
        final ParticleSystem smoke;

        Chimney(Vector3f smoke_origin, ParticleSystem smoke) {
            this.smoke_origin = smoke_origin;
            this.smoke = smoke;
        }
    }

    private final ArrayList<Chimney> chimneys = new ArrayList<>();
    private final ArrayList<Vector3f> taken = new ArrayList<>();

    public Scenery(Terrain terrain, int seed) {
        Random random = new Random(seed);
        buildChimneys(terrain, random);
        buildTrees(terrain, random);
    }

    private void buildTrees(Terrain terrain, Random random) {
        Mesh trunk_mesh = new Cylinder(2, 8, TRUNK_RADIUS * 0.75f, TRUNK_RADIUS, TRUNK_HALF_HEIGHT, true);
        trunk_mesh.getPart(0).material.color.set(105, 72, 40);
        trunk_mesh.initialize();

        Mesh foliage_mesh = new Cylinder(2, 10, 0.02f, 1.8f, 2.2f, true);
        foliage_mesh.getPart(0).material.color.set(38, 105, 48);
        foliage_mesh.initialize();

        int trunk_shape = CollisionShape.createCapsule(TRUNK_RADIUS, TRUNK_HALF_HEIGHT * 2f - TRUNK_RADIUS * 2f);

        for (int i = 0, placed = 0; i < TREE_COUNT * 20 && placed < TREE_COUNT; i++) {
            Vector3f ground = pickSpot(terrain, random, TREE_SPACING);
            if (ground == null) {
                continue;
            }
            placed++;

            float yaw = random.nextFloat() * 360f;
            float size = 0.8f + random.nextFloat() * 0.8f;

            // tronco con colision, un poco hundido para que no flote en las pendientes
            Matrix4f trunk_transform = Matrix4f.fromTransform(
                    Quaternion.fromEulerAngles(0, yaw, 0),
                    new Vector3f(ground.x, ground.y + TRUNK_HALF_HEIGHT - 0.15f, ground.z));
            solids.add(new PhysicObject(trunk_shape, 0.0f, trunk_transform, trunk_mesh.clone()));

            // follaje, escalado desde la base del cono (la copa nace sobre el tronco)
            float foliage_half = 2.2f * size;
            ModelObject foliage = new ModelObject(foliage_mesh.clone());
            foliage.setTransform(Matrix4f.fromTransform(
                    Quaternion.fromEulerAngles(0, yaw, 0),
                    new Vector3f(ground.x, ground.y + 2.2f + foliage_half, ground.z))
                    .multLocal(Matrix4f.scale(size)));
            decorations.add(foliage);
        }
    }

    private void buildChimneys(Terrain terrain, Random random) {
        Mesh body_mesh = new Box(CHIMNEY_HALF_WIDTH, CHIMNEY_HALF_HEIGHT, CHIMNEY_HALF_WIDTH);
        body_mesh.getPart(0).material.color.set(150, 72, 52);
        body_mesh.initialize();

        Mesh cap_mesh = new Box(CHIMNEY_HALF_WIDTH + 0.25f, 0.2f, CHIMNEY_HALF_WIDTH + 0.25f);
        cap_mesh.getPart(0).material.color.set(95, 90, 88);
        cap_mesh.initialize();

        int body_shape = CollisionShape.createBox(new Vector3f(CHIMNEY_HALF_WIDTH, CHIMNEY_HALF_HEIGHT, CHIMNEY_HALF_WIDTH));

        for (int i = 0, placed = 0; i < CHIMNEY_COUNT * 40 && placed < CHIMNEY_COUNT; i++) {
            // las chimeneas piden mas espacio libre y un terreno casi plano
            Vector3f ground = pickSpot(terrain, random, 18f);
            if (ground == null) {
                continue;
            }
            placed++;

            float yaw = random.nextFloat() * 90f;
            // base hundida para asentarse bien aunque el suelo no sea del todo plano
            float base_y = ground.y - 0.6f;
            Quaternion rotation = Quaternion.fromEulerAngles(0, yaw, 0);

            solids.add(new PhysicObject(body_shape, 0.0f,
                    Matrix4f.fromTransform(rotation, new Vector3f(ground.x, base_y + CHIMNEY_HALF_HEIGHT, ground.z)),
                    body_mesh.clone()));

            float top_y = base_y + CHIMNEY_HALF_HEIGHT * 2f;
            ModelObject cap = new ModelObject(cap_mesh.clone());
            cap.setTransform(Matrix4f.fromTransform(rotation, new Vector3f(ground.x, top_y + 0.2f, ground.z)));
            decorations.add(cap);

            ParticleSystem smoke = new ParticleSystem(4.5f, 12f, 1.3f)
                    .setSpread(0.18f)
                    .setEmitRadius(0.3f)
                    // flotacion hacia arriba con una brisa lateral y rozamiento: el humo se frena y se abre
                    .setAcceleration(0.3f, 0.45f, 0.1f)
                    .setDrag(0.55f)
                    .setScale(0.5f, 3.4f)
                    .setColors(new Color(95, 95, 95, 170), new Color(210, 210, 210, 0));
            chimneys.add(new Chimney(new Vector3f(ground.x, top_y + 0.5f, ground.z), smoke));
        }
    }

    // punto sobre el terreno, lejos del spawn y de lo ya colocado, en suelo poco inclinado
    private Vector3f pickSpot(Terrain terrain, Random random, float spacing) {
        float range = Terrain.HALF_SIZE - EDGE_MARGIN;
        float x = (random.nextFloat() * 2f - 1f) * range;
        float z = (random.nextFloat() * 2f - 1f) * range;

        float dx = x - SPAWN_X;
        float dz = z - SPAWN_Z;
        if (dx * dx + dz * dz < SPAWN_CLEAR_RADIUS * SPAWN_CLEAR_RADIUS) {
            return null;
        }

        float h = terrain.heightAt(x, z);
        float step = 1.5f;
        float slope_x = Math.abs(terrain.heightAt(x + step, z) - terrain.heightAt(x - step, z)) / (2f * step);
        float slope_z = Math.abs(terrain.heightAt(x, z + step) - terrain.heightAt(x, z - step)) / (2f * step);
        if (Math.max(slope_x, slope_z) > MAX_SLOPE) {
            return null;
        }

        for (Vector3f other : taken) {
            float ox = other.x - x;
            float oz = other.z - z;
            if (ox * ox + oz * oz < spacing * spacing) {
                return null;
            }
        }
        Vector3f spot = new Vector3f(x, h, z);
        taken.add(spot);
        return spot;
    }

    /** Emite humo de las chimeneas cercanas a la camara. */
    public void update(ParticleRenderer particles, float delta, Vector3f camera_position) {
        for (Chimney chimney : chimneys) {
            float dx = chimney.smoke_origin.x - camera_position.x;
            float dz = chimney.smoke_origin.z - camera_position.z;
            if (dx * dx + dz * dz > SMOKE_RANGE * SMOKE_RANGE) {
                continue;
            }
            chimney.smoke.emit(particles, delta, chimney.smoke_origin.x, chimney.smoke_origin.y, chimney.smoke_origin.z);
        }
    }
}
