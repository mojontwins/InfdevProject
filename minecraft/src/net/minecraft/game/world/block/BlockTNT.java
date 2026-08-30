package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.entity.misc.EntityTNT;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;

/**
 * TNT: mined or exploded, it spawns a primed {@link EntityTNT} (with a random
 * shortened fuse variant when set off by an explosion) instead of dropping.
 */
public final class BlockTNT extends Block {
	public BlockTNT(int blockID, int textureIndex) {
		super(blockID, textureIndex, Material.tnt);
	}

	@Override
	public final int getBlockTextureFromSide(int side) {
		return side == 0 ? this.blockIndexInTexture + 2 : (side == 1 ? this.blockIndexInTexture + 1 : this.blockIndexInTexture);
	}

	@Override
	public final int quantityDropped(Random random) {
		return 0;
	}

	@Override
	public final void onBlockDestroyedByExplosion(World world, int x, int y, int z) {
		EntityTNT tntEntity = new EntityTNT(world, (float)x + 0.5F, (float)y + 0.5F, (float)z + 0.5F);
		tntEntity.fuse = world.rand.nextInt(tntEntity.fuse / 4) + tntEntity.fuse / 8;
		world.spawnEntityInWorld(tntEntity);
	}

	@Override
	public final void onBlockDestroyedByPlayer(World world, int x, int y, int z, int metadata) {
		EntityTNT tntEntity = new EntityTNT(world, (float)x + 0.5F, (float)y + 0.5F, (float)z + 0.5F);
		world.spawnEntityInWorld(tntEntity);
		world.playSoundAtEntity(tntEntity, "random.fuse", 1.0F, 1.0F);
	}
}