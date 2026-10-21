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
    private float elapsedTime;

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
        updateState();
        return elapsedTime < lifetime;
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
