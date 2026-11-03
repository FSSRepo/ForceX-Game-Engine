package com.forcex.app;

public interface InputListener {
	public void onKeyEvent(byte key,boolean down);

	public void onTouch(float x, float y, byte type, int pointer);

	/**
	 * Texto Unicode introducido por el usuario (teclado virtual o IME).
	 * <p>
	 * Permite recibir caracteres que no se pueden expresar con {@link Key}
	 * (acentos, símbolos, otros alfabetos). En escritorio no se invoca: las
	 * teclas llegan por {@link #onKeyEvent}.
	 */
	public default void onTextInput(String text) {}
}
