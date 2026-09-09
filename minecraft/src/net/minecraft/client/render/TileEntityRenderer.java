package net.minecraft.client.render;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.render.tileentity.TileEntitySignRenderer;
import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.tileentity.TileEntity;
import net.minecraft.game.world.block.tileentity.TileEntitySign;
import org.lwjgl.opengl.GL11;

/**
 * Singleton registry and dispatcher for special tile-entity renderers. One
 * {@link TileEntitySpecialRenderer} is registered per tile-entity class; a
 * lookup falls back through the superclass chain, so subclasses without their
 * own renderer inherit the base class's. Before a pass of tile-entity
 * rendering, the caller snapshots the render state (world, render engine, font
 * and the interpolated camera) with {@link #cacheActiveRenderInfo}, then draws
 * each tile entity with {@link #renderTileEntity}.
 */
public class TileEntityRenderer {
	private final Map<Class<? extends TileEntity>, TileEntitySpecialRenderer> specialRendererMap = new HashMap<>();
	public static TileEntityRenderer instance = new TileEntityRenderer();
	private FontRenderer fontRenderer;
	/** Static (non-interpolated) camera position; the base of the world-space GL offsets. */
	public static double staticPlayerX;
	public static double staticPlayerY;
	public static double staticPlayerZ;
	public RenderEngine renderEngine;
	public World worldObj;
	public EntityPlayer entityPlayer;
	/** Interpolated camera yaw/pitch/position snapshot for the current frame. */
	public float playerYaw;
	public float playerPitch;
	public double playerX;
	public double playerY;
	public double playerZ;

	private TileEntityRenderer() {
		this.specialRendererMap.put(TileEntitySign.class, new TileEntitySignRenderer());

		for(TileEntitySpecialRenderer specialRenderer : this.specialRendererMap.values()) {
			specialRenderer.setTileEntityRenderer(this);
		}

	}

	/**
	 * Returns the special renderer for the given tile-entity class, walking up
	 * the class hierarchy until a registration is found. The result (even a
	 * miss) is cached on the concrete class so the walk happens only once.
	 */
	public TileEntitySpecialRenderer getSpecialRendererForClass(Class<? extends TileEntity> tileEntityClass) {
		TileEntitySpecialRenderer specialRenderer = this.specialRendererMap.get(tileEntityClass);
		if(specialRenderer == null && tileEntityClass != TileEntity.class) {
			specialRenderer = this.getSpecialRendererForClass(tileEntityClass.getSuperclass().asSubclass(TileEntity.class));
			this.specialRendererMap.put(tileEntityClass, specialRenderer);
		}

		return specialRenderer;
	}

	/** True when a special renderer is registered for the given tile entity's class. */
	public boolean hasSpecialRenderer(TileEntity tileEntity) {
		return this.getSpecialRendererForEntity(tileEntity) != null;
	}

	/** Returns the special renderer for the given tile entity's own class, or null. */
	public TileEntitySpecialRenderer getSpecialRendererForEntity(TileEntity tileEntity) {
		return this.getSpecialRendererForClass(tileEntity.getClass());
	}

	/**
	 * Snapshots the render state for an upcoming pass: the world, render
	 * engine, font renderer and the camera's yaw/pitch/position interpolated
	 * by {@code partialTick}.
	 */
	public void cacheActiveRenderInfo(World world, RenderEngine renderEngine, FontRenderer fontRenderer, EntityPlayer entityPlayer, float partialTick) {
		this.worldObj = world;
		this.renderEngine = renderEngine;
		this.entityPlayer = entityPlayer;
		this.fontRenderer = fontRenderer;
		this.playerYaw = entityPlayer.prevRotationYaw + (entityPlayer.rotationYaw - entityPlayer.prevRotationYaw) * partialTick;
		this.playerPitch = entityPlayer.prevRotationPitch + (entityPlayer.rotationPitch - entityPlayer.prevRotationPitch) * partialTick;
		this.playerX = entityPlayer.lastTickPosX + (entityPlayer.posX - entityPlayer.lastTickPosX) * (double)partialTick;
		this.playerY = entityPlayer.lastTickPosY + (entityPlayer.posY - entityPlayer.lastTickPosY) * (double)partialTick;
		this.playerZ = entityPlayer.lastTickPosZ + (entityPlayer.posZ - entityPlayer.lastTickPosZ) * (double)partialTick;
	}

	/**
	 * Renders a tile entity at its world position, applying the ambient block
	 * brightness and culling it when farther than 64 blocks from the camera.
	 * The GL offset uses the static camera position so compiled world geometry
	 * and tile entities share the same origin.
	 */
	public void renderTileEntity(TileEntity tileEntity, float partialTick) {
		double offsetX = (double)tileEntity.xCoord + 0.5D - this.playerX;
		double offsetY = (double)tileEntity.yCoord + 0.5D - this.playerY;
		double offsetZ = (double)tileEntity.zCoord + 0.5D - this.playerZ;
		if(offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ < 4096.0D) {
			float brightness = this.worldObj.getBrightness(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
			GL11.glColor3f(brightness, brightness, brightness);
			this.renderTileEntityAt(tileEntity, (double)tileEntity.xCoord - staticPlayerX, (double)tileEntity.yCoord - staticPlayerY, (double)tileEntity.zCoord - staticPlayerZ, partialTick);
		}

	}

	/** Dispatches the tile entity to its registered special renderer (no-op when none is registered). */
	public void renderTileEntityAt(TileEntity tileEntity, double x, double y, double z, float partialTick) {
		TileEntitySpecialRenderer specialRenderer = this.getSpecialRendererForEntity(tileEntity);
		if(specialRenderer != null) {
			specialRenderer.renderTileEntityAt(tileEntity, x, y, z, partialTick);
		}

	}

	/** Returns the font renderer cached by {@link #cacheActiveRenderInfo}. */
	public FontRenderer getFontRenderer() {
		return this.fontRenderer;
	}
}