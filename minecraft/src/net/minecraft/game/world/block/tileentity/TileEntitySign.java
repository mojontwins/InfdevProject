package net.minecraft.game.world.block.tileentity;

import com.mojang.nbt.NBTTagCompound;

/**
 * The tile entity behind a sign block. Stores four lines of text (each
 * limited to 15 characters on load) and the index of the line currently
 * being edited by the player (or -1 when no GUI is open).
 *
 * <p>Derived from the Minecraft Alpha 1.1.2 {@code TileEntitySign},
 * modernised to Java 8 and adapted to this codebase's naming conventions.
 */
public final class TileEntitySign extends TileEntity {
	public String[] signText = {"", "", "", ""};
	public int lineBeingEdited = -1;

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setString("Text1", this.signText[0]);
		tag.setString("Text2", this.signText[1]);
		tag.setString("Text3", this.signText[2]);
		tag.setString("Text4", this.signText[3]);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		for(int i = 0; i < 4; ++i) {
			this.signText[i] = tag.getString("Text" + (i + 1));
			if(this.signText[i].length() > 15) {
				this.signText[i] = this.signText[i].substring(0, 15);
			}
		}
	}

	public void onInventoryChanged() {
		// Sign text is stored in NBT and the GUI syncs on close
	}
}
