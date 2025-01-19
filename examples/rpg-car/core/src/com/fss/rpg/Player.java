package com.fss.rpg;

import static com.openpl.PL10.*;

import com.forcex.FX;
import com.forcex.anim.Animation;
import com.forcex.anim.AnimationControl;
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
    float distance = 14f;

    private static final float PLAYER_SPEED = 8;
    boolean walking_animation = false, walking_start = false;

    boolean onFoot = true;
    Animator animator;

    Animation anim_walk, anim_walk_start, anim_idle, anim_turn_left, anim_turn_right;

    int turning_animation_status = -1;

    public Player(ModelObject player_obj, DFFSDK dff) {
        player_transform.setLocation(93, 7, -60);

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
        IFPReader ifp = new IFPReader("ped.ifp");
        for(int i = 0; i < ifp.animations.size();i ++) {
            System.out.println(i + " -> " + ifp.animations.get(i).name);
        }
        anim_walk = ifp.getAnimation("WALK_player", true);
        anim_walk_start = ifp.getAnimation("WALK_start", true);
        anim_idle = ifp.getAnimation("IDLE_stance", true);

        anim_turn_left = ifp.getAnimation("Turn_L", true);
        anim_turn_right = ifp.getAnimation("Turn_R", true);
//        anim_idle = ifp.getAnimation("run_player", true);

        animator.doAnimation(anim_idle, false);
        animator.control.putCommand(AnimationControl.CMD_LOOP);
        animator.control.speed = 0.8f;
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
            follow_fixed.rotateOnSphereOrigin(distance, rot_x, rot_y, false);
            Vector3f follow = player_position.add(follow_fixed);
            camera.setPosition(follow.x, follow.y, follow.z);
            camera.lookAt(player_position);
        }

        // controls
        {
            plBindCharacter(character);
            float move = 0.0f;
            if(keys[Key.A_KEY]) {
                player_direction.rotY(120 * delta);
                player_transform.lookAt(player_direction, false);
                plCharacterfv(PL_TRANSFORM, player_transform.data);
//                if(turning_animation_status != 1) {
//                    animator.doAnimation(anim_turn_left, false);
//                    turning_animation_status = 1;
//                }
            } else if(keys[Key.D_KEY]) {
                player_direction.rotY(-120 * delta);
                player_transform.lookAt(player_direction, false);
                plCharacterfv(PL_TRANSFORM, player_transform.data);
//                if(turning_animation_status != 2) {
//                    animator.doAnimation(anim_turn_right, false);
//                    turning_animation_status = 2;
//                }
            }
//            else {
//                if(turning_animation_status != -1 && animator.finishedNotify()) {
//                    animator.doAnimation(anim_idle, false);
//                    System.out.println("Animator: idle");
//                    animator.control.putCommand(AnimationControl.CMD_LOOP);
//                    turning_animation_status = -1;
//                }
//            }
            if(keys[Key.W_KEY]) {
                move = PLAYER_SPEED * delta;
            } else if(keys[Key.S_KEY]) {
                move = -PLAYER_SPEED * delta;
            }
            if(move == 0f) {
                if(walking_animation) {
                    if(walking_start) {
                        animator.doAnimation(anim_walk_start, true);
                        animator.control.putCommand(AnimationControl.CMD_NO_LOOP);
                        walking_start = false;
                    } else if(animator.finished()) {
                        animator.control.putCommand(AnimationControl.CMD_PLAY);
                        animator.doAnimation(anim_idle, false);
                        animator.control.putCommand(AnimationControl.CMD_LOOP);
                        walking_animation = false;
                    }
                }
            } else {
                if(!walking_animation) {
                    if(!walking_start) {
                        animator.doAnimation(anim_walk_start, false);
                        animator.control.putCommand(AnimationControl.CMD_NO_LOOP);
                        walking_start = true;
                    } else if(animator.finished()) {
                        animator.control.putCommand(AnimationControl.CMD_PLAY);
                        animator.doAnimation(anim_walk, false);
                        animator.control.putCommand(AnimationControl.CMD_LOOP);
                        walking_animation = true;
                    }
                }
            }
            Vector3f walk = player_direction.mult(walking_animation ?  move : 0);
            plCharacter3f(PL_CHAR_WALK_DIRECTION, walk.x, walk.y, walk.z);
            plBindCharacter(0);
        }
    }

    public void render(ModelRenderer renderer) {
        if(!onFoot) {
            return;
        }
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
