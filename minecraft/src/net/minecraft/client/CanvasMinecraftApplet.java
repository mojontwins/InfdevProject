package net.minecraft.client;

import java.awt.Canvas;

/**
 * The AWT Canvas embedded inside the Minecraft applet.
 *
 * When the canvas is attached to a native peer ({@code addNotify}) the main
 * render thread is started; when it is detached the game is shut down.
 */
final class CanvasMinecraftApplet extends Canvas {
	private static final long serialVersionUID = 1L;
	private MinecraftApplet mcApplet;

	CanvasMinecraftApplet(MinecraftApplet mcApplet) {
		this.mcApplet = mcApplet;
	}

	public final synchronized void addNotify() {
		super.addNotify();
		this.mcApplet.startMainThread();
	}

	public final synchronized void removeNotify() {
		this.mcApplet.shutdown();
		super.removeNotify();
	}
}
