package net.minecraft.client.render;

import java.util.Comparator;
import net.minecraft.game.entity.Entity;

/**
 * Sorts rendered (translucent) entities by squared distance to a reference
 * entity — typically the player — so the painter's algorithm draws faraway
 * sprites first. The original 2010 comparator returned only 1/-1 and never 0,
 * which violates the {@link Comparator} contract (reflexivity/antisymmetry);
 * Java 7+'s TimSort rejects that and crashes with "Comparison method violates
 * its general contract!" whenever two renderers compared equal. This keeps the
 * exact same ordering for distinct distances but returns 0 for equal ones.
 */
public final class EntitySorter implements Comparator<WorldRenderer> {
	private final Entity entity;

	public EntitySorter(Entity entity) {
		this.entity = entity;
	}

	public final int compare(WorldRenderer renderer1, WorldRenderer renderer2) {
		float distance1 = renderer1.distanceToEntitySquared(this.entity);
		float distance2 = renderer2.distanceToEntitySquared(this.entity);
		if(distance1 < distance2) {
			return -1;
		}
		return distance1 > distance2 ? 1 : 0;
	}
}