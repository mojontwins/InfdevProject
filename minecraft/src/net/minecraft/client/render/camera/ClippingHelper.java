package net.minecraft.client.render.camera;

/** Holds the six frustum planes and the matrices used to derive them. */
public class ClippingHelper {
	public float[][] frustrum = new float[16][16];
	public float[] projectionMatrix = new float[16];
	public float[] modelViewMatrix = new float[16];
	public float[] clippingMatrix = new float[16];
}