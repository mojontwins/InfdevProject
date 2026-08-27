package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.IInventory;
import net.minecraft.game.InventoryLargeChest;
import net.minecraft.game.entity.misc.EntityItem;
import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.tileentity.TileEntity;
import net.minecraft.game.world.block.tileentity.TileEntityChest;
import net.minecraft.game.world.material.Material;

public final class BlockChest extends BlockContainer {
	private Random random = new Random();

	protected BlockChest(int blockID) {
		super(blockID, Material.wood);
		this.blockIndexInTexture = 26;
	}

	@Override
	public final int getBlockTexture(World world, int x, int y, int z, int side) {
		if(side == 1 || side == 0) {
			return this.blockIndexInTexture - 1;
		} else {
			int neighborNegZ = world.getBlockId(x, y, z - 1);
			int neighborPosZ = world.getBlockId(x, y, z + 1);
			int neighborNegX = world.getBlockId(x - 1, y, z);
			int neighborPosX = world.getBlockId(x + 1, y, z);
			if(neighborNegZ != this.blockID && neighborPosZ != this.blockID) {
				if(neighborNegX != this.blockID && neighborPosX != this.blockID) {
					byte facing = 3;
					if(Block.opaqueCubeLookup[neighborNegZ] && !Block.opaqueCubeLookup[neighborPosZ]) {
						facing = 3;
					}
					if(Block.opaqueCubeLookup[neighborPosZ] && !Block.opaqueCubeLookup[neighborNegZ]) {
						facing = 2;
					}
					if(Block.opaqueCubeLookup[neighborNegX] && !Block.opaqueCubeLookup[neighborPosX]) {
						facing = 5;
					}
					if(Block.opaqueCubeLookup[neighborPosX] && !Block.opaqueCubeLookup[neighborNegX]) {
						facing = 4;
					}
					return side == facing ? this.blockIndexInTexture + 1 : this.blockIndexInTexture;
				} else if(side != 4 && side != 5) {
					int offset = 0;
					if(neighborNegX == this.blockID) {
						offset = -1;
					}
					int cornerNegZ = world.getBlockId(neighborNegX == this.blockID ? x - 1 : x + 1, y, z - 1);
					int cornerPosZ = world.getBlockId(neighborNegX == this.blockID ? x - 1 : x + 1, y, z + 1);
					if(side == 3) {
						offset = -1 - offset;
					}
					byte facing = 3;
					if((Block.opaqueCubeLookup[neighborNegZ] || Block.opaqueCubeLookup[cornerNegZ]) && !Block.opaqueCubeLookup[neighborPosZ] && !Block.opaqueCubeLookup[cornerPosZ]) {
						facing = 3;
					}
					if((Block.opaqueCubeLookup[neighborPosZ] || Block.opaqueCubeLookup[cornerPosZ]) && !Block.opaqueCubeLookup[neighborNegZ] && !Block.opaqueCubeLookup[cornerNegZ]) {
						facing = 2;
					}
					return (side == facing ? this.blockIndexInTexture + 16 : this.blockIndexInTexture + 32) + offset;
				} else {
					return this.blockIndexInTexture;
				}
			} else if(side != 2 && side != 3) {
				int offset = 0;
				if(neighborNegZ == this.blockID) {
					offset = -1;
				}
				int cornerNegX = world.getBlockId(x - 1, y, neighborNegZ == this.blockID ? z - 1 : z + 1);
				int cornerPosX = world.getBlockId(x + 1, y, neighborNegZ == this.blockID ? z - 1 : z + 1);
				if(side == 4) {
					offset = -1 - offset;
				}
				byte facing = 5;
				if((Block.opaqueCubeLookup[neighborNegX] || Block.opaqueCubeLookup[cornerNegX]) && !Block.opaqueCubeLookup[neighborPosX] && !Block.opaqueCubeLookup[cornerPosX]) {
					facing = 5;
				}
				if((Block.opaqueCubeLookup[neighborPosX] || Block.opaqueCubeLookup[cornerPosX]) && !Block.opaqueCubeLookup[neighborNegX] && !Block.opaqueCubeLookup[cornerNegX]) {
					facing = 4;
				}
				return (side == facing ? this.blockIndexInTexture + 16 : this.blockIndexInTexture + 32) + offset;
			} else {
				return this.blockIndexInTexture;
			}
		}
	}

	@Override
	public final int getBlockTextureFromSide(int side) {
		return side == 1 ? this.blockIndexInTexture - 1 : (side == 0 ? this.blockIndexInTexture - 1 : (side == 3 ? this.blockIndexInTexture + 1 : this.blockIndexInTexture));
	}

	@Override
	public final boolean canPlaceBlockAt(World world, int x, int y, int z) {
		int neighborChestCount = 0;
		if(world.getBlockId(x - 1, y, z) == this.blockID) {
			++neighborChestCount;
		}
		if(world.getBlockId(x + 1, y, z) == this.blockID) {
			++neighborChestCount;
		}
		if(world.getBlockId(x, y, z - 1) == this.blockID) {
			++neighborChestCount;
		}
		if(world.getBlockId(x, y, z + 1) == this.blockID) {
			++neighborChestCount;
		}
		return neighborChestCount <= 1 && !this.hasNeighborChest(world, x - 1, y, z) && !this.hasNeighborChest(world, x + 1, y, z) && !this.hasNeighborChest(world, x, y, z - 1) && !this.hasNeighborChest(world, x, y, z + 1);
	}

	private boolean hasNeighborChest(World world, int x, int y, int z) {
		return world.getBlockId(x, y, z) != this.blockID ? false : (world.getBlockId(x - 1, y, z) == this.blockID || world.getBlockId(x + 1, y, z) == this.blockID || world.getBlockId(x, y, z - 1) == this.blockID || world.getBlockId(x, y, z + 1) == this.blockID);
	}

	@Override
	public final void onBlockRemoval(World world, int x, int y, int z) {
		TileEntityChest chest = (TileEntityChest)world.getBlockTileEntity(x, y, z);

		for(int i = 0; i < chest.getInventorySize(); ++i) {
			ItemStack itemStack = chest.getStackInSlot(i);
			if(itemStack != null) {
				float dropX = this.random.nextFloat() * 0.8F + 0.1F;
				float dropY = this.random.nextFloat() * 0.8F + 0.1F;
				float dropZ = this.random.nextFloat() * 0.8F + 0.1F;

				while(itemStack.stackSize > 0) {
					int dropAmount = this.random.nextInt(21) + 10;
					if(dropAmount > itemStack.stackSize) {
						dropAmount = itemStack.stackSize;
					}
					itemStack.stackSize -= dropAmount;
					EntityItem itemEntity = new EntityItem(world, (double)((float)x + dropX), (double)((float)y + dropY), (double)((float)z + dropZ), new ItemStack(itemStack.itemID, dropAmount, itemStack.itemDamage));
					itemEntity.motionX = (double)((float)this.random.nextGaussian() * 0.05F);
					itemEntity.motionY = (double)((float)this.random.nextGaussian() * 0.05F + 0.2F);
					itemEntity.motionZ = (double)((float)this.random.nextGaussian() * 0.05F);
					world.spawnEntityInWorld(itemEntity);
				}
			}
		}

		super.onBlockRemoval(world, x, y, z);
	}

	@Override
	public final boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
		IInventory inventory = (TileEntityChest)world.getBlockTileEntity(x, y, z);
		if(world.isSolid(x, y + 1, z)) {
			return true;
		} else if(world.getBlockId(x - 1, y, z) == this.blockID && world.isSolid(x - 1, y + 1, z)) {
			return true;
		} else if(world.getBlockId(x + 1, y, z) == this.blockID && world.isSolid(x + 1, y + 1, z)) {
			return true;
		} else if(world.getBlockId(x, y, z - 1) == this.blockID && world.isSolid(x, y + 1, z - 1)) {
			return true;
		} else if(world.getBlockId(x, y, z + 1) == this.blockID && world.isSolid(x, y + 1, z + 1)) {
			return true;
		} else {
			if(world.getBlockId(x - 1, y, z) == this.blockID) {
				inventory = new InventoryLargeChest("Large chest", (TileEntityChest)world.getBlockTileEntity(x - 1, y, z), inventory);
			}
			if(world.getBlockId(x + 1, y, z) == this.blockID) {
				inventory = new InventoryLargeChest("Large chest", inventory, (TileEntityChest)world.getBlockTileEntity(x + 1, y, z));
			}
			if(world.getBlockId(x, y, z - 1) == this.blockID) {
				inventory = new InventoryLargeChest("Large chest", (TileEntityChest)world.getBlockTileEntity(x, y, z - 1), inventory);
			}
			if(world.getBlockId(x, y, z + 1) == this.blockID) {
				inventory = new InventoryLargeChest("Large chest", inventory, (TileEntityChest)world.getBlockTileEntity(x, y, z + 1));
			}
			player.displayChestGUI(inventory);
			return true;
		}
	}

	@Override
	protected final TileEntity getBlockEntity() {
		return new TileEntityChest();
	}
}