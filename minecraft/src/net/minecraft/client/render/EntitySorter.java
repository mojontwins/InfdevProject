package net.minecraft.client.render;

import java.util.Comparator;
import net.minecraft.game.entity.Entity;

/**
 * Sorts rendered (translucent) entities by squared distance to a reference
 * entity — typically the player — so the painter's algorithm draws faraway
 * sprites first. Returns only 1/-1, matching the original comparator.
 */
public final class EntitySorter implements Comparator<WorldRenderer> {
	private final Entity entity;

	public EntitySorter(Entity entity) {
		this.entity = entity;
	}

	public final int compare(WorldRenderer renderer1, WorldRenderer renderer2) {
		return renderer1.distanceToEntitySquared(this.entity) < renderer2.distanceToEntitySquared(this.entity) ? -1 : 1;
	}
}