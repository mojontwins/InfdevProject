package net.minecraft.client.render;

import java.util.Comparator;
import net.minecraft.game.entity.player.EntityPlayer;

public final class RenderSorter implements Comparator<WorldRenderer> {
	private EntityPlayer entityPlayer;

	public RenderSorter(EntityPlayer var1) {
		this.entityPlayer = var1;
	}

	public final int compare(WorldRenderer var1, WorldRenderer var2) {
		boolean var4 = var1.isInFrustum;
		boolean var5 = var2.isInFrustum;
		return var4 && !var5 ? 1 : ((!var5 || var4) && var1.distanceToEntitySquared(this.entityPlayer) < var2.distanceToEntitySquared(this.entityPlayer) ? 1 : -1);
	}
}
