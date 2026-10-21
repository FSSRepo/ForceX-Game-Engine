package com.forcex.rmlui;

import com.forcex.app.EventType;
import com.forcex.app.InputListener;
import com.forcex.app.Key;

/**
 * Adaptador de entrada ForceX → RmlUi.
 * <p>
 * Regístralo en el sistema de listeners que recibe eventos de
 * {@code InputProcessor} (desktop) o {@code AndroidInput}. Los callbacks de
 * ForceX no incluyen modificadores ni caracteres de texto; el adaptador mapea
 * las teclas conocidas y emite texto ASCII básico.
 * <p>
 * Debe usarse en el hilo de render, donde RmlUi recibe la entrada.
 */
public final class RmlUIInput implements InputListener {

    private RmlContext context;
    private int mouseX;
    private int mouseY;
    private int modifiers;
    private boolean leftDown;
    private boolean rightDown;
    private boolean middleDown;
    private boolean leftShiftDown;
    private boolean rightShiftDown;
    private boolean capsLock;

    public RmlUIInput(RmlContext context) {
        setContext(context);
    }

    public RmlUIInput() {
        setContext(RmlUI.getContext());
    }

    /** Cambia el contexto receptor, por ejemplo después de un shutdown/init. */
    public void setContext(RmlContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context no puede ser null");
        }
        this.context = context;
    }

    @Override
    public void onTouch(float x, float y, byte type, int pointer) {
        if (context == null) {
            return;
        }

        int px = Math.round(x);
        int py = Math.round(y);
        switch (type) {
            case EventType.MOUSE_HOVER:
                mouseX = px;
                mouseY = py;
                context.processMouseMove(mouseX, mouseY, modifiers);
                break;
            case EventType.TOUCH_PRESSED:
                mouseX = px;
                mouseY = py;
                context.processMouseMove(mouseX, mouseY, modifiers);
                if (pointer == 0) {
                    leftDown = true;
                    context.processMouseButton(0, true, modifiers);
                } else if (pointer == 1) {
                    rightDown = true;
                    context.processMouseButton(1, true, modifiers);
                } else {
                    middleDown = true;
                    context.processMouseButton(2, true, modifiers);
                }
                break;
            case EventType.TOUCH_DROPPED:
                mouseX = px;
                mouseY = py;
                context.processMouseMove(mouseX, mouseY, modifiers);
                if (pointer == 0 && leftDown) {
                    context.processMouseButton(0, false, modifiers);
                    leftDown = false;
                } else if (pointer == 1 && rightDown) {
                    context.processMouseButton(1, false, modifiers);
                    rightDown = false;
                } else if (middleDown) {
                    context.processMouseButton(2, false, modifiers);
                    middleDown = false;
                }
                break;
            case EventType.TOUCH_DRAGGING:
            case EventType.TOUCH_DRAGGING_2:
                mouseX = px;
                mouseY = py;
                context.processMouseMove(mouseX, mouseY, modifiers);
                break;
            case EventType.TOUCH_PRESSED_2:
            case EventType.TOUCH_DROPPED_2:
                // InputListener no garantiza identidad/posición de todos los
                // punteros activos; no se reinterpretan como botones de ratón.
                break;
            case EventType.MOUSE_SCROLL:
                context.processMouseMove(mouseX, mouseY, modifiers);
                // GLFW reporta rueda positiva hacia arriba; RmlUi espera positivo hacia abajo.
                context.processMouseWheel((float) x, -(float) y, modifiers);
                break;
            default:
                break;
        }
    }

    @Override
    public void onKeyEvent(byte key, boolean down) {
        if (context == null) {
            return;
        }

        int identifier = toRmlKey(key);
        if (identifier == 0) {
            return;
        }

        if (key == Key.KEY_LEFT_SHIFT) {
            leftShiftDown = down;
            setModifier(2, leftShiftDown || rightShiftDown);
        } else if (key == Key.KEY_RIGHT_SHIFT) {
            rightShiftDown = down;
            setModifier(2, leftShiftDown || rightShiftDown);
        } else if (key == Key.KEY_CAPITAL && down) {
            capsLock = !capsLock;
            setModifier(16, capsLock);
        }

        context.processKey(identifier, down, modifiers);

        if (down) {
            int codePoint = toCodePoint(key);
            if (codePoint != 0) {
                context.processText(codePoint);
            }
        }
    }

    private void setModifier(int bit, boolean enabled) {
        if (enabled) {
            modifiers |= bit;
        } else {
            modifiers &= ~bit;
        }
    }

    private int toCodePoint(byte key) {
        if (key >= Key.A_KEY && key <= Key.Z_KEY) {
            boolean upper = ((modifiers & 2) != 0) ^ capsLock;
            return Key.toKeyChar(key, upper);
        }
        if (key >= Key.KEY_0 && key <= Key.KEY_9) {
            return Key.toKeyChar(key, false);
        }
        switch (key) {
            case Key.KEY_SPACE: return ' ';
            case Key.KEY_ENTER: return '\n';
            case Key.KEY_DOT: return '.';
            case Key.KEY_MINUS: return (modifiers & 2) != 0 ? '_' : '-';
            default: return 0;
        }
    }

    private int toRmlKey(byte key) {
        if (key >= Key.A_KEY && key <= Key.Z_KEY) {
            return 12 + (key - Key.A_KEY);
        }
        if (key >= Key.KEY_0 && key <= Key.KEY_9) {
            return 2 + (key - Key.KEY_0);
        }
        switch (key) {
            case Key.KEY_SPACE: return 1;
            case Key.KEY_DEL: return 69;
            case Key.KEY_TAB: return 70;
            case Key.KEY_ENTER: return 72;
            case Key.KEY_CAPITAL: return 74;
            case Key.KEY_ESC: return 81;
            case Key.KEY_UP: return 91;
            case Key.KEY_DOWN: return 93;
            case Key.KEY_RIGHT: return 92;
            case Key.KEY_LEFT: return 90;
            case Key.KEY_LEFT_SHIFT: return 138;
            case Key.KEY_RIGHT_SHIFT: return 139;
            case Key.KEY_DOT: return 42;
            case Key.KEY_MINUS: return 41;
            default: return 0;
        }
    }
}
