package net.minecraft.client.controller;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.EntityPlayerSP;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.game.item.Item;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.block.StepSound;

public final class PlayerControllerSP extends PlayerController {
	// Single-player controller: adds block-breaking progress, damage repair.
	private int curBlockX = -1;
	private int curBlockY = -1;
	private int curBlockZ = -1;
	private float curBlockDamage = 0.0F;
	private float prevBlockDamage = 0.0F;
	private float blockDestroySoundCounter = 0.0F;
	private int blockHitWait = 0;

	public PlayerControllerSP(Minecraft mc) {
		super(mc);
	}

	public final boolean sendBlockRemoved(int x, int y, int z) {
		int blockId = this.mc.theWorld.getBlockId(x, y, z);
		int metadata = this.mc.theWorld.getBlockMetadata(x, y, z);
		boolean removed = super.sendBlockRemoved(x, y, z);
		EntityPlayerSP player = this.mc.thePlayer;
		ItemStack currentItem = player.inventory.getCurrentItem();
		// Damaging the held item on each broken block.
		if(currentItem != null) {
			Item.itemsList[currentItem.itemID].onBlockDestroyed(currentItem);
			if(currentItem.stackSize == 0) {
				this.mc.thePlayer.displayInventoryGUI();
			}
		}

		// Drop harvested drops only if the player can actually harvest this block type.
		if(removed && this.mc.thePlayer.canHarvestBlock(Block.blocksList[blockId])) {
			Block.blocksList[blockId].dropBlockAsItem(this.mc.theWorld, x, y, z, metadata);
		}

		return removed;
	}

	// Creative-instant break: blocks that are brittle enough are broken in one click.
	public final void clickBlock(int x, int y, int z) {
		int blockId = this.mc.theWorld.getBlockId(x, y, z);
		if(blockId > 0 && Block.blocksList[blockId].blockStrength(this.mc.thePlayer, this.mc.theWorld.getBlockMetadata(x, y, z)) >= 1.0F) {
			this.sendBlockRemoved(x, y, z);
		}

	}

	// Cancel any in-progress block damage.
	public final void resetBlockRemoving() {
		this.curBlockDamage = 0.0F;
		this.blockHitWait = 0;
	}

	public final void sendBlockRemoving(int x, int y, int z, int side) {
		if(this.blockHitWait > 0) {
			--this.blockHitWait;
		} else {
			super.sendBlockRemoving(x, y, z, side);
			if(x == this.curBlockX && y == this.curBlockY && z == this.curBlockZ) {
				int blockId = this.mc.theWorld.getBlockId(x, y, z);
				if(blockId != 0) {
					Block block = Block.blocksList[blockId];
					this.curBlockDamage += block.blockStrength(this.mc.thePlayer, this.mc.theWorld.getBlockMetadata(x, y, z));
					// Play a periodic chip/dig sound while destroying the block.
					if(this.blockDestroySoundCounter % 4.0F == 0.0F && block != null) {
						SoundManager sndManager = this.mc.sndManager;
						String digSound = block.stepSound.getStepSound();
						float soundX = (float)x + 0.5F;
						float soundY = (float)y + 0.5F;
						float soundZ = (float)z + 0.5F;
						StepSound stepSound = block.stepSound;
						float volume = (stepSound.stepSoundVolume + 1.0F) / 8.0F;
						stepSound = block.stepSound;
						sndManager.playSound(digSound, soundX, soundY, soundZ, volume, stepSound.stepSoundPitch * 0.5F);
					}

					++this.blockDestroySoundCounter;
					// Block fully destroyed: remove it and pause briefly before the next break.
					if(this.curBlockDamage >= 1.0F) {
						this.sendBlockRemoved(x, y, z);
						this.curBlockDamage = 0.0F;
						this.prevBlockDamage = 0.0F;
						this.blockDestroySoundCounter = 0.0F;
						this.blockHitWait = 5;
					}

				}
			} else {
				// Switched to a different block: reset damage progress and track the new target.
				this.curBlockDamage = 0.0F;
				this.prevBlockDamage = 0.0F;
				this.blockDestroySoundCounter = 0.0F;
				this.curBlockX = x;
				this.curBlockY = y;
				this.curBlockZ = z;
			}
		}
	}

	public final void setPartialTime(float partialTick) {
		if(this.curBlockDamage <= 0.0F) {
			this.mc.renderGlobal.damagePartialTime = 0.0F;
		} else {
			// Interpolate the crack overlay the surviving block between last frame and now.
			partialTick = this.prevBlockDamage + (this.curBlockDamage - this.prevBlockDamage) * partialTick;
			this.mc.renderGlobal.damagePartialTime = partialTick;
		}
	}

	public final float getBlockReachDistance() {
		return 4.0F;
	}

	public final void onUpdate() {
		this.prevBlockDamage = this.curBlockDamage;
	}
}
