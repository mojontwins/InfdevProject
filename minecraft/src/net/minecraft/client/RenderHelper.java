package net.minecraft.client;

import java.nio.FloatBuffer;
import net.minecraft.game.physics.Vec3D;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * Utilities for setting up OpenGL lighting and colour buffers used when
 * rendering items and GUI elements.
 */
public final class RenderHelper {
	private static FloatBuffer colorBuffer = BufferUtils.createFloatBuffer(16);

	public static void disableStandardItemLighting() {
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_LIGHT0);
		GL11.glDisable(GL11.GL_LIGHT1);
		GL11.glDisable(GL11.GL_COLOR_MATERIAL);
	}

	public static void enableStandardItemLighting() {
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_LIGHT0);
		GL11.glEnable(GL11.GL_LIGHT1);
		GL11.glEnable(GL11.GL_COLOR_MATERIAL);
		GL11.glColorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE);
		Vec3D lightDirection = new Vec3D((double)0.3F, 1.0D, (double)-0.7F);
		GL11.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, setColorBuffer(lightDirection.xCoord, lightDirection.yCoord, lightDirection.zCoord, 0.0D));
		GL11.glLight(GL11.GL_LIGHT0, GL11.GL_DIFFUSE, setColorBufferD(0.5F, 0.5F, 0.5F, 1.0F));
		GL11.glLight(GL11.GL_LIGHT0, GL11.GL_AMBIENT, setColorBufferD(0.0F, 0.0F, 0.0F, 1.0F));
		GL11.glLight(GL11.GL_LIGHT0, GL11.GL_SPECULAR, setColorBufferD(0.0F, 0.0F, 0.0F, 1.0F));
		lightDirection = new Vec3D((double)-0.7F, 1.0D, (double)0.2F);
		GL11.glLight(GL11.GL_LIGHT1, GL11.GL_POSITION, setColorBuffer(lightDirection.xCoord, lightDirection.yCoord, lightDirection.zCoord, 0.0D));
		GL11.glLight(GL11.GL_LIGHT1, GL11.GL_DIFFUSE, setColorBufferD(0.5F, 0.5F, 0.5F, 1.0F));
		GL11.glLight(GL11.GL_LIGHT1, GL11.GL_AMBIENT, setColorBufferD(0.0F, 0.0F, 0.0F, 1.0F));
		GL11.glLight(GL11.GL_LIGHT1, GL11.GL_SPECULAR, setColorBufferD(0.0F, 0.0F, 0.0F, 1.0F));
		GL11.glShadeModel(GL11.GL_SMOOTH);
		GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, setColorBufferD(0.5F, 0.5F, 0.5F, 1.0F));
	}

	// Double variant: the alpha channel is forced to zero (not translucent).
	private static FloatBuffer setColorBuffer(double red, double green, double blue, double alpha) {
		return setColorBufferD((float)red, (float)green, (float)blue, 0.0F);
	}

	private static FloatBuffer setColorBufferD(float red, float green, float blue, float alpha) {
		colorBuffer.clear();
		colorBuffer.put(red).put(green).put(blue).put(alpha);
		colorBuffer.flip();
		return colorBuffer;
	}
}
