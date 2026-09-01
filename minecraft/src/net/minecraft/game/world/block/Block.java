package net.minecraft.game.world.block;

import java.util.Random;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.misc.EntityItem;
import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.entity.player.InventoryPlayer;
import net.minecraft.game.item.Item;
import net.minecraft.game.item.ItemBlock;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.physics.AxisAlignedBB;
import net.minecraft.game.physics.MovingObjectPosition;
import net.minecraft.game.physics.Vec3D;
import net.minecraft.game.world.IBlockAccess;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;

public class Block {
	public static final StepSound soundPowderFootstep = new StepSound("stone", 1.0F, 1.0F);
	public static final StepSound soundWoodFootstep = new StepSound("wood", 1.0F, 1.0F);
	public static final StepSound soundGravelFootstep = new StepSound("gravel", 1.0F, 1.0F);
	public static final StepSound soundGrassFootstep = new StepSound("grass", 1.0F, 1.0F);
	public static final StepSound soundStoneFootstep = new StepSound("stone", 1.0F, 1.0F);
	public static final StepSound soundMetalFootstep = new StepSound("stone", 1.0F, 1.5F);
	public static final StepSound soundGlassFootstep = new StepSoundGlass("glass", 1.0F, 1.0F);
	public static final StepSound soundClothFootstep = new StepSound("cloth", 1.0F, 1.0F);
	public static final StepSound soundSandFootstep = new StepSoundSand("sand", 1.0F, 1.0F);

	public static final Block[] blocksList = new Block[256];
	public static final boolean[] tickOnLoad = new boolean[256];
	public static final boolean[] opaqueCubeLookup = new boolean[256];
	public static final int[] lightOpacity = new int[256];
	private static boolean[] canBlockGrass = new boolean[256];
	public static final boolean[] isBlockContainer = new boolean[256];
	public static final int[] lightValue = new int[256];

	public static final Block stone = (new BlockStone(1, 1)).setHardness(1.5F).setResistance(10.0F).setStepSound(soundStoneFootstep);
	public static final BlockGrass grass = (BlockGrass)(new BlockGrass(2)).setHardness(0.6F).setStepSound(soundGrassFootstep);
	public static final Block dirt = (new BlockDirt(3, 2)).setHardness(0.5F).setStepSound(soundGravelFootstep);
	public static final Block cobblestone = (new Block(4, 16, Material.rock)).setHardness(2.0F).setResistance(10.0F).setStepSound(soundStoneFootstep);
	public static final Block planks = (new Block(5, 4, Material.wood)).setHardness(2.0F).setResistance(5.0F).setStepSound(soundWoodFootstep);
	public static final Block sapling = (new BlockSapling(6, 15)).setHardness(0.0F).setStepSound(soundGrassFootstep);
	public static final Block bedrock = (new Block(7, 17, Material.rock)).setHardness(-1.0F).setResistance(6000000.0F).setStepSound(soundStoneFootstep);
	public static final Block waterMoving = (new BlockFlowing(8, Material.water)).setHardness(100.0F).setLightOpacity(3);
	public static final Block waterStill = (new BlockStationary(9, Material.water)).setHardness(100.0F).setLightOpacity(3);
	public static final Block lavaMoving = (new BlockFlowing(10, Material.lava)).setHardness(0.0F).setLightValue(1.0F).setLightOpacity(255);
	public static final Block lavaStill = (new BlockStationary(11, Material.lava)).setHardness(100.0F).setLightValue(1.0F).setLightOpacity(255);
	public static final Block sand = (new BlockSand(12, 18)).setHardness(0.5F).setStepSound(soundSandFootstep);
	public static final Block gravel = (new BlockGravel(13, 19)).setHardness(0.6F).setStepSound(soundGravelFootstep);
	public static final Block oreGold = (new BlockOre(14, 32)).setHardness(3.0F).setResistance(5.0F).setStepSound(soundStoneFootstep);
	public static final Block oreIron = (new BlockOre(15, 33)).setHardness(3.0F).setResistance(5.0F).setStepSound(soundStoneFootstep);
	public static final Block oreCoal = (new BlockOre(16, 34)).setHardness(3.0F).setResistance(5.0F).setStepSound(soundStoneFootstep);
	public static final Block wood = (new BlockLog(17)).setHardness(2.0F).setStepSound(soundWoodFootstep);
	public static final Block leaves = (new BlockLeaves(18, 52)).setHardness(0.2F).setLightOpacity(1).setStepSound(soundGrassFootstep);
	public static final Block sponge = (new BlockSponge(19)).setHardness(0.6F).setStepSound(soundGrassFootstep);
	public static final Block glass = (new BlockGlass(20, 49, Material.glass, false)).setHardness(0.3F).setStepSound(soundGlassFootstep);
	public static final Block cloth = (new BlockCloth(35, 64)).setHardness(0.8F).setStepSound(soundClothFootstep);
	public static final BlockFlower flowers = (BlockFlower)(new BlockFlower(37, 12)).setHardness(0.0F).setStepSound(soundGrassFootstep);
	public static final BlockFlower mushrooms = (BlockFlower)(new BlockMushroom(38, 29)).setHardness(0.0F).setStepSound(soundGrassFootstep);
	public static final Block blockGold = (new BlockOreStorage(41, 39)).setHardness(3.0F).setResistance(10.0F).setStepSound(soundMetalFootstep);
	public static final Block blockSteel = (new BlockOreStorage(42, 38)).setHardness(5.0F).setResistance(10.0F).setStepSound(soundMetalFootstep);
	public static final Block stairDouble = (new BlockStep(43, true)).setHardness(2.0F).setResistance(10.0F).setStepSound(soundStoneFootstep);
	public static final Block stairSingle = (new BlockStep(44, false)).setHardness(2.0F).setResistance(10.0F).setStepSound(soundStoneFootstep);
	public static final Block brick = (new Block(45, 7, Material.rock)).setHardness(2.0F).setResistance(10.0F).setStepSound(soundStoneFootstep);
	public static final Block tnt = (new BlockTNT(46, 8)).setHardness(0.0F).setStepSound(soundGrassFootstep);
	public static final Block bookshelf = (new BlockBookshelf(47, 35)).setHardness(1.5F).setStepSound(soundWoodFootstep);
	public static final Block cobblestoneMossy = (new Block(48, 36, Material.rock)).setHardness(2.0F).setResistance(10.0F).setStepSound(soundStoneFootstep);
	public static final Block obsidian = (new BlockStone(49, 37)).setHardness(10.0F).setResistance(10.0F).setStepSound(soundStoneFootstep);
	public static final Block torch = (new BlockTorch(50, 80)).setHardness(0.0F).setLightValue(15.0F / 16.0F).setStepSound(soundWoodFootstep);
	public static final BlockFire fire = (BlockFire)(new BlockFire(51, 31)).setHardness(0.0F).setLightValue(1.0F).setStepSound(soundWoodFootstep);
	public static final Block chest = (new BlockChest(54)).setHardness(2.5F).setStepSound(soundWoodFootstep);
	public static final Block cog = (new BlockGears(55, 62)).setHardness(0.5F).setStepSound(soundMetalFootstep);
	public static final Block oreDiamond = (new BlockOre(56, 50)).setHardness(3.0F).setResistance(5.0F).setStepSound(soundStoneFootstep);
	public static final Block blockDiamond = (new BlockOreStorage(57, 40)).setHardness(5.0F).setResistance(10.0F).setStepSound(soundMetalFootstep);
	public static final Block workbench = (new BlockWorkbench(58)).setHardness(2.5F).setStepSound(soundWoodFootstep);
	public static final Block crops = (new BlockCrops(59, 88)).setHardness(0.0F).setStepSound(soundGrassFootstep);
	public static final Block tilledField = (new BlockFarmland(60)).setHardness(0.6F).setStepSound(soundGravelFootstep);
	public static final Block stoneOvenIdle = (new BlockFurnace(61, false)).setHardness(3.5F).setStepSound(soundStoneFootstep);
	public static final Block stoneOvenActive = (new BlockFurnace(62, true)).setHardness(3.5F).setStepSound(soundStoneFootstep).setLightValue(14.0F / 16.0F);
	public int blockIndexInTexture;
	public final int blockID;
	private float blockHardness;
	private float blockResistance;
	public double minX;
	public double minY;
	public double minZ;
	public double maxX;
	public double maxY;
	public double maxZ;
	public StepSound stepSound;
	public float blockParticleGravity;
	public final Material blockMaterial;

	protected Block(int blockID, Material material) {
		this.stepSound = soundPowderFootstep;
		this.blockParticleGravity = 1.0F;
		if(blocksList[blockID] != null) {
			throw new IllegalArgumentException("Slot " + blockID + " is already occupied by " + blocksList[blockID] + " when adding " + this);
		}

		this.blockMaterial = material;
		blocksList[blockID] = this;
		this.blockID = blockID;
		this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
		opaqueCubeLookup[blockID] = this.isOpaqueCube();
		lightOpacity[blockID] = this.isOpaqueCube() ? 255 : 0;
		canBlockGrass[blockID] = this.renderAsNormalBlock();
		isBlockContainer[blockID] = false;
	}

	protected Block(int blockID, int textureIndex, Material material) {
		this(blockID, material);
		this.blockIndexInTexture = textureIndex;
	}

	protected final Block setStepSound(StepSound stepSound) {
		this.stepSound = stepSound;
		return this;
	}

	protected final Block setLightOpacity(int opacity) {
		lightOpacity[this.blockID] = opacity;
		return this;
	}

	private Block setLightValue(float value) {
		lightValue[this.blockID] = (int)(15.0F * value);
		return this;
	}

	protected final Block setResistance(float resistance) {
		this.blockResistance = resistance * 3.0F;
		return this;
	}

	public boolean renderAsNormalBlock() {
		return true;
	}

	public int getRenderType() {
		return 0;
	}

	protected final Block setHardness(float hardness) {
		this.blockHardness = hardness;
		if(this.blockResistance < hardness * 5.0F) {
			this.blockResistance = hardness * 5.0F;
		}

		return this;
	}

	protected final void setTickOnLoad(boolean value) {
		tickOnLoad[this.blockID] = value;
	}

	protected final void setBlockBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		this.minX = (double)minX;
		this.minY = (double)minY;
		this.minZ = (double)minZ;
		this.maxX = (double)maxX;
		this.maxY = (double)maxY;
		this.maxZ = (double)maxZ;
	}

	public float getBlockBrightness(IBlockAccess blockAccess, int x, int y, int z) {
		return blockAccess.getBrightness(x, y, z);
	}

	public boolean shouldSideBeRendered(IBlockAccess blockAccess, int x, int y, int z, int side) {
		return !blockAccess.isSolid(x, y, z);
	}

	public int getBlockTexture(IBlockAccess blockAccess, int x, int y, int z, int side) {
		return this.getBlockTextureFromSideAndMetadata(side, blockAccess.getBlockMetadata(x, y, z));
	}

	public int getBlockTextureFromSideAndMetadata(int side, int metadata) {
		return this.getBlockTextureFromSide(side);
	}

	public int getBlockTextureFromSide(int side) {
		return this.blockIndexInTexture;
	}

	public final AxisAlignedBB getSelectedBoundingBoxFromPool(int x, int y, int z) {
		return new AxisAlignedBB((double)x + this.minX, (double)y + this.minY, (double)z + this.minZ, (double)x + this.maxX, (double)y + this.maxY, (double)z + this.maxZ);
	}

	public AxisAlignedBB getCollisionBoundingBoxFromPool(int x, int y, int z) {
		return new AxisAlignedBB((double)x + this.minX, (double)y + this.minY, (double)z + this.minZ, (double)x + this.maxX, (double)y + this.maxY, (double)z + this.maxZ);
	}

	public boolean isOpaqueCube() {
		return true;
	}

	public boolean isCollidable() {
		return true;
	}

	public void updateTick(World world, int x, int y, int z, Random random) {
	}

	public void randomDisplayTick(World world, int x, int y, int z, Random random) {
	}

	public void onBlockDestroyedByPlayer(World world, int x, int y, int z, int metadata) {
	}

	public void onNeighborBlockChange(World world, int x, int y, int z, int neighborID) {
	}

	public int tickRate() {
		return 5;
	}

	public void onBlockAdded(World world, int x, int y, int z) {
	}

	public void onBlockRemoval(World world, int x, int y, int z) {
	}

	/**
	 * Returns the RGB tint applied to this block when it is rendered. The
	 * 24-bit value packs red, green and blue as the high, middle and low
	 * 8 bits respectively (so {@code 0xRRGGBB}). The default is
	 * {@code 0xFFFFFF} (no tint — the texture colour shows through unchanged);
	 * blocks such as cloth override this and let their metadata select a
	 * coloured swatch.
	 *
	 * @param metadata the block's current metadata nibble
	 * @return the tint colour in 0xRRGGBB form
	 */
	public int getRenderColor(int metadata) {
		return 0xFFFFFF;
	}

	public int quantityDropped(Random random) {
		return 1;
	}

	public int idDropped(int metadata, Random random) {
		return this.blockID;
	}

	/**
	 * The item damage that should be attached to the {@link #idDropped dropped}
	 * item in place of the block's metadata. The default keeps the original
	 * behaviour — every block drops a damage-0 item regardless of metadata.
	 * Blocks that consolidate several variants into one id (flowers, mushrooms)
	 * override this so the dropped item still carries its variant.
	 */
	public int damageDropped(int metadata) {
		return 0;
	}

	/**
	 * Whether a plant (flower, sapling, mushroom) may grow on this block. Override
	 * to return {@code true} for dirt, grass and any other block that should be a
	 * valid plant base. The metadata parameter allows a block to accept or reject
	 * based on its own state (e.g. farmland moisture level, mycelium variant).
	 *
	 * @return {@code true} if this block is a valid plant base
	 */
	public boolean canGrowPlants(int metadata) {
		return false;
	}

	/** Digging strength without knowing the block's state; treats metadata as "any". */
	public final float blockStrength(EntityPlayer player) {
		return this.blockStrength(player, -1);
	}

	/**
	 * How much digging progress one swing makes, given the held tool and the
	 * exact block state being mined (used by the tool speed tables that match
	 * specific metadata).
	 */
	public final float blockStrength(EntityPlayer player, int metadata) {
		if(this.blockHardness < 0.0F) {
			return 0.0F;
		} else if(!player.canHarvestBlock(this)) {
			return 1.0F / this.blockHardness / 100.0F;
		} else {
			InventoryPlayer inventory = player.inventory;
			float strength = 1.0F;
			if(inventory.mainInventory[inventory.currentItem] != null) {
				ItemStack currentItemStack = inventory.mainInventory[inventory.currentItem];
				strength = currentItemStack.getItem().getStrVsBlock(this, metadata);
			}

			float effectiveStrength = strength;
			if(player.isInsideOfMaterial()) {
				effectiveStrength = strength / 5.0F;
			}

			if(!player.onGround) {
				effectiveStrength /= 5.0F;
			}

			return effectiveStrength / this.blockHardness / 30.0F;
		}
	}

	public final void dropBlockAsItem(World world, int x, int y, int z, int metadata) {
		this.dropBlockAsItemWithChance(world, x, y, z, metadata, 1.0F);
	}

	public final void dropBlockAsItemWithChance(World world, int x, int y, int z, int metadata, float chance) {
		int itemCount = this.quantityDropped(world.rand);

		for(int i = 0; i < itemCount; ++i) {
			if(world.rand.nextFloat() <= chance) {
				int droppedItemID = this.idDropped(metadata, world.rand);
				if(droppedItemID > 0) {
					double offsetX = (double)(world.rand.nextFloat() * 0.7F) + (double)0.15F;
					double offsetY = (double)(world.rand.nextFloat() * 0.7F) + (double)0.15F;
					double offsetZ = (double)(world.rand.nextFloat() * 0.7F) + (double)0.15F;
					EntityItem entityItem = new EntityItem(world, (double)x + offsetX, (double)y + offsetY, (double)z + offsetZ, new ItemStack(droppedItemID, 1, this.damageDropped(metadata)));
					entityItem.delayBeforeCanPickup = 10;
					world.spawnEntityInWorld(entityItem);
				}
			}
		}

	}

	public final float getExplosionResistance() {
		return this.blockResistance / 5.0F;
	}

	public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3D startVector, Vec3D endVector) {
		startVector = startVector.addVector((double)(-x), (double)(-y), (double)(-z));
		endVector = endVector.addVector((double)(-x), (double)(-y), (double)(-z));
		Vec3D intermediateMinX = startVector.getIntermediateWithXValue(endVector, this.minX);
		Vec3D intermediateMaxX = startVector.getIntermediateWithXValue(endVector, this.maxX);
		Vec3D intermediateMinY = startVector.getIntermediateWithYValue(endVector, this.minY);
		Vec3D intermediateMaxY = startVector.getIntermediateWithYValue(endVector, this.maxY);
		Vec3D intermediateMinZ = startVector.getIntermediateWithZValue(endVector, this.minZ);
		Vec3D intermediateMaxZ = startVector.getIntermediateWithZValue(endVector, this.maxZ);
		if(!this.isVecInsideYZBounds(intermediateMinX)) {
			intermediateMinX = null;
		}

		if(!this.isVecInsideYZBounds(intermediateMaxX)) {
			intermediateMaxX = null;
		}

		if(!this.isVecInsideXZBounds(intermediateMinY)) {
			intermediateMinY = null;
		}

		if(!this.isVecInsideXZBounds(intermediateMaxY)) {
			intermediateMaxY = null;
		}

		if(!this.isVecInsideXYBounds(intermediateMinZ)) {
			intermediateMinZ = null;
		}

		if(!this.isVecInsideXYBounds(intermediateMaxZ)) {
			intermediateMaxZ = null;
		}

		Vec3D closestPoint = null;
		if(intermediateMinX != null) {
			closestPoint = intermediateMinX;
		}

		if(intermediateMaxX != null && (closestPoint == null || startVector.distance(intermediateMaxX) < startVector.distance(closestPoint))) {
			closestPoint = intermediateMaxX;
		}

		if(intermediateMinY != null && (closestPoint == null || startVector.distance(intermediateMinY) < startVector.distance(closestPoint))) {
			closestPoint = intermediateMinY;
		}

		if(intermediateMaxY != null && (closestPoint == null || startVector.distance(intermediateMaxY) < startVector.distance(closestPoint))) {
			closestPoint = intermediateMaxY;
		}

		if(intermediateMinZ != null && (closestPoint == null || startVector.distance(intermediateMinZ) < startVector.distance(closestPoint))) {
			closestPoint = intermediateMinZ;
		}

		if(intermediateMaxZ != null && (closestPoint == null || startVector.distance(intermediateMaxZ) < startVector.distance(closestPoint))) {
			closestPoint = intermediateMaxZ;
		}

		if(closestPoint == null) {
			return null;
		} else {
			byte sideHit = -1;
			if(closestPoint == intermediateMinX) {
				sideHit = 4;
			}

			if(closestPoint == intermediateMaxX) {
				sideHit = 5;
			}

			if(closestPoint == intermediateMinY) {
				sideHit = 0;
			}

			if(closestPoint == intermediateMaxY) {
				sideHit = 1;
			}

			if(closestPoint == intermediateMinZ) {
				sideHit = 2;
			}

			if(closestPoint == intermediateMaxZ) {
				sideHit = 3;
			}

			return new MovingObjectPosition(x, y, z, sideHit, closestPoint.addVector((double)x, (double)y, (double)z));
		}
	}

	private boolean isVecInsideYZBounds(Vec3D point) {
		return point == null ? false : point.yCoord >= this.minY && point.yCoord <= this.maxY && point.zCoord >= this.minZ && point.zCoord <= this.maxZ;
	}

	private boolean isVecInsideXZBounds(Vec3D point) {
		return point == null ? false : point.xCoord >= this.minX && point.xCoord <= this.maxX && point.zCoord >= this.minZ && point.zCoord <= this.maxZ;
	}

	private boolean isVecInsideXYBounds(Vec3D point) {
		return point == null ? false : point.xCoord >= this.minX && point.xCoord <= this.maxX && point.yCoord >= this.minY && point.yCoord <= this.maxY;
	}

	public void onBlockDestroyedByExplosion(World world, int x, int y, int z) {
	}

	public int getRenderBlockPass() {
		return 0;
	}

	public boolean canPlaceBlockAt(World world, int x, int y, int z) {
		return true;
	}

	public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
		return false;
	}

	public void onEntityWalking(World world, int x, int y, int z) {
	}

	public void onBlockPlaced(World world, int x, int y, int z, int side) {
	}

	public void onBlockPlaced(World world, int x, int y, int z, int side, float xWithinFace, float yWithinFace, float zWithinFace) {
		this.onBlockPlaced(world, x, y, z, side);
	}

	/**
	 * The horizontal face (side index 2..5) a freshly placed orientation block
	 * should draw its front on so it faces the player, i.e. the side pointing
	 * back at the nearest player's position: +X -> 5, -X -> 4, +Z -> 3, -Z -> 2.
	 * The choice is taken from the player's position relative to the block, not
	 * from the player's viewing direction, and never consults the surrounding
	 * blocks. Falls back to the default +Z facing when no player exists yet.
	 */
	protected static final int getPlayerFacing(World world, int x, int y, int z) {
		Entity player = world.playerEntity;
		if(player == null) {
			return 3;
		}

		double offsetX = player.posX - ((double)x + 0.5D);
		double offsetZ = player.posZ - ((double)z + 0.5D);
		if(Math.abs(offsetX) >= Math.abs(offsetZ)) {
			return offsetX > 0.0D ? 5 : 4;
		} else {
			return offsetZ > 0.0D ? 3 : 2;
		}
	}

	static {
		(new BlockSource(52, waterMoving.blockID)).setHardness(0.0F).setStepSound(soundWoodFootstep);
		(new BlockSource(53, lavaMoving.blockID)).setHardness(0.0F).setStepSound(soundWoodFootstep);

		for(int blockID = 0; blockID < 256; ++blockID) {
			if(blocksList[blockID] != null) {
				Item.itemsList[blockID] = new ItemBlock(blockID - 256);
			}
		}

	}
}