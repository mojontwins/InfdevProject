package net.minecraft.client.player;

public class MovementInput {
	// Base class for reading player movement input (keyboard, mouse, etc.).
	public float moveStrafe = 0.0F;
	public float moveForward = 0.0F;
	public boolean jump = false;
	/** Sneak: set from the sneak key; slows movement and shields the player from mob targeting. */
	public boolean sneak = false;

	public void updatePlayerMoveState() {
	}

	public void resetKeyState() {
	}

	// Records which movement action a pressed key corresponds to.
	public void checkKeyForMovementInput(int keyCode, boolean isPressed) {
	}
}
