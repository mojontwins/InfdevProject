package net.minecraft.client.render.entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPig;
import net.minecraft.client.model.ModelSheep;
import net.minecraft.client.model.ModelSheepWool;
import net.minecraft.client.model.ModelSkeleton;
import net.minecraft.client.model.ModelZombie;
import net.minecraft.client.render.RenderEngine;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.entity.EntityPainting;
import net.minecraft.game.entity.animal.EntityPig;
import net.minecraft.game.entity.animal.EntitySheep;
import net.minecraft.game.entity.misc.EntityItem;
import net.minecraft.game.entity.misc.EntityTNT;
import net.minecraft.game.entity.monster.EntityCreeper;
import net.minecraft.game.entity.monster.EntityGiant;
import net.minecraft.game.entity.monster.EntitySkeleton;
import net.minecraft.game.entity.monster.EntitySpider;
import net.minecraft.game.entity.monster.EntityZombie;
import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.entity.projectile.EntityArrow;
import net.minecraft.game.world.World;
import org.lwjgl.opengl.GL11;

public final class RenderManager {
	private Map<Class<? extends Entity>, Render> entityRenderMap = new HashMap<>();
	public static RenderManager instance = new RenderManager();
	public static double renderPosX;
	public static double renderPosY;
	public static double renderPosZ;
	public RenderEngine renderEngine;
	public World worldObj;
	public float playerViewY;
	private double tickPosX;
	private double tickPosY;
	private double tickPosZ;

	private RenderManager() {
		this.entityRenderMap.put(EntitySpider.class, new RenderSpider());
		this.entityRenderMap.put(EntityPig.class, new RenderLiving(new ModelPig(), 0.7F));
		this.entityRenderMap.put(EntitySheep.class, new RenderSheep(new ModelSheep(), new ModelSheepWool(), 0.7F));
		this.entityRenderMap.put(EntityCreeper.class, new RenderCreeper());
		this.entityRenderMap.put(EntitySkeleton.class, new RenderLiving(new ModelSkeleton(), 0.5F));
		this.entityRenderMap.put(EntityZombie.class, new RenderLiving(new ModelZombie(), 0.5F));
		this.entityRenderMap.put(EntityPlayer.class, new RenderPlayer());
		this.entityRenderMap.put(EntityGiant.class, new RenderGiantZombie(new ModelZombie(), 0.5F, 6.0F));
		this.entityRenderMap.put(EntityLiving.class, new RenderLiving(new ModelBiped(), 0.5F));
		this.entityRenderMap.put(Entity.class, new RenderEntity());
		this.entityRenderMap.put(EntityPainting.class, new RenderPainting());
		this.entityRenderMap.put(EntityArrow.class, new RenderArrow());
		this.entityRenderMap.put(EntityItem.class, new RenderItem());
		this.entityRenderMap.put(EntityTNT.class, new RenderTNT());
		Iterator<Render> renderIterator = this.entityRenderMap.values().iterator();

		while(renderIterator.hasNext()) {
			Render render = renderIterator.next();
			render.setRenderManager(this);
		}
	}

	public final Render getEntityRenderObject(Entity entity) {
		Class<? extends Entity> entityClass = entity.getClass();
		Render render = this.entityRenderMap.get(entityClass);
		if(render == null && entityClass != Entity.class) {
			render = this.entityRenderMap.get(entityClass.getSuperclass());
			this.entityRenderMap.put(entityClass, render);
		}

		return render;
	}

	public final void cacheActiveRenderInfo(World world, RenderEngine renderEngine, EntityPlayer player, float partialTick) {
		this.worldObj = world;
		this.renderEngine = renderEngine;
		this.playerViewY = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTick;
		this.tickPosX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)partialTick;
		this.tickPosY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)partialTick;
		this.tickPosZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)partialTick;
	}

	public final void renderEntity(Entity entity, float partialTick) {
		double interpolatedPosX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double)partialTick;
		double interpolatedPosY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double)partialTick;
		double interpolatedPosZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double)partialTick;
		float interpolatedYaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTick;
		float brightness = entity.getEntityBrightness(partialTick);
		GL11.glColor3f(brightness, brightness, brightness);
		this.renderEntityWithPosYaw(entity, interpolatedPosX - renderPosX, interpolatedPosY - renderPosY, interpolatedPosZ - renderPosZ, interpolatedYaw, partialTick);
	}

	public final void renderEntityWithPosYaw(Entity entity, double x, double y, double z, float yaw, float partialTick) {
		Render render = this.getEntityRenderObject(entity);
		if(render != null) {
			render.doRender(entity, x, y, z, yaw, partialTick);
			render.renderShadow(entity, x, y, z, partialTick);
		}

	}

	public final void set(World world) {
		this.worldObj = world;
	}

	public final double getDistanceToCamera(double x, double y, double z) {
		double deltaX = x - this.tickPosX;
		double deltaY = y - this.tickPosY;
		double deltaZ = z - this.tickPosZ;
		return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
	}
}