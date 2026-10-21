package com.forcex.gui.widgets;

import com.forcex.FX;
import com.forcex.app.EventType;
import com.forcex.core.GL;
import com.forcex.gui.Drawer;
import com.forcex.gui.View;
import com.forcex.math.Maths;
import com.forcex.math.Vector2f;
import com.forcex.utils.Color;
import com.forcex.utils.GameUtils;

public class Joystick extends View {
    public static final byte LEFT_DIR = 1;
    public static final byte RIGHT_DIR = 2;
    public static final byte UP_DIR = 3;
    public static final byte DOWN_DIR = 4;
    public static final byte LEFT_UP_DIR = 5;
    public static final byte LEFT_DOWN_DIR = 6;
    public static final byte RIGHT_UP_DIR = 7;
    public static final byte RIGHT_DOWN_DIR = 8;
    GL gl = FX.gl;
    float radius;
    boolean request_delete = false;
    Color color;
    private float distance, angle, min_dist;
    private int texture = -1, knob_texture = -1;
    private Vector2f pivot, delta;
    private boolean touch = false;

    public Joystick(float radius) {
        // width/height es el tamaño completo; el radio indicado es el radio en píxeles.
        setWidth(radius * 2f);
        setHeight(radius * 2f);
        this.radius = radius * 1.5f;
        pivot = new Vector2f();
        delta = new Vector2f();
        color = new Color(220, 220, 220, 255);
        min_dist = this.radius * 0.2f;
    }

    public float getAngle() {
        return angle;
    }

    @Override
    public void onTouch(float x, float y, byte type) {
        // y crece hacia abajo: se invierte para conservar el cálculo angular.
        delta.set(x - local.x, local.y - y);
        distance = delta.length();
        angle = calculateAngle(delta.x, delta.y);
        switch (type) {
            case EventType.TOUCH_PRESSED:
                pivot.set(x, y);
                touch = true;
                break;
            case EventType.TOUCH_DRAGGING:
                if (distance <= radius) {
                    pivot.set(x, y);
                } else {
                    // El dedo sigue controlando el stick: el pivote se limita a la circunferencia.
                    float cs = (Maths.cos(angle * Maths.toRadians) * radius) + local.x;
                    float sn = local.y - (Maths.sin(angle * Maths.toRadians) * radius);
                    pivot.set(cs, sn);
                }
                break;
            case EventType.TOUCH_DROPPED:
                touch = false;
                break;
        }
    }

    public void setTextureID(int id) {
        texture = id;
    }

    /** Textura de la perilla; si no se define se usa la de la base. */
    public void setKnobTextureID(int id) {
        knob_texture = id;
    }

    @Override
    public boolean testTouch(float x, float y) {
        if (isVisible() && GameUtils.testRect(x, y, local, extent.x * 1.5f, extent.y * 1.5f)) {
            return true;
        } else {
            pivot.set(local);
            return false;
        }
    }

    @Override
    public void onDraw(Drawer drawer) {
        drawer.setScale(extent.x, extent.y);
        drawer.renderQuad(local, null, texture);
        drawer.setScale(extent.x * 0.5f, extent.y * 0.5f);
        drawer.renderQuad(pivot, color, knob_texture != -1 ? knob_texture : texture);
        if (!touch) {
            pivot.set(local);
        }
    }

    @Override
    public void onDestroy() {
        setVisibility(INVISIBLE);
        if (texture != -1) {
            gl.glDeleteTexture(texture);
        }
        pivot = null;
        delta = null;
        color = null;
    }

    public boolean isTouching() {
        return touch;
    }

    public float porcent() {
        return distance / radius;
    }

    /** Eje horizontal analógico (-1 izquierda, 1 derecha); 0 si no se toca o está dentro de la zona muerta. */
    public float getAxisX() {
        return axis(Maths.cos(angle * Maths.toRadians));
    }

    /** Eje vertical analógico (-1 abajo, 1 arriba); 0 si no se toca o está dentro de la zona muerta. */
    public float getAxisY() {
        return axis(Maths.sin(angle * Maths.toRadians));
    }

    private float axis(float component) {
        if (!touch || distance <= min_dist) {
            return 0f;
        }
        // se reescala para que la magnitud crezca suavemente desde 0 al salir de la zona muerta
        float strength = Math.min(1f, (distance - min_dist) / (radius - min_dist));
        return component * strength;
    }

    public byte get8Direction() {
        float angle = (360f - this.angle) % 360f; // las comparaciones usan 270 = arriba
        if (distance > min_dist && touch) {
            if (angle >= 247.5 && angle < 292.5) {
                return UP_DIR;
            } else if (angle >= 292.5 && angle < 337.5) {
                return RIGHT_UP_DIR;
            } else if (angle >= 337.5 || angle < 22.5) {
                return RIGHT_DIR;
            } else if (angle >= 22.5 && angle < 67.5) {
                return RIGHT_DOWN_DIR;
            } else if (angle >= 67.5 && angle < 112.5) {
                return DOWN_DIR;
            } else if (angle >= 112.5 && angle < 157.5) {
                return LEFT_DOWN_DIR;
            } else if (angle >= 157.5 && angle < 202.5) {
                return LEFT_DIR;
            } else if (angle >= 202.5 && angle < 247.5) {
                return LEFT_UP_DIR;
            }
        } else if (distance <= min_dist && touch) {
            return 0;
        }
        return -1;
    }

    public byte get4Direction() {
        float angle = (360f - this.angle) % 360f;
        if (distance > min_dist && touch) {
            if (angle >= 225 && angle < 315) {
                return UP_DIR;
            } else if (angle >= 315 || angle < 45) {
                return RIGHT_DIR;
            } else if (angle >= 45 && angle < 135) {
                return DOWN_DIR;
            } else if (angle >= 135 && angle < 225) {
                return LEFT_DIR;
            }
        } else if (distance <= min_dist && touch) {
            return 0;
        }
        return -1;
    }

    private float calculateAngle(float x, float y) {
        if (x >= 0 && y >= 0)
            return Maths.atan(y / x) * Maths.toDegrees;
        else if (x < 0 && y >= 0)
            return (Maths.atan(y / x) * Maths.toDegrees) + 180;
        else if (x < 0 && y < 0)
            return (Maths.atan(y / x) * Maths.toDegrees) + 180;
        else if (x >= 0 && y < 0)
            return (Maths.atan(y / x) * Maths.toDegrees) + 360;
        return 0;
    }
}
