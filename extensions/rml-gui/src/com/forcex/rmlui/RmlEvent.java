package com.forcex.rmlui;

/**
 * Evento RML entregado a los listeners de un {@link RmlDocument}.
 */
public final class RmlEvent {

    private final String type;
    private final String targetId;
    private final String currentId;

    RmlEvent(String type, String targetId, String currentId) {
        this.type = type;
        this.targetId = targetId;
        this.currentId = currentId;
    }

    /** Tipo de evento ({@code click}, {@code change}, {@code submit}, ...). */
    public String getType() {
        return type;
    }

    /** {@code id} del elemento que originó el evento. */
    public String getTargetId() {
        return targetId;
    }

    /** {@code id} del elemento que está procesando el evento durante la propagación. */
    public String getCurrentId() {
        return currentId;
    }

    @Override
    public String toString() {
        return "RmlEvent{type='" + type + "', target='" + targetId + "', current='" + currentId + "'}";
    }
}
