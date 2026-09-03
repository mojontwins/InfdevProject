package net.minecraft.client.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for all entity models. It owns the master list of
 * renderers, the named texture-offset registry, and the texture grid
 * dimensions shared by every part of the model.
 */
public abstract class ModelBase {
	public float onGround;
	public boolean isRiding;
	public List<ModelRenderer> boxList = new ArrayList<ModelRenderer>();
	public boolean isChild;
	private Map<String, TextureOffset> modelTextureMap = new HashMap<String, TextureOffset>();
	public int textureWidth = 64;
	public int textureHeight = 32;

	public abstract void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor);

	/**
	 * Renders the model. Subclasses typically call
	 * {@link #setRotationAngles} first to pose the parts, then draw each one.
	 *
	 * @param limbSwing    accumulated walking/swimming animation time
	 * @param limbSwingAmount how far into a swing step the model is (0 to 1)
	 * @param ageInTicks   time in ticks the entity has been alive
	 * @param netHeadYaw   head yaw offset, in degrees
	 * @param headPitch    head pitch offset, in degrees
	 * @param scaleFactor  uniform scale applied to the whole model
	 */
	public void render(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor) {
	}

	/**
	 * Registers the named texture offset for use by {@link ModelRenderer}
	 * parts that look themselves up by name.
	 */
	protected void setTextureOffset(String partName, int x, int y) {
		this.modelTextureMap.put(partName, new TextureOffset(x, y));
	}

	/**
	 * Returns the registered texture offset for the given part name, or
	 * {@code null} if the name has not been registered.
	 */
	public TextureOffset getTextureOffset(String partName) {
		return this.modelTextureMap.get(partName);
	}

	/**
	 * Called by {@link net.minecraft.client.render.entity.RenderLiving} to
	 * drive per-frame animations. Subclasses should override
	 * {@link #setRotationAngles} instead of this.
	 */
	public void setLivingAnimations(float limbSwing, float limbSwingAmount, float partialTick) {
	}
}
