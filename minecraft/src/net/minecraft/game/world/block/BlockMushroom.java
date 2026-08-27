package net.minecraft.game.world.block;

import net.minecraft.game.world.World;

public final class BlockMushroom extends BlockFlower {
	protected BlockMushroom(int blockID, int textureIndex) {
		super(blockID, textureIndex);
		this.setBlockBounds(0.3F, 0.0F, 0.3F, 0.7F, 0.4F, 0.7F);
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