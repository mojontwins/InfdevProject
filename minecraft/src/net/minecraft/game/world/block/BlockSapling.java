package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.world.World;
import net.minecraft.game.world.terrain.generate.WorldGenBigTree;

public final class BlockSapling extends BlockFlower {
	protected BlockSapling(int blockID, int textureIndex) {
		super(blockID, textureIndex);
		this.setBlockBounds(10.0F * 0.01F, 0.0F, 10.0F * 0.01F, 0.9F, 0.8F, 0.9F);
	}

	@Override
	public final void updateTick(World world, int x, int y, int z, Random random) {
		super.updateTick(world, x, y, z, random);
		if(world.getBlockLightValue(x, y + 1, z) >= 9 && random.nextInt(5) == 0) {
			int metadata = world.getBlockMetadata(x, y, z);
			if(metadata < 15) {
				world.setBlockMetadataWithNotify(x, y, z, metadata + 1);
				return;
			}
			world.setTileNoUpdate(x, y, z, 0);
			WorldGenBigTree treeGenerator = new WorldGenBigTree();
			if(!treeGenerator.generate(world, random, x, y, z)) {
				world.setTileNoUpdate(x, y, z, this.blockID);
			}
		}
	}
}