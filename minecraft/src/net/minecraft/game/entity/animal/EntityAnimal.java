package net.minecraft.game.entity.animal;

import net.minecraft.game.entity.EntityCreature;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import util.MathHelper;

/**
 * A passive, light-loving creature: it prefers grass underfoot and only spawns
 * where the sun reaches.
 */
public abstract class EntityAnimal extends EntityCreature {
	public EntityAnimal(World world) {
		super(world);
	}

	/** Grass below the spot scores a full 10 points; otherwise a spot's appeal tracks its daylight. */
	protected final float getBlockPathWeight(int x, int y, int z) {
		return this.worldObj.getBlockId(x, y - 1, z) == Block.grass.blockID ? 10.0F : this.worldObj.getBrightness(x, y, z) - 0.5F;
	}

	@Override
	public final boolean getCanSpawnHere(float x, float y, float z) {
		return this.worldObj.getBlockLightValue(MathHelper.floor_float(x), MathHelper.floor_float(y), MathHelper.floor_float(z)) > 8 && super.getCanSpawnHere(x, y, z);
	}
}