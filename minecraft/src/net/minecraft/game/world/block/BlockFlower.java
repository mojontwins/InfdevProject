package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.physics.AxisAlignedBB;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;

/**
 * Base of all short plants (flowers, saplings, mushrooms, crops): a
 * non-colliding, non-cube block that sits on a supporting block below, is
 * re-checked every tick, and drops itself when that support is removed.
 * Subclasses tune the {@link #canThisPlantGrowOnThisBlockID} support rule and
 * the {@link #canBlockStay} light requirement, and may replace the
 * {@link #metadataToTexture} map so the renderer selects the right tile.
 * The base flower maps metadata 0 to the red flower tile (12) and metadata 1
 * to the yellow flower tile (13).
 */
public class BlockFlower extends Block {
	/** Maps metadata to the atlas tile index. Subclasses override this. */
	protected int[] metadataToTexture;

	protected BlockFlower(int blockID, int textureIndex) {
		super(blockID, Material.plants);
		this.blockIndexInTexture = textureIndex;
		this.metadataToTexture = new int[]{12, 13};
		this.setTickOnLoad(true);
		this.setBlockBounds(0.3F, 0.0F, 0.3F, 0.7F, 0.6F, 0.7F);
	}

	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int z) {
		return world.canPlantsGrowOn(x, y - 1, z);
	}

	@Override
	public final void onNeighborBlockChange(World world, int x, int y, int z, int neighborID) {
		super.onNeighborBlockChange(world, x, y, z, neighborID);
		this.checkFlowerChange(world, x, y, z);
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		this.checkFlowerChange(world, x, y, z);
	}

	private void checkFlowerChange(World world, int x, int y, int z) {
		if(!this.canBlockStay(world, x, y, z)) {
			this.dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z));
			world.setBlockWithNotify(x, y, z, 0);
		}
	}

	public boolean canBlockStay(World world, int x, int y, int z) {
		return (world.getBlockLightValue(x, y, z) >= 8 || world.canBlockSeeTheSky(x, y, z)) && world.canPlantsGrowOn(x, y - 1, z);
	}

	@Override
	public final AxisAlignedBB getCollisionBoundingBoxFromPool(int x, int y, int z) {
		return null;
	}

	@Override
	public final boolean isOpaqueCube() {
		return false;
	}

	@Override
	public final boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public int getRenderType() {
		return 1;
	}

	/**
	 * Looks up the atlas tile for the given metadata. Out-of-range indices
	 * wrap around so it is always safe to call even for unknown metadata values.
	 */
	@Override
	public int getBlockTextureFromSideAndMetadata(int side, int metadata) {
		return this.metadataToTexture[metadata % this.metadataToTexture.length];
	}

	/**
	 * Drops the item carrying the block's metadata, so the variant survives
	 * being harvested and can be re-placed: a yellow flower breaks into a
	 * yellow-flower item, a red mushroom into a red one. The base class drops
	 * damage-0 items for every other block.
	 */
	@Override
	public int damageDropped(int metadata) {
		return metadata;
	}

	/** Flowers (and all plant subclasses) are ephemeral: right-clicking them with a block replaces them. */
	@Override
	public boolean canBeSubstituted() {
		return true;
	}

	/** Flowers drop themselves as an item when replaced. */
	@Override
	public void onSubstituted(World world, int x, int y, int z, int metadata) {
		this.dropBlockAsItem(world, x, y, z, metadata);
	}
}
