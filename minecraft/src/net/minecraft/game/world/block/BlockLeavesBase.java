package net.minecraft.game.world.block;

import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.material.Material;

/**
 * Foliage superclass shared by leaves and similar translucent plants.
 *
 * <p>It has two render modes selected by {@link #setGraphicsLevel}:
 * <strong>fancy</strong> (the default, {@code graphicsLevel} true) renders each
 * leaf as a transparent cube whose touching faces are merged away, while
 * <strong>fast</strong> ({@code graphicsLevel} false) renders leaves as plain
 * opaque cubes using a fully opaque texture tile one past the transparent one.
 * Fast mode is wired but left off so the client keeps the translucent look.
 */
public class BlockLeavesBase extends Block {
	/** True renders transparent, merged-face "fancy" leaves; false renders opaque "fast" leaves. */
	private boolean graphicsLevel = true;
	/** The transparent leaf texture; fast mode swaps to {@code leafTextureIndex + 1}. */
	private final int leafTextureIndex;

	protected BlockLeavesBase(int blockID, int textureIndex, Material material) {
		super(blockID, textureIndex, material);
		this.leafTextureIndex = textureIndex;
	}

	@Override
	public boolean isOpaqueCube() {
		return !this.graphicsLevel;
	}

	/**
	 * Switches between fancy (transparent) and fast (opaque) leaf rendering. Not
	 * currently invoked anywhere: leaves stay fancy unless a caller opts in.
	 */
	public void setGraphicsLevel(boolean fancy) {
		this.graphicsLevel = fancy;
		this.blockIndexInTexture = this.leafTextureIndex + (fancy ? 0 : 1);
	}

	@Override
	public boolean shouldSideBeRendered(IBlockAccess blockAccess, int x, int y, int z, int side) {
		int neighborBlockID = blockAccess.getBlockId(x, y, z);
		// In fancy mode two leaf cubes hide their shared face to avoid double-draw.
		return !this.graphicsLevel && neighborBlockID == this.blockID ? false : super.shouldSideBeRendered(blockAccess, x, y, z, side);
	}
}