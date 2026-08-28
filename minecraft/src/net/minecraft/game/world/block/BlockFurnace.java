package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.tileentity.TileEntity;
import net.minecraft.game.world.block.tileentity.TileEntityFurnace;
import net.minecraft.game.world.material.Material;

public final class BlockFurnace extends BlockContainer {
	private final boolean isActive;

	protected BlockFurnace(int blockID, boolean isActive) {
		super(blockID, Material.rock);
		this.isActive = isActive;
		this.blockIndexInTexture = 45;
	}

	@Override
	public final void onBlockAdded(World world, int x, int y, int z) {
		super.onBlockAdded(world, x, y, z);
		setDefaultDirection(world, x, y, z);
	}

	private static void setDefaultDirection(World world, int x, int y, int z) {
		int neighborNegZ = world.getBlockId(x, y, z - 1);
		int neighborPosZ = world.getBlockId(x, y, z + 1);
		int neighborNegX = world.getBlockId(x - 1, y, z);
		int neighborPosX = world.getBlockId(x + 1, y, z);
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
		world.setBlockMetadataWithNotify(x, y, z, facing);
	}

	@Override
	public final int getBlockTexture(IBlockAccess blockAccess, int x, int y, int z, int side) {
		if(side == 1 || side == 0) {
			return Block.stone.blockIndexInTexture;
		} else {
			int metadata = blockAccess.getBlockMetadata(x, y, z);
			return side != metadata ? this.blockIndexInTexture : (this.isActive ? this.blockIndexInTexture + 16 : this.blockIndexInTexture - 1);
		}
	}

	@Override
	public final void randomDisplayTick(World world, int x, int y, int z, Random random) {
		if(this.isActive) {
			int metadata = world.getBlockMetadata(x, y, z);
			float centerX = (float)x + 0.5F;
			float centerY = (float)y + random.nextFloat() * 6.0F / 16.0F;
			float centerZ = (float)z + 0.5F;
			float offset = random.nextFloat() * 0.6F - 0.3F;
			if(metadata == 4) {
				world.spawnParticle("smoke", (double)(centerX - 0.52F), (double)centerY, (double)(centerZ + offset), 0.0D, 0.0D, 0.0D);
				world.spawnParticle("flame", (double)(centerX - 0.52F), (double)centerY, (double)(centerZ + offset), 0.0D, 0.0D, 0.0D);
			} else if(metadata == 5) {
				world.spawnParticle("smoke", (double)(centerX + 0.52F), (double)centerY, (double)(centerZ + offset), 0.0D, 0.0D, 0.0D);
				world.spawnParticle("flame", (double)(centerX + 0.52F), (double)centerY, (double)(centerZ + offset), 0.0D, 0.0D, 0.0D);
			} else if(metadata == 2) {
				world.spawnParticle("smoke", (double)(centerX + offset), (double)centerY, (double)(centerZ - 0.52F), 0.0D, 0.0D, 0.0D);
				world.spawnParticle("flame", (double)(centerX + offset), (double)centerY, (double)(centerZ - 0.52F), 0.0D, 0.0D, 0.0D);
			} else if(metadata == 3) {
				world.spawnParticle("smoke", (double)(centerX + offset), (double)centerY, (double)(centerZ + 0.52F), 0.0D, 0.0D, 0.0D);
				world.spawnParticle("flame", (double)(centerX + offset), (double)centerY, (double)(centerZ + 0.52F), 0.0D, 0.0D, 0.0D);
			}
		}
	}

	@Override
	public final int getBlockTextureFromSide(int side) {
		return side == 1 ? Block.stone.blockID : (side == 0 ? Block.stone.blockID : (side == 3 ? this.blockIndexInTexture - 1 : this.blockIndexInTexture));
	}

	@Override
	public final boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
		TileEntityFurnace furnace = (TileEntityFurnace)world.getBlockTileEntity(x, y, z);
		player.displayFurnaceGUI(furnace);
		return true;
	}

	@Override
	protected final TileEntity getBlockEntity() {
		return new TileEntityFurnace();
	}
}