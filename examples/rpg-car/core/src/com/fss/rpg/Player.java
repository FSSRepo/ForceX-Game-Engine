package com.fss.rpg;

import static com.openpl.PL10.*;

import com.forcex.FX;
import com.forcex.anim.Animation;
import com.forcex.anim.Animator;
import com.forcex.app.Key;
import com.forcex.core.gpu.Texture;
import com.forcex.gfx3d.Camera;
import com.forcex.gfx3d.ModelObject;
import com.forcex.gfx3d.ModelRenderer;
import com.forcex.gfx3d.shapes.Cylinder;
import com.forcex.gtasdk.DFFSDK;
import com.forcex.gtasdk.IFPReader;
import com.forcex.math.Matrix4f;
import com.forcex.math.Quaternion;
import com.forcex.math.Vector3f;

public class Player {
    int character;
    ModelObject player;

    Matrix4f player_transform = new Matrix4f();
    Matrix4f seat_transform = new Matrix4f();
    private final CameraCollision camera_collision = new CameraCollision();
    Matrix4f fix_transform = Matrix4f.fromRotation(Quaternion.fromEulerAngles(-90, 180, 0));
    Vector3f player_direction = new Vector3f(0, 0, 1);
    Quaternion start_cam_rot = new Quaternion();
    Quaternion end_cam_rot;
    Quaternion tmp_rot = new Quaternion();
    float refresh_cam_time = 0.0f;
    float camera_blend = -1.0f;
    float CAM_REORIENT_TIME = 1.4f;
    Vector3f follow_fixed = new Vector3f(0, 0, 0);
    float rot_x = 180;
    float rot_y = 20;

    // camara automatica: espera tras mover la camara a mano, velocidad de giro (1/s) y limites de inclinacion
    private static final float CAMERA_FOLLOW_DELAY = 1.2f;
    private static final float CAMERA_FOLLOW_SMOOTH = 2.0f;
    private static final float CAMERA_PITCH_SMOOTH = 1.0f;
    private static final float CAMERA_DEFAULT_PITCH = 20f;
    private static final float CAMERA_MIN_PITCH = 0f;
    private static final float CAMERA_MAX_PITCH = 85f;
    // segundos desde que se toco la camara por ultima vez; empieza alto para que siga desde el inicio
    private float camera_idle = CAMERA_FOLLOW_DELAY;
    // cuanto de la entrada es hacia adelante (0..1), fijado por el control en el frame anterior
    private float camera_follow_weight = 0.0f;
    float distance = 14f;

    // velocidades de desplazamiento
    private static final float WALK_SPEED = 3f;
    private static final float RUN_SPEED = 6.5f;

    // aceleracion y frenado (unidades/s^2)
    private static final float ACCELERATION = 14f;
    private static final float DECELERATION = 20f;

    // velocidad maxima de giro del personaje hacia la direccion deseada (grados/s)
    private static final float TURN_SPEED = 480f;

    // entrada (0..1) a partir de la cual se considera "a fondo" y se puede correr
    private static final float RUN_INPUT_THRESHOLD = 0.7f;
    // tiempo manteniendo la entrada a fondo para pasar de caminar a correr
    private static final float RUN_HOLD_TIME = 0.35f;

    // velocidad de animacion base y limites al escalarla con la velocidad real
    private static final float ANIM_BASE_SPEED = 0.8f;
    private static final float ANIM_MIN_SCALE = 0.6f;
    private static final float ANIM_MAX_SCALE = 1.3f;

    boolean walking_animation = false, walking_start = false;
    boolean run_animation = false, running = false;
    boolean stopping = false;
    float w_hold_time = 0.0f;
    float current_speed = 0.0f;
    // entrada analogica del joystick (x derecha, y adelante, -1..1); la fija el juego cada frame
    boolean joystick_active = false;
    float joy_x = 0.0f, joy_y = 0.0f;
    Vector3f camera_forward = new Vector3f(0, 0, 1);

    boolean onFoot = true;
    Animator animator;

    Animation anim_walk, anim_walk_start, anim_idle, anim_run, anim_run_stop;
    Animation anim_car_open, anim_car_getin, anim_car_getout, anim_car_sit;
    boolean exiting = false;
    // caminar hasta la puerta antes de abrirla
    private boolean approaching = false;
    private float approach_time = 0.0f;
    private final Vector3f approach_target = new Vector3f();
    private final Vector3f approach_facing = new Vector3f();
    private static final float APPROACH_REACH = 0.3f;
    private static final float APPROACH_TIMEOUT = 4f;
    // apertura objetivo (0 cerrada, 1 abierta) de la puerta del auto que el juego anima
    float door_target = 0.0f;

    // secuencia de subir al auto: 0 abrir puerta, 1 entrar
    boolean entering = false;
    private int enter_stage = 0;
    private Runnable on_entered;

    public Player(ModelObject player_obj, DFFSDK dff) {
        player_transform.setLocation(23, 3, 19);

        character = plGenCharacter();

        // radius 0.4, height 1.0
        plCharacter3f(PL_CHAR_CAPSULE_Y, 0.4f, 1.1f, 0);
        plCharacter3f(PL_CHAR_UPVEC, 0, 1, 0);
        plCharacterf(PL_CHAR_STEP_HEIGHT, 0.35f);
        plCreate(PL_CHARACTER);

        plDynamicWorldi(PL_ADD_CHARACTER, character);

        plBindCharacter(character);
        plCharacterfv(PL_TRANSFORM, player_transform.data);
        plCharacterf(PL_CHAR_MAX_JUMP_HEIGHT, 2f);
        plBindCharacter(0);

        player = player_obj;

        animator = new Animator(dff.getSkeleton(dff.getFrameRoot()), dff.bones.size());
        player_obj.setAnimator(animator);
        IFPReader ifp = new IFPReader("game/ped.ifp");
        for(int i = 0; i < ifp.animations.size();i ++) {
            System.out.println(i + " -> " + ifp.animations.get(i).name);
        }
        anim_walk = ifp.getAnimation("WALK_player", true);
        anim_walk_start = ifp.getAnimation("WALK_start", true);
        anim_run = ifp.getAnimation("run_player", true);
        anim_run_stop = ifp.getAnimation("Run_stop", true);
        anim_idle = ifp.getAnimation("IDLE_stance", true);
        anim_car_open = ifp.getAnimation("CAR_open_LHS", false);
        anim_car_getin = ifp.getAnimation("CAR_getin_LHS", false);
        anim_car_getout = ifp.getAnimation("CAR_getout_LHS", false);
        anim_car_sit = ifp.getAnimation("CAR_sit", true);

//        anim_idle = ifp.getAnimation("run_player", true);

        animator.doAnimation(anim_idle, false);
        animator.control.setLoop(true);
        animator.control.speed = ANIM_BASE_SPEED;
    }

    public void update(Camera camera, boolean[] keys, float delta) {
        if(!onFoot) {
            return;
        }
        plGetCharacterfv(character, PL_TRANSFORM, player_transform.data, 16);
        player.setTransform(player_transform.multLocal(fix_transform));

        // camera follow
        {
            Vector3f player_position = player.getPosition();
//            end_cam_rot = player_transform.getRotation(tmp_rot);
//            if (refresh_cam_time == 0.0f) {
//                start_cam_rot.set(end_cam_rot);
//            }
//            Quaternion cur = start_cam_rot;
//
//            if (refresh_cam_time > 0.3f) {
//                if (camera_blend >= 0.0f) {
//                    if (camera_blend > CAM_REORIENT_TIME) {
//                        cur = end_cam_rot;
//                        start_cam_rot.set(end_cam_rot);
//                        refresh_cam_time = 0.0f;
//                        camera_blend = -1.0f;
//                    } else {
//                        cur = start_cam_rot.slerp(end_cam_rot, camera_blend / CAM_REORIENT_TIME);
//                        camera_blend += delta;
//                    }
//                } else if (!cur.equals(end_cam_rot)) {
//                    camera_blend = 0.0f;
//                }
//            }
//
//            refresh_cam_time += delta;
//
//            Matrix3f cam_orientation = cur.toMatrix();
//            Vector3f local_follow = .mult();
            updateAutoFollow(delta);
            follow_fixed.rotateOnSphereOrigin(distance, rot_x, rot_y, false);

            // direccion horizontal hacia la que apunta la camara (de la camara al personaje)
            camera_forward.set(-follow_fixed.x, 0.0f, -follow_fixed.z);
            camera_forward.normalize();

            // el rayo parte de la altura de la cabeza; el propio personaje (fantasma) no bloquea
            Vector3f follow = camera_collision.resolve(
                    player_position.add(new Vector3f(0, 0.5f, 0)), player_position.add(follow_fixed), 0.0f, delta);

            camera.setPosition(follow.x, follow.y, follow.z);
            camera.lookAt(player_position);
        }

        if(entering && !approaching) {
            updateEntering();
            return;
        }
        if(exiting) {
            updateExiting();
            return;
        }

        // controls
        {
            plBindCharacter(character);

            // entrada: joystick analogico si se esta tocando, si no teclado (x derecha, y adelante)
            float in_x, in_y;
            float auto_x = 0.0f, auto_z = 0.0f;
            if(approaching) {
                // camina solo hacia la puerta a paso de caminata
                Vector3f pos = player.getPosition();
                float to_x = approach_target.x - pos.x;
                float to_z = approach_target.z - pos.z;
                float dist = (float) Math.sqrt(to_x * to_x + to_z * to_z);
                approach_time += delta;
                if(dist < APPROACH_REACH || approach_time > APPROACH_TIMEOUT) {
                    beginDoorSequence();
                    plCharacter3f(PL_CHAR_WALK_DIRECTION, 0, 0, 0);
                    plBindCharacter(0);
                    return;
                }
                auto_x = to_x / dist;
                auto_z = to_z / dist;
                in_x = 0.0f;
                in_y = 0.6f;
            } else if(joystick_active) {
                in_x = joy_x;
                in_y = joy_y;
            } else {
                in_x = (keys[Key.D_KEY] ? 1.0f : 0.0f) - (keys[Key.A_KEY] ? 1.0f : 0.0f);
                in_y = (keys[Key.W_KEY] ? 1.0f : 0.0f) - (keys[Key.S_KEY] ? 1.0f : 0.0f);
            }
            float magnitude = (float) Math.sqrt(in_x * in_x + in_y * in_y);
            boolean has_input = magnitude > 0.01f;
            if(magnitude > 1.0f) {
                in_x /= magnitude;
                in_y /= magnitude;
                magnitude = 1.0f;
            }

            // solo el avance hacia adelante hace que la camara se recoloque; de lado o hacia atras no
            camera_follow_weight = approaching ? 0.0f : Math.max(0.0f, in_y);

            // mantener la entrada a fondo: primero camina y, tras RUN_HOLD_TIME, corre
            if(magnitude >= RUN_INPUT_THRESHOLD) {
                w_hold_time += delta;
                running = w_hold_time >= RUN_HOLD_TIME;
            } else {
                w_hold_time = 0.0f;
                running = false;
            }

            // direccion deseada relativa a la camara
            float facing_dot = 1.0f;
            if(has_input) {
                float right_x = -camera_forward.z;
                float right_z = camera_forward.x;
                float des_x = approaching ? auto_x : camera_forward.x * in_y + right_x * in_x;
                float des_z = approaching ? auto_z : camera_forward.z * in_y + right_z * in_x;
                float des_len = (float) Math.sqrt(des_x * des_x + des_z * des_z);
                des_x /= des_len;
                des_z /= des_len;

                // girar progresivamente hacia la direccion deseada por el camino mas corto
                float sin = player_direction.z * des_x - player_direction.x * des_z;
                float cos = player_direction.x * des_x + player_direction.z * des_z;
                float angle = (float) Math.toDegrees(Math.atan2(sin, cos));
                float max_turn = TURN_SPEED * delta;
                player_direction.rotY(Math.max(-max_turn, Math.min(max_turn, angle)));
                player_direction.normalize();
                facing_dot = Math.max(0.0f, player_direction.x * des_x + player_direction.z * des_z);

                player_transform.lookAt(player_direction, false);
                plCharacterfv(PL_TRANSFORM, player_transform.data);
            }

            // velocidad objetivo: proporcional a la entrada y reducida mientras el personaje aun esta girando
            float target_speed = 0.0f;
            if(has_input) {
                target_speed = running ? RUN_SPEED : WALK_SPEED * Math.max(0.4f, Math.min(1.0f, magnitude / RUN_INPUT_THRESHOLD));
                target_speed *= 0.15f + 0.85f * facing_dot;
            }
            if(target_speed > current_speed) {
                current_speed = Math.min(target_speed, current_speed + ACCELERATION * delta);
            } else {
                current_speed = Math.max(target_speed, current_speed - DECELERATION * delta);
            }
            boolean moving = current_speed > 0.15f;
            float move = current_speed * delta;

            // animaciones, guiadas por la velocidad real (con histeresis entre caminar y correr)
            boolean run_state = moving && current_speed > (run_animation ? WALK_SPEED * 1.15f : WALK_SPEED * 1.5f);
            if(!moving) {
                if(stopping) {
                    // esperar a que termine la transicion de parada antes de volver a idle
                    if(animator.finished()) {
                        animator.doAnimation(anim_idle, false);
                        animator.control.setLoop(true);
                        animator.control.speed = ANIM_BASE_SPEED;
                        animator.control.play();
                        stopping = false;
                    }
                } else if(walking_animation || walking_start || run_animation) {
                    // transicion de parada: reversa de WALK_start al caminar, Run_stop al correr
                    animator.doAnimation(run_animation ? anim_run_stop : anim_walk_start, !run_animation);
                    animator.control.setLoop(false);
                    animator.control.speed = ANIM_BASE_SPEED;
                    animator.control.play();
                    walking_animation = false;
                    walking_start = false;
                    run_animation = false;
                    stopping = true;
                }
            } else {
                stopping = false;
                if(run_state) {
                    if(!run_animation) {
                        // mantener la fase del ciclo al pasar de caminar a correr
                        float phase = walking_animation ? animationPhase() : 0.0f;
                        animator.doAnimation(anim_run, false);
                        animator.control.time = phase * anim_run.getDuration();
                        animator.control.setLoop(true);
                        animator.control.play();
                        run_animation = true;
                        walking_animation = false;
                        walking_start = false;
                    }
                } else if(run_animation) {
                    // mantener la fase del ciclo al pasar de correr a caminar
                    float phase = animationPhase();
                    animator.doAnimation(anim_walk, false);
                    animator.control.time = phase * anim_walk.getDuration();
                    animator.control.setLoop(true);
                    animator.control.play();
                    run_animation = false;
                    walking_animation = true;
                } else if(!walking_animation) {
                    if(!walking_start) {
                        animator.doAnimation(anim_walk_start, false);
                        animator.control.setLoop(false);
                        animator.control.play();
                        walking_start = true;
                    } else if(animator.finished()) {
                        animator.doAnimation(anim_walk, false);
                        animator.control.setLoop(true);
                        animator.control.play();
                        walking_animation = true;
                    }
                }

                // ritmo de pasos acorde a la velocidad real para evitar que los pies patinen
                if(walking_animation || run_animation) {
                    float reference = run_animation ? RUN_SPEED : WALK_SPEED;
                    float scale = Math.max(ANIM_MIN_SCALE, Math.min(ANIM_MAX_SCALE, current_speed / reference));
                    animator.control.speed = ANIM_BASE_SPEED * scale;
                }
            }

            Vector3f walk = player_direction.mult(move);
            plCharacter3f(PL_CHAR_WALK_DIRECTION, walk.x, walk.y, walk.z);
            plBindCharacter(0);
        }
    }

    // la camara se coloca sola detras del personaje al avanzar, pasado un tiempo sin tocarla
    private void updateAutoFollow(float delta) {
        camera_idle += delta;
        rot_y = Math.max(CAMERA_MIN_PITCH, Math.min(CAMERA_MAX_PITCH, rot_y));
        if(camera_idle < CAMERA_FOLLOW_DELAY || current_speed < 0.5f || camera_follow_weight <= 0.0f) {
            return;
        }
        // angulo orbital que deja a la camara exactamente detras de la direccion del personaje
        float target_x = (float) Math.toDegrees(Math.atan2(-player_direction.x, -player_direction.z));
        float diff = ((target_x - rot_x) % 360.0f + 540.0f) % 360.0f - 180.0f;
        float blend = Math.min(1.0f, CAMERA_FOLLOW_SMOOTH * camera_follow_weight * delta);
        rot_x += diff * blend;
        // y vuelve poco a poco a la inclinacion normal
        rot_y += (CAMERA_DEFAULT_PITCH - rot_y) * Math.min(1.0f, CAMERA_PITCH_SMOOTH * camera_follow_weight * delta);
    }

    /** Llamar cuando el jugador mueve la camara a mano: pausa el recolocado automatico. */
    public void onCameraInput() {
        camera_idle = 0.0f;
    }

    // camina hasta la puerta, la abre y entra; luego llama a on_entered
    public void startEnterVehicle(Vector3f door_position, Vector3f facing, Runnable on_entered) {
        entering = true;
        approaching = true;
        approach_time = 0.0f;
        enter_stage = 0;
        approach_target.set(door_position);
        approach_facing.set(facing);
        this.on_entered = on_entered;
    }

    // al llegar a la puerta: se alinea exactamente y reproduce abrir puerta
    private void beginDoorSequence() {
        approaching = false;
        current_speed = 0.0f;
        running = false;
        w_hold_time = 0.0f;
        walking_animation = walking_start = run_animation = stopping = false;

        player_direction.set(approach_facing);
        player_transform.setLocation(approach_target.x, approach_target.y, approach_target.z);
        player_transform.lookAt(player_direction, false);
        plCharacterfv(PL_TRANSFORM, player_transform.data);

        door_target = 1.0f;
        animator.doAnimation(anim_car_open, false);
        animator.control.setLoop(false);
        animator.control.speed = 1.0f;
        animator.control.play();
    }

    // coloca al personaje junto a la puerta y reproduce bajar del auto; al terminar recupera el control
    public void startExitVehicle(Vector3f door_position, Vector3f facing) {
        exiting = true;
        door_target = 1.0f;

        current_speed = 0.0f;
        running = false;
        w_hold_time = 0.0f;
        walking_animation = walking_start = run_animation = stopping = false;

        player_direction.set(facing);
        player_transform.setLocation(door_position.x, door_position.y, door_position.z);
        player_transform.lookAt(player_direction, false);
        plBindCharacter(character);
        plCharacterfv(PL_TRANSFORM, player_transform.data);
        plCharacter3f(PL_CHAR_WALK_DIRECTION, 0, 0, 0);
        plBindCharacter(0);

        animator.doAnimation(anim_car_getout, false);
        animator.control.setLoop(false);
        animator.control.speed = 1.0f;
        animator.control.play();
    }

    private void updateExiting() {
        if(animator.control.time > anim_car_getout.getDuration() * 0.6f) {
            door_target = 0.0f;
        }
        if(!animator.finished()) {
            return;
        }
        door_target = 0.0f;
        exiting = false;
        animator.doAnimation(anim_idle, false);
        animator.control.setLoop(true);
        animator.control.speed = ANIM_BASE_SPEED;
        animator.control.play();
    }

    private void updateEntering() {
        if(!animator.finished()) {
            return;
        }
        if(enter_stage == 0) {
            enter_stage = 1;
            animator.doAnimation(anim_car_getin, false);
            animator.control.setLoop(false);
            animator.control.speed = 1.0f;
            animator.control.play();
        } else {
            entering = false;
            door_target = 0.0f;
            // queda sentado en el asiento mientras conduce (ver renderSeated)
            animator.doAnimation(anim_car_sit, false);
            animator.control.setLoop(true);
            animator.control.speed = ANIM_BASE_SPEED;
            animator.control.play();
            if(on_entered != null) {
                on_entered.run();
            }
        }
    }

    // fase normalizada (0..1) de la animacion actual, para alinear ciclos al cambiar de estado
    private float animationPhase() {
        Animation current = animator.getAnimation();
        if(current == null) {
            return 0.0f;
        }
        float duration = current.getDuration();
        return duration > 0.0f ? animator.control.time / duration : 0.0f;
    }

    public void render(ModelRenderer renderer) {
        if(!onFoot) {
            return;
        }
        renderer.render(player);
    }

    // dibuja al personaje sentado en el asiento del conductor, orientado hacia donde avanza el auto
    public void renderSeated(ModelRenderer renderer, Vector3f seat, Vector3f forward) {
        seat_transform.setLocation(seat.x, seat.y, seat.z);
        seat_transform.lookAt(forward, false);
        player.setTransform(seat_transform.multLocal(fix_transform));
        renderer.render(player);
    }

    public void control(int key, boolean down) {
        if(!onFoot) {
            return;
        }
        if(down) {
            plBindCharacter(character);
            if(key == Key.Q_KEY) {
                if(plGetCharacterb(character, PL_CHAR_CAN_JUMP)) {
                    plBindCharacter(character);
                    plCharacterf(PL_CHAR_JUMP, 0);
                }
                return;
            }
            plBindCharacter(0);
        }
    }
}
