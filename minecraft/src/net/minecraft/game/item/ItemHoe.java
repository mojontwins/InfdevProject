package net.minecraft.game.item;

import net.minecraft.game.entity.misc.EntityItem;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.material.Material;

/**
 * The hoe turns grass or dirt into a tilled field (unless something solid is
 * sitting on the spot), and — like the game's classic easter egg — sometimes
 * seeds drop out of the freshly turned grass.
 */
public final class ItemHoe extends Item {
	public ItemHoe(int itemID, int materialTier) {
		super(itemID);
		this.maxStackSize = 1;
		this.maxDamage = 32 << materialTier;
	}

	@Override
	public final boolean onItemUse(ItemStack stack, World world, int x, int y, int z, int side) {
		int clickedBlockID = world.getBlockId(x, y, z);
		Material materialAbove = world.getBlockMaterial(x, y + 1, z);
		if ((materialAbove.isSolid() || clickedBlockID != Block.grass.blockID) && clickedBlockID != Block.dirt.blockID) {
			return false;
		} else {
			Block tilledField = Block.tilledField;
			world.playSoundEffect((double) ((float) x + 0.5F), (double) ((float) y + 0.5F), (double) ((float) z + 0.5F), tilledField.stepSound.getStepSound(), (tilledField.stepSound.stepSoundVolume + 1.0F) / 2.0F, tilledField.stepSound.stepSoundPitch * 0.8F);
			world.setBlockWithNotify(x, y, z, tilledField.blockID);
			stack.damageItem(1);
			if (world.rand.nextInt(8) == 0 && clickedBlockID == Block.grass.blockID) {
				float offsetX = world.rand.nextFloat() * 0.7F + 0.15F;
				float offsetZ = world.rand.nextFloat() * 0.7F + 0.15F;
				EntityItem seedDrop = new EntityItem(world, (double) ((float) x + offsetX), (double) ((float) y + 1.2F), (double) ((float) z + offsetZ), new ItemStack(Item.seeds));
				seedDrop.delayBeforeCanPickup = 10;
				world.spawnEntityInWorld(seedDrop);
			}

			return true;
		}
	}
}