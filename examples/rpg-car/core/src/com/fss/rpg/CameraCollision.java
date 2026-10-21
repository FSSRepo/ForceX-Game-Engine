package com.fss.rpg;

import static com.openpl.PL10.PL_NUM_RAY_RESULT;
import static com.openpl.PL10.PL_RAY_ALL_TEST;
import static com.openpl.PL10.PL_RAY_POINT_RESULT;
import static com.openpl.PL10.plDynamicWorldfv;
import static com.openpl.PL10.plGetDynamicWorldfv;
import static com.openpl.PL10.plGetDynamicWorldi;

import com.forcex.math.Vector3f;

/**
 * Evita que la camara de seguimiento atraviese terreno, arboles, chimeneas u otros autos:
 * lanza un rayo desde el objetivo hacia la posicion deseada y acerca la camara al primer obstaculo.
 * Se acerca al instante pero se aleja de forma gradual para no dar tirones.
 */
public class CameraCollision {
    // separacion con la superficie golpeada (cubre el plano cercano de la camara)
    private static final float MARGIN = 0.45f;
    // distancia minima al objetivo aunque haya un obstaculo pegado
    private static final float MIN_DISTANCE = 1.4f;
    // velocidad (m/s) con que la camara vuelve a su distancia normal cuando se libera
    private static final float RECOVER_SPEED = 9f;
    // debe ser >= PL_MAX_RAY_RESULTS del motor fisico
    private static final int MAX_RESULTS = 10;

    private final float[] ray = new float[6];
    private final float[] points = new float[MAX_RESULTS * 3];
    private final Vector3f result = new Vector3f();
    // fraccion (0..1) de la distancia deseada que se esta usando
    private float clearance = 1.0f;

    /**
     * @param target        punto que mira la camara (donde empieza el rayo)
     * @param desired       posicion ideal de la camara sin obstaculos
     * @param ignoreRadius  los impactos a menos de esta distancia del objetivo se ignoran
     *                      (para no chocar con el propio auto que se sigue)
     * @return posicion de la camara ya corregida; el vector devuelto se reutiliza entre llamadas
     */
    public Vector3f resolve(Vector3f target, Vector3f desired, float ignoreRadius, float delta) {
        float dx = desired.x - target.x;
        float dy = desired.y - target.y;
        float dz = desired.z - target.z;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.0001f) {
            return result.set(desired);
        }

        ray[0] = target.x;
        ray[1] = target.y;
        ray[2] = target.z;
        ray[3] = desired.x;
        ray[4] = desired.y;
        ray[5] = desired.z;
        plDynamicWorldfv(PL_RAY_ALL_TEST, ray, 6);

        float limit = length;
        int count = Math.min(plGetDynamicWorldi(PL_NUM_RAY_RESULT), MAX_RESULTS);
        if (count > 0) {
            plGetDynamicWorldfv(PL_RAY_POINT_RESULT, points, count * 3);
            for (int i = 0; i < count; i++) {
                float hx = points[i * 3] - target.x;
                float hy = points[i * 3 + 1] - target.y;
                float hz = points[i * 3 + 2] - target.z;
                float hit_distance = (float) Math.sqrt(hx * hx + hy * hy + hz * hz);
                if (hit_distance < ignoreRadius) {
                    continue;
                }
                limit = Math.min(limit, hit_distance - MARGIN);
            }
        }
        limit = Math.max(limit, Math.min(MIN_DISTANCE, length));

        float allowed = limit / length;
        if (allowed < clearance) {
            clearance = allowed;
        } else {
            clearance = Math.min(allowed, clearance + RECOVER_SPEED * delta / length);
        }

        return result.set(target.x + dx * clearance, target.y + dy * clearance, target.z + dz * clearance);
    }
}
