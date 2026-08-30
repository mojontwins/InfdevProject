package net.minecraft.client.gui;

/** Computes a "scaled" resolution for GUI drawing so the UI stays a constant size regardless of window pixel size. */
public final class ScaledResolution {
	private int scaledWidth;
	private int scaledHeight;

	public ScaledResolution(int width, int height) {
		this.scaledWidth = width;
		this.scaledHeight = height;
		int scale;
		// Increase the scale factor until the resulting pixel dimensions drop below
		// the minimum 320x240 required for the smallest GUI.
		for(scale = 1; this.scaledWidth / (scale + 1) >= 320 && this.scaledHeight / (scale + 1) >= 240; ++scale) {
		}

		this.scaledWidth /= scale;
		this.scaledHeight /= scale;
	}

	public final int getScaledWidth() {
		return this.scaledWidth;
	}

	public final int getScaledHeight() {
		return this.scaledHeight;
	}
}
