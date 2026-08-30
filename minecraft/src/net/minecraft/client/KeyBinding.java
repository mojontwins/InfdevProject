package net.minecraft.client;

/**
 * Binds a user-facing description to a physical key code so that keyboard
 * options can be displayed and configured.
 */
public final class KeyBinding {
	public String keyDescription;
	public int keyCode;

	public KeyBinding(String keyDescription, int keyCode) {
		this.keyDescription = keyDescription;
		this.keyCode = keyCode;
	}
}
