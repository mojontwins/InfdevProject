package net.minecraft.client.player;

public class MovementInput {
	public float moveStrafe = 0.0F;
	public float moveForward = 0.0F;
	public boolean jump = false;
	/** Sneak: set from the sneak key; slows movement and shields the player from mob targeting. */
	public boolean sneak = false;

	public void updatePlayerMoveState() {
	}

	public void resetKeyState() {
	}

	public void checkKeyForMovementInput(int var1, boolean var2) {
	}
}
