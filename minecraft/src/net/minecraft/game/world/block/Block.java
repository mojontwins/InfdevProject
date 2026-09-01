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

	// --- the block catalogue (in register order, grouped by tens) ---------------
	// ID 0
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
	public static final Block oreCoal = (new BlockOreCoal(16, 34)).setHardness(3.0F).setResistance(5.0F).setStepSound(soundStoneFootstep);
	public static final Block wood = (new BlockLog(17)).setHardness(2.0F).setStepSound(soundWoodFootstep);
	public static final Block leaves = (new BlockLeaves(18, 52)).setHardness(0.2F).setLightOpacity(1).setStepSound(soundGrassFootstep);
	public static final Block sponge = (new BlockSponge(19)).setHardness(0.6F).setStepSound(soundGrassFootstep);

	public static final Block glass = (new BlockGlass(20, 49, Material.glass, false)).setHardness(0.3F).setStepSound(soundGlassFootstep);
	// ID 21
	// ID 22
	// ID 23
	// ID 24
	// ID 25
	// ID 26
	// ID 27
	// ID 28
	// ID 29

	// ID 30
	// ID 31
	// ID 32
	// ID 33
	// ID 34
	public static final Block cloth = (new BlockCloth(35, 64)).setHardness(0.8F).setStepSound(soundClothFootstep);
	// ID 36
	public static final BlockFlower flowers = (BlockFlower)(new BlockFlower(37, 12)).setHardness(0.0F).setStepSound(soundGrassFootstep);
	public static final BlockFlower mushrooms = (BlockFlower)(new BlockMushroom(38, 29)).setHardness(0.0F).setStepSound(soundGrassFootstep);
	// ID 39
	// ID 40
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
	// ID 52
	// ID 53
	public static final Block chest = (new BlockChest(54)).setHardness(2.5F).setStepSound(soundWoodFootstep);
	public static final Block cog = (new BlockGears(55, 62)).setHardness(0.5F).setStepSound(soundMetalFootstep);
	public static final Block oreDiamond = (new BlockOreDiamond(56, 50)).setHardness(3.0F).setResistance(5.0F).setStepSound(soundStoneFootstep);
	public static final Block blockDiamond = (new BlockOreStorage(57, 40)).setHardness(5.0F).setResistance(10.0F).setStepSound(soundMetalFootstep);
	public static final Block workbench = (new BlockWorkbench(58)).setHardness(2.5F).setStepSound(soundWoodFootstep);
	public static final Block crops = (new BlockCrops(59, 88)).setHardness(0.0F).setStepSound(soundGrassFootstep);
	public static final Block tilledField = (new BlockFarmland(60)).setHardness(0.6F).setStepSound(soundGravelFootstep);
	public static final Block stoneOvenIdle = (new BlockFurnace(61, false)).setHardness(3.5F).setStepSound(soundStoneFootstep);
	public static final Block stoneOvenActive = (new BlockFurnace(62, true)).setHardness(3.5F).setStepSound(soundStoneFootstep).setLightValue(14.0F / 16.0F);

	// ID 63
	// ID 64
	// ID 65
	// ID 66
	// ID 67
	// ID 68
	// ID 69
	// ID 70
	// ID 71

	// ID 72
	// ID 73
	// ID 74
	// ID 75
	// ID 76
	// ID 77
	// ID 78
	// ID 79
	// ID 80
	// ID 81

	// ID 82
	// ID 83
	// ID 84
	// ID 85
	// ID 86
	// ID 87
	// ID 88
	// ID 89
	// ID 90
	// ID 91

	// ID 92
	// ID 93
	// ID 94
	// ID 95
	// ID 96
	// ID 97
	// ID 98
	// ID 99
	// ID 100
	// ID 101

	// ID 102
	// ID 103
	// ID 104
	// ID 105
	// ID 106
	// ID 107
	// ID 108
	// ID 109
	// ID 110
	// ID 111

	// ID 112
	// ID 113
	// ID 114
	// ID 115
	// ID 116
	// ID 117
	// ID 118
	// ID 119
	// ID 120
	// ID 121

	// ID 122
	// ID 123
	// ID 124
	// ID 125
	// ID 126
	// ID 127
	// ID 128
	// ID 129
	// ID 130
	// ID 131

	// ID 132
	// ID 133
	// ID 134
	// ID 135
	// ID 136
	// ID 137
	// ID 138
	// ID 139
	// ID 140
	// ID 141

	// ID 142
	// ID 143
	// ID 144
	// ID 145
	// ID 146
	// ID 147
	// ID 148
	// ID 149
	// ID 150
	// ID 151

	// ID 152
	// ID 153
	// ID 154
	// ID 155
	// ID 156
	// ID 157
	// ID 158
	// ID 159
	// ID 160
	// ID 161

	// ID 162
	// ID 163
	// ID 164
	// ID 165
	// ID 166
	// ID 167
	// ID 168
	// ID 169
	// ID 170
	// ID 171

	// ID 172
	// ID 173
	// ID 174
	// ID 175
	// ID 176
	// ID 177
	// ID 178
	// ID 179
	// ID 180
	// ID 181

	// ID 182
	// ID 183
	// ID 184
	// ID 185
	// ID 186
	// ID 187
	// ID 188
	// ID 189
	// ID 190
	// ID 191

	// ID 192
	// ID 193
	// ID 194
	// ID 195
	// ID 196
	// ID 197
	// ID 198
	// ID 199
	// ID 200
	// ID 201

	// ID 202
	// ID 203
	// ID 204
	// ID 205
	// ID 206
	// ID 207
	// ID 208
	// ID 209
	// ID 210
	// ID 211

	// ID 212
	// ID 213
	// ID 214
	// ID 215
	// ID 216
	// ID 217
	// ID 218
	// ID 219
	// ID 220
	// ID 221

	// ID 222
	// ID 223
	// ID 224
	// ID 225
	// ID 226
	// ID 227
	// ID 228
	// ID 229
	// ID 230
	// ID 231

	// ID 232
	// ID 233
	// ID 234
	// ID 235
	// ID 236
	// ID 237
	// ID 238
	// ID 239
	// ID 240
	// ID 241

	// ID 242
	// ID 243
	// ID 244
	// ID 245
	// ID 246
	// ID 247
	// ID 248
	// ID 249
	// ID 250
	// ID 251

	// ID 252
	// ID 253
	// ID 254
	// ID 255
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

	public int getLightValue(int metadata) {
		return lightValue[this.blockID];
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
	 * Builds the {@link ItemStack} that this block drops when broken. The default
	 * implementation calls {@link #idDropped} for the item id and {@link #damageDropped}
	 * for the damage value; both hooks are already overridable. Subclasses that need
	 * to attach extra data to the dropped stack (enchantments, display name, NBT) can
	 * override this method without touching the drop-chance or entity-spawning logic.
	 *
	 * @param metadata  the block's current metadata (growth stage, colour variant, etc.)
	 * @param rand      the shared world random, for blocks that randomise their drop
	 * @return the stack to drop; the caller discards it when the item id is non-positive
	 */
	public ItemStack itemStackDropped(int metadata, Random rand) {
		return new ItemStack(this.idDropped(metadata, rand), 1, this.damageDropped(metadata));
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

	/**
	 * Whether crops (wheat) may grow on this block. Distinct from
	 * {@link #canGrowPlants} because crops can only grow on tilled soil,
	 * not on generic plant bases like dirt or grass. Override to return
	 * {@code true} on farmland blocks.
	 *
	 * @param metadata  the block's current metadata
	 * @return {@code true} if crops can grow on this block
	 */
	public boolean canGrowCrops(int metadata) {
		return false;
	}

	/**
	 * Whether this block is on fire / made of burning material. The default is
	 * true for any block made of {@link net.minecraft.game.world.material.Material#lava}
	 * (covers both flowing and stationary lava); {@link BlockFire} overrides
	 * to return {@code true} for itself too. Used by entity AI
	 * ({@code EntityQueryService.isBoundingBoxBurning}) to test whether an
	 * entity is touching a burning block.
	 */
	public boolean isBurning() {
		return this.blockMaterial == net.minecraft.game.world.material.Material.lava;
	}

	/**
	 * Whether this block takes its light from the cell above it (and its four
	 * horizontal neighbours) instead of from itself. Single slabs and farmland
	 * are full-bright-look-through because the cell below them is too dark
	 * (a slab is half-height; farmland is a thin slice over dirt). Override
	 * to {@code true} on those blocks; default {@code false}.
	 *
	 * <p>Used by {@code World.getBlockLightValue_do} and
	 * {@code ChunkCache.getLightValueExt} — both of which previously listed
	 * {@link #stairSingle} and {@link #tilledField} by id.
	 */
	public boolean takesLightFromAbove() {
		return false;
	}

	/**
	 * The animal pathfinding bonus this block contributes when directly beneath an
	 * {@link EntityAnimal}. Used to bias mob spawning and wandering toward
	 * preferred ground. Default 0.0F; grass overrides to 10.0F.
	 *
	 * @return the path bonus value
	 */
	public float getAnimalPathBonus() {
		return 0.0F;
	}

	/**
	 * Whether this block is air-equivalent for the purposes of placing or
	 * dropping another block in its cell. A block is substitutable when the
	 * player meaningfully "puts a block there" — i.e. the cell can take a new
	 * block, and the existing one disappears without conflict.
	 *
	 * <p>The default is true for fire and for any block made of
	 * {@link net.minecraft.game.world.material.Material#water} or
	 * {@link net.minecraft.game.world.material.Material#lava}. Subclasses of
	 * {@link BlockFlower} (flowers, mushrooms, saplings, crops) override to
	 * return true so right-clicking a flower with a block places the new
	 * block <em>in the flower's cell</em> (replacing it) instead of next to
	 * it.
	 *
	 * <p>Used by:
	 * <ul>
	 *   <li>{@link net.minecraft.game.world.block.BlockSand#canFallBelow}
	 *       (sand falling through a cell occupied by a substitutable block);</li>
	 *   <li>{@code EntityFallingSand.canBlockBePlacedAt} (a sand entity
	 *       settling into a cell occupied by a substitutable block);</li>
	 *   <li>{@link net.minecraft.game.item.ItemBlock#onItemUse} (player
	 *       right-click placement when pointing at a substitutable block).</li>
	 * </ul>
	 *
	 * @return {@code true} if placing another block in this block's cell is safe
	 */
	public boolean canBeSubstituted() {
		return this == Block.fire
			|| this.blockMaterial == net.minecraft.game.world.material.Material.water
			|| this.blockMaterial == net.minecraft.game.world.material.Material.lava;
	}

	/**
	 * Called when this block is about to be replaced by another block via the
	 * substitution path: a player right-clicks it with a block item
	 * ({@link net.minecraft.game.item.ItemBlock#onItemUse}), a falling sand
	 * entity settles into its cell
	 * ({@link net.minecraft.game.entity.misc.EntityFallingSand#onUpdate}), or
	 * water flows over it
	 * ({@link BlockFlowing#flowIntoBlock}). Note: when lava flows, this
	 * method is <em>not</em> called; the lava fizz effect is the property of
	 * the lava, not the displaced block, and is handled in place.
	 *
	 * <p>The default is no-op: fire, water, and lava vanish silently when
	 * replaced. {@link BlockFlower} overrides to drop the corresponding
	 * item, so right-clicking a flower with a block item (or sand falling
	 * onto it, or water flowing over it) yields a flower drop.
	 *
	 * <p>The call site is responsible for reading {@code metadata} from
	 * the cell <em>before</em> the new block is written, since the
	 * substituted block's metadata is otherwise lost.
	 */
	public void onSubstituted(net.minecraft.game.world.World world, int x, int y, int z, int metadata) {
	}

	/**
	 * How readily this block encourages fire to spread to its neighbours
	 * (a 0-300 rating used by {@link BlockFire#updateTick}). Higher means
	 * fire spreads onto this block more eagerly. The default of 0 means
	 * the block is fireproof; burnable blocks (planks, wood, leaves, …)
	 * override this.
	 *
	 * @return the encouragement rating, 0-300
	 */
	public int getEncouragementToFire() {
		return 0;
	}

	/**
	 * How long this block continues to burn once it has caught fire (a
	 * 0-300 rating used by {@link BlockFire#tryToCatchBlockOnFire}).
	 * Higher means the block burns longer. The default of 0 means the
	 * block is non-flammable; burnable blocks override this.
	 *
	 * @return the burn-time rating, 0-300
	 */
	public int getAbilityToCatchFire() {
		return 0;
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
				ItemStack drop = this.itemStackDropped(metadata, world.rand);
				if(drop.itemID > 0) {
					double offsetX = (double)(world.rand.nextFloat() * 0.7F) + (double)0.15F;
					double offsetY = (double)(world.rand.nextFloat() * 0.7F) + (double)0.15F;
					double offsetZ = (double)(world.rand.nextFloat() * 0.7F) + (double)0.15F;
					EntityItem entityItem = new EntityItem(world, (double)x + offsetX, (double)y + offsetY, (double)z + offsetZ, drop);
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