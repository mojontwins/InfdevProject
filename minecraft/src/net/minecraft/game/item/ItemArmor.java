package net.minecraft.game.item;

/**
 * A piece of wearable armour. {@code armorType} tells which slot it belongs to
 * (0 = helmet, 1 = chestplate, 2 = leggings, 3 = boots) and drives three things:
 * how much damage it reduces (see {@link #damageReduceAmount} and
 * {@link net.minecraft.game.entity.player.InventoryPlayer#getPlayerArmorValue()}),
 * how much durability it gets, and — through {@link #renderIndex} — which armour
 * texture layer the player renderer uses.
 */
public final class ItemArmor extends Item {
	private static final int[] damageReduceAmountArray = new int[]{3, 8, 6, 3};
	private static final int[] maxDamageArray = new int[]{11, 16, 15, 13};
	public final int armorType;
	public final int damageReduceAmount;
	public final int renderIndex;

	/**
	 * @param materialTier 0 = leather, 1 = chainmail/iron, 2 = steel, 3 = diamond.
	 * Gold reuses tier 1 durability but renders with its own layer (renderIndex 4).
	 */
	public ItemArmor(int itemID, int materialTier, int renderIndex, int armorType) {
		super(itemID);
		this.armorType = armorType;
		this.renderIndex = renderIndex;
		this.damageReduceAmount = damageReduceAmountArray[armorType];
		this.maxDamage = maxDamageArray[armorType] * 3 << materialTier;
		this.maxStackSize = 1;
	}
}