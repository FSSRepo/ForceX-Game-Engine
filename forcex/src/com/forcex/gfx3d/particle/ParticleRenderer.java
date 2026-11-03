package com.forcex.gfx3d.particle;

import com.forcex.FX;
import com.forcex.core.GL;
import com.forcex.core.gpu.Texture;
import com.forcex.gfx3d.Camera;
import com.forcex.math.Matrix4f;
import com.forcex.math.Vector3f;
import com.forcex.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class ParticleRenderer {
    // limite de particulas vivas; las nuevas se descartan al llegar al tope
    public int maxParticles = 3000;
    // suma el color (SRC_ALPHA, ONE) en vez de mezclar: coronas y destellos
    public boolean additive = false;
    // funciones de mezcla (GL_*); additive fuerza el destino a GL_ONE
    public int blendSrc = GL.GL_SRC_ALPHA, blendDst = GL.GL_ONE_MINUS_SRC_ALPHA;

    ArrayList<Particle> particles = new ArrayList<>();
    int vbo, texture;
    boolean ownTexture = false;
    ParticleShader shader;
    GL gl = FX.gl;
    Matrix4f mm = new Matrix4f();
    Matrix4f mv = new Matrix4f();
    Matrix4f ls = new Matrix4f();
    float[] vertices = {
            -1, 1, 0, 0,
            -1, -1, 0, 1,
            1, 1, 1, 0,
            1, -1, 1, 1
    };

    private Vector3f camera_position = new Vector3f();

    public void addParticle(Particle particle) {
        if (particles.size() < maxParticles) {
            particles.add(particle);
        }
    }

    public int getParticleCount() {
        return particles.size();
    }

    public void create() {
        vbo = gl.glGenBuffer();
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, vertices.length * 4, BufferUtils.createFloatBuffer(vertices), GL.GL_STATIC_DRAW);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        shader = new ParticleShader();
        if (texture == 0) {
            // sin textura propia: disco blando (alfa que decae hacia el borde)
            texture = createSoftTexture(64);
            ownTexture = true;
        }
    }

    public void setTextureID(int id) {
        if (ownTexture) {
            gl.glDeleteTexture(texture);
            ownTexture = false;
        }
        texture = id;
    }

    /** Avanza la simulacion y elimina las particulas muertas. */
    public void update(float delta) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            if (!it.next().update(delta)) {
                it.remove();
            }
        }
    }

    public void render(Camera cam) {
        if (particles.isEmpty()) {
            return;
        }
        // de atras hacia adelante para que la mezcla alfa se vea bien
        camera_position.set(cam.getPosition());
        Collections.sort(particles, (a, b) -> Float.compare(distance2(b.position), distance2(a.position)));

        begin();
        for (Particle p : particles) {
            renderParticle(p, cam);
        }
        end();
    }

    private float distance2(Vector3f p) {
        float dx = p.x - camera_position.x;
        float dy = p.y - camera_position.y;
        float dz = p.z - camera_position.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private void renderParticle(Particle particle, Camera camera) {
        shader.setMVPMatrix(updateMVP(particle, camera));
        shader.setParticleColor(particle.color);
        shader.setUvRect(particle.u0, particle.v0, particle.u1 - particle.u0, particle.v1 - particle.v0);
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
    }

    // billboard: la rotacion del modelo es la transpuesta de la de la vista
    private Matrix4f updateMVP(Particle particle, Camera cam) {
        Vector3f position = particle.position;
        Matrix4f vm = cam.getViewMatrix();
        mm.setLocation(position);
        mm.data[Matrix4f.M00] = vm.data[Matrix4f.M00];
        mm.data[Matrix4f.M01] = vm.data[Matrix4f.M10];
        mm.data[Matrix4f.M02] = vm.data[Matrix4f.M20];
        mm.data[Matrix4f.M10] = vm.data[Matrix4f.M01];
        mm.data[Matrix4f.M11] = vm.data[Matrix4f.M11];
        mm.data[Matrix4f.M12] = vm.data[Matrix4f.M21];
        mm.data[Matrix4f.M20] = vm.data[Matrix4f.M02];
        mm.data[Matrix4f.M21] = vm.data[Matrix4f.M12];
        mm.data[Matrix4f.M22] = vm.data[Matrix4f.M22];
        vm.mult(mv, mm);
        // giro alrededor del eje de vista y proporcion del sprite
        float cos = (float) Math.cos(particle.rotation), sin = (float) Math.sin(particle.rotation);
        ls.setScale(1f);
        ls.data[Matrix4f.M00] = cos * particle.scale;
        ls.data[Matrix4f.M01] = sin * particle.scale;
        ls.data[Matrix4f.M10] = -sin * particle.scale * particle.aspect;
        ls.data[Matrix4f.M11] = cos * particle.scale * particle.aspect;
        ls.data[Matrix4f.M22] = particle.scale;
        mv.multLocal(ls);
        return cam.getProjectionMatrix().mult(null, mv);
    }

    private void begin() {
        shader.start();
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(blendSrc, additive ? GL.GL_ONE : blendDst);
        gl.glDepthMask(false);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
        gl.glVertexAttribPointer(shader.attribute_vertex, 4, GL.GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(shader.attribute_vertex);
        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, texture);
    }

    private void end() {
        gl.glDisableVertexAttribArray(shader.attribute_vertex);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        gl.glDepthMask(true);
        gl.glDisable(GL.GL_BLEND);
        shader.stop();
    }

    // RGBA blanco con alfa 1 en el centro y 0 en el borde
    private int createSoftTexture(int size) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(size * size * 4).order(ByteOrder.nativeOrder());
        float c = (size - 1) * 0.5f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float d = (float) Math.sqrt((x - c) * (x - c) + (y - c) * (y - c)) / c;
                float a = Math.max(0f, 1f - d);
                a = a * a * (3f - 2f * a);
                buffer.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) (a * 255f));
            }
        }
        buffer.position(0);
        return Texture.load(size, size, buffer, false);
    }

    public void delete() {
        particles.clear();
        FX.gl.glDeleteBuffer(vbo);
        if (texture != 0) {
            FX.gl.glDeleteTexture(texture);
        }
        shader.cleanUp();
        shader = null;
    }
}
