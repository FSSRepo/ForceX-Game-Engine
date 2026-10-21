package com.forcex.gfx3d.particle;

import com.forcex.math.Vector3f;
import com.forcex.utils.Color;

/** Emisor: configura como nacen las particulas y las agrega a un ParticleRenderer. */
public class ParticleSystem {
    float lifeTime;
    float particlesPerSecond;
    float speed;

    // 0 = chorro recto hacia arriba, 1 = hemisferio completo
    float spread = 0.3f;
    float emitRadius = 0.0f;
    float drag = 0.0f;
    float startScale = 1f, endScale = 1f;
    final Vector3f acceleration = new Vector3f(0, -9.8f, 0);
    final Color startColor = new Color();
    final Color endColor = new Color();

    // particulas pendientes de emitir (el ritmo puede ser menor a una por frame)
    private float pending = 0f;

    public ParticleSystem(float lifeTime, float pss, float speed) {
        this.lifeTime = lifeTime;
        particlesPerSecond = pss;
        this.speed = speed;
    }

    public ParticleSystem setSpread(float spread) {
        this.spread = spread;
        return this;
    }

    /** Radio del disco horizontal donde nacen las particulas. */
    public ParticleSystem setEmitRadius(float radius) {
        this.emitRadius = radius;
        return this;
    }

    /** Aceleracion constante: gravedad negativa en y, positiva para humo, o viento en x/z. */
    public ParticleSystem setAcceleration(float x, float y, float z) {
        acceleration.set(x, y, z);
        return this;
    }

    public ParticleSystem setDrag(float drag) {
        this.drag = drag;
        return this;
    }

    public ParticleSystem setScale(float start, float end) {
        startScale = start;
        endScale = end;
        return this;
    }

    public ParticleSystem setColors(Color start, Color end) {
        startColor.set(start.r, start.g, start.b, start.a);
        endColor.set(end.r, end.g, end.b, end.a);
        return this;
    }

    public void setRate(float particlesPerSecond) {
        this.particlesPerSecond = particlesPerSecond;
    }

    public void emit(ParticleRenderer render, float delta, float x, float y, float z) {
        pending += particlesPerSecond * delta;
        int count = (int) pending;
        pending -= count;
        for (int i = 0; i < count; i++) {
            Particle p = new Particle(generateValue(lifeTime, 0.25f * lifeTime));

            // posicion dentro del disco de emision
            float angle = (float) (Math.random() * Math.PI * 2.0);
            float radius = emitRadius * (float) Math.sqrt(Math.random());
            p.position.set(x + (float) Math.cos(angle) * radius, y, z + (float) Math.sin(angle) * radius);

            // direccion: hacia arriba, desviada dentro del cono
            float dx = ((float) Math.random() * 2f - 1f) * spread;
            float dz = ((float) Math.random() * 2f - 1f) * spread;
            p.velocity.set(dx, 1f, dz).normalize().multLocal(generateValue(speed, 0.3f * speed));

            p.acceleration.set(acceleration);
            p.drag = drag;
            float size = generateValue(1f, 0.2f);
            p.startScale = startScale * size;
            p.endScale = endScale * size;
            p.startColor.set(startColor.r, startColor.g, startColor.b, startColor.a);
            p.endColor.set(endColor.r, endColor.g, endColor.b, endColor.a);
            render.addParticle(p);
        }
    }

    /** Alias de emit para mantener la API anterior. */
    public void generate(ParticleRenderer render, float delta, float x, float y, float z) {
        emit(render, delta, x, y, z);
    }

    private float generateValue(float average, float errorMargin) {
        float offset = (float) (Math.random() - 0.5f) * 2f * errorMargin;
        return average + offset;
    }
}
