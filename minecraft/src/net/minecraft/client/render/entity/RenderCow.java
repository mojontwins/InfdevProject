package net.minecraft.client.render.entity;

import net.minecraft.client.model.ModelBase;

/**
 * Renders cows. The base {@link RenderLiving} does all the work; this class
 * exists so the renderer registry can name a model and shadow size that
 * match the cow's silhouette, and so future cow-specific render tweaks
 * (e.g. a vest overlay) have a natural home.
 */
public class RenderCow extends RenderLiving {
	public RenderCow(ModelBase model, float shadowSize) {
		super(model, shadowSize);
	}
}
