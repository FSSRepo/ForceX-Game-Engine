package com.forcex.windows;

import static org.lwjgl.glfw.GLFW.*;

import com.forcex.FX;
import com.forcex.app.EventType;
import com.forcex.app.InputListener;
import com.forcex.app.Key;

import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import java.util.ArrayList;

public class InputProcessor {
    public float mouseX, mouseY;
    boolean holdRightButton = false, holdLeftButton = false;
    protected ArrayList<InputListener> inputs;
    private final ForceXApp app;

    public InputProcessor(ForceXApp app) {
        inputs = new ArrayList<>();
        this.app = app;
    }

    protected void init() {
        glfwSetMouseButtonCallback(app.window, (long win, int button, int action, int mods) -> {
            for (InputListener input : inputs) {
                if(action == GLFW_PRESS) {
                    input.onTouch(mouseX, mouseY, EventType.TOUCH_PRESSED, button == GLFW_MOUSE_BUTTON_LEFT ? 0 : 1);
                    if(button == GLFW_MOUSE_BUTTON_LEFT) {
                        holdLeftButton = true;
                    } else if(button == GLFW_MOUSE_BUTTON_RIGHT) {
                        holdRightButton = true;
                    }
                } else if(action == GLFW_RELEASE) {
                    input.onTouch(mouseX, mouseY, EventType.TOUCH_DROPPED, button == GLFW_MOUSE_BUTTON_LEFT ? 0 : 1);
                    if(button == GLFW_MOUSE_BUTTON_LEFT) {
                        holdLeftButton = false;
                    } else if(button == GLFW_MOUSE_BUTTON_RIGHT) {
                        holdRightButton = false;
                    }
                }
            }
        });

        glfwSetCursorPosCallback(app.window, new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double x, double y) {
                for (InputListener input : inputs) {
                    if(holdLeftButton || holdRightButton) {
                        input.onTouch(mouseX, mouseY, EventType.TOUCH_DRAGGING, holdLeftButton ? 0 : 1);
                    } else {
                        input.onTouch((float)x, (float)y, EventType.MOUSE_HOVER, -1);
                    }
                }
                mouseX = (float)x;
                mouseY = (float)y;
            }
        });

        glfwSetKeyCallback(app.window, new GLFWKeyCallback() {
            public void invoke(long window, int key, int scancode, int action, int mods) {
                byte fx_key = getFxKey(key);
                if (fx_key == -1)
                    return;
                for (InputListener input : inputs) {
                    input.onKeyEvent(fx_key, action == GLFW_PRESS || action == GLFW_REPEAT);
                }
            }
        });

        glfwSetScrollCallback(app.window, new GLFWScrollCallback() {
            @Override
            public void invoke(long window, double x, double y) {
                for (InputListener input : inputs) {
                    input.onTouch((float)x, (float)y, EventType.MOUSE_SCROLL, 0);
                }
            }
        });
    }

    public byte getFxKey(int key) {
        switch (key) {
            case GLFW_KEY_A:
                return Key.A_KEY;
            case GLFW_KEY_B:
                return Key.B_KEY;
            case GLFW_KEY_C:
                return Key.C_KEY;
            case GLFW_KEY_D:
                return Key.D_KEY;
            case GLFW_KEY_E:
                return Key.E_KEY;
            case GLFW_KEY_F:
                return Key.F_KEY;
            case GLFW_KEY_G:
                return Key.G_KEY;
            case GLFW_KEY_H:
                return Key.H_KEY;
            case GLFW_KEY_I:
                return Key.I_KEY;
            case GLFW_KEY_J:
                return Key.J_KEY;
            case GLFW_KEY_K:
                return Key.K_KEY;
            case GLFW_KEY_L:
                return Key.L_KEY;
            case GLFW_KEY_M:
                return Key.M_KEY;
            case GLFW_KEY_N:
                return Key.N_KEY;
            case GLFW_KEY_O:
                return Key.O_KEY;
            case GLFW_KEY_P:
                return Key.P_KEY;
            case GLFW_KEY_Q:
                return Key.Q_KEY;
            case GLFW_KEY_R:
                return Key.R_KEY;
            case GLFW_KEY_S:
                return Key.S_KEY;
            case GLFW_KEY_T:
                return Key.T_KEY;
            case GLFW_KEY_V:
                return Key.V_KEY;
            case GLFW_KEY_U:
                return Key.U_KEY;
            case GLFW_KEY_W:
                return Key.W_KEY;
            case GLFW_KEY_X:
                return Key.X_KEY;
            case GLFW_KEY_Y:
                return Key.Y_KEY;
            case GLFW_KEY_Z:
                return Key.Z_KEY;
            case GLFW_KEY_UP:
                return Key.KEY_UP;
            case GLFW_KEY_DOWN:
                return Key.KEY_DOWN;
            case GLFW_KEY_RIGHT:
                return Key.KEY_RIGHT;
            case GLFW_KEY_LEFT:
                return Key.KEY_LEFT;
            case GLFW_KEY_0:
                return Key.KEY_0;
            case GLFW_KEY_1:
                return Key.KEY_1;
            case GLFW_KEY_2:
                return Key.KEY_2;
            case GLFW_KEY_3:
                return Key.KEY_3;
            case GLFW_KEY_4:
                return Key.KEY_4;
            case GLFW_KEY_5:
                return Key.KEY_5;
            case GLFW_KEY_6:
                return Key.KEY_6;
            case GLFW_KEY_7:
                return Key.KEY_7;
            case GLFW_KEY_8:
                return Key.KEY_8;
            case GLFW_KEY_9:
                return Key.KEY_9;
            case GLFW_KEY_ESCAPE:
                return Key.KEY_ESC;
            case GLFW_KEY_BACKSPACE:
                return Key.KEY_DEL;
            case GLFW_KEY_ENTER:
                return Key.KEY_ENTER;
            case GLFW_KEY_SPACE:
                return Key.KEY_SPACE;
            case GLFW_KEY_CAPS_LOCK:
                return Key.KEY_CAPITAL;
            case GLFW_KEY_PERIOD:
                return Key.KEY_DOT;
            case GLFW_KEY_LEFT_SHIFT:
                return Key.KEY_LEFT_SHIFT;
            case GLFW_KEY_RIGHT_SHIFT:
                return Key.KEY_RIGHT_SHIFT;
            case GLFW_KEY_MINUS:
                return Key.KEY_MINUS;
        }
        return -1;
    }
}
