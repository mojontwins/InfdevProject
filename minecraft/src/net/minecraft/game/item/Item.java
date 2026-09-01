package net.minecraft.game.item;

import java.util.Random;
import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;

/**
 * The base class of every stackable, usable thing that is not a terrain block,
 * and the central item registry: {@link #itemsList} maps a numeric id to the
 * item instance, exactly like {@link Block#blocksList} does for blocks.
 *
 * <p>Item ids are "shifted" by 256: a block's {@code ItemBlock} lives at the
 * slot matching its block id (0..255), while every custom item (apple, bow,
 * sword, ...) registers at {@code 256 + registerOrder}. Registers happen in the
 * static initialiser below, in release order — the exact sequence is part of the
 * save format and must never change.
 */
public class Item {
	/** Shared randomness for sound pitch jitter and similar cosmetic effects. */
	protected static Random itemRand = new Random();
	/** Registry of every item, indexed by the item's shifted id. */
	public static Item[] itemsList = new Item[1024];

	// --- the item catalogue (in register order; see static initialiser) -----
	public static Item shovel;
	public static Item pickaxeSteel;
	public static Item axeSteel;
	public static Item flintAndSteel;
	public static Item apple;
	public static Item bow;
	public static Item arrow;
	public static Item coal;
	/** The diamond gem — the historic "diamod" misspelling is part of the public API. */
	public static Item diamod;
	public static Item ingotIron;
	public static Item ingotGold;
	public static Item swordSteel;
	public static Item swordWood;
	public static Item shovelWood;
	public static Item pickaxeWood;
	public static Item axeWood;
	public static Item swordStone;
	public static Item shovelStone;
	public static Item pickaxeStone;
	public static Item axeStone;
	public static Item swordDiamond;
	public static Item shovelDiamond;
	public static Item pickaxeDiamond;
	public static Item axeDiamond;
	public static Item stick;
	public static Item bowlEmpty;
	public static Item bowlSoup;
	public static Item swordGold;
	public static Item shovelGold;
	public static Item pickaxeGold;
	public static Item axeGold;
	public static Item silk;
	public static Item feather;
	public static Item gunpowder;
	public static Item hoeWood;
	public static Item hoeStone;
	public static Item hoeSteel;
	public static Item hoeDiamond;
	public static Item hoeGold;
	public static Item seeds;
	public static Item wheat;
	public static Item bread;
	public static Item helmetLeather;
	public static Item plateLeather;
	public static Item legsLeather;
	public static Item bootsLeather;
	public static Item helmetChain;
	public static Item plateChain;
	public static Item legsChain;
	public static Item bootsChain;
	public static Item helmetSteel;
	public static Item plateSteel;
	public static Item legsSteel;
	public static Item bootsSteel;
	public static Item helmetDiamond;
	public static Item plateDiamond;
	public static Item legsDiamond;
	public static Item bootsDiamond;
	public static Item helmetGold;
	public static Item plateGold;
	public static Item legsGold;
	public static Item bootsGold;
	public static Item flint;
	public static Item porkRaw;
	public static Item porkCooked;
	public static Item painting;
	public static Item appleGold;
	public static Item leather;
	public static Item bucketEmpty;
	public static Item bucketMilk;

	/** The item's id in {@link #itemsList} (= the register order plus the 256 shift). */
	public final int shiftedIndex;
	protected int maxStackSize = 64;
	protected int maxDamage = 32;
	private int iconIndex;

	protected Item(int itemID) {
		this.shiftedIndex = itemID + 256;
		if (itemsList[itemID + 256] != null) {
			System.out.println("CONFLICT @ " + itemID);
		}

		itemsList[itemID + 256] = this;
	}

	/** Picks which sprite of the items atlas this item renders with, and returns the item for chaining. */
	public final Item setIconIndex(int iconIndex) {
		this.iconIndex = iconIndex;
		return this;
	}

	public final int getIconFromDamage() {
		return this.iconIndex;
	}

	/**
	 * Called when the item is used on a block face. The narrower signature is
	 * the historically available one; subclasses override this, while the
	 * expanded overload in {@link #onItemUse(ItemStack, World, int, int, int, int, float, float, float)}
	 * adds the exact click position inside the face for items that care.
	 */
	public boolean onItemUse(ItemStack stack, World world, int x, int y, int z, int side) {
		return false;
	}

	public boolean onItemUse(ItemStack stack, World world, int x, int y, int z, int side, float xWithinFace, float yWithinFace, float zWithinFace) {
		return this.onItemUse(stack, world, x, y, z, side);
	}

	/** How quickly this item digs the given block (material-dependent; 1.0 = bare hand speed). */
	public float getStrVsBlock(Block block) {
		return 1.0F;
	}

	/**
	 * How quickly this item digs the given block knowing the *exact block state*
	 * in the world ({@code metadata}). Callers that only have a block instance
	 * and no position use the one-argument form, which resolves to a match that
	 * ignores metadata.
	 */
	public float getStrVsBlock(Block block, int metadata) {
		return this.getStrVsBlock(block);
	}

	/** Called on a right click that does not hit a block; lets the item act (eat, shoot, ...). */
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		return stack;
	}

	public final int getItemStackLimit() {
		return this.maxStackSize;
	}

	public final int getMaxDamage() {
		return this.maxDamage;
	}

	/** Damage dealt to this item's durability when it is used to hit a creature. */
	public void hitEntity(ItemStack stack) {
	}

	/** Durability cost when the item breaks a block. */
	public void onBlockDestroyed(ItemStack stack) {
	}

	public int getDamageVsEntity() {
		return 1;
	}

	public boolean canHarvestBlock(Block block) {
		return false;
	}

	/**
	 * Constructs the item, applies its atlas sprite and registers it into the
	 * static fields above. Keeps the original register order, which is what item
	 * ids in save files are derived from.
	 */
	private static Item register(Item item, int iconIndex) {
		item.setIconIndex(iconIndex);
		return item;
	}

	static {
		shovel = register(new ItemSpade(0, 2), 82);
		pickaxeSteel = register(new ItemPickaxe(1, 2), 98);
		axeSteel = register(new ItemAxe(2, 2), 114);
		flintAndSteel = register(new ItemFlintAndSteel(3), 5);
		apple = register(new ItemFood(4, 4), 10);
		bow = register(new ItemBow(5), 21);
		arrow = register(new Item(6), 37);
		coal = register(new Item(7), 7);
		diamod = register(new Item(8), 55);
		ingotIron = register(new Item(9), 23);
		ingotGold = register(new Item(10), 39);
		swordSteel = register(new ItemSword(11, 2), 66);
		swordWood = register(new ItemSword(12, 0), 64);
		shovelWood = register(new ItemSpade(13, 0), 80);
		pickaxeWood = register(new ItemPickaxe(14, 0), 96);
		axeWood = register(new ItemAxe(15, 0), 112);
		swordStone = register(new ItemSword(16, 1), 65);
		shovelStone = register(new ItemSpade(17, 1), 81);
		pickaxeStone = register(new ItemPickaxe(18, 1), 97);
		axeStone = register(new ItemAxe(19, 1), 113);
		swordDiamond = register(new ItemSword(20, 3), 67);
		shovelDiamond = register(new ItemSpade(21, 3), 83);
		pickaxeDiamond = register(new ItemPickaxe(22, 3), 99);
		axeDiamond = register(new ItemAxe(23, 3), 115);
		stick = register(new Item(24), 53);
		bowlEmpty = register(new Item(25), 71);
		bowlSoup = register(new ItemSoup(26, 10), 72);
		swordGold = register(new ItemSword(27, 0), 68);
		shovelGold = register(new ItemSpade(28, 0), 84);
		pickaxeGold = register(new ItemPickaxe(29, 0), 100);
		axeGold = register(new ItemAxe(30, 0), 116);
		silk = register(new Item(31), 8);
		feather = register(new Item(32), 24);
		gunpowder = register(new Item(33), 40);
		hoeWood = register(new ItemHoe(34, 0), 128);
		hoeStone = register(new ItemHoe(35, 1), 129);
		hoeSteel = register(new ItemHoe(36, 2), 130);
		hoeDiamond = register(new ItemHoe(37, 3), 131);
		hoeGold = register(new ItemHoe(38, 4), 132);
		seeds = register(new ItemSeeds(39, Block.crops.blockID), 9);
		wheat = register(new Item(40), 25);
		bread = register(new ItemFood(41, 5), 41);
		helmetLeather = register(new ItemArmor(42, 0, 0, 0), 0);
		plateLeather = register(new ItemArmor(43, 0, 0, 1), 16);
		legsLeather = register(new ItemArmor(44, 0, 0, 2), 32);
		bootsLeather = register(new ItemArmor(45, 0, 0, 3), 48);
		helmetChain = register(new ItemArmor(46, 1, 1, 0), 1);
		plateChain = register(new ItemArmor(47, 1, 1, 1), 17);
		legsChain = register(new ItemArmor(48, 1, 1, 2), 33);
		bootsChain = register(new ItemArmor(49, 1, 1, 3), 49);
		helmetSteel = register(new ItemArmor(50, 2, 2, 0), 2);
		plateSteel = register(new ItemArmor(51, 2, 2, 1), 18);
		legsSteel = register(new ItemArmor(52, 2, 2, 2), 34);
		bootsSteel = register(new ItemArmor(53, 2, 2, 3), 50);
		helmetDiamond = register(new ItemArmor(54, 3, 3, 0), 3);
		plateDiamond = register(new ItemArmor(55, 3, 3, 1), 19);
		legsDiamond = register(new ItemArmor(56, 3, 3, 2), 35);
		bootsDiamond = register(new ItemArmor(57, 3, 3, 3), 51);
		helmetGold = register(new ItemArmor(58, 1, 4, 0), 4);
		plateGold = register(new ItemArmor(59, 1, 4, 1), 20);
		legsGold = register(new ItemArmor(60, 1, 4, 2), 36);
		bootsGold = register(new ItemArmor(61, 1, 4, 3), 52);
		flint = register(new Item(62), 6);
		porkRaw = register(new ItemFood(63, 3), 87);
		porkCooked = register(new ItemFood(64, 8), 88);
		painting = register(new ItemPainting(65), 26);
		appleGold = register(new ItemFood(66, 42), 11);
		leather = register(new Item(67), 112);
		bucketEmpty = register(new Item(68), 117);
		bucketMilk = register(new Item(69), 122);
	}

	/**
	 * Returns the coordinates of the cell reached by stepping through the given
	 * face of a block, i.e. the neighbour the player "missed". Side codes are the
	 * standard block faces: 0 = -Y, 1 = +Y, 2 = -Z, 3 = +Z, 4 = -X, 5 = +X.
	 */
	protected static int[] neighbourAcrossFace(int side, int x, int y, int z) {
		switch (side) {
			case 0:
				return new int[]{x, y - 1, z};
			case 1:
				return new int[]{x, y + 1, z};
			case 2:
				return new int[]{x, y, z - 1};
			case 3:
				return new int[]{x, y, z + 1};
			case 4:
				return new int[]{x - 1, y, z};
			default:
				return new int[]{x + 1, y, z};
		}
	}
}