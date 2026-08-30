package net.minecraft.client.effect;

import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.entity.RenderManager;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.world.World;
import org.lwjgl.opengl.GL11;
import util.MathHelper;

// Renders the "item flying into your inventory" effect: the picked-up entity
// animates along an eased arc from its drop point up to the player, offset by
// yOffs to settle into the hotbar slot area.
public final class EntityPickupFX extends EntityFX {
	private Entity entityToPickUp;
	private EntityLiving entityPickingUp;
	private int age = 0;
	private int maxAge = 0;
	private float yOffs;

	public EntityPickupFX(World world, Entity entityToPickUp, EntityLiving entityPickingUp, float scale) {
		super(world, entityToPickUp.posX, entityToPickUp.posY, entityToPickUp.posZ, entityToPickUp.motionX, entityToPickUp.motionY, entityToPickUp.motionZ);
		this.entityToPickUp = entityToPickUp;
		this.entityPickingUp = entityPickingUp;
		this.maxAge = 3;
		this.yOffs = -0.5F;
	}

	public final void renderParticle(Tessellator tessellator, float partialTick, float offsetX, float offsetY, float offsetZ, float surfU, float surfV) {
		// Normalized interpolation factor; squaring it gives an ease-in that
		// starts fast then slows as the item arcs toward its target.
		float progress = ((float)this.age + partialTick) / (float)this.maxAge;
		progress *= progress;
		double startX = this.entityToPickUp.posX;
		double startY = this.entityToPickUp.posY;
		double startZ = this.entityToPickUp.posZ;
		double targetX = this.entityPickingUp.lastTickPosX + (this.entityPickingUp.posX - this.entityPickingUp.lastTickPosX) * (double)partialTick;
		double targetY = this.entityPickingUp.lastTickPosY + (this.entityPickingUp.posY - this.entityPickingUp.lastTickPosY) * (double)partialTick + (double)this.yOffs;
		double targetZ = this.entityPickingUp.lastTickPosZ + (this.entityPickingUp.posZ - this.entityPickingUp.lastTickPosZ) * (double)partialTick;
		double interpolatedX = startX + (targetX - startX) * (double)progress;
		double interpolatedY = startY + (targetY - startY) * (double)progress;
		double interpolatedZ = startZ + (targetZ - startZ) * (double)progress;
		int blockX = MathHelper.floor_double(interpolatedX);
		int blockY = MathHelper.floor_double(interpolatedY + (double)(this.yOffset / 2.0F));
		int blockZ = MathHelper.floor_double(interpolatedZ);
		// Color the item by the brightness of the block it currently hovers over.
		float brightness = this.worldObj.getBrightness(blockX, blockY, blockZ);
		interpolatedX -= interpPosX;
		interpolatedY -= interpPosY;
		interpolatedZ -= interpPosZ;
		GL11.glColor4f(brightness, brightness, brightness, 1.0F);
		RenderManager.instance.renderEntityWithPosYaw(this.entityToPickUp, (double)((float)interpolatedX), (double)((float)interpolatedY), (double)((float)interpolatedZ), this.entityToPickUp.rotationYaw, partialTick);
	}

	public final void onUpdate() {
		++this.age;
		if(this.age == this.maxAge) {
			super.isDead = true;
		}

	}

	public final int getFXLayer() {
		return 2;
	}
}
