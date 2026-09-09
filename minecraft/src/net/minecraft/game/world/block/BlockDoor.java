package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.item.Item;
import net.minecraft.game.physics.AxisAlignedBB;
import net.minecraft.game.physics.MovingObjectPosition;
import net.minecraft.game.physics.Vec3D;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;

/**
 * A two-block-tall door. The lower half stores the facing direction in the
 * bottom two bits of its metadata; the upper half stores the same facing
 * with bit 3 (value 8) set. Bit 2 (value 4) records whether the door is
 * open. Iron doors cannot be toggled by hand.
 *
 * <p>Derived from the Minecraft Alpha 1.1.2 {@code BlockDoor}, modernised
 * to Java 8 and adapted to this codebase's naming conventions.
 */
public final class BlockDoor extends Block {
	protected BlockDoor(int blockID, Material material) {
		super(blockID, material);
		this.blockIndexInTexture = 97;
		if(material == Material.iron) {
			++this.blockIndexInTexture;
		}

		float half = 0.5F;
		this.setBlockBounds(0.5F - half, 0.0F, 0.5F - half, 0.5F + half, 1.0F, 0.5F + half);
	}

	@Override
	public final int getBlockTextureFromSideAndMetadata(int side, int metadata) {
		if(side != 0 && side != 1) {
			int state = this.getState(metadata);
			if((state == 0 || state == 2) ^ side <= 3) {
				return this.blockIndexInTexture;
			} else {
				int variant = state / 2 + (side & 1 ^ state);
				variant += (metadata & 4) / 4;
				int texture = this.blockIndexInTexture - (metadata & 8) * 2;
				if((variant & 1) != 0) {
					texture = -texture;
				}
				return texture;
			}
		} else {
			return this.blockIndexInTexture;
		}
	}

	@Override
	public final boolean isOpaqueCube() {
		return false;
	}

	@Override
	public final boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public final int getRenderType() {
		return 7;
	}

	@Override
	public final AxisAlignedBB getCollisionBoundingBoxFromPool(int x, int y, int z) {
		// getCollisionBoundingBoxFromPool has no World/IBlockAccess parameter,
		// so we cannot read metadata here.  Use full-block default; the
		// precise panel shape is set by collisionRayTrace.
		this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
		return super.getCollisionBoundingBoxFromPool(x, y, z);
	}

	@Override
	public final MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3D startVector, Vec3D endVector) {
		this.setBlockBoundsBasedOnState(world, x, y, z);
		return super.collisionRayTrace(world, x, y, z, startVector, endVector);
	}

	/**
	 * Sets the block bounds to a thin panel matching the door's current
	 * open/closed state and facing direction.
	 */
	public void setBlockBoundsBasedOnState(IBlockAccess blockAccess, int x, int y, int z) {
		this.setDoorRotation(this.getState(blockAccess.getBlockMetadata(x, y, z)));
	}

	/**
	 * Sets the block bounds to a thin panel for the given door state
	 * (0-3 representing the four horizontal orientations).
	 */
	private void setDoorRotation(int state) {
		float thickness = 3.0F / 16.0F;
		this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F);
		if(state == 0) {
			this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, thickness);
		}
		if(state == 1) {
			this.setBlockBounds(1.0F - thickness, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
		}
		if(state == 2) {
			this.setBlockBounds(0.0F, 0.0F, 1.0F - thickness, 1.0F, 1.0F, 1.0F);
		}
		if(state == 3) {
			this.setBlockBounds(0.0F, 0.0F, 0.0F, thickness, 1.0F, 1.0F);
		}
	}

	/**
	 * Returns the visual state of the door (0-3) from its raw metadata,
	 * accounting for the open/closed bit.
	 */
	public int getState(int metadata) {
		return (metadata & 4) == 0 ? (metadata - 1) & 3 : metadata & 3;
	}

	@Override
	public final boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
		if(this.blockMaterial == Material.iron) {
			return true;
		}

		int metadata = world.getBlockMetadata(x, y, z);
		if((metadata & 8) != 0) {
			// Upper half: delegate to lower half.
			if(world.getBlockId(x, y - 1, z) == this.blockID) {
				this.blockActivated(world, x, y - 1, z, player);
			}
			return true;
		}

		// Toggle the open/closed bit on this half and the upper half.
		if(world.getBlockId(x, y + 1, z) == this.blockID) {
			world.setBlockMetadataWithNotify(x, y + 1, z, (metadata ^ 4) + 8);
		}
		world.setBlockMetadataWithNotify(x, y, z, metadata ^ 4);
		world.markBlocksDirty(x, y - 1, z, x, y, z);

		if(Math.random() < 0.5D) {
			world.playSoundEffect(
				(double)x + 0.5D, (double)y + 0.5D, (double)z + 0.5D,
				"random.door_open", 1.0F, world.rand.nextFloat() * 0.1F + 0.9F);
		} else {
			world.playSoundEffect(
				(double)x + 0.5D, (double)y + 0.5D, (double)z + 0.5D,
				"random.door_close", 1.0F, world.rand.nextFloat() * 0.1F + 0.9F);
		}

		return true;
	}

	@Override
	public final void onNeighborBlockChange(World world, int x, int y, int z, int neighborID) {
		int metadata = world.getBlockMetadata(x, y, z);
		if((metadata & 8) != 0) {
			// Upper half: drop if the lower half is missing.
			if(world.getBlockId(x, y - 1, z) != this.blockID) {
				world.setBlockWithNotify(x, y, z, 0);
			}
		} else {
			boolean drop = false;
			if(world.getBlockId(x, y + 1, z) != this.blockID) {
				drop = true;
			}
			if(!world.isSolid(x, y - 1, z)) {
				drop = true;
				if(world.getBlockId(x, y + 1, z) == this.blockID) {
					world.setBlockWithNotify(x, y + 1, z, 0);
				}
			}
			if(drop) {
				this.dropBlockAsItem(world, x, y, z, metadata);
				world.setBlockWithNotify(x, y, z, 0);
			}
		}
	}

	@Override
	public final int idDropped(int metadata, Random random) {
		if((metadata & 8) != 0) {
			return 0;
		}
		return this.blockMaterial == Material.iron
			? Item.doorSteel.shiftedIndex
			: Item.doorWood.shiftedIndex;
	}

	@Override
	public final boolean canPlaceBlockAt(World world, int x, int y, int z) {
		return y < 127
			&& world.isSolid(x, y - 1, z)
			&& super.canPlaceBlockAt(world, x, y, z)
			&& super.canPlaceBlockAt(world, x, y + 1, z);
	}
}
