package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.tileentity.TileEntity;
import net.minecraft.game.world.block.tileentity.TileEntityFurnace;
import net.minecraft.game.world.material.Material;

/**
 * Furnace: a container block offered in an idle and an active variant. On
 * placement it faces the player who placed it - the front sprite and the
 * burning smoke/flame particles always point at them, regardless of the
 * surroundings. While burning, the active sprite is swapped in by
 * {@link TileEntityFurnace} and smoke/flame particles puff out of the front.
 */
public final class BlockFurnace extends BlockContainer {
	private final boolean isActive;

	protected BlockFurnace(int blockID, boolean isActive) {
		super(blockID, Material.rock);
		this.isActive = isActive;
		this.blockIndexInTexture = 45;
	}

	/**
	 * The furnace orientation is only meaningful at placement: the facing is
	 * stored in the block metadata so it survives reloads unchanged. Placement
	 * runs through {@link #onBlockPlaced}, which {@link ItemBlock} calls right
	 * after the block is set - the base no-op is not needed here.
	 */
	@Override
	public final void onBlockPlaced(World world, int x, int y, int z, int side, float xWithinFace, float yWithinFace, float zWithinFace) {
		world.setBlockMetadataWithNotify(x, y, z, getPlayerFacing(world, x, y, z));
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