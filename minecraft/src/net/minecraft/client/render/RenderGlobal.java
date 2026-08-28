package net.minecraft.client.render;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.effect.EntityBubbleFX;
import net.minecraft.client.effect.EntityExplodeFX;
import net.minecraft.client.effect.EntityFlameFX;
import net.minecraft.client.effect.EntityLavaFX;
import net.minecraft.client.effect.EntitySmokeFX;
import net.minecraft.client.effect.EntitySplashFX;
import net.minecraft.client.player.EntityPlayerSP;
import net.minecraft.client.render.camera.Frustrum;
import net.minecraft.client.render.entity.RenderManager;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.physics.AxisAlignedBB;
import net.minecraft.game.physics.MovingObjectPosition;
import net.minecraft.game.physics.Vec3D;
import net.minecraft.game.world.IWorldAccess;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBOcclusionQuery;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import util.MathHelper;

/**
 * Renders the whole world: sky, clouds, chunked terrain, entities, particle
 * effects and the block interaction overlays (breaking cracks, selection wire
 * box). Also serves as the world's {@link IWorldAccess} so chunks recompile
 * when blocks change.
 */
public final class RenderGlobal implements IWorldAccess {
	private World worldObj;
	private RenderEngine renderEngine;
	private final List<WorldRenderer> worlRenderersToUpdate = new ArrayList<>();
	private WorldRenderer[] sortedWorldRenderers;
	private WorldRenderer[] worldRenderers;
	private int renderChunksWide;
	private int renderChunksTall;
	private int renderChunksDeep;
	private int glRenderListBase;
	private final Minecraft mc;
	private RenderBlocks globalRenderBlocks;
	private IntBuffer glOcclusionQueryBase;
	private boolean occlusionEnabled = false;
	private int cloudOffsetX = 0;
	private int glSkyList;
	private int glSkyList2;
	/** Spiral ring bounds used when repositioning renderers after the player moves. */
	private int minBlockX;
	private int minBlockY;
	private int minBlockZ;
	private int maxBlockX;
	private int maxBlockY;
	private int maxBlockZ;
	private int renderDistance = -1;
	private int countEntitiesTotal;
	private int countEntitiesRendered;
	private final IntBuffer occlusionResult = BufferUtils.createIntBuffer(64);
	private int renderersLoaded;
	private int renderersBeingClipped;
	private int renderersBeingOccluded;
	private int renderersBeingRendered;
	private final List<WorldRenderer> glRenderLists = new ArrayList<>();
	/** Last position the renderers were re-sorted for; avoids resorting every frame. */
	private double prevSortX = -9999.0D;
	private double prevSortY = -9999.0D;
	private double prevSortZ = -9999.0D;
	public float damagePartialTime;

	public RenderGlobal(Minecraft mc, RenderEngine renderEngine) {
		this.mc = mc;
		this.renderEngine = renderEngine;
		this.glRenderListBase = GL11.glGenLists(786432);
		this.occlusionEnabled = false;
		if(this.occlusionEnabled) {
			this.occlusionResult.clear();
			this.glOcclusionQueryBase = BufferUtils.createIntBuffer(262144);
			this.glOcclusionQueryBase.clear();
			this.glOcclusionQueryBase.position(0);
			this.glOcclusionQueryBase.limit(262144);
			ARBOcclusionQuery.glGenQueriesARB(this.glOcclusionQueryBase);
		}

		// Pre-compile the star field: 500 randomly oriented small textured quads
		// positioned far below the camera (the sky sphere's "ground").
		this.glSkyList = GL11.glGenLists(1);
		GL11.glNewList(this.glSkyList, GL11.GL_COMPILE);
		Random random = new Random(10842L);
		Tessellator tessellator;
		for(int star = 0; star < 500; ++star) {
			GL11.glRotatef(random.nextFloat() * 360.0F, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(random.nextFloat() * 360.0F, 0.0F, 1.0F, 0.0F);
			GL11.glRotatef(random.nextFloat() * 360.0F, 0.0F, 0.0F, 1.0F);
			tessellator = Tessellator.instance;
			float starSize = 0.25F + random.nextFloat() * 0.25F;
			tessellator.startDrawingQuads();
			tessellator.addVertexWithUV((double)(-starSize), -100.0D, (double)starSize, 1.0D, 1.0D);
			tessellator.addVertexWithUV((double)starSize, -100.0D, (double)starSize, 0.0D, 1.0D);
			tessellator.addVertexWithUV((double)starSize, -100.0D, (double)(-starSize), 0.0D, 0.0D);
			tessellator.addVertexWithUV((double)(-starSize), -100.0D, (double)(-starSize), 1.0D, 0.0D);
			tessellator.draw();
		}
		GL11.glEndList();
		// The sky base: a huge flat grid at y=16 that gets colored with the sky
		// color to act as the horizon ground plane.
		this.glSkyList2 = GL11.glGenLists(1);
		GL11.glNewList(this.glSkyList2, GL11.GL_COMPILE);
		tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		for(int gridX = -256; gridX <= 256; gridX += 32) {
			for(int gridZ = -256; gridZ <= 256; gridZ += 32) {
				tessellator.addVertex((double)gridX, 16.0D, (double)gridZ);
				tessellator.addVertex((double)(gridX + 32), 16.0D, (double)gridZ);
				tessellator.addVertex((double)(gridX + 32), 16.0D, (double)(gridZ + 32));
				tessellator.addVertex((double)gridX, 16.0D, (double)(gridZ + 32));
			}
		}
		tessellator.draw();
		GL11.glEndList();
	}

	public final void changeWorld(World world) {
		if(this.worldObj != null) {
			this.worldObj.removeWorldAccess(this);
		}

		this.prevSortX = -9999.0D;
		this.prevSortY = -9999.0D;
		this.prevSortZ = -9999.0D;
		RenderManager.instance.set(world);
		this.worldObj = world;
		this.globalRenderBlocks = new RenderBlocks(world);
		if(world != null) {
			world.addWorldAccess(this);
			this.loadRenderers();
		}
	}

	/** Rebuilds the renderer grid for the current render distance setting. */
	private void loadRenderers() {
		this.renderDistance = this.mc.gameSettings.renderDistance;
		int chunkSpan;
		if(this.worldRenderers != null) {
			for(chunkSpan = 0; chunkSpan < this.worldRenderers.length; ++chunkSpan) {
				this.worldRenderers[chunkSpan].stopRendering();
			}
		}

		// Render distance in chunks: 0 -> 40, 1 -> 20, 2 -> 10, 3 -> 5.
		chunkSpan = 5 << 3 - this.renderDistance;
		if(chunkSpan > 28) {
			chunkSpan = 28;
		}

		this.renderChunksWide = chunkSpan;
		this.renderChunksTall = 8;
		this.renderChunksDeep = chunkSpan;
		this.worldRenderers = new WorldRenderer[this.renderChunksWide * this.renderChunksTall * this.renderChunksDeep];
		this.sortedWorldRenderers = new WorldRenderer[this.renderChunksWide * this.renderChunksTall * this.renderChunksDeep];
		chunkSpan = 0;
		int occlusionQueryIndex = 0;
		this.minBlockX = 0;
		this.minBlockY = 0;
		this.minBlockZ = 0;
		this.maxBlockX = this.renderChunksWide;
		this.maxBlockY = this.renderChunksTall;
		this.maxBlockZ = this.renderChunksDeep;

		for(int i = 0; i < this.worlRenderersToUpdate.size(); ++i) {
			this.worlRenderersToUpdate.get(i).needsUpdate = false;
		}
		this.worlRenderersToUpdate.clear();

		// Create one renderer (owning 3 call lists) per 16x128x16 chunk column.
		for(int chunkX = 0; chunkX < this.renderChunksWide; ++chunkX) {
			for(int chunkY = 0; chunkY < this.renderChunksTall; ++chunkY) {
				for(int chunkZ = 0; chunkZ < this.renderChunksDeep; ++chunkZ) {
					WorldRenderer renderer = this.worldRenderers[(chunkZ * this.renderChunksTall + chunkY) * this.renderChunksWide + chunkX] = new WorldRenderer(this.worldObj, chunkX << 4, chunkY << 4, chunkZ << 4, 16, this.glRenderListBase + chunkSpan);
					if(this.occlusionEnabled) {
						renderer.glOcclusionQuery = this.glOcclusionQueryBase.get(occlusionQueryIndex);
					}
					renderer.isWaitingOnOcclusionQuery = false;
					renderer.isVisible = true;
					renderer.isInFrustum = true;
					++occlusionQueryIndex;
					renderer.needsUpdate = true;
					this.sortedWorldRenderers[(chunkZ * this.renderChunksTall + chunkY) * this.renderChunksWide + chunkX] = renderer;
					this.worlRenderersToUpdate.add(renderer);
					chunkSpan += 3;
				}
			}
		}

		Entity player = this.worldObj.playerEntity;
		this.markRenderersForNewPosition(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ));
		Arrays.sort(this.sortedWorldRenderers, new EntitySorter(player));
	}

	/** Draws every entity near the player that is inside the view frustum. */
	public final void renderEntities(Vec3D cameraPosition, Frustrum frustrum, float partialTick) {
		RenderManager.instance.cacheActiveRenderInfo(this.worldObj, this.renderEngine, this.mc.thePlayer, partialTick);
		this.countEntitiesTotal = 0;
		this.countEntitiesRendered = 0;
		Entity player = this.worldObj.playerEntity;
		RenderManager.renderPosX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)partialTick;
		RenderManager.renderPosY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)partialTick;
		RenderManager.renderPosZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)partialTick;
		List<Entity> entities = this.worldObj.getLoadedEntityList();
		this.countEntitiesTotal = entities.size();

		for(int i = 0; i < entities.size(); ++i) {
			Entity entity = entities.get(i);
			double dx = entity.posX - cameraPosition.xCoord;
			double dy = entity.posY - cameraPosition.yCoord;
			double dz = entity.posZ - cameraPosition.zCoord;
			double distanceSq = dx * dx + dy * dy + dz * dz;
			AxisAlignedBB boundingBox = entity.boundingBox;
			double width = boundingBox.maxX - boundingBox.minX;
			double height = boundingBox.maxY - boundingBox.minY;
			double depth = boundingBox.maxZ - boundingBox.minZ;
			double maxCullDistance = (width + height + depth) / 3.0D;
			maxCullDistance *= 64.0D;
			if(distanceSq < maxCullDistance * maxCullDistance && frustrum.isBoundingBoxInFrustrum(entity.boundingBox) && (entity != this.worldObj.playerEntity || this.mc.gameSettings.thirdPersonView)) {
				++this.countEntitiesRendered;
				RenderManager.instance.renderEntity(entity, partialTick);
			}
		}
	}

	public final String getDebugInfoRenders() {
		return "C: " + this.renderersBeingRendered + "/" + this.renderersLoaded + ". F: " + this.renderersBeingClipped + ", O: " + this.renderersBeingOccluded;
	}

	public final String getDebugInfoEntities() {
		return "E: " + this.countEntitiesRendered + "/" + this.countEntitiesTotal + ". B: " + 0 + ", I: " + (this.countEntitiesTotal - this.countEntitiesRendered);
	}

	/**
	 * Re-positions the ring of renderers after the player moves more than a
	 * couple blocks, keeping the player always near the centre of the chunk
	 * grid. The chunk coordinate is snapped back into a [0, width) window by
	 * shifting the whole ring forward/back.
	 */
	private void markRenderersForNewPosition(int playerBlockX, int playerBlockY, int playerBlockZ) {
		playerBlockX -= 8;
		playerBlockZ -= 8;
		this.minBlockX = Integer.MAX_VALUE;
		this.minBlockY = Integer.MAX_VALUE;
		this.minBlockZ = Integer.MAX_VALUE;
		this.maxBlockX = Integer.MIN_VALUE;
		this.maxBlockY = Integer.MIN_VALUE;
		this.maxBlockZ = Integer.MIN_VALUE;
		int chunkWindowSize = this.renderChunksWide << 4;
		int halfWindow = chunkWindowSize / 2;

		for(int chunkX = 0; chunkX < this.renderChunksWide; ++chunkX) {
			int blockX = chunkX << 4;
			int offset = blockX + halfWindow - playerBlockX;
			if(offset < 0) {
				offset -= chunkWindowSize - 1;
			}
			offset /= chunkWindowSize;
			blockX -= offset * chunkWindowSize;
			if(blockX < this.minBlockX) {
				this.minBlockX = blockX;
			}
			if(blockX > this.maxBlockX) {
				this.maxBlockX = blockX;
			}

			for(int chunkZ = 0; chunkZ < this.renderChunksDeep; ++chunkZ) {
				int blockZ = chunkZ << 4;
				int zOffset = blockZ + halfWindow - playerBlockZ;
				if(zOffset < 0) {
					zOffset -= chunkWindowSize - 1;
				}
				zOffset /= chunkWindowSize;
				blockZ -= zOffset * chunkWindowSize;
				if(blockZ < this.minBlockZ) {
					this.minBlockZ = blockZ;
				}
				if(blockZ > this.maxBlockZ) {
					this.maxBlockZ = blockZ;
				}

				for(int chunkY = 0; chunkY < this.renderChunksTall; ++chunkY) {
					int blockY = chunkY << 4;
					if(blockY < this.minBlockY) {
						this.minBlockY = blockY;
					}
					if(blockY > this.maxBlockY) {
						this.maxBlockY = blockY;
					}

					WorldRenderer renderer = this.worldRenderers[(chunkZ * this.renderChunksTall + chunkY) * this.renderChunksWide + chunkX];
					boolean wasUpToDate = renderer.needsUpdate;
					renderer.setPosition(blockX, blockY, blockZ);
					if(!wasUpToDate && renderer.needsUpdate) {
						this.worlRenderersToUpdate.add(renderer);
					}
				}
			}
		}
	}

	/**
	 * Sorts the renderers by distance from the player, marks which of them lie
	 * in front of the player (repositioning/recompiling as needed) and then
	 * renders the visible ones for the given render pass. Returns the number of
	 * call lists actually drawn.
	 */
	public final int sortAndRender(EntityPlayer player, int pass, double partialTick) {
		if(this.mc.gameSettings.renderDistance != this.renderDistance) {
			this.loadRenderers();
		}

		if(pass == 0) {
			this.renderersLoaded = 0;
			this.renderersBeingClipped = 0;
			this.renderersBeingOccluded = 0;
			this.renderersBeingRendered = 0;
		}

		double playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTick;
		double playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTick;
		double playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTick;
		double dx = player.posX - this.prevSortX;
		double dy = player.posY - this.prevSortY;
		double dz = player.posZ - this.prevSortZ;
		if(dx * dx + dy * dy + dz * dz > 16.0D) {
			this.prevSortX = player.posX;
			this.prevSortY = player.posY;
			this.prevSortZ = player.posZ;
			this.markRenderersForNewPosition(MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ));
			Arrays.sort(this.sortedWorldRenderers, new EntitySorter(player));
		}

		int renderListCount;
		if(this.occlusionEnabled && !this.mc.gameSettings.anaglyph && pass == 0) {
			// Hierarchical occlusion culling: query batches of renderers with the
			// bounding boxes, using last frame's visibility to decide order, and
			// only draw the ones visible.
			int queryEnd = 16;
			this.checkOcclusionQueryResult(0, 16);
			for(int i = 0; i < 16; ++i) {
				this.sortedWorldRenderers[i].isVisible = true;
			}
			renderListCount = 0 + this.renderSortedRenderers(0, 16, pass, partialTick);

			do {
				int queryStart = queryEnd;
				queryEnd <<= 1;
				if(queryEnd > this.sortedWorldRenderers.length) {
					queryEnd = this.sortedWorldRenderers.length;
				}

				GL11.glDisable(GL11.GL_TEXTURE_2D);
				GL11.glDisable(GL11.GL_LIGHTING);
				GL11.glDisable(GL11.GL_ALPHA_TEST);
				GL11.glDisable(GL11.GL_FOG);
				GL11.glColorMask(false, false, false, false);
				GL11.glDepthMask(false);
				this.checkOcclusionQueryResult(queryStart, queryEnd);
				GL11.glPushMatrix();
				float accumulatedX = 0.0F;
				float accumulatedY = 0.0F;
				float accumulatedZ = 0.0F;

				for(int i = queryStart; i < queryEnd; ++i) {
					if(this.sortedWorldRenderers[i].skipAllRenderPasses()) {
						this.sortedWorldRenderers[i].isInFrustum = false;
					} else {
						if(!this.sortedWorldRenderers[i].isInFrustum) {
							this.sortedWorldRenderers[i].isVisible = true;
						}

						if(this.sortedWorldRenderers[i].isInFrustum && !this.sortedWorldRenderers[i].isWaitingOnOcclusionQuery) {
							// Stagger the queries so nearby chunks are issued
							// first; each renderer issues an occlusion sample.
							float distance = MathHelper.sqrt_float(this.sortedWorldRenderers[i].distanceToEntitySquared(player));
							int queryStagger = (int)(1.0F + distance / 64.0F);
							if(this.cloudOffsetX % queryStagger == i % queryStagger) {
								WorldRenderer renderer = this.sortedWorldRenderers[i];
								float renderX = (float)((double)renderer.posXMinus - playerX);
								float renderY = (float)((double)renderer.posYMinus - playerY);
								float renderZ = (float)((double)renderer.posZMinus - playerZ);
								renderX -= accumulatedX;
								renderY -= accumulatedY;
								renderZ -= accumulatedZ;
								if(renderX != 0.0F || renderY != 0.0F || renderZ != 0.0F) {
									GL11.glTranslatef(renderX, renderY, renderZ);
									accumulatedX += renderX;
									accumulatedY += renderY;
									accumulatedZ += renderZ;
								}
								ARBOcclusionQuery.glBeginQueryARB(GL15.GL_SAMPLES_PASSED, this.sortedWorldRenderers[i].glOcclusionQuery);
								this.sortedWorldRenderers[i].callOcclusionQueryList();
								ARBOcclusionQuery.glEndQueryARB(GL15.GL_SAMPLES_PASSED);
								this.sortedWorldRenderers[i].isWaitingOnOcclusionQuery = true;
							}
						}
					}
				}

				GL11.glPopMatrix();
				GL11.glColorMask(true, true, true, true);
				GL11.glDepthMask(true);
				GL11.glEnable(GL11.GL_TEXTURE_2D);
				GL11.glEnable(GL11.GL_ALPHA_TEST);
				GL11.glEnable(GL11.GL_FOG);
				renderListCount += this.renderSortedRenderers(queryStart, queryEnd, pass, partialTick);
			} while(queryEnd < this.sortedWorldRenderers.length);
		} else {
			renderListCount = 0 + this.renderSortedRenderers(0, this.sortedWorldRenderers.length, pass, partialTick);
		}

		return renderListCount;
	}

	/** Picks up finished occlusion query results for a range of renderers. */
	private void checkOcclusionQueryResult(int start, int end) {
		for(int i = start; i < end; ++i) {
			if(this.sortedWorldRenderers[i].isWaitingOnOcclusionQuery) {
				this.occlusionResult.clear();
				ARBOcclusionQuery.glGetQueryObjectuARB(this.sortedWorldRenderers[i].glOcclusionQuery, GL15.GL_QUERY_RESULT_AVAILABLE, this.occlusionResult);
				if(this.occlusionResult.get(0) != 0) {
					this.sortedWorldRenderers[i].isWaitingOnOcclusionQuery = false;
					this.occlusionResult.clear();
					ARBOcclusionQuery.glGetQueryObjectuARB(this.sortedWorldRenderers[i].glOcclusionQuery, GL15.GL_QUERY_RESULT, this.occlusionResult);
					this.sortedWorldRenderers[i].isVisible = this.occlusionResult.get(0) != 0;
				}
			}
		}
	}

	/** Collects the visible renderers for one pass into {@link #glRenderLists}. */
	private int renderSortedRenderers(int start, int end, int pass, double partialTick) {
		this.glRenderLists.clear();
		int listCount = 0;
		for(int i = start; i < end; ++i) {
			if(pass == 0) {
				// Tally debug counters on the opaque pass only.
				++this.renderersLoaded;
				if(!this.sortedWorldRenderers[i].isInFrustum) {
					++this.renderersBeingClipped;
				}
				if(this.sortedWorldRenderers[i].isInFrustum && !this.sortedWorldRenderers[i].isVisible) {
					++this.renderersBeingOccluded;
				}
				if(this.sortedWorldRenderers[i].isInFrustum && this.sortedWorldRenderers[i].isVisible) {
					++this.renderersBeingRendered;
				}
			}

			if(this.sortedWorldRenderers[i].isInFrustum && this.sortedWorldRenderers[i].isVisible) {
				int glCallList = this.sortedWorldRenderers[i].getGLCallListForPass(pass);
				if(glCallList >= 0) {
					this.glRenderLists.add(this.sortedWorldRenderers[i]);
					++listCount;
				}
			}
		}
		this.renderAllRenderLists(pass, partialTick);
		return listCount;
	}

	/** Draws every collected call list, translating each chunk into place relative to the camera. */
	public final void renderAllRenderLists(int pass, double partialTick) {
		EntityPlayerSP player = this.mc.thePlayer;
		double playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTick;
		double playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTick;
		double playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTick;
		GL11.glPushMatrix();
		float accumulatedX = 0.0F;
		float accumulatedY = 0.0F;
		float accumulatedZ = 0.0F;

		for(int i = 0; i < this.glRenderLists.size(); ++i) {
			WorldRenderer renderer = this.glRenderLists.get(i);
			float renderX = (float)((double)renderer.posXMinus - playerX);
			float renderY = (float)((double)renderer.posYMinus - playerY);
			float renderZ = (float)((double)renderer.posZMinus - playerZ);
			renderX -= accumulatedX;
			renderY -= accumulatedY;
			renderZ -= accumulatedZ;
			if(renderX != 0.0F || renderY != 0.0F || renderZ != 0.0F) {
				GL11.glTranslatef(renderX, renderY, renderZ);
				accumulatedX += renderX;
				accumulatedY += renderY;
				accumulatedZ += renderZ;
			}
			GL11.glCallList(renderer.getGLCallListForPass(pass));
		}
		GL11.glPopMatrix();
	}

	public final void updateClouds() {
		++this.cloudOffsetX;
	}

	/** Draws the sky gradient base, sun, moon, stars and drifting clouds. */
	public final void renderSky(float partialTick) {
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		float playerHeight = (float)(this.mc.thePlayer.lastTickPosY + (this.mc.thePlayer.posY - this.mc.thePlayer.lastTickPosY) * (double)partialTick);
		Vec3D skyColorVec = this.worldObj.getSkyColor(partialTick);
		float red = (float)skyColorVec.xCoord;
		float green = (float)skyColorVec.yCoord;
		float blue = (float)skyColorVec.zCoord;
		float luminance;
		if(this.mc.gameSettings.anaglyph) {
			luminance = (red * 30.0F + green * 59.0F + blue * 11.0F) / 100.0F;
			green = (red * 30.0F + green * 70.0F) / 100.0F;
			blue = (red * 30.0F + blue * 70.0F) / 100.0F;
			red = luminance;
		}

		GL11.glColor3f(red, green, blue);
		Tessellator tessellator = Tessellator.instance;
		GL11.glDepthMask(false);
		GL11.glCallList(this.glSkyList2);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_FOG);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
		GL11.glPushMatrix();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glTranslatef(0.0F, 0.0F, 0.0F);
		GL11.glRotatef(0.0F, 0.0F, 0.0F, 1.0F);
		GL11.glRotatef(this.worldObj.getCelestialAngle(partialTick) * 360.0F, 1.0F, 0.0F, 0.0F);
		// The sun sits high above the sky base, the moon opposite.
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.renderEngine.getTexture("/terrain/sun.png"));
		tessellator.startDrawingQuads();
		tessellator.addVertexWithUV(-30.0D, 100.0D, -30.0D, 0.0D, 0.0D);
		tessellator.addVertexWithUV(30.0D, 100.0D, -30.0D, 1.0D, 0.0D);
		tessellator.addVertexWithUV(30.0D, 100.0D, 30.0D, 1.0D, 1.0D);
		tessellator.addVertexWithUV(-30.0D, 100.0D, 30.0D, 0.0D, 1.0D);
		tessellator.draw();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.renderEngine.getTexture("/terrain/moon.png"));
		tessellator.startDrawingQuads();
		tessellator.addVertexWithUV(-20.0D, -100.0D, 20.0D, 1.0D, 1.0D);
		tessellator.addVertexWithUV(20.0D, -100.0D, 20.0D, 0.0D, 1.0D);
		tessellator.addVertexWithUV(20.0D, -100.0D, -20.0D, 0.0D, 0.0D);
		tessellator.addVertexWithUV(-20.0D, -100.0D, -20.0D, 1.0D, 0.0D);
		tessellator.draw();
		// Stars only appear once the sky darkens enough.
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		float starBrightness = this.worldObj.getStarBrightness(partialTick);
		GL11.glColor4f(starBrightness, starBrightness, starBrightness, starBrightness);
		GL11.glCallList(this.glSkyList);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_FOG);
		GL11.glPopMatrix();
		GL11.glDepthMask(true);
		// A cloud plane drifts with the player; its texture is tiled across a
		// 512 block square that follows the player inside a 2048-block window.
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.renderEngine.getTexture("/clouds.png"));
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Vec3D cloudColorVec = this.worldObj.getCloudColor(partialTick);
		green = (float)cloudColorVec.xCoord;
		luminance = (float)cloudColorVec.yCoord;
		red = (float)cloudColorVec.zCoord;
		if(this.mc.gameSettings.anaglyph) {
			starBrightness = (green * 30.0F + luminance * 59.0F + red * 11.0F) / 100.0F;
			float cloudGreen = (green * 30.0F + luminance * 70.0F) / 100.0F;
			float cloudBlue = (green * 30.0F + red * 70.0F) / 100.0F;
			green = starBrightness;
			luminance = cloudGreen;
			red = cloudBlue;
		}

		double cloudCenterX = this.worldObj.playerEntity.prevPosX + (this.worldObj.playerEntity.posX - this.worldObj.playerEntity.prevPosX) * (double)partialTick + (double)(((float)this.cloudOffsetX + partialTick) * 0.03F);
		double cloudCenterZ = this.worldObj.playerEntity.prevPosZ + (this.worldObj.playerEntity.posZ - this.worldObj.playerEntity.prevPosZ) * (double)partialTick;
		int cloudTileX = MathHelper.floor_double(cloudCenterX / 2048.0D);
		int cloudTileZ = MathHelper.floor_double(cloudCenterZ / 2048.0D);
		cloudCenterX -= (double)(cloudTileX << 11);
		cloudCenterZ -= (double)(cloudTileZ << 11);
		float cloudHeight = 120.0F - playerHeight + 0.33F;
		float cloudOffsetU = (float)(cloudCenterX * 4.8828125E-4D);
		float cloudOffsetV = (float)(cloudCenterZ * 4.8828125E-4D);
		tessellator.startDrawingQuads();
		tessellator.setColorOpaque_F(green, luminance, red);

		for(int cloudX = -256; cloudX < 256; cloudX += 32) {
			for(int cloudZ = -256; cloudZ < 256; cloudZ += 32) {
				// Double-sided (front and back) quads keep the cloud visible
				// from both below and above.
				tessellator.addVertexWithUV((double)cloudX, (double)cloudHeight, (double)(cloudZ + 32), (double)((float)cloudX * (0.5F / 1024.0F) + cloudOffsetU), (double)((float)(cloudZ + 32) * (0.5F / 1024.0F) + cloudOffsetV));
				tessellator.addVertexWithUV((double)(cloudX + 32), (double)cloudHeight, (double)(cloudZ + 32), (double)((float)(cloudX + 32) * (0.5F / 1024.0F) + cloudOffsetU), (double)((float)(cloudZ + 32) * (0.5F / 1024.0F) + cloudOffsetV));
				tessellator.addVertexWithUV((double)(cloudX + 32), (double)cloudHeight, (double)cloudZ, (double)((float)(cloudX + 32) * (0.5F / 1024.0F) + cloudOffsetU), (double)((float)cloudZ * (0.5F / 1024.0F) + cloudOffsetV));
				tessellator.addVertexWithUV((double)cloudX, (double)cloudHeight, (double)cloudZ, (double)((float)cloudX * (0.5F / 1024.0F) + cloudOffsetU), (double)((float)cloudZ * (0.5F / 1024.0F) + cloudOffsetV));
				tessellator.addVertexWithUV((double)cloudX, (double)cloudHeight, (double)cloudZ, (double)((float)cloudX * (0.5F / 1024.0F) + cloudOffsetU), (double)((float)cloudZ * (0.5F / 1024.0F) + cloudOffsetV));
				tessellator.addVertexWithUV((double)(cloudX + 32), (double)cloudHeight, (double)cloudZ, (double)((float)(cloudX + 32) * (0.5F / 1024.0F) + cloudOffsetU), (double)((float)cloudZ * (0.5F / 1024.0F) + cloudOffsetV));
				tessellator.addVertexWithUV((double)(cloudX + 32), (double)cloudHeight, (double)(cloudZ + 32), (double)((float)(cloudX + 32) * (0.5F / 1024.0F) + cloudOffsetU), (double)((float)(cloudZ + 32) * (0.5F / 1024.0F) + cloudOffsetV));
				tessellator.addVertexWithUV((double)cloudX, (double)cloudHeight, (double)(cloudZ + 32), (double)((float)cloudX * (0.5F / 1024.0F) + cloudOffsetU), (double)((float)(cloudZ + 32) * (0.5F / 1024.0F) + cloudOffsetV));
			}
		}

		tessellator.draw();
	}

	public final int getGridWidth() {
		return this.renderChunksWide;
	}

	/**
	 * Compiles the changed chunk renderers, farthest first. Everything within
	 * the player's near radius is rebuilt immediately; beyond that only the
	 * three farthest renderers are compiled per frame. This matches the version's
	 * original pacing so a grid recenter cannot stall a whole frame; the fog wall
	 * now hides the outer ring filling in.
	 */
	public final void updateRenderers(EntityPlayer player) {
		Collections.sort(this.worlRenderersToUpdate, new RenderSorter(player));
		int lastIndex = this.worlRenderersToUpdate.size() - 1;
		int updateCount = this.worlRenderersToUpdate.size();

		for(int i = 0; i < updateCount; ++i) {
			WorldRenderer renderer = this.worlRenderersToUpdate.get(lastIndex - i);
			if(renderer.distanceToEntitySquared(player) > 2500.0F && i > 2) {
				return;
			}

			this.worlRenderersToUpdate.remove(renderer);
			renderer.updateRenderer();
			renderer.needsUpdate = false;
		}
	}

	/** Draws the pulsing block-breaking crack overlay on the targeted block. */
	public final void drawBlockBreaking(EntityPlayer player, MovingObjectPosition hitResult, int blockRenderType, ItemStack itemStack, float partialTick) {
		Tessellator tessellator = Tessellator.instance;
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, (MathHelper.sin((float)System.currentTimeMillis() / 100.0F) * 0.2F + 0.4F) * 0.5F);
		if(this.damagePartialTime > 0.0F) {
			GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR);
			int crackTexture = this.renderEngine.getTexture("/terrain.png");
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, crackTexture);
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.5F);
			GL11.glPushMatrix();
			crackTexture = this.worldObj.getBlockId(hitResult.blockX, hitResult.blockY, hitResult.blockZ);
			Block block = crackTexture > 0 ? Block.blocksList[crackTexture] : null;
			GL11.glDisable(GL11.GL_ALPHA_TEST);
			GL11.glPolygonOffset(-1.0F, -1.0F);
			GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
			tessellator.startDrawingQuads();
			double playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)partialTick;
			double playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)partialTick;
			double playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)partialTick;
			tessellator.setTranslationD(-playerX, -playerY, -playerZ);
			tessellator.disableColor();
			if(block == null) {
				block = Block.stone;
			}

			// Re-render the block but with the destructive crack tile (damage
			// stage drives which of the 10 damage tiles is used).
			this.globalRenderBlocks.renderBlockUsingTexture(block, hitResult.blockX, hitResult.blockY, hitResult.blockZ, 240 + (int)(this.damagePartialTime * 10.0F));
			tessellator.draw();
			tessellator.setTranslationD(0.0D, 0.0D, 0.0D);
			GL11.glPolygonOffset(0.0F, 0.0F);
			GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
			GL11.glEnable(GL11.GL_ALPHA_TEST);
			GL11.glDepthMask(true);
			GL11.glPopMatrix();
		}

		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
	}

	/** Draws the translucent black wireframe around the targeted block. */
	public final void drawSelectionBox(EntityPlayer player, MovingObjectPosition hitResult, int blockRenderType, float partialTick) {
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
		GL11.glLineWidth(2.0F);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDepthMask(false);
		blockRenderType = this.worldObj.getBlockId(hitResult.blockX, hitResult.blockY, hitResult.blockZ);
		if(blockRenderType > 0) {
			double playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)partialTick;
			double playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)partialTick;
			double playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)partialTick;
			AxisAlignedBB box = Block.blocksList[blockRenderType].getSelectedBoundingBoxFromPool(hitResult.blockX, hitResult.blockY, hitResult.blockZ).expand((double)0.002F, (double)0.002F, (double)0.002F).offsetCopy(-playerX, -playerY, -playerZ);
			Tessellator tessellator = Tessellator.instance;
			// Bottom and top edge loops (line strips, mode 3).
			tessellator.startDrawing(3);
			tessellator.addVertex(box.minX, box.minY, box.minZ);
			tessellator.addVertex(box.maxX, box.minY, box.minZ);
			tessellator.addVertex(box.maxX, box.minY, box.maxZ);
			tessellator.addVertex(box.minX, box.minY, box.maxZ);
			tessellator.addVertex(box.minX, box.minY, box.minZ);
			tessellator.draw();
			tessellator.startDrawing(3);
			tessellator.addVertex(box.minX, box.maxY, box.minZ);
			tessellator.addVertex(box.maxX, box.maxY, box.minZ);
			tessellator.addVertex(box.maxX, box.maxY, box.maxZ);
			tessellator.addVertex(box.minX, box.maxY, box.maxZ);
			tessellator.addVertex(box.minX, box.maxY, box.minZ);
			tessellator.draw();
			// The four vertical edges (pairs of lines, mode 1).
			tessellator.startDrawing(1);
			tessellator.addVertex(box.minX, box.minY, box.minZ);
			tessellator.addVertex(box.minX, box.maxY, box.minZ);
			tessellator.addVertex(box.maxX, box.minY, box.minZ);
			tessellator.addVertex(box.maxX, box.maxY, box.minZ);
			tessellator.addVertex(box.maxX, box.minY, box.maxZ);
			tessellator.addVertex(box.maxX, box.maxY, box.maxZ);
			tessellator.addVertex(box.minX, box.minY, box.maxZ);
			tessellator.addVertex(box.minX, box.maxY, box.maxZ);
			tessellator.draw();
		}

		GL11.glDepthMask(true);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
	}

	/** Marks every renderer overlapping a block range as needing a rebuild. */
	private void markBlocksForUpdate(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		minX >>= 4;
		minY >>= 4;
		minZ >>= 4;
		maxX >>= 4;
		maxY >>= 4;
		maxZ >>= 4;

		for(; minX <= maxX; ++minX) {
			int chunkX = minX % this.renderChunksWide;
			if(chunkX < 0) {
				chunkX += this.renderChunksWide;
			}
			for(int blockY = minY; blockY <= maxY; ++blockY) {
				int chunkY = blockY % this.renderChunksTall;
				if(chunkY < 0) {
					chunkY += this.renderChunksTall;
				}
				for(int blockZ = minZ; blockZ <= maxZ; ++blockZ) {
					int chunkZ = blockZ % this.renderChunksDeep;
					if(chunkZ < 0) {
						chunkZ += this.renderChunksDeep;
					}
					int rendererIndex = (chunkZ * this.renderChunksTall + chunkY) * this.renderChunksWide + chunkX;
					WorldRenderer renderer = this.worldRenderers[rendererIndex];
					if(!renderer.needsUpdate) {
						renderer.needsUpdate = true;
						this.worlRenderersToUpdate.add(renderer);
					}
				}
			}
		}
	}

	public final void markBlockAndNeighborsNeedsUpdate(int x, int y, int z) {
		this.markBlocksForUpdate(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1);
	}

	public final void markBlockRangeNeedsUpdate(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		this.markBlocksForUpdate(minX - 1, minY - 1, minZ - 1, maxX + 1, maxY + 1, maxZ + 1);
	}

	public final void clipRenderersByFrustrum(Frustrum frustrum) {
		for(int i = 0; i < this.worldRenderers.length; ++i) {
			this.worldRenderers[i].updateInFrustrum(frustrum);
		}
	}

	public final void playSound(String soundName, double x, double y, double z, float volume, float pitch) {
		this.mc.sndManager.playSound(soundName, (float)x, (float)y, (float)z, volume, pitch);
	}

	/**
	 * Spawns a particle effect by name near the player. Note: names are compared
	 * by string identity (==) in this vintage — callers pass string literals so
	 * it works, preserved deliberately for fidelity.
	 */
	public final void spawnParticle(String particleName, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
		double dx = this.worldObj.playerEntity.posX - x;
		double dy = this.worldObj.playerEntity.posY - y;
		double dz = this.worldObj.playerEntity.posZ - z;
		if(dx * dx + dy * dy + dz * dz <= 256.0D) {
			if(particleName == "bubble") {
				this.mc.effectRenderer.addEffect(new EntityBubbleFX(this.worldObj, x, y, z, velocityX, velocityY, velocityZ));
			} else if(particleName == "smoke") {
				this.mc.effectRenderer.addEffect(new EntitySmokeFX(this.worldObj, x, y, z));
			} else if(particleName == "explode") {
				this.mc.effectRenderer.addEffect(new EntityExplodeFX(this.worldObj, x, y, z, velocityX, velocityY, velocityZ));
			} else if(particleName == "flame") {
				this.mc.effectRenderer.addEffect(new EntityFlameFX(this.worldObj, x, y, z));
			} else if(particleName == "lava") {
				this.mc.effectRenderer.addEffect(new EntityLavaFX(this.worldObj, x, y, z));
			} else if(particleName == "splash") {
				this.mc.effectRenderer.addEffect(new EntitySplashFX(this.worldObj, x, y, z));
			} else if(particleName == "largesmoke") {
				this.mc.effectRenderer.addEffect(new EntitySmokeFX(this.worldObj, x, y, z, 2.5F));
			}
		}
	}

	public final void obtainEntitySkin(Entity entity) {
		if(entity.skinUrl != null) {
			this.renderEngine.obtainImageData(entity.skinUrl, new ImageBufferDownload());
		}
	}

	public final void releaseEntitySkin(Entity entity) {
		if(entity.skinUrl != null) {
			this.renderEngine.releaseImageData(entity.skinUrl);
		}
	}

	/** Forces every already-lit chunk renderer to recompile. */
	public final void updateAllRenderers() {
		for(int i = 0; i < this.worldRenderers.length; ++i) {
			if(!this.worldRenderers[i].needsUpdate && this.worldRenderers[i].isChunkLit) {
				this.worldRenderers[i].needsUpdate = true;
				this.worlRenderersToUpdate.add(this.worldRenderers[i]);
			}
		}
	}
}