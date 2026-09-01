package net.minecraft.game.world.block;

import net.minecraft.game.world.World;

/**
 * A mushroom: instead of the flower's grass/dirt support rule it will grow on
 * any opaque block, but only while it is dimly lit (light level 13 is the
 * cutoff). The metadata selects the mushroom variant (0 = brown tile 29,
 * 1 = red tile 28); the {@link #metadataToTexture} array makes the mapping
 * easy to extend with more variants later.
 */
public final class BlockMushroom extends BlockFlower {
	protected BlockMushroom(int blockID, int textureIndex) {
		super(blockID, textureIndex);
		this.setBlockBounds(0.3F, 0.0F, 0.3F, 0.7F, 0.4F, 0.7F);
		this.metadataToTexture = new int[]{29, 28};
	}

	@Override
	protected final boolean canThisPlantGrowOnThisBlockID(int belowBlockID) {
		return Block.opaqueCubeLookup[belowBlockID];
	}

	@Override
	public final boolean canBlockStay(World world, int x, int y, int z) {
		if(world.getBlockLightValue(x, y, z) <= 13) {
			int belowBlockID = world.getBlockId(x, y - 1, z);
			if(Block.opaqueCubeLookup[belowBlockID]) {
				return true;
			}
		}
		return false;
	}
}
