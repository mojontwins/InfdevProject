package net.minecraft.client.player;

import net.minecraft.client.GameSettings;

public final class MovementInputFromOptions extends MovementInput {
	// Reads movement from the configured key bindings.
	private boolean[] movementKeyStates = new boolean[10];
	private GameSettings gameSettings;

	public MovementInputFromOptions(GameSettings gameSettings) {
		this.gameSettings = gameSettings;
	}

	// Track which movement key (forward/back/left/right/jump/sneak) was pressed or released.
	public final void checkKeyForMovementInput(int keyCode, boolean isPressed) {
		byte action = -1;
		if(keyCode == this.gameSettings.keyBindForward.keyCode) {
			action = 0;
		}

		if(keyCode == this.gameSettings.keyBindBack.keyCode) {
			action = 1;
		}

		if(keyCode == this.gameSettings.keyBindLeft.keyCode) {
			action = 2;
		}

		if(keyCode == this.gameSettings.keyBindRight.keyCode) {
			action = 3;
		}

		if(keyCode == this.gameSettings.keyBindJump.keyCode) {
			action = 4;
		}

		if(keyCode == this.gameSettings.keyBindSneak.keyCode) {
			action = 5;
		}

		if(action >= 0) {
			this.movementKeyStates[action] = isPressed;
		}

	}

	// Release all held movement keys.
	public final void resetKeyState() {
		for(int i = 0; i < 10; ++i) {
			this.movementKeyStates[i] = false;
		}

	}

	// Combine the current key states into forward/strafe/jump/sneak movement for this tick.
	public final void updatePlayerMoveState() {
		this.moveStrafe = 0.0F;
		this.moveForward = 0.0F;
		if(this.movementKeyStates[0]) {
			++this.moveForward;
		}

		if(this.movementKeyStates[1]) {
			--this.moveForward;
		}

		if(this.movementKeyStates[2]) {
			++this.moveStrafe;
		}

		if(this.movementKeyStates[3]) {
			--this.moveStrafe;
		}

		this.jump = this.movementKeyStates[4];
		this.sneak = this.movementKeyStates[5];
		if(this.sneak) {
			// Sneaking: pace cut to 30% of the walk speed.
			this.moveStrafe = (float)((double)this.moveStrafe * 0.3D);
			this.moveForward = (float)((double)this.moveForward * 0.3D);
		}
	}
}
