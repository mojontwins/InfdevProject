package net.minecraft.game.item;

import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.block.tileentity.TileEntitySign;
import util.MathHelper;

/**
 * The item form of a sign. Right-clicking on a solid block places either
 * a freestanding sign (top face) or a wall sign (side face), then opens
 * the sign-editing GUI.
 *
 * <p>Derived from the Minecraft Alpha 1.1.2 {@code ItemSign}, modernised
 * to Java 8 and adapted to this codebase's naming conventions.
 */
public final class ItemSign extends Item {
	public ItemSign(int itemID) {
		super(itemID);
		this.maxDamage = 64;
		this.maxStackSize = 1;
	}

	@Override
	public boolean onItemUse(ItemStack stack, World world, int x, int y, int z, int side) {
		if(side == 0) {
			return false;
		}
		if(!world.getBlockMaterial(x, y, z).isSolid()) {
			return false;
		}

		if(side == 1) {
			++y;
		}
		if(side == 2) {
			--z;
		}
		if(side == 3) {
			++z;
		}
		if(side == 4) {
			--x;
		}
		if(side == 5) {
			++x;
		}

		if(!Block.signStanding.canPlaceBlockAt(world, x, y, z)) {
			return false;
		}

		EntityPlayer player = (EntityPlayer)world.playerEntity;
		if(side == 1) {
			int rotation = MathHelper.floor_double(
				(double)((player.rotationYaw + 180.0F) * 16.0F / 360.0F) + 0.5D) & 15;
			world.setBlockAndMetadataWithNotify(x, y, z, Block.signStanding.blockID, rotation);
		} else {
			world.setBlockAndMetadataWithNotify(x, y, z, Block.signWall.blockID, side);
		}

		--stack.stackSize;
		TileEntitySign sign = (TileEntitySign)world.getBlockTileEntity(x, y, z);
		player.displayGUIEditSign(sign);
		return true;
	}
}
