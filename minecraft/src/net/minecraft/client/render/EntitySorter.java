package net.minecraft.client.render;

import java.util.Comparator;
import net.minecraft.game.entity.Entity;

public final class EntitySorter implements Comparator<WorldRenderer> {
	private Entity entity;

	public EntitySorter(Entity var1) {
		this.entity = var1;
	}

	public final int compare(WorldRenderer var1, WorldRenderer var2) {
		return var1.distanceToEntitySquared(this.entity) < var2.distanceToEntitySquared(this.entity) ? -1 : 1;
	}
}
