package net.minecraft.client.effect;

import net.minecraft.game.world.World;

// A droplet kicked up when rain hits a surface: it is a rain drop with
// slightly stronger gravity and the next texture tile (the splash variant).
public final class EntitySplashFX extends EntityRainFX {
	public EntitySplashFX(World world, double x, double y, double z) {
		super(world, x, y, z);
		this.particleGravity = 0.04F;
		++this.particleTextureIndex;
	}
}
