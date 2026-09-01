package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.physics.AxisAlignedBB;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;

/**
 * Base of water and lava, backported from the b1.7.3 flowing-liquids engine.
 * The block id space is split in two roles per liquid: a <em>moving</em> id
 * (placed, actively spreading — {@link BlockFlowing}) and a <em>still</em> id
 * (settled, quiet — {@link BlockStationary}). The shared metadata format is the
 * <em>flow decay</em>: 0 means a full source, 1-7 a surface cell that many
 * levels downstream, and {@code decay + 8} a column that is currently falling
 * (see {@link #getPercentAir}). Everything both roles share lives here:
 *
 * <ul>
 *   <li>the decay reading helpers ({@link #getFlowDecay}, {@link #getEffectiveFlowDecay});</li>
 *   <li>the liquid rendering contract (render type 4, lowered surfaces);</li>
 *   <li>the lava/water reaction ({@link #checkForHarden}) and its fizz effects;</li>
 *   <li>the ambient floating debris of a water or lava surface.</li>
 * </ul>
 */
public class BlockFluid extends Block {
	protected int stillId;
	protected int movingId;

	protected BlockFluid(int blockID, Material material) {
		super(blockID, material);
		this.blockIndexInTexture = 14;
		if(material == Material.lava) {
			this.blockIndexInTexture = 30;
		}
		this.movingId = blockID;
		this.stillId = blockID + 1;
		this.setBlockBounds(0.01F, -0.09F, 0.01F, 1.01F, 0.90999997F, 1.01F);
		this.setTickOnLoad(true);
		this.setResistance(2.0F);
	}

	/**
	 * The per-cell air gap above a liquid's surface, as a fraction of a block
	 * height. A source (decay 0) and any falling column leave one ninth open;
	 * the strongest flow (decay 7) shows almost the whole cell.
	 */
	public static float getPercentAir(int decay) {
		if(decay >= 8) {
			decay = 0;
		}
		return (float)(decay + 1) / 9.0F;
	}

	@Override
	public final int getBlockTextureFromSide(int side) {
		return this.blockMaterial == Material.lava ? this.blockIndexInTexture : (side == 1 ? this.blockIndexInTexture : (side == 0 ? this.blockIndexInTexture : this.blockIndexInTexture + 32));
	}

	/** Liquids use their own renderer (type 4) so surfaces dip with the flow decay. */
	@Override
	public int getRenderType() {
		return 4;
	}

	@Override
	public final boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public final boolean isOpaqueCube() {
		return false;
	}

	@Override
	public final boolean isCollidable() {
		return false;
	}

	@Override
	public final AxisAlignedBB getCollisionBoundingBoxFromPool(int x, int y, int z) {
		return null;
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		this.checkForHarden(world, x, y, z);
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, int neighborID) {
		this.checkForHarden(world, x, y, z);
	}

	/**
	 * The raw decay stored in a cell that is of this liquid, or -1 when the cell
	 * holds anything else. Unlike {@link #getEffectiveFlowDecay} it keeps the +8
	 * "falling" bit so the flow logic can tell a vertical column apart.
	 */
	protected int getFlowDecay(World world, int x, int y, int z) {
		return world.getBlockMaterial(x, y, z) != this.blockMaterial ? -1 : world.getBlockMetadata(x, y, z);
	}

	/**
	 * Decay as a renderer treats it: the falling bit is dropped (a falling column
	 * is a full-depth source), anything non-liquid yields -1.
	 */
	protected int getEffectiveFlowDecay(IBlockAccess blockAccess, int x, int y, int z) {
		if(blockAccess.getBlockMaterial(x, y, z) != this.blockMaterial) {
			return -1;
		}
		int decay = blockAccess.getBlockMetadata(x, y, z);
		return decay >= 8 ? 0 : decay;
	}

	/** The six neighbours scanned for the lava/water reaction, as x/y/z offsets. */
	private static final int[][] LAVA_WATER_NEIGHBORS = {
		{0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}
	};

	/**
	 * The block this liquid hardens into when meeting the other fluid type.
	 * Water does not harden; lava hardens into cobblestone (decay 1–4) or
	 * obsidian (decay 0, a source block). The base default is {@code null}
	 * for water; the lava override at the bottom of this class returns the
	 * right block. Used by {@link #checkForHarden}.
	 *
	 * @param decay  the flow-decay level of the cell (0 = source, 1–7 = flow)
	 * @return the hardened block, or {@code null} if this liquid does not harden
	 */
	protected Block hardenedBlock(int decay) {
		return this.blockMaterial == Material.lava ? (decay == 0 ? Block.obsidian : (decay <= 4 ? Block.cobblestone : null)) : null;
	}

	/**
	 * Lava meeting water hardens: a lava source becomes obsidian, any flowing
	 * lava within four decay levels turns to cobblestone. Only the lava side
	 * reacts; water flowing over lava does nothing.
	 */
	private void checkForHarden(World world, int x, int y, int z) {
		if(world.getBlockId(x, y, z) != this.blockID || this.blockMaterial != Material.lava) {
			return;
		}

		boolean touchesWater = java.util.Arrays.stream(LAVA_WATER_NEIGHBORS)
			.anyMatch(offset -> world.getBlockMaterial(x + offset[0], y + offset[1], z + offset[2]) == Material.water);
		if(!touchesWater) {
			return;
		}

		int decay = world.getBlockMetadata(x, y, z);
		Block hardened = this.hardenedBlock(decay);
		if(hardened != null) {
			world.setBlockWithNotify(x, y, z, hardened.blockID);
		}
		this.triggerLavaMixEffects(world, x, y, z);
	}

	/**
	 * Wet sizzle and a burst of smoke when lava churns into stone.
	 */
	protected void triggerLavaMixEffects(World world, int x, int y, int z) {
		world.playSoundEffect((double)((float)x + 0.5F), (double)((float)y + 0.5F), (double)((float)z + 0.5F), "random.fizz", 0.5F, 2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);

		for(int i = 0; i < 8; ++i) {
			world.spawnParticle("largesmoke", (double)x + Math.random(), (double)y + 1.2D, (double)z + Math.random(), 0.0D, 0.0D, 0.0D);
		}
	}

	@Override
	public final float getBlockBrightness(IBlockAccess blockAccess, int x, int y, int z) {
		return this.blockMaterial == Material.lava ? 100.0F : super.getBlockBrightness(blockAccess, x, y, z);
	}

	@Override
	public boolean shouldSideBeRendered(IBlockAccess blockAccess, int x, int y, int z, int side) {
		Material neighborMaterial = blockAccess.getBlockMaterial(x, y, z);
		return neighborMaterial == this.blockMaterial ? false : (side == 1 ? true : super.shouldSideBeRendered(blockAccess, x, y, z, side));
	}

	@Override
	public final void randomDisplayTick(World world, int x, int y, int z, Random random) {
		// Flowing water (not still, not a source) gurgles occasionally.
		if(this.blockMaterial == Material.water && random.nextInt(64) == 0) {
			int decay = world.getBlockMetadata(x, y, z);
			if(decay > 0 && decay < 8) {
				world.playSoundEffect((double)((float)x + 0.5F), (double)((float)y + 0.5F), (double)((float)z + 0.5F), "liquid.water", random.nextFloat() * 0.25F + 0.75F, random.nextFloat() + 0.5F);
			}
		}

		// Lava with open air above spits the occasional ember.
		if(this.blockMaterial == Material.lava && world.getBlockMaterial(x, y + 1, z) == Material.air && !world.isSolid(x, y + 1, z) && random.nextInt(100) == 0) {
			world.spawnParticle("lava", (double)((float)x + random.nextFloat()), (double)y + this.maxY, (double)((float)z + random.nextFloat()), 0.0D, 0.0D, 0.0D);
		}
	}

	@Override
	public int tickRate() {
		return this.blockMaterial == Material.lava ? 25 : 5;
	}

	@Override
	public final int quantityDropped(Random random) {
		return 0;
	}

	@Override
	public final int getRenderBlockPass() {
		return this.blockMaterial == Material.water ? 1 : 0;
	}
}