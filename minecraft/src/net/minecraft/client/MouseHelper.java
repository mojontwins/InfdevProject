package net.minecraft.client;

import java.awt.AWTException;
import java.awt.Component;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Mouse;

/**
 * Handles capturing the mouse for first-person look.
 *
 * The cursor is hidden (by swapping in a blank cursor), and each frame the
 * pointer is recentred on the window so the game can read the movement delta
 * between centres. The first few frames discard the large jump that occurs
 * while the pointer initially recentres.
 */
public final class MouseHelper {
	private Component windowComponent;
	private Robot robot;
	private int mouseX;
	private int mouseY;
	private Cursor cursor;
	public int deltaX;
	public int deltaY;
	private int mouseInt = 10;

	public MouseHelper(Component windowComponent) {
		this.windowComponent = windowComponent;

		try {
			this.robot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}

		// A blank (all-transparent) cursor that will hide the visible pointer.
		IntBuffer cursorImage = BufferUtils.createIntBuffer(1);
		cursorImage.put(0);
		cursorImage.flip();
		IntBuffer cursorPixels = BufferUtils.createIntBuffer(1024);

		try {
			this.cursor = new Cursor(32, 32, 16, 16, 1, cursorPixels, cursorImage);
		} catch (LWJGLException e) {
			e.printStackTrace();
		}
	}

	public final void grabMouseCursor() {
		try {
			Mouse.setNativeCursor(this.cursor);
		} catch (LWJGLException e) {
			e.printStackTrace();
		}

		this.ungrabMouseCursor();
		this.deltaX = 0;
		this.deltaY = 0;
	}

	public final void ungrabMouseCursor() {
		Point pointerLocation = MouseInfo.getPointerInfo().getLocation();
		Point componentLocation = this.windowComponent.getLocationOnScreen();
		this.robot.mouseMove(this.mouseX, this.mouseY);
		// Recentre the pointer on the window so deltas are relative to centre.
		this.mouseX = componentLocation.x + this.windowComponent.getWidth() / 2;
		this.mouseY = componentLocation.y + this.windowComponent.getHeight() / 2;
		if(this.mouseInt == 0) {
			this.deltaX = pointerLocation.x - this.mouseX;
			this.deltaY = pointerLocation.y - this.mouseY;
		} else {
			// First few moves: ignore the jump from the original position.
			this.deltaX = this.deltaY = 0;
			--this.mouseInt;
		}
	}
}
