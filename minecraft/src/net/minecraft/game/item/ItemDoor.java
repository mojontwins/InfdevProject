package net.minecraft.game.item;

import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.material.Material;
import util.MathHelper;

/**
 * The item form of a wooden or iron door. Right-clicking on the top face
 * of a solid block places a two-block-tall door. The facing direction is
 * determined by the player's yaw and the surrounding wall layout (mirrored
 * when the opposite wall is closer).
 *
 * <p>Derived from the Minecraft Alpha 1.1.2 {@code ItemDoor}, modernised
 * to Java 8 and adapted to this codebase's naming conventions.
 */
public final class ItemDoor extends Item {
	private final Material material;

	public ItemDoor(int itemID, Material material) {
		super(itemID);
		this.material = material;
		this.maxDamage = 64;
		this.maxStackSize = 1;
	}

	@Override
	public boolean onItemUse(ItemStack stack, World world, int x, int y, int z, int side) {
		if(side != 1) {
			return false;
		}

		++y;

		Block doorBlock;
		if(this.material == Material.wood) {
			doorBlock = Block.doorWood;
		} else {
			doorBlock = Block.doorSteel;
		}

		if(!doorBlock.canPlaceBlockAt(world, x, y, z)) {
			return false;
		}

		EntityPlayer player = (EntityPlayer)world.playerEntity;
		int facing = MathHelper.floor_double(
			(double)((player.rotationYaw + 180.0F) * 4.0F / 360.0F) - 0.5D) & 3;

		int offsetX = 0;
		int offsetZ = 0;
		if(facing == 0) {
			offsetZ = 1;
		}
		if(facing == 1) {
			offsetX = -1;
		}
		if(facing == 2) {
			offsetZ = -1;
		}
		if(facing == 3) {
			offsetX = 1;
		}

		int solidLeft = (world.isSolid(x - offsetX, y, z - offsetZ) ? 1 : 0)
			+ (world.isSolid(x - offsetX, y + 1, z - offsetZ) ? 1 : 0);
		int solidRight = (world.isSolid(x + offsetX, y, z + offsetZ) ? 1 : 0)
			+ (world.isSolid(x + offsetX, y + 1, z + offsetZ) ? 1 : 0);

		boolean doorAtLeft = world.getBlockId(x - offsetX, y, z - offsetZ) == doorBlock.blockID
			|| world.getBlockId(x - offsetX, y + 1, z - offsetZ) == doorBlock.blockID;
		boolean doorAtRight = world.getBlockId(x + offsetX, y, z + offsetZ) == doorBlock.blockID
			|| world.getBlockId(x + offsetX, y + 1, z + offsetZ) == doorBlock.blockID;

		boolean mirror = false;
		if(doorAtLeft && !doorAtRight) {
			mirror = true;
		} else if(solidRight > solidLeft) {
			mirror = true;
		}

		if(mirror) {
			facing = (facing - 1) & 3;
			facing += 4;
		}

		world.setBlockWithNotify(x, y, z, doorBlock.blockID);
		world.setBlockMetadataWithNotify(x, y, z, facing);
		world.setBlockWithNotify(x, y + 1, z, doorBlock.blockID);
		world.setBlockMetadataWithNotify(x, y + 1, z, facing + 8);

		--stack.stackSize;
		return true;
	}
}
