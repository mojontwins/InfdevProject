package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.item.Item;
import net.minecraft.game.physics.AxisAlignedBB;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.tileentity.TileEntity;
import net.minecraft.game.world.block.tileentity.TileEntitySign;
import net.minecraft.game.world.material.Material;

/**
 * A sign block — either freestanding (placed on top of a block) or
 * wall-mounted (attached to the side of a solid block). Freestanding
 * signs use metadata as a 16-step rotation; wall signs use metadata 2-5
 * to indicate the attached face.
 *
 * <p>The sign text is stored in the associated {@link TileEntitySign} and
 * edited via a GUI. The block has no collision box; entities pass through
 * it freely.
 *
 * <p>Derived from the Minecraft Alpha 1.1.2 {@code BlockSign}, modernised
 * to Java 8 and adapted to this codebase's naming conventions.
 */
public final class BlockSign extends BlockContainer {
	private final Class<?> signEntityClass;
	private final boolean isFreestanding;

	public BlockSign(int blockID, Class<?> signEntityClass, boolean isFreestanding) {
		super(blockID, Material.wood);
		this.isFreestanding = isFreestanding;
		this.blockIndexInTexture = 4;
		this.signEntityClass = signEntityClass;
		float half = 0.25F;
		this.setBlockBounds(0.5F - half, 0.0F, 0.5F - half, 0.5F + half, 1.0F, 0.5F + half);
	}

	@Override
	public final AxisAlignedBB getCollisionBoundingBoxFromPool(int x, int y, int z) {
		return null;
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBoxFromPool(int x, int y, int z) {
		this.setBlockBoundsBasedOnState((IBlockAccess) null, x, y, z);
		return super.getSelectedBoundingBoxFromPool(x, y, z);
	}

	/**
	 * Sets the block bounds based on the sign's type and metadata.
	 * Freestanding signs use a post-sized bounding box; wall signs use
	 * a thin panel against the attached wall.
	 */
	public void setBlockBoundsBasedOnState(IBlockAccess blockAccess, int x, int y, int z) {
		if(!this.isFreestanding) {
			int metadata = blockAccess.getBlockMetadata(x, y, z);
			float minY = 9.0F / 32.0F;
			float maxY = 25.0F / 32.0F;
			float fullMin = 0.0F;
			float fullMax = 1.0F;
			float thickness = 2.0F / 16.0F;
			this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
			if(metadata == 2) {
				this.setBlockBounds(fullMin, minY, 1.0F - thickness, fullMax, maxY, 1.0F);
			}
			if(metadata == 3) {
				this.setBlockBounds(fullMin, minY, 0.0F, fullMax, maxY, thickness);
			}
			if(metadata == 4) {
				this.setBlockBounds(1.0F - thickness, minY, fullMin, 1.0F, maxY, fullMax);
			}
			if(metadata == 5) {
				this.setBlockBounds(0.0F, minY, fullMin, thickness, maxY, fullMax);
			}
		}
	}

	@Override
	public final int getRenderType() {
		return -1;
	}

	@Override
	public final boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public final boolean isOpaqueCube() {
		return false;
	}

	@Override
	protected final TileEntity getBlockEntity() {
		try {
			return (TileEntity)this.signEntityClass.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public final int idDropped(int metadata, Random random) {
		return Item.sign.shiftedIndex;
	}

	@Override
	public final void onNeighborBlockChange(World world, int x, int y, int z, int neighborID) {
		boolean unsupported = false;
		if(this.isFreestanding) {
			if(!world.getBlockMaterial(x, y - 1, z).isSolid()) {
				unsupported = true;
			}
		} else {
			int metadata = world.getBlockMetadata(x, y, z);
			unsupported = true;
			if(metadata == 2 && world.getBlockMaterial(x, y, z + 1).isSolid()) {
				unsupported = false;
			}
			if(metadata == 3 && world.getBlockMaterial(x, y, z - 1).isSolid()) {
				unsupported = false;
			}
			if(metadata == 4 && world.getBlockMaterial(x + 1, y, z).isSolid()) {
				unsupported = false;
			}
			if(metadata == 5 && world.getBlockMaterial(x - 1, y, z).isSolid()) {
				unsupported = false;
			}
		}

		if(unsupported) {
			this.dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z));
			world.setBlockWithNotify(x, y, z, 0);
		}

		super.onNeighborBlockChange(world, x, y, z, neighborID);
	}
}
