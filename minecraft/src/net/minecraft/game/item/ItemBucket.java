package net.minecraft.game.item;

import net.minecraft.game.entity.animal.EntityCow;
import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.physics.MovingObjectPosition;
import net.minecraft.game.physics.Vec3D;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.material.Material;
import util.MathHelper;

/**
 * A bucket — empty, water, lava or milk — and its right-click logic.
 *
 * <p>Empty: right-clicking a source block of water or lava picks it up; the
 * block disappears and the empty bucket becomes a water / lava bucket.
 * Right-clicking a cow milks it.
 *
 * <p>Water / lava: right-clicking a block face places the corresponding
 * fluid source in the neighbour cell. The neighbour is resolved through
 * {@link #neighbourAcrossFace} — the same helper used by
 * {@link ItemBlock} and {@link ItemFlintAndSteel} — so the placement rules
 * are consistent across block items. The placed block id is the field
 * {@link #isFull}: 8 = waterMoving, 10 = lavaMoving.
 *
 * <p>Milk: right-clicking a block face consumes the bucket (returns empty).
 * A milk bucket cannot be refilled from a cow in this code — that is the
 * a1.1.2 semantics, faithfully preserved.
 *
 * <p>Empty + picking up water plays {@code liquid.water}; lava plays
 * {@code liquid.lava}; placing water plays {@code liquid.water} and lava
 * plays {@code random.fizz}. The placed fluid's own block-tick pass then
 * handles any lava-meets-water reaction.
 */
public class ItemBucket extends Item {
	/** 0 = empty, 8 = waterMoving, 10 = lavaMoving, -1 = milk. */
	private final int isFull;

	public ItemBucket(int itemID, int isFull) {
		super(itemID);
		this.maxStackSize = 1;
		this.maxDamage = 64;
		this.isFull = isFull;
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		float partialTick = 1.0F;
		float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTick;
		float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTick;
		double eyeX = player.prevPosX + (player.posX - player.prevPosX) * (double) partialTick;
		double eyeY = player.prevPosY + (player.posY - player.prevPosY) * (double) partialTick;
		double eyeZ = player.prevPosZ + (player.posZ - player.prevPosZ) * (double) partialTick;
		Vec3D eye = new Vec3D(eyeX, eyeY, eyeZ);

		float cosYaw = MathHelper.cos(-yaw * ((float) Math.PI / 180.0F) - (float) Math.PI);
		float sinYaw = MathHelper.sin(-yaw * ((float) Math.PI / 180.0F) - (float) Math.PI);
		float cosPitch = -MathHelper.cos(-pitch * ((float) Math.PI / 180.0F));
		float sinPitch = MathHelper.sin(-pitch * ((float) Math.PI / 180.0F));
		float dirX = sinYaw * cosPitch;
		float dirZ = cosYaw * cosPitch;
		double reach = 5.0D;
		Vec3D target = eye.addVector((double) dirX * reach, (double) sinPitch * reach, (double) dirZ * reach);

		// Empty bucket: stop on liquids (so the trace finds the water / lava
		// source cell).  Filled / milk buckets pass through the fluid.
		MovingObjectPosition hit = world.rayTraceBlocks(eye, target, this.isFull == 0);
		if (hit == null) {
			return stack;
		}

		if (hit.typeOfHit == MovingObjectPosition.HIT_BLOCK) {
			int x = hit.blockX;
			int y = hit.blockY;
			int z = hit.blockZ;

			if (this.isFull == 0) {
				// Empty bucket: pick up a water / lava source block.
				Material mat = world.getBlockMaterial(x, y, z);
				if (mat == Material.water && world.getBlockMetadata(x, y, z) == 0) {
					world.setBlockWithNotify(x, y, z, 0);
					world.playSoundEffect(
						(double) ((float) x + 0.5F), (double) ((float) y + 0.5F), (double) ((float) z + 0.5F),
						"liquid.water", 0.5F, world.rand.nextFloat() * 0.1F + 0.9F);
					return new ItemStack(Item.bucketWater);
				}
				if (mat == Material.lava && world.getBlockMetadata(x, y, z) == 0) {
					world.setBlockWithNotify(x, y, z, 0);
					world.playSoundEffect(
						(double) ((float) x + 0.5F), (double) ((float) y + 0.5F), (double) ((float) z + 0.5F),
						"liquid.lava", 0.5F, world.rand.nextFloat() * 0.4F + 1.6F);
					return new ItemStack(Item.bucketLava);
				}
			} else if (this.isFull > 0) {
				// Filled bucket: place the fluid in the neighbour cell across
				// the struck face, but only if the neighbour is empty or
				// non-solid.
				int[] neighbour = neighbourAcrossFace(hit.sideHit, x, y, z);
				int nx = neighbour[0];
				int ny = neighbour[1];
				int nz = neighbour[2];
				if (world.getBlockId(nx, ny, nz) == 0 || !world.getBlockMaterial(nx, ny, nz).isSolid()) {
					world.setBlockAndMetadataWithNotify(nx, ny, nz, this.isFull, 0);
					world.playSoundEffect(
						(double) ((float) nx + 0.5F), (double) ((float) ny + 0.5F), (double) ((float) nz + 0.5F),
						this.isFull == Block.lavaMoving.blockID ? "random.fizz" : "liquid.water",
						0.5F,
						this.isFull == Block.lavaMoving.blockID
							? 2.6F + world.rand.nextFloat() * 0.4F
							: world.rand.nextFloat() * 0.1F + 0.9F);
					return new ItemStack(Item.bucketEmpty);
				}
			} else {
				// Milk bucket on a block face: consume (return empty).
				return new ItemStack(Item.bucketEmpty);
			}
		} else if (this.isFull == 0 && hit.entityHit instanceof EntityCow) {
			// Empty bucket: milk a cow.
			return new ItemStack(Item.bucketMilk);
		}

		return stack;
	}
}
