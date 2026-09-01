package net.minecraft.client.render;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.RenderHelper;
import net.minecraft.client.controller.PlayerControllerCreative;
import net.minecraft.client.effect.EffectRenderer;
import net.minecraft.client.effect.EntityRainFX;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.player.EntityPlayerSP;
import net.minecraft.client.render.camera.ClippingHelperImplementation;
import net.minecraft.client.render.camera.Frustrum;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.physics.AxisAlignedBB;
import net.minecraft.game.physics.MovingObjectPosition;
import net.minecraft.game.physics.Vec3D;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.material.Material;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.NVFogDistance;
import org.lwjgl.util.glu.GLU;
import util.MathHelper;

/**
 * Owns the per-frame 3D pipeline: mouse look, fog color, the camera transform
 * (hurt flash, view bobbing, third-person pull-back), the world passes
 * (opaque + transparent), rain, the first-person item, the target highlight
 * and the torches around the player when underground.
 */
public final class EntityRenderer {
	private Minecraft mc;
	private boolean anaglyphEnabled = false;
	private float farPlaneDistance = 0.0F;
	public ItemRenderer itemRenderer;
	private int rendererUpdateCount;
	private Entity pointedEntity = null;
	/** Sticky deltas of the disabled-mouse-grab path (retained for fidelity). */
	private int lastMouseX;
	private int lastMouseY;
	private Random random = new Random();
	/** RGBA scratch buffer handed to glFog. */
	private FloatBuffer fogColorBuffer = BufferUtils.createFloatBuffer(16);
	private float fogColorRed;
	private float fogColorGreen;
	private float fogColorBlue;
	/** Smoothed sky brightness (previous and current values, blended per frame). */
	private float fogColor2;
	private float fogColor1;

	public EntityRenderer(Minecraft mc) {
		this.mc = mc;
		this.itemRenderer = new ItemRenderer(mc);
	}

	public final void updateRenderer() {
		this.fogColor2 = this.fogColor1;
		float brightness = this.mc.theWorld.getBrightness(MathHelper.floor_double(this.mc.thePlayer.posX), MathHelper.floor_double(this.mc.thePlayer.posY), MathHelper.floor_double(this.mc.thePlayer.posZ));
		float distanceBias = (float)(3 - this.mc.gameSettings.renderDistance) / 3.0F;
		brightness = brightness * (1.0F - distanceBias) + distanceBias;
		this.fogColor1 += (brightness - this.fogColor1) * 0.1F;
		++this.rendererUpdateCount;
		this.itemRenderer.updateEquippedItem();
		if(this.mc.inGameHasFocus) {
			EntityPlayerSP player = this.mc.thePlayer;
			World world = this.mc.theWorld;
			int playerX = MathHelper.floor_double(player.posX);
			int playerY = MathHelper.floor_double(player.posY);
			int playerZ = MathHelper.floor_double(player.posZ);

			// Spawn rain droplets in a 9x9 pattern around the player. The hardcoded
			// y=63 is the sea-level rain ceiling in this version.
			for(int i = 0; i < 50; ++i) {
				int rainX = playerX + this.random.nextInt(9) - 4;
				int rainZ = playerZ + this.random.nextInt(9) - 4;
				int groundBlockId = world.getBlockId(rainX, 63, rainZ);
				if(64 <= playerY + 4 && 64 >= playerY - 4) {
					float offsetX = this.random.nextFloat();
					float offsetZ = this.random.nextFloat();
					if(groundBlockId > 0) {
						this.mc.effectRenderer.addEffect(new EntityRainFX(world, (double)((float)rainX + offsetX), (double)64.1F - Block.blocksList[groundBlockId].minY, (double)((float)rainZ + offsetZ)));
					}
				}
			}
		}

	}

	/** Interpolated camera (eye) position for this frame. */
	private Vec3D orientCamera(float partialTick) {
		EntityPlayerSP player = this.mc.thePlayer;
		double x = player.prevPosX + (player.posX - player.prevPosX) * (double)partialTick;
		double y = player.prevPosY + (player.posY - player.prevPosY) * (double)partialTick;
		double z = player.prevPosZ + (player.posZ - player.prevPosZ) * (double)partialTick;
		return new Vec3D(x, y, z);
	}

	/** Rolls and sway the view when the player is hurt (or dying). */
	private void hurtCameraEffect(float partialTick) {
		EntityPlayerSP player = this.mc.thePlayer;
		float hurtProgress = (float)player.hurtTime - partialTick;
		if(player.health <= 0) {
			partialTick += (float)player.deathTime;
			GL11.glRotatef(40.0F - 8000.0F / (partialTick + 200.0F), 0.0F, 0.0F, 1.0F);
		}

		if(hurtProgress >= 0.0F) {
			hurtProgress /= (float)player.maxHurtTime;
			hurtProgress = MathHelper.sin(hurtProgress * hurtProgress * hurtProgress * hurtProgress * (float)Math.PI);
			float attackedAtYaw = player.attackedAtYaw;
			GL11.glRotatef(-attackedAtYaw, 0.0F, 1.0F, 0.0F);
			GL11.glRotatef(-hurtProgress * 14.0F, 0.0F, 0.0F, 1.0F);
			GL11.glRotatef(attackedAtYaw, 0.0F, 1.0F, 0.0F);
		}
	}

	/** Sways the view to the rhythm of the player's footsteps. */
	private void setupViewBobbing(float partialTick) {
		if(!this.mc.gameSettings.thirdPersonView) {
			EntityPlayerSP player = this.mc.thePlayer;
			float walkDelta = player.distanceWalkedModified - player.prevDistanceWalkedModified;
			walkDelta = player.distanceWalkedModified + walkDelta * partialTick;
			float cameraYaw = player.prevCameraYaw + (player.cameraYaw - player.prevCameraYaw) * partialTick;
			partialTick = player.prevCameraPitch + (player.cameraPitch - player.prevCameraPitch) * partialTick;
			GL11.glTranslatef(MathHelper.sin(walkDelta * (float)Math.PI) * cameraYaw * 0.5F, -Math.abs(MathHelper.cos(walkDelta * (float)Math.PI) * cameraYaw), 0.0F);
			GL11.glRotatef(MathHelper.sin(walkDelta * (float)Math.PI) * cameraYaw * 3.0F, 0.0F, 0.0F, 1.0F);
			GL11.glRotatef(Math.abs(MathHelper.cos(walkDelta * (float)Math.PI + 0.2F) * cameraYaw) * 5.0F, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(partialTick, 1.0F, 0.0F, 0.0F);
		}
	}

	/** Handles pointer input, the overlay and then everything rendered in-game. */
	public final void updateCameraAndRender(float partialTick) {
		if(this.anaglyphEnabled && !Display.isActive()) {
			this.mc.displayInGameMenu();
		}

		this.anaglyphEnabled = Display.isActive();
		int guiMouseX;
		int guiMouseY;
		if(this.mc.inventoryScreen) {
			// Disabled mouse-grab path: the raw deltas are captured but unused
			// (kept verbatim from the original 2010 client).
			Mouse.getDX();
			int capturedMouseX = 0;
			Mouse.getDY();
			int capturedMouseY = 0;
			this.mc.mouseHelper.ungrabMouseCursor();
			int invertMouse = 1;
			if(this.mc.gameSettings.invertMouse) {
				invertMouse = -1;
			}

			guiMouseX = capturedMouseX + this.mc.mouseHelper.deltaX;
			guiMouseY = capturedMouseY - this.mc.mouseHelper.deltaY;
			if(capturedMouseX != 0 || this.lastMouseX != 0) {
				System.out.println("xxo: " + capturedMouseX + ", " + this.lastMouseX + ": " + this.lastMouseX + ", xo: " + guiMouseX);
			}

			if(this.lastMouseX != 0) {
				this.lastMouseX = 0;
			}

			if(this.lastMouseY != 0) {
				this.lastMouseY = 0;
			}

			if(capturedMouseX != 0) {
				this.lastMouseX = capturedMouseX;
			}

			if(capturedMouseY != 0) {
				this.lastMouseY = capturedMouseY;
			}

			float sensitivityYaw = (float)guiMouseX;
			float sensitivityPitch = (float)(guiMouseY * invertMouse);
			float yawDelta = sensitivityYaw;
			EntityPlayerSP player = this.mc.thePlayer;
			float prevPitch = player.rotationPitch;
			float prevYaw = player.rotationYaw;
			player.rotationYaw = (float)((double)player.rotationYaw + (double)yawDelta * 0.15D);
			player.rotationPitch = (float)((double)player.rotationPitch - (double)sensitivityPitch * 0.15D);
			if(player.rotationPitch < -90.0F) {
				player.rotationPitch = -90.0F;
			}

			if(player.rotationPitch > 90.0F) {
				player.rotationPitch = 90.0F;
			}

			player.prevRotationPitch += player.rotationPitch - prevPitch;
			player.prevRotationYaw += player.rotationYaw - prevYaw;
		}

		ScaledResolution scaledResolution = new ScaledResolution(this.mc.displayWidth, this.mc.displayHeight);
		int scaledWidth = scaledResolution.getScaledWidth();
		int scaledHeight = scaledResolution.getScaledHeight();
		guiMouseX = Mouse.getX() * scaledWidth / this.mc.displayWidth;
		guiMouseY = scaledHeight - Mouse.getY() * scaledHeight / this.mc.displayHeight - 1;
		if(this.mc.theWorld != null) {
			this.getMouseOver(partialTick);
			this.mc.ingameGUI.renderGameOverlay(partialTick);
		} else {
			GL11.glViewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
			GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
			GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glLoadIdentity();
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			GL11.glLoadIdentity();
			this.setupOverlayRendering();
		}

		if(this.mc.currentScreen != null) {
			GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
			this.mc.currentScreen.drawScreen(guiMouseX, guiMouseY, partialTick);
		}

		Thread.yield();
		Display.update();
	}

	/**
	 * Computes what the crosshair points at (block or entity), then runs the
	 * whole 3D frame: sky, terrain passes, entities, particles, rain and the
	 * held item. The eye pass runs twice when anaglyph is on.
	 */
	private void getMouseOver(float partialTick) {
		EntityPlayerSP player = this.mc.thePlayer;
		float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTick;
		float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTick;
		Vec3D eyePos = this.orientCamera(partialTick);
		float cosYaw = MathHelper.cos(-yaw * ((float)Math.PI / 180.0F) - (float)Math.PI);
		float sinYaw = MathHelper.sin(-yaw * ((float)Math.PI / 180.0F) - (float)Math.PI);
		float negCosPitch = -MathHelper.cos(-pitch * ((float)Math.PI / 180.0F));
		float sinPitch = MathHelper.sin(-pitch * ((float)Math.PI / 180.0F));
		float lookX = sinYaw * negCosPitch;
		float lookZ = cosYaw * negCosPitch;
		double reachDistance = (double)this.mc.playerController.getBlockReachDistance();
		Vec3D targetPoint = eyePos.addVector((double)lookX * reachDistance, (double)sinPitch * reachDistance, (double)lookZ * reachDistance);
		this.mc.objectMouseOver = this.mc.theWorld.rayTraceBlocks(eyePos, targetPoint);
		double bestDistance = reachDistance;
		eyePos = this.orientCamera(partialTick);
		if(this.mc.objectMouseOver != null) {
			bestDistance = this.mc.objectMouseOver.hitVec.distance(eyePos);
		}

		if(this.mc.playerController instanceof PlayerControllerCreative) {
			reachDistance = 32.0D;
		} else {
			if(bestDistance > 3.0D) {
				bestDistance = 3.0D;
			}

			reachDistance = bestDistance;
		}

		targetPoint = eyePos.addVector((double)lookX * reachDistance, (double)sinPitch * reachDistance, (double)lookZ * reachDistance);
		this.pointedEntity = null;
		List<Entity> entitiesInReach = this.mc.theWorld.getEntitiesWithinAABBExcludingEntity(player, player.boundingBox.addCoord((double)lookX * reachDistance, (double)sinPitch * reachDistance, (double)lookZ * reachDistance));
		double closestEntityDistance = 0.0D;

		for(int entityIndex = 0; entityIndex < entitiesInReach.size(); ++entityIndex) {
			Entity entity = entitiesInReach.get(entityIndex);
			if(entity.canBeCollidedWith()) {
				AxisAlignedBB expandedBox = entity.boundingBox.expand((double)0.1F, (double)0.1F, (double)0.1F);
				MovingObjectPosition hit = expandedBox.calculateIntercept(eyePos, targetPoint);
				if(hit != null) {
					double hitDistance = eyePos.distance(hit.hitVec);
					if(hitDistance < closestEntityDistance || closestEntityDistance == 0.0D) {
						this.pointedEntity = entity;
						closestEntityDistance = hitDistance;
					}
				}
			}
		}

		if(this.pointedEntity != null && !(this.mc.playerController instanceof PlayerControllerCreative)) {
			this.mc.objectMouseOver = new MovingObjectPosition(this.pointedEntity);
		}

		World world = this.mc.theWorld;
		RenderGlobal renderGlobal = this.mc.renderGlobal;
		EffectRenderer effectRenderer = this.mc.effectRenderer;
		double playerRenderX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)partialTick;
		double playerRenderY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)partialTick;
		double playerRenderZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)partialTick;

		for(int eyePass = 0; eyePass < 2; ++eyePass) {
			if(this.mc.gameSettings.anaglyph) {
				if(eyePass == 0) {
					GL11.glColorMask(false, true, true, false);
				} else {
					GL11.glColorMask(true, false, false, false);
				}
			}

			GL11.glViewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
			// Blend the distance haze into the sky colour.
			float fogBlend = 1.0F / (float)(4 - this.mc.gameSettings.renderDistance);
			fogBlend = 1.0F - (float)Math.pow((double)fogBlend, 0.25D);
			Vec3D skyColorVec = world.getSkyColor(partialTick);
			float skyRed = (float)skyColorVec.xCoord;
			float skyGreen = (float)skyColorVec.yCoord;
			float skyBlue = (float)skyColorVec.zCoord;
			Vec3D fogColorVec = world.getFogColor(partialTick);
			this.fogColorRed = (float)fogColorVec.xCoord;
			this.fogColorGreen = (float)fogColorVec.yCoord;
			this.fogColorBlue = (float)fogColorVec.zCoord;
			this.fogColorRed += (skyRed - this.fogColorRed) * fogBlend;
			this.fogColorGreen += (skyGreen - this.fogColorGreen) * fogBlend;
			this.fogColorBlue += (skyBlue - this.fogColorBlue) * fogBlend;
			Block eyeBlock = Block.blocksList[world.getBlockId(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY + (double)0.12F), MathHelper.floor_double(player.posZ))];
			if(eyeBlock != null && eyeBlock.blockMaterial != Material.air) {
				Material eyeMaterial = eyeBlock.blockMaterial;
				if(eyeMaterial == Material.water) {
					this.fogColorRed = 0.02F;
					this.fogColorGreen = 0.02F;
					this.fogColorBlue = 0.2F;
				} else if(eyeMaterial == Material.lava) {
					this.fogColorRed = 0.6F;
					this.fogColorGreen = 0.1F;
					this.fogColorBlue = 0.0F;
				}
			}

			float ambientBrightness = this.fogColor2 + (this.fogColor1 - this.fogColor2) * partialTick;
			this.fogColorRed *= ambientBrightness;
			this.fogColorGreen *= ambientBrightness;
			this.fogColorBlue *= ambientBrightness;
			if(this.mc.gameSettings.anaglyph) {
				float luminance = (this.fogColorRed * 30.0F + this.fogColorGreen * 59.0F + this.fogColorBlue * 11.0F) / 100.0F;
				float anaglyphGreen = (this.fogColorRed * 30.0F + this.fogColorGreen * 70.0F) / 100.0F;
				float anaglyphBlue = (this.fogColorRed * 30.0F + this.fogColorBlue * 70.0F) / 100.0F;
				this.fogColorRed = luminance;
				this.fogColorGreen = anaglyphGreen;
				this.fogColorBlue = anaglyphBlue;
			}

			GL11.glClearColor(this.fogColorRed, this.fogColorGreen, this.fogColorBlue, 0.0F);
			GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
			GL11.glEnable(GL11.GL_CULL_FACE);
			// The far plane can no longer fall short of the loaded chunk grid:
			// the original fixed it at 256 >> renderDistance, which at render
			// distance 2 was only 64 blocks against an 80-block half-extent,
			// culling a visible shell of chunks inside the world and making
			// terrain pop in/out as the camera turned or moved.
			this.farPlaneDistance = (float)(this.mc.renderGlobal.getGridWidth() << 4);
			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glLoadIdentity();
			if(this.mc.gameSettings.anaglyph) {
				GL11.glTranslatef((float)(-((eyePass << 1) - 1)) * 0.07F, 0.0F, 0.0F);
			}

			// Field of view: 70 normally, 60 underwater, and it widens while dying.
			float fieldOfView = 70.0F;
			if(player.isInsideOfMaterial()) {
				fieldOfView = 60.0F;
			}

			if(player.health <= 0) {
				float deathTime = (float)player.deathTime + partialTick;
				fieldOfView /= (1.0F - 500.0F / (deathTime + 500.0F)) * 2.0F + 1.0F;
			}

			GLU.gluPerspective(fieldOfView, (float)this.mc.displayWidth / (float)this.mc.displayHeight, 0.05F, this.farPlaneDistance);
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			GL11.glLoadIdentity();
			if(this.mc.gameSettings.anaglyph) {
				GL11.glTranslatef((float)((eyePass << 1) - 1) * 0.1F, 0.0F, 0.0F);
			}

			this.hurtCameraEffect(partialTick);
			if(this.mc.gameSettings.viewBobbing) {
				this.setupViewBobbing(partialTick);
			}

			// Camera origin: the interpolated eye position, snapped to the grid in
			// first person, or pulled back and collision-tested in third person.
			double cameraX = player.prevPosX + (player.posX - player.prevPosX) * (double)partialTick;
			double cameraY = player.prevPosY + (player.posY - player.prevPosY) * (double)partialTick;
			double cameraZ = player.prevPosZ + (player.posZ - player.prevPosZ) * (double)partialTick;
			if(!this.mc.gameSettings.thirdPersonView) {
				GL11.glTranslatef(0.0F, 0.0F, -0.1F);
			} else {
				double cameraDistance = 4.0D;
				double lookBackX = (double)(-MathHelper.sin(player.rotationYaw / 180.0F * (float)Math.PI) * MathHelper.cos(player.rotationPitch / 180.0F * (float)Math.PI)) * 4.0D;
				double lookBackZ = (double)(MathHelper.cos(player.rotationYaw / 180.0F * (float)Math.PI) * MathHelper.cos(player.rotationPitch / 180.0F * (float)Math.PI)) * 4.0D;
				double lookBackY = (double)(-MathHelper.sin(player.rotationPitch / 180.0F * (float)Math.PI)) * 4.0D;

				// Cast 8 rays from the desired pull-back position and shorten the
				// camera until none of them clips a block.
				for(int corner = 0; corner < 8; ++corner) {
					float cornerOffsetX = (float)(((corner & 1) << 1) - 1);
					float cornerOffsetY = (float)(((corner >> 1 & 1) << 1) - 1);
					float cornerOffsetZ = (float)(((corner >> 2 & 1) << 1) - 1);
					cornerOffsetX *= 0.1F;
					cornerOffsetY *= 0.1F;
					cornerOffsetZ *= 0.1F;
					MovingObjectPosition hit = world.rayTraceBlocks(new Vec3D(cameraX + (double)cornerOffsetX, cameraY + (double)cornerOffsetY, cameraZ + (double)cornerOffsetZ), new Vec3D(cameraX - lookBackX + (double)cornerOffsetX + (double)cornerOffsetZ, cameraY - lookBackY + (double)cornerOffsetY, cameraZ - lookBackZ + (double)cornerOffsetZ));
					if(hit != null) {
						double hitDistance = hit.hitVec.distance(new Vec3D(cameraX, cameraY, cameraZ));
						if(hitDistance < cameraDistance) {
							cameraDistance = hitDistance;
						}
					}
				}

				GL11.glTranslatef(0.0F, 0.0F, (float)(-cameraDistance));
			}

			GL11.glRotatef(player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTick, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTick + 180.0F, 0.0F, 1.0F, 0.0F);
			ClippingHelperImplementation.init();
			this.setupFog();
			GL11.glEnable(GL11.GL_FOG);
			renderGlobal.renderSky(partialTick);
			this.setupFog();
			Frustrum frustrum = new Frustrum();
			frustrum.setPosition(playerRenderX, playerRenderY, playerRenderZ);
			this.mc.renderGlobal.clipRenderersByFrustrum(frustrum);
			this.mc.renderGlobal.updateRenderers(player);
			this.setupFog();
			GL11.glEnable(GL11.GL_FOG);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.mc.renderEngine.getTexture("/terrain.png"));
			RenderHelper.disableStandardItemLighting();
			renderGlobal.sortAndRender(player, 0, (double)partialTick);
			// When the camera ends up inside a solid block (tunnel vision), force
			// the surrounding blocks to render so the player can find a way out.
			int playerBlockX = MathHelper.floor_double(player.posX);
			int playerBlockY = MathHelper.floor_double(player.posY);
			int playerBlockZ = MathHelper.floor_double(player.posZ);
			int surroundBlockId;
			if(world.isSolid(playerBlockX, playerBlockY, playerBlockZ)) {
				RenderBlocks renderBlocks = new RenderBlocks(world);

				for(int renderX = playerBlockX - 1; renderX <= playerBlockX + 1; ++renderX) {
					for(int renderY = playerBlockY - 1; renderY <= playerBlockY + 1; ++renderY) {
						for(int renderZ = playerBlockZ - 1; renderZ <= playerBlockZ + 1; ++renderZ) {
							surroundBlockId = world.getBlockId(renderX, renderY, renderZ);
							if(surroundBlockId > 0) {
								renderBlocks.renderBlockAllFaces(Block.blocksList[surroundBlockId], renderX, renderY, renderZ);
							}
						}
					}
				}
			}

			RenderHelper.enableStandardItemLighting();
			GL11.glPushMatrix();
			renderGlobal.renderEntities(this.orientCamera(partialTick), frustrum, partialTick);
			effectRenderer.renderLitParticles(partialTick);
			GL11.glPopMatrix();
			RenderHelper.disableStandardItemLighting();
			this.setupFog();
			effectRenderer.renderParticles(player, partialTick);
			if(this.mc.objectMouseOver != null && player.isInsideOfMaterial()) {
				GL11.glDisable(GL11.GL_ALPHA_TEST);
				renderGlobal.drawBlockBreaking(player, this.mc.objectMouseOver, 0, player.inventory.getCurrentItem(), partialTick);
				renderGlobal.drawSelectionBox(player, this.mc.objectMouseOver, 0, partialTick);
				GL11.glEnable(GL11.GL_ALPHA_TEST);
			}

			// Transparent pass: rebuild the (inverted) depth mask, draw in two
			// stages so the front-most faces of the held-over material win.
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			this.setupFog();
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glColorMask(false, false, false, false);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.mc.renderEngine.getTexture("/terrain.png"));
			int transparentListCount = renderGlobal.sortAndRender(player, 1, (double)partialTick);
			GL11.glColorMask(true, true, true, true);
			if(this.mc.gameSettings.anaglyph) {
				if(eyePass == 0) {
					GL11.glColorMask(false, true, true, false);
				} else {
					GL11.glColorMask(true, false, false, false);
				}
			}

			if(transparentListCount > 0) {
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.mc.renderEngine.getTexture("/terrain.png"));
				renderGlobal.renderAllRenderLists(1, (double)partialTick);
			}

			GL11.glDepthMask(true);
			GL11.glEnable(GL11.GL_CULL_FACE);
			GL11.glDisable(GL11.GL_BLEND);
			if(this.mc.objectMouseOver != null && !player.isInsideOfMaterial()) {
				GL11.glDisable(GL11.GL_ALPHA_TEST);
				renderGlobal.drawBlockBreaking(player, this.mc.objectMouseOver, 0, player.inventory.getCurrentItem(), partialTick);
				renderGlobal.drawSelectionBox(player, this.mc.objectMouseOver, 0, partialTick);
				GL11.glEnable(GL11.GL_ALPHA_TEST);
			}

			GL11.glDisable(GL11.GL_FOG);
			if(this.mc.inGameHasFocus) {
				// The rain screen: transparent quads descending from just above
				// the camera to (at most) the rain ceiling at y=64.
				int rainCenterX = MathHelper.floor_double(player.posX);
				int rainCenterY = MathHelper.floor_double(player.posY);
				int rainCenterZ = MathHelper.floor_double(player.posZ);
				Tessellator tessellator = Tessellator.instance;
				GL11.glDisable(GL11.GL_CULL_FACE);
				GL11.glNormal3f(0.0F, 1.0F, 0.0F);
				GL11.glEnable(GL11.GL_BLEND);
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.mc.renderEngine.getTexture("/rain.png"));
				for(int rainX = rainCenterX - 5; rainX <= rainCenterX + 5; ++rainX) {
					for(int rainZ = rainCenterZ - 5; rainZ <= rainCenterZ + 5; ++rainZ) {
						int rainTopY = rainCenterY - 5;
						int rainBottomY = rainCenterY + 5;
						if(rainTopY < 64) {
							rainTopY = 64;
						}

						if(rainBottomY < 64) {
							rainBottomY = 64;
						}

						if(rainTopY != rainBottomY) {
							float rainScroll = ((float)((this.rendererUpdateCount + rainX * 3121 + rainZ * 418711) % 32) + partialTick) / 32.0F;
							double dxToColumn = (double)((float)rainX + 0.5F) - player.posX;
							double dzToColumn = (double)((float)rainZ + 0.5F) - player.posZ;
							float distanceFade = MathHelper.sqrt_double(dxToColumn * dxToColumn + dzToColumn * dzToColumn) / 5.0F;
							GL11.glColor4f(1.0F, 1.0F, 1.0F, (1.0F - distanceFade * distanceFade) * 0.7F);
							tessellator.startDrawingQuads();
							tessellator.addVertexWithUV((double)rainX, (double)rainTopY, (double)rainZ, 0.0D, (double)((float)rainTopY * 2.0F / 8.0F + rainScroll * 2.0F));
							tessellator.addVertexWithUV((double)(rainX + 1), (double)rainTopY, (double)(rainZ + 1), 2.0D, (double)((float)rainTopY * 2.0F / 8.0F + rainScroll * 2.0F));
							tessellator.addVertexWithUV((double)(rainX + 1), (double)rainBottomY, (double)(rainZ + 1), 2.0D, (double)((float)rainBottomY * 2.0F / 8.0F + rainScroll * 2.0F));
							tessellator.addVertexWithUV((double)rainX, (double)rainBottomY, (double)rainZ, 0.0D, (double)((float)rainBottomY * 2.0F / 8.0F + rainScroll * 2.0F));
							tessellator.addVertexWithUV((double)rainX, (double)rainTopY, (double)(rainZ + 1), 0.0D, (double)((float)rainTopY * 2.0F / 8.0F + rainScroll * 2.0F));
							tessellator.addVertexWithUV((double)(rainX + 1), (double)rainTopY, (double)rainZ, 2.0D, (double)((float)rainTopY * 2.0F / 8.0F + rainScroll * 2.0F));
							tessellator.addVertexWithUV((double)(rainX + 1), (double)rainBottomY, (double)rainZ, 2.0D, (double)((float)rainBottomY * 2.0F / 8.0F + rainScroll * 2.0F));
							tessellator.addVertexWithUV((double)rainX, (double)rainBottomY, (double)(rainZ + 1), 0.0D, (double)((float)rainBottomY * 2.0F / 8.0F + rainScroll * 2.0F));
							tessellator.draw();
						}
					}
				}
				GL11.glEnable(GL11.GL_CULL_FACE);
				GL11.glDisable(GL11.GL_BLEND);
			}

			GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
			GL11.glLoadIdentity();
			if(this.mc.gameSettings.anaglyph) {
				GL11.glTranslatef((float)((eyePass << 1) - 1) * 0.1F, 0.0F, 0.0F);
			}

			GL11.glPushMatrix();
			this.hurtCameraEffect(partialTick);
			if(this.mc.gameSettings.viewBobbing) {
				this.setupViewBobbing(partialTick);
			}

			if(!this.mc.gameSettings.thirdPersonView) {
				this.itemRenderer.renderItemInFirstPerson(partialTick);
			}

			GL11.glPopMatrix();
			if(!this.mc.gameSettings.thirdPersonView) {
				this.itemRenderer.renderOverlays(partialTick);
				this.hurtCameraEffect(partialTick);
			}

			if(this.mc.gameSettings.viewBobbing) {
				this.setupViewBobbing(partialTick);
			}

			if(!this.mc.gameSettings.anaglyph) {
				return;
			}
		}

		GL11.glColorMask(true, true, true, false);
	}

	public final void setupOverlayRendering() {
		ScaledResolution scaledResolution = new ScaledResolution(this.mc.displayWidth, this.mc.displayHeight);
		int scaledWidth = scaledResolution.getScaledWidth();
		int scaledHeight = scaledResolution.getScaledHeight();
		GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glOrtho(0.0D, (double)scaledWidth, (double)scaledHeight, 0.0D, 1000.0D, 3000.0D);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		GL11.glTranslatef(0.0F, 0.0F, -2000.0F);
	}

	/** Re-applies the fog colour, mode and material state for the current eye material. */
	private void setupFog() {
		World world = this.mc.theWorld;
		this.fogColorBuffer.clear();
		this.fogColorBuffer.put(this.fogColorRed).put(this.fogColorGreen).put(this.fogColorBlue).put(1.0F);
		this.fogColorBuffer.flip();
		GL11.glFog(GL11.GL_FOG_COLOR, this.fogColorBuffer);
		GL11.glNormal3f(0.0F, -1.0F, 0.0F);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Block eyeBlock = Block.blocksList[world.getBlockId(MathHelper.floor_double(this.mc.thePlayer.posX), MathHelper.floor_double(this.mc.thePlayer.posY + (double)0.12F), MathHelper.floor_double(this.mc.thePlayer.posZ))];
		if(eyeBlock != null && eyeBlock.blockMaterial.getIsLiquid()) {
			Material eyeMaterial = eyeBlock.blockMaterial;
			GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP);
			if(eyeMaterial == Material.water) {
				GL11.glFogf(GL11.GL_FOG_DENSITY, 0.1F);
			} else if(eyeMaterial == Material.lava) {
				GL11.glFogf(GL11.GL_FOG_DENSITY, 2.0F);
			}
		} else {
			GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
			// The fog is anchored to the loaded grid's half-extent (not the far
			// plane) so it is fully opaque at the grid edge: freshly generated
			// terrain materializes behind the fog wall instead of popping in.
			float gridHalfExtent = (float)(this.mc.renderGlobal.getGridWidth() << 3);
			GL11.glFogf(GL11.GL_FOG_START, gridHalfExtent * 0.25F);
			GL11.glFogf(GL11.GL_FOG_END, gridHalfExtent);
			if(GLContext.getCapabilities().GL_NV_fog_distance) {
				GL11.glFogi(NVFogDistance.GL_FOG_DISTANCE_MODE_NV, NVFogDistance.GL_EYE_RADIAL_NV);
			}
		}

		GL11.glEnable(GL11.GL_COLOR_MATERIAL);
		GL11.glColorMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT);
	}
}