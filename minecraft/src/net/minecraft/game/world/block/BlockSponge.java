package net.minecraft.game.world.block;

import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;

/**
 * Sponge. This is a faithful stub of the era: on placement it scans the 5x5x5
 * neighbourhood but its water-absorption behaviour never shipped (the block
 * reads the adjacent materials and does nothing with them).
 */
public final class BlockSponge extends Block {
	protected BlockSponge(int blockID) {
		super(blockID, Material.sponge);
		this.blockIndexInTexture = 48;
	}

	@Override
	public final void onBlockAdded(World world, int x, int y, int z) {
		for(int scanX = x - 2; scanX <= x + 2; ++scanX) {
			for(int scanY = y - 2; scanY <= y + 2; ++scanY) {
				for(int scanZ = z - 2; scanZ <= z + 2; ++scanZ) {
					world.getBlockMaterial(scanX, scanY, scanZ);
				}
			}
		}
	}

	@Override
	public final void onBlockRemoval(World world, int x, int y, int z) {
		for(int scanX = x - 2; scanX <= x + 2; ++scanX) {
			for(int scanY = y - 2; scanY <= y + 2; ++scanY) {
				for(int scanZ = z - 2; scanZ <= z + 2; ++scanZ) {
					world.notifyBlocksOfNeighborChange(scanX, scanY, scanZ, world.getBlockId(scanX, scanY, scanZ));
				}
			}
		}
	}
}