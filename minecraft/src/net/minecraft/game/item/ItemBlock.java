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
	 * Places the block on the far side of the clicked face, but only where the
	 * target cell is empty or holds replaceable material (fluids, fire) and the
	 * block's own placement rules pass. The exact click position on the face is
	 * forwarded so blocks can react to where precisely they were placed.
	 */
	@Override
	public final boolean onItemUse(ItemStack stack, World world, int x, int y, int z, int side, float xWithinFace, float yWithinFace, float zWithinFace) {
		int[] target = neighbourAcrossFace(side, x, y, z);
		x = target[0];
		y = target[1];
		z = target[2];
		if (stack.stackSize == 0) {
			return false;
		} else {
			Block existingBlock = Block.blocksList[world.getBlockId(x, y, z)];
			AxisAlignedBB placementBox = Block.blocksList[this.blockID].getCollisionBoundingBoxFromPool(x, y, z);
			if ((this.blockID > 0 && existingBlock == null) || existingBlock == Block.waterMoving || existingBlock == Block.waterStill || existingBlock == Block.lavaMoving || existingBlock == Block.lavaStill || existingBlock == Block.fire) {
				Block blockToPlace = Block.blocksList[this.blockID];
				if ((placementBox == null || world.checkIfAABBIsClear1(placementBox)) && blockToPlace.canPlaceBlockAt(world, x, y, z) && world.setBlockWithNotify(x, y, z, this.blockID)) {
					blockToPlace.onBlockPlaced(world, x, y, z, side, xWithinFace, yWithinFace, zWithinFace);
					world.playSoundEffect((double) ((float) x + 0.5F), (double) ((float) y + 0.5F), (double) ((float) z + 0.5F), blockToPlace.stepSound.getStepSound(), (blockToPlace.stepSound.stepSoundVolume + 1.0F) / 2.0F, blockToPlace.stepSound.stepSoundPitch * 0.8F);
					--stack.stackSize;
				}
			}

			return true;
		}
	}
}