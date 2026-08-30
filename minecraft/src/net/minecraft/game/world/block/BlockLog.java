package net.minecraft.game.world.block;

import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;

/**
 * Wood log: bark on the sides, a ring texture on the top and bottom faces.
 *
 * <p>Chopping one also starts leaf decay: on removal it stamps
 * {@link BlockLeaves#DECAY_CHECK_BIT} onto every leaf in a 4-block box around
 * it, so each of those leaves re-verifies its trunk connection the next time it
 * ticks (see {@link BlockLeaves#updateTick}).
 */
public final class BlockLog extends Block {
	/** How far around a removed log to flag leaves for a re-check. */
	private static final int DECAY_FLAG_RADIUS = 4;

	protected BlockLog(int blockID) {
		super(blockID, Material.wood);
		this.blockIndexInTexture = 20;
	}

	@Override
	public final void onBlockRemoval(World world, int x, int y, int z) {
		int extent = DECAY_FLAG_RADIUS + 1;
		if(world.checkChunksExist(x - extent, y - extent, z - extent, x + extent, y + extent, z + extent)) {
			for(int dx = -DECAY_FLAG_RADIUS; dx <= DECAY_FLAG_RADIUS; ++dx) {
				for(int dy = -DECAY_FLAG_RADIUS; dy <= DECAY_FLAG_RADIUS; ++dy) {
					for(int dz = -DECAY_FLAG_RADIUS; dz <= DECAY_FLAG_RADIUS; ++dz) {
						if(world.getBlockId(x + dx, y + dy, z + dz) == Block.leaves.blockID) {
							int metadata = world.getBlockMetadata(x + dx, y + dy, z + dz);
							world.setBlockAndMetadata(x + dx, y + dy, z + dz, Block.leaves.blockID, metadata | BlockLeaves.DECAY_CHECK_BIT);
						}
					}
				}
			}
		}
	}

	@Override
	public final int getBlockTextureFromSide(int side) {
		return side == 1 ? this.blockIndexInTexture + 1 : (side == 0 ? this.blockIndexInTexture + 1 : this.blockIndexInTexture);
	}
}