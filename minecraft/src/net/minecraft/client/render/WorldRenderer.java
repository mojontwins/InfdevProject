package net.minecraft.client.render;

import net.minecraft.client.render.camera.Frustrum;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.physics.AxisAlignedBB;
import net.minecraft.game.world.ChunkCache;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.chunk.Chunk;
import org.lwjgl.opengl.GL11;
import util.MathHelper;

/**
 * Holds the pre-compiled terrain geometry of one 16x16 chunk region. The block
 * faces are baked into two GL call lists: pass 0 for opaque rendering, pass 1
 * for translucent (water etc.). A third list stores a bounding-box used by
 * occlusion queries.
 */
public final class WorldRenderer {
	private World worldObj;
	private int glRenderList = -1;
	private static final Tessellator tessellator = Tessellator.instance;
	public static int chunksUpdated = 0;
	/** Chunk origin in world coordinates. */
	private int posX;
	private int posY;
	private int posZ;
	private int sizeWidth;
	private int sizeHeight;
	private int sizeDepth;
	/** Chunk origin masked into a 512-block wide "render at" space. */
	public int posXMinus;
	public int posYMinus;
	public int posZMinus;
	private int posXClip;
	private int posYClip;
	private int posZClip;
	public boolean isInFrustum = false;
	private final boolean[] skipRenderPass = new boolean[2];
	private int posXPlus;
	private int posYPlus;
	private int posZPlus;
	public boolean needsUpdate;
	private AxisAlignedBB rendererBoundingBox;
	public boolean isVisible = true;
	public boolean isWaitingOnOcclusionQuery;
	public int glOcclusionQuery;
	public boolean isChunkLit;

	public WorldRenderer(World world, int chunkX, int chunkY, int chunkZ, int chunkSize, int glRenderList) {
		this.worldObj = world;
		this.sizeWidth = this.sizeHeight = this.sizeDepth = 16;
		// Legacy no-op kept for behavioural fidelity (its result is discarded).
		MathHelper.sqrt_float((float)(this.sizeWidth * this.sizeWidth + this.sizeHeight * this.sizeHeight + this.sizeDepth * this.sizeDepth));
		this.glRenderList = glRenderList;
		this.posX = -999;
		this.setPosition(chunkX, chunkY, chunkZ);
		this.needsUpdate = false;
	}

	/** Moves this renderer to a new chunk; anything outside moves again is flagged to redraw. */
	public final void setPosition(int chunkX, int chunkY, int chunkZ) {
		if(chunkX != this.posX || chunkY != this.posY || chunkZ != this.posZ) {
			this.setDontDraw();
			this.posX = chunkX;
			this.posY = chunkY;
			this.posZ = chunkZ;
			this.posXPlus = chunkX + this.sizeWidth / 2;
			this.posYPlus = chunkY + this.sizeHeight / 2;
			this.posZPlus = chunkZ + this.sizeDepth / 2;
			// Mask the world position down to a 512^3 window so large
			// coordinates stay inside floating point precision.
			this.posXClip = chunkX & 511;
			this.posYClip = chunkY & 511;
			this.posZClip = chunkZ & 511;
			this.posXMinus = chunkX - this.posXClip;
			this.posYMinus = chunkY - this.posYClip;
			this.posZMinus = chunkZ - this.posZClip;
			this.rendererBoundingBox = (new AxisAlignedBB((double)chunkX, (double)chunkY, (double)chunkZ, (double)(chunkX + this.sizeWidth), (double)(chunkY + this.sizeHeight), (double)(chunkZ + this.sizeDepth))).expand(2.0D, 2.0D, 2.0D);
			// Compile the occlusion-query box (a single unit cube at the chunk
			// origin in the masked space) into call list glRenderList+2.
			GL11.glNewList(this.glRenderList + 2, GL11.GL_COMPILE);
			AxisAlignedBB occlusionBox = new AxisAlignedBB((double)((float)this.posXClip - 2.0F), (double)((float)this.posYClip - 2.0F), (double)((float)this.posZClip - 2.0F), (double)((float)(this.posXClip + this.sizeWidth) + 2.0F), (double)((float)(this.posYClip + this.sizeHeight) + 2.0F), (double)((float)(this.posZClip + this.sizeDepth) + 2.0F));
			Tessellator boxTessellator = Tessellator.instance;
			boxTessellator.startDrawingQuads();
			boxTessellator.addVertex(occlusionBox.minX, occlusionBox.maxY, occlusionBox.minZ);
			boxTessellator.addVertex(occlusionBox.maxX, occlusionBox.maxY, occlusionBox.minZ);
			boxTessellator.addVertex(occlusionBox.maxX, occlusionBox.minY, occlusionBox.minZ);
			boxTessellator.addVertex(occlusionBox.minX, occlusionBox.minY, occlusionBox.minZ);
			boxTessellator.addVertex(occlusionBox.minX, occlusionBox.minY, occlusionBox.maxZ);
			boxTessellator.addVertex(occlusionBox.maxX, occlusionBox.minY, occlusionBox.maxZ);
			boxTessellator.addVertex(occlusionBox.maxX, occlusionBox.maxY, occlusionBox.maxZ);
			boxTessellator.addVertex(occlusionBox.minX, occlusionBox.maxY, occlusionBox.maxZ);
			boxTessellator.addVertex(occlusionBox.minX, occlusionBox.minY, occlusionBox.minZ);
			boxTessellator.addVertex(occlusionBox.maxX, occlusionBox.minY, occlusionBox.minZ);
			boxTessellator.addVertex(occlusionBox.maxX, occlusionBox.minY, occlusionBox.maxZ);
			boxTessellator.addVertex(occlusionBox.minX, occlusionBox.minY, occlusionBox.maxZ);
			boxTessellator.addVertex(occlusionBox.minX, occlusionBox.maxY, occlusionBox.maxZ);
			boxTessellator.addVertex(occlusionBox.maxX, occlusionBox.maxY, occlusionBox.maxZ);
			boxTessellator.addVertex(occlusionBox.maxX, occlusionBox.maxY, occlusionBox.minZ);
			boxTessellator.addVertex(occlusionBox.minX, occlusionBox.maxY, occlusionBox.minZ);
			boxTessellator.addVertex(occlusionBox.minX, occlusionBox.minY, occlusionBox.maxZ);
			boxTessellator.addVertex(occlusionBox.minX, occlusionBox.maxY, occlusionBox.maxZ);
			boxTessellator.addVertex(occlusionBox.minX, occlusionBox.maxY, occlusionBox.minZ);
			boxTessellator.addVertex(occlusionBox.minX, occlusionBox.minY, occlusionBox.minZ);
			boxTessellator.addVertex(occlusionBox.maxX, occlusionBox.minY, occlusionBox.minZ);
			boxTessellator.addVertex(occlusionBox.maxX, occlusionBox.maxY, occlusionBox.minZ);
			boxTessellator.addVertex(occlusionBox.maxX, occlusionBox.maxY, occlusionBox.maxZ);
			boxTessellator.addVertex(occlusionBox.maxX, occlusionBox.minY, occlusionBox.maxZ);
			boxTessellator.draw();
			GL11.glEndList();
			this.needsUpdate = true;
		}
	}

	/**
	 * Rebuilds the two geometry call lists by tessellating every block in the
	 * chunk. Pass 0 renders opaque blocks, pass 1 renders translucent ones; if
	 * the chunk contains no blocks of a pass that pass is skipped.
	 */
	public final void updateRenderer() {
		if(this.needsUpdate) {
			++chunksUpdated;
			int chunkMinX = this.posX;
			int chunkMinY = this.posY;
			int chunkMinZ = this.posZ;
			int chunkMaxX = this.posX + this.sizeWidth;
			int chunkMaxY = this.posY + this.sizeHeight;
			int chunkMaxZ = this.posZ + this.sizeDepth;

			for(int pass = 0; pass < 2; ++pass) {
				this.skipRenderPass[pass] = true;
			}

			// Track whether any block in the chunk lit up this rebuild so the
			// lighting HUD can report when chunks are fully lit.
			Chunk.isLit = false;
			// Snapshot the chunk references of this region plus a one-block
			// margin, so every block lookup below — including the neighbour
			// checks RenderBlocks makes for face culling and lighting —
			// resolves through this cache instead of re-walking the world.
			ChunkCache chunkCache = new ChunkCache(this.worldObj, chunkMinX - 1, chunkMinY - 1, chunkMinZ - 1, chunkMaxX + 1, chunkMaxY + 1, chunkMaxZ + 1);
			RenderBlocks renderBlocks = new RenderBlocks(chunkCache);
			// The renderer cell is exactly one 16-block chunk column, so resolve
			// the owning chunk once and read its blocks directly below.
			Chunk chunk = this.worldObj.getChunkFromChunkCoords(this.posX >> 4, this.posZ >> 4);

			for(int pass = 0; pass < 2; ++pass) {
				boolean foundWrongPassBlock = false;
				boolean foundBlocksInPass = false;
				GL11.glNewList(this.glRenderList + pass, GL11.GL_COMPILE);
				GL11.glPushMatrix(); 
				// Translate into masked space so vertex floats stay small.
				GL11.glTranslatef((float)this.posXClip, (float)this.posYClip, (float)this.posZClip);
				tessellator.startDrawingQuads();
				// And bake the negative world offset into the vertices, so the
				// call list is self-contained at its origin.
				tessellator.setTranslationD((double)(-this.posX), (double)(-this.posY), (double)(-this.posZ));

				for(int blockY = chunkMinY; blockY < chunkMaxY; ++blockY) {
					for(int blockZ = chunkMinZ; blockZ < chunkMaxZ; ++blockZ) {
						for(int blockX = chunkMinX; blockX < chunkMaxX; ++blockX) {
							int blockId = chunk.getBlockID(blockX & 15, blockY, blockZ & 15);
							if(blockId > 0) {
								Block block = Block.blocksList[blockId];
								if(block.getRenderBlockPass() != pass) {
									// A block for the other pass exists, so we
									// must keep compiling that pass's list too.
									foundWrongPassBlock = true;
								} else {
									foundBlocksInPass |= renderBlocks.renderBlockByRenderType(block, blockX, blockY, blockZ);
								}
							}
						}
					}
				}

				tessellator.draw();
				GL11.glPopMatrix();
				GL11.glEndList();
				tessellator.setTranslationD(0.0D, 0.0D, 0.0D);
				if(foundBlocksInPass) {
					this.skipRenderPass[pass] = false;
				}

				// If no block of the other pass exists, break early.
				if(!foundWrongPassBlock) {
					break;
				}
			}

			this.isChunkLit = Chunk.isLit;
		}
	}

	public final float distanceToEntitySquared(Entity entity) {
		float dx = (float)(entity.posX - (double)this.posXPlus);
		float dy = (float)(entity.posY - (double)this.posYPlus);
		float dz = (float)(entity.posZ - (double)this.posZPlus);
		return dx * dx + dy * dy + dz * dz;
	}

	private void setDontDraw() {
		for(int pass = 0; pass < 2; ++pass) {
			this.skipRenderPass[pass] = true;
		}
	}

	public final void stopRendering() {
		this.setDontDraw();
		this.worldObj = null;
	}

	public final int getGLCallListForPass(int pass) {
		return !this.isInFrustum ? -1 : (!this.skipRenderPass[pass] ? this.glRenderList + pass : -1);
	}

	public final void updateInFrustrum(Frustrum frustrum) {
		this.isInFrustum = frustrum.isBoundingBoxInFrustrum(this.rendererBoundingBox);
	}

	public final void callOcclusionQueryList() {
		GL11.glCallList(this.glRenderList + 2);
	}

	public final boolean skipAllRenderPasses() {
		return this.skipRenderPass[0] && this.skipRenderPass[1];
	}
}