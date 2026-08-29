package net.minecraft.game.entity.misc;

import com.mojang.nbt.NBTTagCompound;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.material.Material;
import util.MathHelper;

/**
 * A dropped item lying or floating in the world: it falls, sinks into lava,
 * pushes itself out of any opaque block it ends up partially inside of, and
 * despawns or is picked up by a player after 6000 ticks.
 */
public class EntityItem extends Entity {
	/** The stack being carried. Public because the renderer needs it. */
	public ItemStack item;
	public int age = 0;
	/** Tick delay before a player may pick this up (spawned drops cannot be picked up instantly). */
	public int delayBeforeCanPickup;
	private int health = 5;
	/** Random phase offset for the item's hover bobbing. Public because the renderer reads it. */
	public float hoverStart = (float) (Math.random() * Math.PI * 2.0D);

	public EntityItem(World world, double x, double y, double z, ItemStack stack) {
		super(world);
		this.setSize(0.25F, 0.25F);
		this.yOffset = this.height / 2.0F;
		this.setPosition(x, y, z);
		this.item = stack;
		this.rotationYaw = (float) (Math.random() * 360.0D);
		this.motionX = (double) ((float) (Math.random() * (double) 0.2F - (double) 0.1F));
		this.motionY = (double) 0.2F;
		this.motionZ = (double) ((float) (Math.random() * (double) 0.2F - (double) 0.1F));
		this.entityWalks = false;
	}

	public EntityItem(World world) {
		super(world);
	}

	public final void onUpdate() {
		super.onUpdate();
		if (this.delayBeforeCanPickup > 0) {
			--this.delayBeforeCanPickup;
		}

		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;
		this.motionY -= (double) 0.04F;
		if (this.worldObj.getBlockMaterial(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY), MathHelper.floor_double(this.posZ)) == Material.lava) {
			this.motionY = (double) 0.2F;
			this.motionX = (double) ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F);
			this.motionZ = (double) ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F);
			this.worldObj.playSoundAtEntity(this, "random.fizz", 0.4F, 2.0F + this.rand.nextFloat() * 0.4F);
		}

		double centerX = this.posX;
		double centerY = this.posY;
		double centerZ = this.posZ;
		int blockX = MathHelper.floor_double(centerX);
		int blockY = MathHelper.floor_double(centerY);
		int blockZ = MathHelper.floor_double(centerZ);
		double xInBlock = centerX - (double) blockX;
		double yInBlock = centerY - (double) blockY;
		double zInBlock = centerZ - (double) blockZ;
		if (Block.opaqueCubeLookup[this.worldObj.getBlockId(blockX, blockY, blockZ)]) {
			// The item is partially sunk into an opaque block; push it out through
			// whichever of its open faces it is closest to.
			boolean openFaceMinusX = !Block.opaqueCubeLookup[this.worldObj.getBlockId(blockX - 1, blockY, blockZ)];
			boolean openFacePlusX = !Block.opaqueCubeLookup[this.worldObj.getBlockId(blockX + 1, blockY, blockZ)];
			boolean openFaceMinusY = !Block.opaqueCubeLookup[this.worldObj.getBlockId(blockX, blockY - 1, blockZ)];
			boolean openFacePlusY = !Block.opaqueCubeLookup[this.worldObj.getBlockId(blockX, blockY + 1, blockZ)];
			boolean openFaceMinusZ = !Block.opaqueCubeLookup[this.worldObj.getBlockId(blockX, blockY, blockZ - 1)];
			boolean openFacePlusZ = !Block.opaqueCubeLookup[this.worldObj.getBlockId(blockX, blockY, blockZ + 1)];
			byte pushDirection = -1;
			double closestDistance = 9999.0D;
			if (openFaceMinusX && xInBlock < closestDistance) {
				closestDistance = xInBlock;
				pushDirection = 0;
			}

			if (openFacePlusX && 1.0D - xInBlock < closestDistance) {
				closestDistance = 1.0D - xInBlock;
				pushDirection = 1;
			}

			if (openFaceMinusY && yInBlock < closestDistance) {
				closestDistance = yInBlock;
				pushDirection = 2;
			}

			if (openFacePlusY && 1.0D - yInBlock < closestDistance) {
				closestDistance = 1.0D - yInBlock;
				pushDirection = 3;
			}

			if (openFaceMinusZ && zInBlock < closestDistance) {
				closestDistance = zInBlock;
				pushDirection = 4;
			}

			if (openFacePlusZ && 1.0D - zInBlock < closestDistance) {
				pushDirection = 5;
			}

			float pushSpeed = this.rand.nextFloat() * 0.2F + 0.1F;
			switch (pushDirection) {
				case 0:
					this.motionX = (double) (-pushSpeed);
					break;
				case 1:
					this.motionX = (double) pushSpeed;
					break;
				case 2:
					this.motionY = (double) (-pushSpeed);
					break;
				case 3:
					this.motionY = (double) pushSpeed;
					break;
				case 4:
					this.motionZ = (double) (-pushSpeed);
					break;
				case 5:
					this.motionZ = (double) pushSpeed;
			}
		}

		this.moveEntity(this.motionX, this.motionY, this.motionZ);
		this.motionX *= (double) 0.98F;
		this.motionY *= (double) 0.98F;
		this.motionZ *= (double) 0.98F;
		if (this.onGround) {
			this.motionX *= (double) 0.7F;
			this.motionZ *= (double) 0.7F;
			this.motionY *= -0.5D;
		}

		++this.age;
		if (this.age >= 6000) {
			super.isDead = true;
		}
	}

	protected final void dealFireDamage(int damage) {
		this.attackEntityFrom((Entity) null, 1);
	}

	public final boolean attackEntityFrom(Entity entity, int damage) {
		this.health -= damage;
		if (this.health <= 0) {
			super.isDead = true;
		}

		return false;
	}

	public final void writeEntityToNBT(NBTTagCompound tag) {
		// Written as a (byte-promoted-to-)short for fidelity with the original save format.
		tag.setShort("Health", (byte) this.health);
		tag.setShort("Age", (short) this.age);
		tag.setCompoundTag("Item", this.item.writeToNBT(new NBTTagCompound()));
	}

	public final void readEntityFromNBT(NBTTagCompound tag) {
		this.health = tag.getShort("Health") & 255;
		this.age = tag.getShort("Age");
		tag = tag.getCompoundTag("Item");
		this.item = new ItemStack(tag);
	}

	public final void onCollideWithPlayer(EntityPlayer player) {
		if (this.delayBeforeCanPickup == 0 && player.inventory.storePartialItemStack(this.item)) {
			this.worldObj.playSoundAtEntity(this, "random.pop", 0.2F, ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);
			player.onItemPickup(this);
			super.isDead = true;
		}
	}
}