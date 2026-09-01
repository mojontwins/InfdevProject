package net.minecraft.game.entity.animal;

import com.mojang.nbt.NBTTagCompound;
import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.item.Item;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.world.World;

/**
 * The cow: a passive, four-legged animal that drops leather when killed.
 * Bucket milking ({@link #interact}) is not yet wired — there is no caller
 * in the current codebase; the method is a stub waiting for a right-click
 * interaction path.
 */
public class EntityCow extends EntityAnimal {

	public EntityCow(World world) {
		super(world);
		this.texture = "/mob/cow.png";
		this.setSize(0.9F, 1.3F);
	}

	public final void writeEntityToNBT(NBTTagCompound compound) {
		super.writeEntityToNBT(compound);
	}

	public final void readEntityFromNBT(NBTTagCompound compound) {
		super.readEntityFromNBT(compound);
	}

	protected final String getLivingSound() {
		return "mob.cow";
	}

	protected final String getHurtSound() {
		return "mob.cowhurt";
	}

	protected final String getDeathSound() {
		return "mob.cowhurt";
	}

	protected final int getDroppedItem() {
		return Item.leather.shiftedIndex;
	}

	public final boolean interact(EntityPlayer player) {
		ItemStack held = player.inventory.getCurrentItem();
		if (held != null && held.itemID == Item.bucketEmpty.shiftedIndex) {
			player.inventory.setInventorySlotContents(player.inventory.currentItem, new ItemStack(Item.bucketMilk));
			return true;
		}
		return false;
	}
}
