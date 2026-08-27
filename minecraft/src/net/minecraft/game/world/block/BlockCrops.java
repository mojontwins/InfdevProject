package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.entity.misc.EntityItem;
import net.minecraft.game.item.Item;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.world.World;

public final class BlockCrops extends BlockFlower {
	protected BlockCrops(int blockID, int textureIndex) {
		super(blockID, textureIndex);
		this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.25F, 1.0F);
	}

	@Override
	protected final boolean canThisPlantGrowOnThisBlockID(int belowBlockID) {
		return belowBlockID == Block.tilledField.blockID;
	}

	@Override
	public final void updateTick(World world, int x, int y, int z, Random random) {
		super.updateTick(world, x, y, z, random);
		if(world.getBlockLightValue(x, y + 1, z) >= 9) {
			int metadata = world.getBlockMetadata(x, y, z);
			if(metadata < 7) {
				float growthChance = 1.0F;
				boolean diagonalNeighbor = world.getBlockId(x - 1, y, z - 1) == this.blockID || world.getBlockId(x + 1, y, z - 1) == this.blockID || world.getBlockId(x + 1, y, z + 1) == this.blockID || world.getBlockId(x - 1, y, z + 1) == this.blockID;
				boolean xNeighbor = world.getBlockId(x - 1, y, z) == this.blockID || world.getBlockId(x + 1, y, z) == this.blockID;
				boolean zNeighbor = world.getBlockId(x, y, z - 1) == this.blockID || world.getBlockId(x, y, z + 1) == this.blockID;

				for(int tileX = x - 1; tileX <= x + 1; ++tileX) {
					for(int tileZ = z - 1; tileZ <= z + 1; ++tileZ) {
						int belowBlockID = world.getBlockId(tileX, y - 1, tileZ);
						float tileChance = 0.0F;
						if(belowBlockID == Block.tilledField.blockID) {
							tileChance = 1.0F;
							if(world.getBlockMetadata(tileX, y - 1, tileZ) > 0) {
								tileChance = 3.0F;
							}
						}
						if(tileX != x || tileZ != z) {
							tileChance /= 4.0F;
						}
						growthChance += tileChance;
					}
				}

				if(diagonalNeighbor || xNeighbor && zNeighbor) {
					growthChance /= 2.0F;
				}

				if(random.nextInt((int)(100.0F / growthChance)) == 0) {
					++metadata;
					world.setBlockMetadataWithNotify(x, y, z, metadata);
				}
			}
		}
	}

	@Override
	public final int getBlockTextureFromSideAndMetadata(int side, int metadata) {
		if(metadata < 0) {
			metadata = 7;
		}
		return this.blockIndexInTexture + metadata;
	}

	@Override
	public final int getRenderType() {
		return 6;
	}

	@Override
	public final void onBlockDestroyedByPlayer(World world, int x, int y, int z, int metadata) {
		super.onBlockDestroyedByPlayer(world, x, y, z, metadata);

		for(int i = 0; i < 3; ++i) {
			if(world.rand.nextInt(15) <= metadata) {
				float randomX = world.rand.nextFloat() * 0.7F + 0.15F;
				float randomY = world.rand.nextFloat() * 0.7F + 0.15F;
				float randomZ = world.rand.nextFloat() * 0.7F + 0.15F;
				EntityItem itemEntity = new EntityItem(world, (double)((float)x + randomX), (double)((float)y + randomY), (double)((float)z + randomZ), new ItemStack(Item.seeds));
				itemEntity.delayBeforeCanPickup = 10;
				world.spawnEntityInWorld(itemEntity);
			}
		}
	}

	@Override
	public final int idDropped(int metadata, Random random) {
		System.out.println("Get resource: " + metadata);
		return metadata == 7 ? Item.wheat.shiftedIndex : -1;
	}
}