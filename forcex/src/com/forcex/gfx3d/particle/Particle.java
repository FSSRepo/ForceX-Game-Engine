package com.forcex.gfx3d.particle;

import com.forcex.math.Vector3f;
import com.forcex.utils.Color;

public class Particle {
    public float lifetime;
    public float liveFactor;
    public final Vector3f position = new Vector3f();
    public final Vector3f velocity = new Vector3f();
    // aceleracion constante (gravedad, flotacion, viento)
    public final Vector3f acceleration = new Vector3f();
    // frenado por rozamiento con el aire (1/s)
    public float drag;
    public float startScale = 1f, endScale = 1f;
    public float scale = 1f;
    // el color se interpola a lo largo de la vida (incluido el alfa)
    public final Color startColor = new Color();
    public final Color endColor = new Color();
    public final Color color = new Color();
    // giro del sprite alrededor del eje de vista (radianes) y su velocidad angular (rad/s)
    public float rotation, rotationSpeed;
    // relacion alto/ancho del sprite
    public float aspect = 1f;
    // rectangulo de la textura que se dibuja (u0,v0)-(u1,v1)
    public float u0 = 0f, v0 = 0f, u1 = 1f, v1 = 1f;
    // comportamiento propio: se llama en cada paso, despues de integrar velocidad y posicion
    public Updater updater;
    // true: el updater fija scale y color; no se interpolan startScale/endScale ni startColor/endColor
    public boolean manualAppearance;
    private float elapsedTime;

    /** Comportamiento a medida de una particula (efectos que no encajan en la interpolacion simple). */
    public interface Updater {
        void update(Particle particle, float delta);
    }

    public Particle(float lifetime) {
        this.lifetime = lifetime;
        updateState();
    }

    /** Avanza la simulacion; devuelve false cuando la particula murio. */
    public boolean update(float delta) {
        velocity.addLocal(acceleration.mult(delta));
        if (drag > 0f) {
            velocity.multLocal(1f / (1f + drag * delta));
        }
        position.addLocal(velocity.mult(delta));
        elapsedTime += delta;
        rotation += rotationSpeed * delta;
        if (manualAppearance) {
            liveFactor = Math.min(1f, elapsedTime / lifetime);
        } else {
            updateState();
        }
        if (updater != null) {
            updater.update(this, delta);
        }
        return elapsedTime < lifetime;
    }

    /** Segundos de vida transcurridos. */
    public float getElapsedTime() {
        return elapsedTime;
    }

    private void updateState() {
        liveFactor = Math.min(1f, elapsedTime / lifetime);
        scale = startScale + (endScale - startScale) * liveFactor;
        color.r = lerp(startColor.r, endColor.r, liveFactor);
        color.g = lerp(startColor.g, endColor.g, liveFactor);
        color.b = lerp(startColor.b, endColor.b, liveFactor);
        color.a = lerp(startColor.a, endColor.a, liveFactor);
    }

    private static short lerp(short a, short b, float t) {
        return (short) (a + (b - a) * t);
    }
}
