package net.minecraft.client.render;

import java.util.Comparator;
import net.minecraft.game.entity.player.EntityPlayer;

/**
 * Orders chunk renderers so transparent stuff draws correctly: renderers whose
 * bounds lie inside the frustum sort after (draw on top of) out-of-frustum
 * ones, and everything sorts farthest first. Returns only 1/-1 as in the
 * original.
 */
public final class RenderSorter implements Comparator<WorldRenderer> {
	private final EntityPlayer entityPlayer;

	public RenderSorter(EntityPlayer entityPlayer) {
		this.entityPlayer = entityPlayer;
	}

	public final int compare(WorldRenderer renderer1, WorldRenderer renderer2) {
		boolean renderer1InFrustum = renderer1.isInFrustum;
		boolean renderer2InFrustum = renderer2.isInFrustum;
		return renderer1InFrustum && !renderer2InFrustum ? 1 : ((!renderer2InFrustum || renderer1InFrustum) && renderer1.distanceToEntitySquared(this.entityPlayer) < renderer2.distanceToEntitySquared(this.entityPlayer) ? 1 : -1);
	}
}