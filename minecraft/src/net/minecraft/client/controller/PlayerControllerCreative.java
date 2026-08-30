package net.minecraft.client.controller;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Session;
import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.item.ItemStack;

public final class PlayerControllerCreative extends PlayerController {
	// Creative-mode controller: gives the player an instant, infinite inventory and disables the HUD.
	public PlayerControllerCreative(Minecraft mc) {
		super(mc);
		this.isInTestMode = true;
	}

	// On respawn, refill the hotbar: give one of each creative block where empty, reset stack sizes otherwise.
	public final void onRespawn(EntityPlayer player) {
		for(int slot = 0; slot < 9; ++slot) {
			if(player.inventory.mainInventory[slot] == null) {
				this.mc.thePlayer.inventory.mainInventory[slot] = new ItemStack(Session.registeredBlocksList.get(slot).blockID);
			} else {
				this.mc.thePlayer.inventory.mainInventory[slot].stackSize = 1;
			}
		}

	}

	public final boolean shouldDrawHUD() {
		return false;
	}

	public final void onUpdate() {
	}
}
