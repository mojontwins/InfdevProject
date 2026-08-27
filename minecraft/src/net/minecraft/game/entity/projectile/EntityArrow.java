package net.minecraft.game.entity.projectile;

import com.mojang.nbt.NBTTagCompound;
import java.util.List;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.item.Item;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.physics.AxisAlignedBB;
import net.minecraft.game.physics.MovingObjectPosition;
import net.minecraft.game.physics.Vec3D;
import net.minecraft.game.world.World;
import util.MathHelper;

public class EntityArrow extends Entity {
	private int xTile = -1;
	private int yTile = -1;
	private int zTile = -1;
	private int inTile = 0;
	private boolean inGround = false;
	public int arrowShake = 0;
	private EntityLiving shootingEntity;
	private int ticksInGround;
	private int ticksInAir = 0;

	public EntityArrow(World var1, EntityLiving var2) {
		super(var1);
		this.shootingEntity = var2;
		this.setSize(0.5F, 0.5F);
		this.setLocationAndAngles(var2.posX, var2.posY, var2.posZ, var2.rotationYaw, var2.rotationPitch);
		this.posX -= (double)(MathHelper.cos(this.rotationYaw / 180.0F * (float)Math.PI) * 0.16F);
		this.posY -= (double)0.1F;
		this.posZ -= (double)(MathHelper.sin(this.rotationYaw / 180.0F * (float)Math.PI) * 0.16F);
		this.setPosition(this.posX, this.posY, this.posZ);
		this.yOffset = 0.0F;
		this.motionX = (double)(-MathHelper.sin(this.rotationYaw / 180.0F * (float)Math.PI) * MathHelper.cos(this.rotationPitch / 180.0F * (float)Math.PI));
		this.motionZ = (double)(MathHelper.cos(this.rotationYaw / 180.0F * (float)Math.PI) * MathHelper.cos(this.rotationPitch / 180.0F * (float)Math.PI));
		this.motionY = (double)(-MathHelper.sin(this.rotationPitch / 180.0F * (float)Math.PI));
		this.setArrowHeading(this.motionX, this.motionY, this.motionZ, 1.5F, 1.0F);
	}

	public final void setArrowHeading(double var1, double var3, double var5, float var7, float var8) {
		float var9 = MathHelper.sqrt_double(var1 * var1 + var3 * var3 + var5 * var5);
		var1 /= (double)var9;
		var3 /= (double)var9;
		var5 /= (double)var9;
		var1 += this.rand.nextGaussian() * (double)0.0075F * (double)var8;
		var3 += this.rand.nextGaussian() * (double)0.0075F * (double)var8;
		var5 += this.rand.nextGaussian() * (double)0.0075F * (double)var8;
		var1 *= (double)var7;
		var3 *= (double)var7;
		var5 *= (double)var7;
		this.motionX = var1;
		this.motionY = var3;
		this.motionZ = var5;
		var7 = MathHelper.sqrt_double(var1 * var1 + var5 * var5);
		this.prevRotationYaw = this.rotationYaw = (float)(Math.atan2(var1, var5) * 180.0D / (double)((float)Math.PI));
		this.prevRotationPitch = this.rotationPitch = (float)(Math.atan2(var3, (double)var7) * 180.0D / (double)((float)Math.PI));
		this.ticksInGround = 0;
	}

	public final void onUpdate() {
		super.onUpdate();
		if(this.arrowShake > 0) {
			--this.arrowShake;
		}

		if(this.inGround) {
			int var1 = this.worldObj.getBlockId(this.xTile, this.yTile, this.zTile);
			if(var1 == this.inTile) {
				++this.ticksInGround;
				if(this.ticksInGround == 1200) {
					super.isDead = true;
				}

				return;
			}

			this.inGround = false;
			this.motionX *= (double)(this.rand.nextFloat() * 0.2F);
			this.motionY *= (double)(this.rand.nextFloat() * 0.2F);
			this.motionZ *= (double)(this.rand.nextFloat() * 0.2F);
			this.ticksInGround = 0;
			this.ticksInAir = 0;
		} else {
			++this.ticksInAir;
		}

		Vec3D var14 = new Vec3D(this.posX, this.posY, this.posZ);
		Vec3D var2 = new Vec3D(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
		MovingObjectPosition var3 = this.worldObj.rayTraceBlocks(var14, var2);
		var14 = new Vec3D(this.posX, this.posY, this.posZ);
		var2 = new Vec3D(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
		if(var3 != null) {
			var2 = new Vec3D(var3.hitVec.xCoord, var3.hitVec.yCoord, var3.hitVec.zCoord);
		}

		Entity var4 = null;
		List<Entity> var5 = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.boundingBox.addCoord(this.motionX, this.motionY, this.motionZ).expand(1.0D, 1.0D, 1.0D));
		double var6 = 0.0D;

		for(int var8 = 0; var8 < var5.size(); ++var8) {
			Entity var9 = var5.get(var8);
			if(var9.canBeCollidedWith() && (var9 != this.shootingEntity || this.ticksInAir >= 5)) {
				AxisAlignedBB var10 = var9.boundingBox.expand((double)0.3F, (double)0.3F, (double)0.3F);
				MovingObjectPosition var17 = var10.calculateIntercept(var14, var2);
				if(var17 != null) {
					double var12 = var14.distance(var17.hitVec);
					if(var12 < var6 || var6 == 0.0D) {
						var4 = var9;
						var6 = var12;
					}
				}
			}
		}

		if(var4 != null) {
			var3 = new MovingObjectPosition(var4);
		}

		float var15;
		if(var3 != null) {
			if(var3.entityHit != null) {
				if(var3.entityHit.attackEntityFrom(this, 4)) {
					this.worldObj.playSoundAtEntity(this, "random.drr", 1.0F, 1.2F / (this.rand.nextFloat() * 0.2F + 0.9F));
					super.isDead = true;
				} else {
					this.motionX *= (double)-0.1F;
					this.motionY *= (double)-0.1F;
					this.motionZ *= (double)-0.1F;
					this.rotationYaw += 180.0F;
					this.prevRotationYaw += 180.0F;
					this.ticksInAir = 0;
				}
			} else {
				this.xTile = var3.blockX;
				this.yTile = var3.blockY;
				this.zTile = var3.blockZ;
				this.inTile = this.worldObj.getBlockId(this.xTile, this.yTile, this.zTile);
				this.motionX = (double)((float)(var3.hitVec.xCoord - this.posX));
				this.motionY = (double)((float)(var3.hitVec.yCoord - this.posY));
				this.motionZ = (double)((float)(var3.hitVec.zCoord - this.posZ));
				var15 = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ);
				this.posX -= this.motionX / (double)var15 * (double)0.05F;
				this.posY -= this.motionY / (double)var15 * (double)0.05F;
				this.posZ -= this.motionZ / (double)var15 * (double)0.05F;
				this.worldObj.playSoundAtEntity(this, "random.drr", 1.0F, 1.2F / (this.rand.nextFloat() * 0.2F + 0.9F));
				this.inGround = true;
				this.arrowShake = 7;
			}
		}

		this.posX += this.motionX;
		this.posY += this.motionY;
		this.posZ += this.motionZ;
		var15 = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
		this.rotationYaw = (float)(Math.atan2(this.motionX, this.motionZ) * 180.0D / (double)((float)Math.PI));

		for(this.rotationPitch = (float)(Math.atan2(this.motionY, (double)var15) * 180.0D / (double)((float)Math.PI)); this.rotationPitch - this.prevRotationPitch < -180.0F; this.prevRotationPitch -= 360.0F) {
		}

		while(this.rotationPitch - this.prevRotationPitch >= 180.0F) {
			this.prevRotationPitch += 360.0F;
		}

		while(this.rotationYaw - this.prevRotationYaw < -180.0F) {
			this.prevRotationYaw -= 360.0F;
		}

		while(this.rotationYaw - this.prevRotationYaw >= 180.0F) {
			this.prevRotationYaw += 360.0F;
		}

		this.rotationPitch = this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * 0.2F;
		this.rotationYaw = this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw) * 0.2F;
		float var16 = 0.99F;
		if(this.handleWaterMovement()) {
			for(int var18 = 0; var18 < 4; ++var18) {
				this.worldObj.spawnParticle("bubble", this.posX - this.motionX * 0.25D, this.posY - this.motionY * 0.25D, this.posZ - this.motionZ * 0.25D, this.motionX, this.motionY, this.motionZ);
			}

			var16 = 0.8F;
		}

		this.motionX *= (double)var16;
		this.motionY *= (double)var16;
		this.motionZ *= (double)var16;
		this.motionY -= (double)0.03F;
		this.setPosition(this.posX, this.posY, this.posZ);
	}

	public final void writeEntityToNBT(NBTTagCompound var1) {
		var1.setShort("xTile", (short)this.xTile);
		var1.setShort("yTile", (short)this.yTile);
		var1.setShort("zTile", (short)this.zTile);
		var1.setByte("inTile", (byte)this.inTile);
		var1.setByte("shake", (byte)this.arrowShake);
		var1.setByte("inGround", (byte)(this.inGround ? 1 : 0));
	}

	public final void readEntityFromNBT(NBTTagCompound var1) {
		this.xTile = var1.getShort("xTile");
		this.yTile = var1.getShort("yTile");
		this.zTile = var1.getShort("zTile");
		this.inTile = var1.getByte("inTile") & 255;
		this.arrowShake = var1.getByte("shake") & 255;
		this.inGround = var1.getByte("inGround") == 1;
	}

	public final void onCollideWithPlayer(EntityPlayer var1) {
		if(this.inGround && this.shootingEntity == var1 && this.arrowShake <= 0 && var1.inventory.storePartialItemStack(new ItemStack(Item.arrow.shiftedIndex, 1))) {
			this.worldObj.playSoundAtEntity(this, "random.pop", 0.2F, ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);
			var1.onItemPickup(this);
			super.isDead = true;
		}

	}
}
