package com.fss.rpg;

import static com.openpl.PL10.*;

import com.forcex.FX;
import com.forcex.anim.Animation;
import com.forcex.anim.Animator;
import com.forcex.anim.SkeletonNode;
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

    // velocidad maxima de giro del personaje (grados/s): 9 grados por frame de 1/50 s, STAT_PLAYER de pedstats.dat [V]
    private static final float TURN_SPEED = 9.0f * 50.0f;

    // entrada de stick (0..1) con la que se camina solo hacia la puerta del coche: queda en estado "caminar"
    private static final float APPROACH_STICK = 0.4f;

    // movimiento a pie: ratio, estado, animaciones y velocidad como GTA SA
    final PedLocomotion loco = new PedLocomotion();
    // sprint pedido por la interfaz tactil (en escritorio se usa shift)
    boolean sprint_touch = false;
    // entrada analogica del joystick (x derecha, y adelante, -1..1); la fija el juego cada frame
    boolean joystick_active = false;
    float joy_x = 0.0f, joy_y = 0.0f;
    Vector3f camera_forward = new Vector3f(0, 0, 1);

    boolean onFoot = true;
    Animator animator;
    // mano derecha, donde se sujeta el arma
    private SkeletonNode hand_node;
    // el juego lo activa mientras se dispara: el personaje encara la camara
    boolean aiming = false;

    Animation anim_idle, anim_aim;
    private boolean aim_animation = false;
    // weapon.dat del AK47: "6 10 7" fotogramas a 30 fps = inicio y fin del bucle de disparo y fotograma del tiro
    private static final float AIM_LOOP_START = 6f / 30f;
    private static final float AIM_LOOP_END = 10f / 30f;
    private static final float AIM_FIRE_TIME = 7f / 30f;
    private float aim_prev_time = 0.0f;
    // delta de mezcla al entrar y salir de la pose de apuntar [NV: valor propio, SA usa 0.25 en TaskUseGun]
    private static final float AIM_BLEND = 8.0f;
    // true en el frame en que la animacion de apuntar llega al fotograma de disparo
    private boolean shot_event = false;

    /** El juego dispara cuando la animacion lo indica; sin la pose de apuntar (en movimiento) dispara libre. */
    public boolean canShoot() {
        return !aim_animation || shot_event;
    }
    private boolean armed = false;
    private Animation armed_idle, armed_walk, armed_run;
    private Animation unarmed_idle, unarmed_walk, unarmed_run;
    // GTA SA usa la misma animacion de sprint con o sin arma
    private Animation anim_sprint;

    /** Con arma equipada se usan las animaciones de sostenerla; al cambiar vuelve a idle. */
    public void setArmed(boolean value) {
        if(armed == value) {
            return;
        }
        armed = value;
        anim_idle = value ? armed_idle : unarmed_idle;
        loco.setAnimations(anim_idle, value ? armed_walk : unarmed_walk, value ? armed_run : unarmed_run, anim_sprint);
        loco.reset();
        aim_animation = false;
        loco.restart(animator);
    }
    Animation anim_car_open, anim_car_getin, anim_car_getout, anim_car_sit, anim_car_closedoor, anim_car_close;
    // cerrando la puerta desde dentro tras subir (CTaskSimpleCarCloseDoorFromInside en GTA SA)
    private boolean closing_door = false;
    boolean exiting = false;
    // etapa de bajar: 0 CAR_getout, 1 cerrar la puerta desde fuera (CAR_close)
    private int exit_stage = 0;
    // ventanas de la puerta (segundos del clip) de data/handling.cfg, grupo de animacion 0 [V]
    private static final float OPEN_OUT_START = 0.41f, OPEN_OUT_STOP = 0.8f;
    private static final float CLOSE_IN_START = 0.30f, CLOSE_IN_STOP = 0.45f;
    private static final float OPEN_IN_START = 0.06f, OPEN_IN_STOP = 0.43f;
    private static final float CLOSE_OUT_START = 0.20f, CLOSE_OUT_STOP = 0.43f;
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

        SkeletonNode skeleton = dff.getSkeleton(dff.getFrameRoot());
        hand_node = findNode(skeleton, "R Hand");
        animator = new Animator(skeleton, dff.bones.size());
        player_obj.setAnimator(animator);
        IFPReader ifp = new IFPReader("game/ped.ifp");
        for(int i = 0; i < ifp.animations.size();i ++) {
            System.out.println(i + " -> " + ifp.animations.get(i).name);
        }
        // animaciones de CPed::SetMoveAnim: idle, walk, run y sprint
        anim_idle = ifp.getAnimation("IDLE_stance", true);
        unarmed_idle = anim_idle;
        unarmed_walk = ifp.getAnimation("WALK_player", true);
        unarmed_run = ifp.getAnimation("run_player", true);
        anim_sprint = ifp.getAnimation("sprint_civi", true);
        // pose de fusil de GTA SA (el grupo "rifle" de weapon.dat para el AK47); vive en rifle.ifp de anim.img
        anim_aim = new IFPReader("game/rifle.ifp").getAnimation("RIFLE_fire", true);
        // juego de animaciones "armed": sostener un arma a un costado
        armed_idle = ifp.getAnimation("IDLE_armed", true);
        armed_walk = ifp.getAnimation("WALK_armed", true);
        armed_run = ifp.getAnimation("run_armed", true);
        loco.setAnimations(unarmed_idle, unarmed_walk, unarmed_run, anim_sprint);
        anim_car_open =ifp.getAnimation("CAR_open_LHS", false);
        anim_car_getin = ifp.getAnimation("CAR_getin_LHS", false);
        anim_car_getout = ifp.getAnimation("CAR_getout_LHS", false);
        anim_car_sit = ifp.getAnimation("CAR_sit", true);
        anim_car_closedoor = ifp.getAnimation("CAR_closedoor_LHS", false);
        anim_car_close = ifp.getAnimation("CAR_close_LHS", false);

        loco.restart(animator);
    }

    private static SkeletonNode findNode(SkeletonNode node, String name) {
        if(name.equalsIgnoreCase(node.name)) {
            return node;
        }
        for(SkeletonNode child : node.children) {
            SkeletonNode found = findNode(child, name);
            if(found != null) {
                return found;
            }
        }
        return null;
    }

    /** Matriz de la mano derecha en espacio del modelo (ya animada), o null si no hay. */
    public Matrix4f getHandMatrix() {
        return hand_node == null ? null : animator.getBoneMap().get(hand_node);
    }

    public void update(Camera camera, boolean[] keys, float delta) {
        if(!onFoot) {
            updateSeated();
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
            updateExiting(joystick_active || keys[Key.W_KEY] || keys[Key.A_KEY] || keys[Key.S_KEY] || keys[Key.D_KEY]);
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
            // al acercarse a la puerta se camina solo, a paso de caminata
            float stick = approaching ? APPROACH_STICK : (has_input ? magnitude : 0.0f);

            // solo el avance hacia adelante hace que la camara se recoloque; de lado o hacia atras no
            camera_follow_weight = approaching ? 0.0f : Math.max(0.0f, in_y);

            // direccion deseada relativa a la camara (SA: rumbo del stick menos rumbo de la camara)
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

                player_transform.lookAt(player_direction, false);
                plCharacterfv(PL_TRANSFORM, player_transform.data);
            }

            // quieto apuntando: la pose de fusil manda sobre las piernas
            boolean aim_pose = aiming && !approaching && stick <= 0.0f;
            boolean sprint_held = !approaching && (keys[Key.KEY_LEFT_SHIFT] || keys[Key.KEY_RIGHT_SHIFT] || sprint_touch);
            float speed = loco.update(delta, animator, stick, sprint_held, !aim_pose);
            float move = speed * delta;

            // pose de disparo mientras se apunta quieto; al soltar o moverse vuelve a idle / caminar
            shot_event = false;
            if(aim_pose && !loco.isMoving()) {
                if(!aim_animation) {
                    animator.blendTo(anim_aim, false, AIM_BLEND);
                    // como GTA SA: sube el fusil y luego repite solo el tramo de disparo del AK47 (weapon.dat)
                    animator.control.setLoop(false);
                    animator.control.speed = 1.0f;
                    animator.control.play();
                    aim_animation = true;
                    aim_prev_time = 0.0f;
                } else {
                    if(animator.control.time >= AIM_LOOP_END) {
                        animator.control.time = AIM_LOOP_START;
                    }
                    float t = animator.control.time;
                    // el disparo sale al cruzar el fotograma de fuego en cada vuelta del bucle
                    shot_event = aim_prev_time < AIM_FIRE_TIME && t >= AIM_FIRE_TIME;
                    aim_prev_time = t;
                }
            } else if(aim_animation) {
                aim_animation = false;
                if(!loco.isMoving()) {
                    animator.blendTo(anim_idle, false, AIM_BLEND);
                    animator.control.setLoop(true);
                    animator.control.speed = 1.0f;
                    animator.control.play();
                }
            }

            if(aiming && !approaching) {
                // encara la direccion de la camara
                float sin = player_direction.z * camera_forward.x - player_direction.x * camera_forward.z;
                float cos = player_direction.x * camera_forward.x + player_direction.z * camera_forward.z;
                float angle = (float) Math.toDegrees(Math.atan2(sin, cos));
                float max_turn = TURN_SPEED * delta;
                player_direction.rotY(Math.max(-max_turn, Math.min(max_turn, angle)));
                player_direction.normalize();
                player_transform.lookAt(player_direction, false);
                plCharacterfv(PL_TRANSFORM, player_transform.data);
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
        if(camera_idle < CAMERA_FOLLOW_DELAY || loco.speed < 0.5f || camera_follow_weight <= 0.0f) {
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
        loco.reset();

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
        exit_stage = 0;
        closing_door = false;
        door_target = 0.0f;

        loco.reset();

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

    // 0 antes de a, 1 despues de b, lineal entre ambos
    private static float ramp(float x, float a, float b) {
        return Math.max(0.0f, Math.min(1.0f, (x - a) / (b - a)));
    }

    private void updateExiting(boolean has_input) {
        float t = animator.control.time;
        if(exit_stage == 0) {
            // CAR_getout: la puerta se abre desde dentro en la ventana OpenIn de handling.cfg
            door_target = ramp(t, OPEN_IN_START, OPEN_IN_STOP);
            if(!animator.finished()) {
                return;
            }
            // como GTA SA (CTaskSimpleCarCloseDoorFromOutside): al bajar cierra la puerta desde fuera
            exit_stage = 1;
            animator.doAnimation(anim_car_close, false);
            animator.control.setLoop(false);
            animator.control.speed = 1.0f;
            animator.control.play();
            return;
        }
        door_target = 1.0f - ramp(t, CLOSE_OUT_START, CLOSE_OUT_STOP);
        // el jugador puede cortar el cierre moviendose: la puerta se cierra sola
        if(!animator.finished() && !has_input) {
            return;
        }
        door_target = 0.0f;
        exiting = false;
        loco.reset();
        loco.restart(animator);
    }
    private void updateEntering() {
        // la puerta se abre en la ventana OpenOut de handling.cfg (la mano llega a la manija) y queda abierta mientras sube
        door_target = enter_stage == 0 ? ramp(animator.control.time, OPEN_OUT_START, OPEN_OUT_STOP) : 1.0f;
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
            // como GTA SA: ya sentado cierra la puerta desde dentro (CAR_closedoor) antes de sentarse a conducir
            animator.doAnimation(anim_car_closedoor, false);
            animator.control.setLoop(false);
            animator.control.speed = 1.0f;
            animator.control.play();
            closing_door = true;
            if(on_entered != null) {
                on_entered.run();
            }
        }
    }

    // sentado: termina de cerrar la puerta y pasa a la pose de conducir
    private void updateSeated() {
        if(!closing_door) {
            return;
        }
        door_target = 1.0f - ramp(animator.control.time, CLOSE_IN_START, CLOSE_IN_STOP);
        if(!animator.finished()) {
            return;
        }
        closing_door = false;
        door_target = 0.0f;
        animator.doAnimation(anim_car_sit, false);
        animator.control.setLoop(true);
        animator.control.speed = 1.0f;
        animator.control.play();
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
