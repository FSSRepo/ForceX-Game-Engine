package com.fss.rpg;

import com.forcex.anim.Animation;
import com.forcex.anim.Animator;
import com.forcex.math.Vector3f;

/**
 * Locomocion a pie como GTA SA (CTaskSimplePlayerOnFoot::PlayerControlZelda + CPed::SetMoveAnim).
 * Ver MECHANICS_GTASA.md, seccion "Movimiento a pie".
 * <ul>
 *   <li>La entrada se escala a la magnitud de stick de SA y el "ratio de movimiento" la sigue a
 *   {@link #RATIO_RATE} por segundo; al soltar el stick se corta de golpe.</li>
 *   <li>El estado (parado, caminar, correr, sprint) sale del ratio y solo se cambia de animacion
 *   cuando cambia el estado, mezclandola con la anterior (blend delta por estado).</li>
 *   <li>La velocidad sale del root motion de la animacion de cada estado, mezclada igual que las
 *   poses, asi el personaje acelera y frena como en el juego y los pies no patinan.</li>
 * </ul>
 */
final class PedLocomotion {
    enum State {
        STAND, WALK, RUN, SPRINT
    }

    // [V] PlayerControlZelda 0x6883d7: m = |stick| / 60 con el stick de -128..128 => 2.1333 a fondo
    static final float STICK_FULL = 128.0f / 60.0f;
    // [V] el ratio sube/baja 0.07 por frame de 1/50 s
    static final float RATIO_RATE = 0.07f * 50.0f;
    // [NV] por encima de este ratio se corre; por debajo se camina
    static final float WALK_MAX_RATIO = 1.0f;

    // [V] CPed::SetMoveAnim 0x5e4a00: blend delta (peso por segundo) de cada estado
    private static final float BLEND_STAND = 4.0f;
    private static final float BLEND_WALK = 1.0f;
    private static final float BLEND_RUN = 1.0f;
    private static final float BLEND_SPRINT = 1.0f;

    // [NV] SetMoveAnimSpeed: clamp(moveSpeed, -0.3, 0.3) + 1.2 - stat * 0.4 / 32768, con stat 0.5 y moveSpeed 0
    private static final float ANIM_SPEED = 1.0f;

    State state = State.STAND;
    // ratio de movimiento suavizado (0..STICK_FULL)
    float ratio = 0.0f;
    // velocidad horizontal actual (m/s)
    float speed = 0.0f;

    private Animation idle, walk, run, sprint;
    private float walk_speed, run_speed, sprint_speed;
    private float speed_from = 0.0f;

    // temporales para medir el root motion
    private final Vector3f p0 = new Vector3f();
    private final Vector3f p1 = new Vector3f();

    /** Define el juego de animaciones (con o sin arma) y mide su velocidad por root motion. */
    void setAnimations(Animation idle, Animation walk, Animation run, Animation sprint) {
        this.idle = idle;
        this.walk = walk;
        this.run = run;
        this.sprint = sprint;
        walk_speed = rootSpeed(walk) * ANIM_SPEED;
        run_speed = rootSpeed(run) * ANIM_SPEED;
        sprint_speed = rootSpeed(sprint) * ANIM_SPEED;
    }

    /** Velocidad hacia adelante (m/s) del ciclo, tomada del desplazamiento de la raiz entre su primer y ultimo fotograma. */
    private float rootSpeed(Animation anim) {
        if(anim == null || anim.getDuration() <= 0.0f
                || !anim.getRootBone().samplePosition(0.0f, p0)
                || !anim.getRootBone().samplePosition(anim.getDuration(), p1)) {
            return 0.0f;
        }
        // IFPReader guarda la posicion como (y, x, z) del archivo: el avance queda en x
        return Math.abs(p1.x - p0.x) / anim.getDuration();
    }

    /** Vuelve a parado sin tocar la animacion (el llamador decide cual reproducir). */
    void reset() {
        state = State.STAND;
        ratio = 0.0f;
        speed = 0.0f;
        speed_from = 0.0f;
    }

    boolean isMoving() {
        return state != State.STAND;
    }

    /** Reproduce el estado actual desde cero, sin mezcla (tras salir de un coche, cambio de arma...). */
    void restart(Animator animator) {
        animator.doAnimation(animFor(state), false);
        animator.control.setLoop(true);
        animator.control.speed = ANIM_SPEED;
        animator.control.play();
    }

    /**
     * Avanza un frame.
     *
     * @param dt          segundos
     * @param stick       magnitud de la entrada normalizada (0..1)
     * @param sprint_held boton de sprint pulsado
     * @param animate     false mientras otra animacion (apuntar) manda sobre las piernas
     * @return velocidad horizontal (m/s) a aplicar en la direccion del personaje
     */
    float update(float dt, Animator animator, float stick, boolean sprint_held, boolean animate) {
        float target = Math.max(0.0f, Math.min(1.0f, stick)) * STICK_FULL;
        if(target <= 0.0f) {
            ratio = 0.0f;
        } else {
            float step = RATIO_RATE * dt;
            ratio += Math.max(-step, Math.min(step, target - ratio));
        }

        State next;
        if(ratio <= 0.0f) {
            next = State.STAND;
        } else if(sprint_held) {
            next = State.SPRINT;
        } else if(ratio > WALK_MAX_RATIO) {
            next = State.RUN;
        } else {
            next = State.WALK;
        }

        if(next != state) {
            speed_from = speed;
            state = next;
            if(animate) {
                // solo se elige otra animacion al cambiar de estado
                animator.blendTo(animFor(state), false, blendFor(state));
                animator.control.setLoop(true);
                animator.control.speed = ANIM_SPEED;
                animator.control.play();
            }
        }

        // la velocidad sigue el mismo peso de mezcla que las poses
        float to = speedFor(state);
        float w = animator.isBlending() ? animator.getBlendWeight() : 1.0f;
        speed = speed_from + (to - speed_from) * w;
        if(!animator.isBlending()) {
            speed_from = to;
        }
        return speed;
    }

    private Animation animFor(State s) {
        switch(s) {
            case WALK:
                return walk;
            case RUN:
                return run;
            case SPRINT:
                return sprint;
            default:
                return idle;
        }
    }

    private float blendFor(State s) {
        switch(s) {
            case WALK:
                return BLEND_WALK;
            case RUN:
                return BLEND_RUN;
            case SPRINT:
                return BLEND_SPRINT;
            default:
                return BLEND_STAND;
        }
    }

    private float speedFor(State s) {
        switch(s) {
            case WALK:
                return walk_speed;
            case RUN:
                return run_speed;
            case SPRINT:
                return sprint_speed;
            default:
                return 0.0f;
        }
    }
}
