package net.minecraft.client.render;

import java.util.Comparator;
import net.minecraft.game.entity.player.EntityPlayer;

/**
 * Orders chunk renderers for the "which changed renderers to compile this
 * frame" pass in {@link RenderGlobal#updateRenderers}. Renderers whose bounds
 * lie inside the frustum sort after (compile before) out-of-frustum ones, and
 * within each frustum group the nearer renderer sorts after the farther one.
 *
 * <p>The original 2010 comparator returned only {@code 1} or {@code -1},
 * never {@code 0}, which violates the {@link Comparator} contract (reflexivity
 * and antisymmetry). The old merge sort tolerated that; Java 7+'s TimSort
 * throws {@link IllegalArgumentException} ("Comparison method violates its
 * general contract!"), crashing the game whenever two renderers compared
 * equal. This version keeps the exact same ordering intent but is a valid,
 * consistent comparator.
 */
public final class RenderSorter implements Comparator<WorldRenderer> {
	private final EntityPlayer entityPlayer;

	public RenderSorter(EntityPlayer entityPlayer) {
		this.entityPlayer = entityPlayer;
	}

	public final int compare(WorldRenderer renderer1, WorldRenderer renderer2) {
		boolean renderer1InFrustum = renderer1.isInFrustum;
		boolean renderer2InFrustum = renderer2.isInFrustum;
		if(renderer1InFrustum != renderer2InFrustum) {
			// In-frustum renderers sort after out-of-frustum ones.
			return renderer1InFrustum ? 1 : -1;
		}
		// Same frustum group: nearer renderer sorts after the farther one
		// (compiled first). Float.compare(d2, d1) yields positive when d1 < d2.
		return Float.compare(
			renderer2.distanceToEntitySquared(this.entityPlayer),
			renderer1.distanceToEntitySquared(this.entityPlayer));
	}
}