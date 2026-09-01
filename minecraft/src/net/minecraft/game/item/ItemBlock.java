package net.minecraft.game.item;

import net.minecraft.game.physics.AxisAlignedBB;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;

/**
 * The inventory representation of a terrain block. Block items live at the same
 * id as their block, so this constructor receives the block id already shifted
 * below zero by one item slot (see {@link Block}); the icon reuses the block's
 * own side texture so block icons match their terrain sprite.
 */
public final class ItemBlock extends Item {
	private final int blockID;

	public ItemBlock(int itemID) {
		super(itemID);
		this.blockID = itemID + 256;
		this.setIconIndex(Block.blocksList[itemID + 256].getBlockTextureFromSide(2));
	}

	/**
	 * Translates an {@link ItemStack#itemDamage item damage} value into the
	 * block metadata that should be written when this item is placed. Default
	 * passes the damage through unchanged so a damaged block item places
	 * with that exact metadata; sub-classes (stairs, logs, slabs) override
	 * this to remap damage to a different nibble.
	 */
	public int getMetadata(int damage) {
		return damage;
	}

	/**
	 * Places the block at the target cell, which is either the cell the player
	 * clicked (for substitutable blocks like flowers, water, fire) or the
	 * neighbour across the clicked face (for solid blocks). Placement only
	 * proceeds when the target cell is empty or holds a substitutable block
	 * (fire, fluids, flowers) and the block's own placement rules pass. The
	 * exact click position on the face is forwarded so blocks can react to
	 * where precisely they were placed. The block's metadata is taken from the
	 * item's damage (clamped via {@link #getMetadata}) so that placing a
	 * damaged item stack places the matching sub-variant of the block.
	 */
	@Override
	public final boolean onItemUse(ItemStack stack, World world, int x, int y, int z, int side, float xWithinFace, float yWithinFace, float zWithinFace) {
		Block clickedBlock = Block.blocksList[world.getBlockId(x, y, z)];
		if(stack.stackSize == 0) {
			return false;
		}

		int targetX = x;
		int targetY = y;
		int targetZ = z;

		// If the clicked block is substitutable (flower, fire, fluid), place
		// into its cell instead of the neighbour across the face.
		if(clickedBlock == null || !clickedBlock.canBeSubstituted()) {
			int[] target = neighbourAcrossFace(side, x, y, z);
			targetX = target[0];
			targetY = target[1];
			targetZ = target[2];
		}

		Block targetBlock = Block.blocksList[world.getBlockId(targetX, targetY, targetZ)];
		if(targetBlock == null || targetBlock.canBeSubstituted()) {
			Block blockToPlace = Block.blocksList[this.blockID];
			AxisAlignedBB placementBox = blockToPlace.getCollisionBoundingBoxFromPool(targetX, targetY, targetZ);
			if((placementBox == null || world.checkIfAABBIsClear1(placementBox)) && blockToPlace.canPlaceBlockAt(world, targetX, targetY, targetZ)) {
				if(targetBlock != null) {
					int targetMeta = world.getBlockMetadata(targetX, targetY, targetZ);
					targetBlock.onSubstituted(world, targetX, targetY, targetZ, targetMeta);
				}
				world.setBlockAndMetadataWithNotify(targetX, targetY, targetZ, this.blockID, this.getMetadata(stack.itemDamage));
				blockToPlace.onBlockPlaced(world, targetX, targetY, targetZ, side, xWithinFace, yWithinFace, zWithinFace);
				world.playSoundEffect(
					(double)((float)targetX + 0.5F),
					(double)((float)targetY + 0.5F),
					(double)((float)targetZ + 0.5F),
					blockToPlace.stepSound.getStepSound(),
					(blockToPlace.stepSound.stepSoundVolume + 1.0F) / 2.0F,
					blockToPlace.stepSound.stepSoundPitch * 0.8F);
				--stack.stackSize;
			}
		}

		return true;
	}
}