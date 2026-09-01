package net.minecraft.game.entity.misc;

import com.mojang.nbt.NBTTagCompound;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.block.BlockSand;
import util.MathHelper;

/**
 * A block that fell off its support (sand or gravel), or is still in free fall
 * after the world generation placed it mid-air: it obeys gravity until it hits
 * solid ground and then is replaced by the actual block again. Note that the
 * "FallingSand" name is shared with the block-entity mechanic introduced much
 * later in the game's history; here it is a plain entity.
 */
public class EntityFallingSand extends Entity {
	public int blockID;
	public int fallTime = 0;

	public EntityFallingSand(World world) {
		super(world);
	}

	public EntityFallingSand(World world, double posX, double posY, double posZ, int blockID) {
		super(world);
		this.blockID = blockID;
		this.preventEntitySpawning = true;
		this.setSize(0.98F, 0.98F);
		this.yOffset = this.height / 2.0F;
		this.setPosition(posX, posY, posZ);
		this.motionX = 0.0D;
		this.motionY = 0.0D;
		this.motionZ = 0.0D;
		this.entityWalks = false;
		this.prevPosX = posX;
		this.prevPosY = posY;
		this.prevPosZ = posZ;
		
	}

	@Override
	public final boolean canBeCollidedWith() {
		return !this.isDead;
	}

	@Override
	public final void onUpdate() {
		if(this.blockID == 0) {
			this.isDead = true;
		} else {
			this.prevPosX = this.posX;
			this.prevPosY = this.posY;
			this.prevPosZ = this.posZ;
			++this.fallTime;
			this.motionY -= 0.04F;
			this.moveEntity(this.motionX, this.motionY, this.motionZ);
			this.motionX *= 0.98F;
			this.motionY *= 0.98F;
			this.motionZ *= 0.98F;
			int blockX = MathHelper.floor_double(this.posX);
			int blockY = MathHelper.floor_double(this.posY);
			int blockZ = MathHelper.floor_double(this.posZ);
			if(this.worldObj.getBlockId(blockX, blockY, blockZ) == this.blockID) {
				this.worldObj.setBlockWithNotify(blockX, blockY, blockZ, 0);
			}
			if(this.onGround) {
				this.motionX *= 0.7F;
				this.motionZ *= 0.7F;
				this.motionY *= -0.5D;
				this.isDead = true;
				if(!this.canBlockBePlacedAt(blockX, blockY, blockZ) || BlockSand.canFallBelow(this.worldObj, blockX, blockY - 1, blockZ) || !this.worldObj.setBlockWithNotify(blockX, blockY, blockZ, this.blockID)) {
					this.dropItemWithOffset(this.blockID, 1);
				}
			} else if(this.fallTime > 100) {
				this.dropItemWithOffset(this.blockID, 1);
				this.isDead = true;
			}
		}
	}

	private boolean canBlockBePlacedAt(int x, int y, int z) {
		Block existingBlock = Block.blocksList[this.worldObj.getBlockId(x, y, z)];
		boolean cellIsClear = existingBlock == null || existingBlock.canBeSubstituted();
		return cellIsClear && Block.blocksList[this.blockID].canPlaceBlockAt(this.worldObj, x, y, z);
	}

	@Override
	protected final void writeEntityToNBT(NBTTagCompound tag) {
		tag.setByte("Tile", (byte)this.blockID);
	}

	@Override
	protected final void readEntityFromNBT(NBTTagCompound tag) {
		this.blockID = tag.getByte("Tile") & 255;
	}

	public World getWorld() {
		return this.worldObj;
	}
}